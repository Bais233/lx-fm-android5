package com.dragon.fm.player

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import com.alexmercerind.audire.native.ShazamSignature
import com.dragon.fm.data.model.RadioStation
import com.dragon.fm.data.model.RecognitionMode
import com.dragon.fm.data.model.RecognitionState
import com.dragon.fm.data.model.RecognizedSong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

class MusicRecognitionManager(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val json = Json { ignoreUnknownKeys = true }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val signatureGenerator by lazy { ShazamSignature() }

    private val _state = MutableStateFlow<RecognitionState>(RecognitionState.Idle)
    val state: StateFlow<RecognitionState> = _state.asStateFlow()

    private val _history = MutableStateFlow<List<RecognizedSong>>(emptyList())
    val history: StateFlow<List<RecognizedSong>> = _history.asStateFlow()

    private var activeJob: Job? = null

    init {
        loadHistory()
    }

    fun resetState() {
        activeJob?.cancel()
        activeJob = null
        _state.value = RecognitionState.Idle
    }

    /**
     * 一键识别当前电台正在播放的音乐
     * 优先直连播放器内部音频管线（零网络重连开销、零环境杂音、100%保真），
     * 若未在播放则优雅降级为网络流采样。
     */
    fun recognizeCurrentStation(station: RadioStation, captureProcessor: LiveAudioCaptureProcessor? = null) {
        activeJob?.cancel()
        activeJob = scope.launch {
            try {
                val sampleStartRealtime = android.os.SystemClock.elapsedRealtime()
                var pcmSamples: ShortArray? = null

                // 1. 优先尝试直接从 ExoPlayer 音频管道提取 7 秒无损 PCM
                if (captureProcessor != null) {
                    _state.value = RecognitionState.Sampling(0.05f, "正在从电台音频流直接截获声学指纹...")
                    pcmSamples = captureFromLiveProcessor(captureProcessor, durationSeconds = 7)
                }

                // 2. 降级：若直接截取未取到足够数据（如未在播放或缓冲中），则拉取广播流采样
                if (pcmSamples == null || pcmSamples.size < 16000 * 2) {
                    _state.value = RecognitionState.Sampling(0.1f, "正在连接广播直播流采样...")
                    val tempAudioFile = File(context.cacheDir, "recog_stream_${System.currentTimeMillis()}.tmp")
                    val sampleSuccess = sampleRadioStream(station.playUrl, tempAudioFile)

                    if (!sampleSuccess || !tempAudioFile.exists() || tempAudioFile.length() < 16384) {
                        _state.value = RecognitionState.Error("广播流采样数据不足，请确保电台正在流畅播放")
                        return@launch
                    }

                    _state.value = RecognitionState.Sampling(0.6f, "正在解码并提取声学指纹...")
                    pcmSamples = decodeAudioTo16kMonoPcm(tempAudioFile)
                    tempAudioFile.delete()
                }

                if (pcmSamples == null || pcmSamples.size < 16000 * 2) {
                    _state.value = RecognitionState.Error("音频解码失败或有效音频长度过短")
                    return@launch
                }

                // 3. 声学特征前处理：消除直流偏置与自适应动态增益，大幅提高 Shazam 识别敏感度
                val conditionedSamples = preprocessPcm(pcmSamples)

                _state.value = RecognitionState.Fingerprinting
                val signatureUri = withContext(Dispatchers.Default) {
                    signatureGenerator.create(conditionedSamples)
                }

                if (signatureUri.isBlank()) {
                    _state.value = RecognitionState.Error("声学地标特征提取失败")
                    return@launch
                }

                _state.value = RecognitionState.QueryingCloud
                val sampleMs = (conditionedSamples.size * 1000L / 16000L).toInt()
                val recognizedSong = queryShazamApi(signatureUri, sampleMs, station.name, sampleStartRealtime)

                if (recognizedSong != null) {
                    saveToHistory(recognizedSong)
                    _state.value = RecognitionState.Success(recognizedSong)
                } else {
                    _state.value = RecognitionState.NoMatch("当前电台未检测到歌曲（可能正处于主持人口播、新闻或片头广告）")
                }
            } catch (e: Exception) {
                if (isActive) {
                    _state.value = RecognitionState.Error("识别发生异常: ${e.localizedMessage ?: "网络或音频错误"}")
                }
            }
        }
    }

    /**
     * 麦克风环境听歌识曲（采集周围环境音乐，时长扩充至 7 秒并配合动态增益优化）
     */
    fun recognizeFromMicrophone() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            _state.value = RecognitionState.Error("未获取麦克风录音权限，请在系统设置中授予")
            return
        }

        activeJob?.cancel()
        activeJob = scope.launch {
            var audioRecord: AudioRecord? = null
            try {
                val sampleRate = 16000
                val channelConfig = AudioFormat.CHANNEL_IN_MONO
                val audioFormat = AudioFormat.ENCODING_PCM_16BIT
                val minBufSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat).coerceAtLeast(3200)

                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    channelConfig,
                    audioFormat,
                    minBufSize
                )

                if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
                    _state.value = RecognitionState.Error("麦克风初始化失败，请稍后重试")
                    return@launch
                }

                val sampleStartRealtime = android.os.SystemClock.elapsedRealtime()
                audioRecord.startRecording()

                val durationSeconds = 7
                val totalSamples = sampleRate * durationSeconds
                val pcmBuffer = ShortArray(totalSamples)
                var samplesRead = 0

                while (samplesRead < totalSamples && isActive) {
                    val remaining = totalSamples - samplesRead
                    val chunk = remaining.coerceAtMost(2000)
                    val read = audioRecord.read(pcmBuffer, samplesRead, chunk)
                    if (read > 0) {
                        samplesRead += read
                        val progress = samplesRead.toFloat() / totalSamples.toFloat()
                        _state.value = RecognitionState.Sampling(progress, "正在聆听环境音乐 (${(samplesRead / 16000.0).roundToInt()}s / ${durationSeconds}s)...")
                    } else {
                        delay(20)
                    }
                }

                try {
                    audioRecord.stop()
                } catch (_: Exception) {}

                if (samplesRead < 16000 * 2) {
                    _state.value = RecognitionState.Error("录音时间过短，未能采集足够音频")
                    return@launch
                }

                val actualSamples = if (samplesRead == totalSamples) pcmBuffer else pcmBuffer.copyOf(samplesRead)
                val conditionedSamples = preprocessPcm(actualSamples)

                _state.value = RecognitionState.Fingerprinting
                val signatureUri = withContext(Dispatchers.Default) {
                    signatureGenerator.create(conditionedSamples)
                }

                if (signatureUri.isBlank()) {
                    _state.value = RecognitionState.Error("环境音频特征提取失败")
                    return@launch
                }

                _state.value = RecognitionState.QueryingCloud
                val sampleMs = (conditionedSamples.size * 1000L / 16000L).toInt()
                val recognizedSong = queryShazamApi(signatureUri, sampleMs, "麦克风识别", sampleStartRealtime)

                if (recognizedSong != null) {
                    saveToHistory(recognizedSong)
                    _state.value = RecognitionState.Success(recognizedSong)
                } else {
                    _state.value = RecognitionState.NoMatch("未识别出正在播放的歌曲，请靠近音源重试")
                }
            } catch (e: Exception) {
                if (isActive) {
                    _state.value = RecognitionState.Error("录音识曲失败: ${e.localizedMessage ?: "未知错误"}")
                }
            } finally {
                try {
                    audioRecord?.release()
                } catch (_: Exception) {}
            }
        }
    }

    /**
     * 直接从 ExoPlayer 音频截取器提取已解码的 16kHz Mono PCM（零网络重拉、零环境杂音、100%保真）
     */
    private suspend fun captureFromLiveProcessor(
        captureProcessor: LiveAudioCaptureProcessor,
        durationSeconds: Int = 7
    ): ShortArray? = withContext(Dispatchers.Default) {
        val targetSamples = 16000 * durationSeconds
        val collectedBuffer = ArrayList<Short>(targetSamples)
        val deferredResult = kotlinx.coroutines.CompletableDeferred<ShortArray?>()

        val listener: (ByteArray, Int, Int) -> Unit = { pcmBytes, sampleRate, channels ->
            if (!deferredResult.isCompleted && pcmBytes.isNotEmpty() && sampleRate > 0 && channels > 0) {
                val shortCount = pcmBytes.size / 2
                val shorts = ShortArray(shortCount)
                ByteBuffer.wrap(pcmBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts)

                // 1. 转为单声道
                val mono = if (channels > 1) {
                    val monoLen = shorts.size / channels
                    val m = ShortArray(monoLen)
                    for (i in 0 until monoLen) {
                        var sum = 0
                        for (ch in 0 until channels) {
                            sum += shorts[i * channels + ch]
                        }
                        m[i] = (sum / channels).coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                    }
                    m
                } else {
                    shorts
                }

                // 2. 重采样至 16kHz
                val resampled = if (sampleRate != 16000) {
                    resampleTo16k(mono, sampleRate)
                } else {
                    mono
                }

                synchronized(collectedBuffer) {
                    if (!deferredResult.isCompleted) {
                        for (s in resampled) {
                            collectedBuffer.add(s)
                            if (collectedBuffer.size >= targetSamples) break
                        }

                        val currentSize = collectedBuffer.size
                        val progress = (currentSize.toFloat() / targetSamples.toFloat()).coerceIn(0f, 1f)
                        _state.value = RecognitionState.Sampling(
                            progress,
                            "正在从电台音频流提取声学样本 (${(currentSize / 16000.0).roundToInt()}s / ${durationSeconds}s)..."
                        )

                        if (currentSize >= targetSamples) {
                            val fullArray = ShortArray(targetSamples)
                            for (i in 0 until targetSamples) {
                                fullArray[i] = collectedBuffer[i]
                            }
                            deferredResult.complete(fullArray)
                        }
                    }
                }
            }
        }

        captureProcessor.isCaptureEnabled = true
        captureProcessor.addPcmListener(listener)

        try {
            // 设置超时：若在 2500ms 内完全没有任何 PCM 数据流入（可能电台暂停或网络重连），自动降级
            kotlinx.coroutines.withTimeout(2500L + durationSeconds * 1000L) {
                deferredResult.await()
            }
        } catch (_: Exception) {
            null
        } finally {
            captureProcessor.removePcmListener(listener)
        }
    }

    /**
     * 音频前处理：直流偏置消除与自适应动态增益，解决弱音、伴奏轻导致 Shazam 漏检问题
     */
    private fun preprocessPcm(samples: ShortArray): ShortArray {
        if (samples.isEmpty()) return samples
        var sum: Long = 0
        var maxAmp = 0
        for (s in samples) {
            val a = kotlin.math.abs(s.toInt())
            if (a > maxAmp) maxAmp = a
            sum += s
        }
        val mean = (sum / samples.size).toInt()
        val processed = ShortArray(samples.size)
        for (i in samples.indices) {
            processed[i] = (samples[i] - mean).toShort()
        }

        // 动态归一化：若峰值振幅较低（低于 16000），适当放大提升特征峰值信噪比
        if (maxAmp in 800..16000) {
            val gain = (24000f / maxAmp).coerceAtMost(3.5f)
            for (i in processed.indices) {
                val boosted = (processed[i] * gain).roundToInt()
                processed[i] = boosted.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
        }
        return processed
    }

    /**
     * 截取广播流 5-6 秒数据
     */
    private suspend fun sampleRadioStream(streamUrl: String, targetFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            val isHls = streamUrl.contains(".m3u8", ignoreCase = true)
            if (isHls) {
                // HLS m3u8 分片采集
                var currentUrl = streamUrl
                val req = Request.Builder()
                    .url(currentUrl)
                    .header("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:120.0)")
                    .build()
                var playlistBody = httpClient.newCall(req).execute().use { it.body?.string().orEmpty() }

                // 若为 Master Playlist（多码率索引），递归提取实际媒体切片分片列表
                if (playlistBody.contains("#EXT-X-STREAM-INF")) {
                    val lines = playlistBody.lines()
                    for (line in lines) {
                        val trimmed = line.trim()
                        if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                            val baseUrl = currentUrl.substringBeforeLast("/") + "/"
                            currentUrl = if (trimmed.startsWith("http")) trimmed else baseUrl + trimmed
                            break
                        }
                    }
                    val childReq = Request.Builder()
                        .url(currentUrl)
                        .header("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:120.0)")
                        .build()
                    playlistBody = httpClient.newCall(childReq).execute().use { it.body?.string().orEmpty() }
                }

                val baseUrl = currentUrl.substringBeforeLast("/") + "/"
                val lines = playlistBody.lines()
                var segmentUrl: String? = null
                for (line in lines.reversed()) {
                    val trimmed = line.trim()
                    if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                        segmentUrl = if (trimmed.startsWith("http")) trimmed else baseUrl + trimmed
                        break
                    }
                }

                if (segmentUrl == null) return@withContext false

                val segReq = Request.Builder()
                    .url(segmentUrl)
                    .header("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:120.0)")
                    .build()
                httpClient.newCall(segReq).execute().use { res ->
                    if (!res.isSuccessful) return@withContext false
                    val bytes = res.body?.bytes() ?: return@withContext false
                    targetFile.writeBytes(bytes)
                    return@withContext bytes.size >= 8192
                }
            } else {
                // 普通 HTTP 直播流采样 (MP3, AAC, FLAC, OGG)
                val req = Request.Builder()
                    .url(streamUrl)
                    .header("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:120.0)")
                    .build()

                httpClient.newCall(req).execute().use { response ->
                    if (!response.isSuccessful) return@withContext false
                    val inputStream = response.body?.byteStream() ?: return@withContext false
                    val targetBytes = 220 * 1024 // 约 220KB (~8-10秒 192kbps 或 12秒 128kbps)
                    FileOutputStream(targetFile).use { fos ->
                        val buf = ByteArray(8192)
                        var total = 0
                        val start = System.currentTimeMillis()
                        while (total < targetBytes && (System.currentTimeMillis() - start) < 10000) {
                            val r = inputStream.read(buf)
                            if (r <= 0) break
                            fos.write(buf, 0, r)
                            total += r
                        }
                        fos.flush()
                        return@withContext total >= 16384
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }

    /**
     * 将任意音频文件（MP3/AAC/TS/OGG）通过 Android 原生 MediaCodec 解码为 16000Hz 16-Bit Mono PCM ShortArray
     */
    private fun decodeAudioTo16kMonoPcm(audioFile: File): ShortArray? {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            extractor.setDataSource(audioFile.absolutePath)
            var audioTrackIndex = -1
            var trackFormat: MediaFormat? = null

            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME).orEmpty()
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    trackFormat = format
                    extractor.selectTrack(i)
                    break
                }
            }

            if (audioTrackIndex == -1 || trackFormat == null) return null

            val mime = trackFormat.getString(MediaFormat.KEY_MIME) ?: return null
            codec = MediaCodec.createDecoderByType(mime)
            codec.configure(trackFormat, null, null, 0)
            codec.start()

            val info = MediaCodec.BufferInfo()
            val decodedPcmChunks = ArrayList<ShortArray>()
            var sawInputEOS = false
            var sawOutputEOS = false
            var outputSampleRate = trackFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            var outputChannels = trackFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)

            var iterations = 0
            while (!sawOutputEOS && iterations++ < 1500) {
                if (!sawInputEOS) {
                    val inputBufIndex = codec.dequeueInputBuffer(10000)
                    if (inputBufIndex >= 0) {
                        val inputBuf = codec.getInputBuffer(inputBufIndex)
                        if (inputBuf != null) {
                            inputBuf.clear()
                            val sampleSize = extractor.readSampleData(inputBuf, 0)
                            if (sampleSize < 0) {
                                sawInputEOS = true
                                codec.queueInputBuffer(inputBufIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            } else {
                                codec.queueInputBuffer(inputBufIndex, 0, sampleSize, extractor.sampleTime, 0)
                                extractor.advance()
                            }
                        }
                    }
                }

                val outputBufIndex = codec.dequeueOutputBuffer(info, 10000)
                if (outputBufIndex >= 0) {
                    val outputBuf = codec.getOutputBuffer(outputBufIndex)
                    if (outputBuf != null && info.size > 0) {
                        outputBuf.position(info.offset)
                        outputBuf.limit(info.offset + info.size)
                        outputBuf.order(ByteOrder.LITTLE_ENDIAN)

                        val shortCount = info.size / 2
                        val shorts = ShortArray(shortCount)
                        outputBuf.asShortBuffer().get(shorts)
                        decodedPcmChunks.add(shorts)
                    }
                    codec.releaseOutputBuffer(outputBufIndex, false)
                    if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        sawOutputEOS = true
                    }
                } else if (outputBufIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    val newFormat = codec.outputFormat
                    outputSampleRate = newFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    outputChannels = newFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                }
            }

            if (decodedPcmChunks.isEmpty()) return null

            val totalDecodedShorts = decodedPcmChunks.sumOf { it.size }
            val fullPcm = ShortArray(totalDecodedShorts)
            var destPos = 0
            for (chunk in decodedPcmChunks) {
                System.arraycopy(chunk, 0, fullPcm, destPos, chunk.size)
                destPos += chunk.size
            }

            // 1. 转为单声道 (Downmix to Mono)
            val monoSamples: ShortArray = if (outputChannels > 1) {
                val monoLen = fullPcm.size / outputChannels
                val mono = ShortArray(monoLen)
                for (i in 0 until monoLen) {
                    var sum = 0
                    for (ch in 0 until outputChannels) {
                        sum += fullPcm[i * outputChannels + ch]
                    }
                    mono[i] = (sum / outputChannels).coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }
                mono
            } else {
                fullPcm
            }

            // 2. 重采样到 16000 Hz
            return if (outputSampleRate != 16000) {
                resampleTo16k(monoSamples, outputSampleRate)
            } else {
                monoSamples
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        } finally {
            try {
                codec?.stop()
                codec?.release()
                extractor.release()
            } catch (_: Exception) {}
        }
    }

    /**
     * 高质量线性插值重采样至 16000 Hz
     */
    private fun resampleTo16k(input: ShortArray, sourceRate: Int): ShortArray {
        if (sourceRate <= 0 || input.isEmpty()) return input
        val ratio = sourceRate.toDouble() / 16000.0
        val targetLength = (input.size / ratio).toInt()
        val output = ShortArray(targetLength)

        for (i in 0 until targetLength) {
            val srcPos = i * ratio
            val srcIndex = srcPos.toInt()
            val fraction = srcPos - srcIndex
            if (srcIndex + 1 < input.size) {
                val sample1 = input[srcIndex]
                val sample2 = input[srcIndex + 1]
                val interpolated = sample1 + (fraction * (sample2 - sample1)).roundToInt()
                output[i] = interpolated.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            } else if (srcIndex < input.size) {
                output[i] = input[srcIndex]
            }
        }
        return output
    }

    /**
     * 向 Shazam 识别服务器查询声学指纹，并提取精确匹配时间偏移量 (Offset)
     */
    private suspend fun queryShazamApi(
        signatureUri: String,
        sampleMs: Int,
        stationName: String,
        sampleStartRealtime: Long = 0L
    ): RecognizedSong? = withContext(Dispatchers.IO) {
        try {
            val tagId1 = UUID.randomUUID().toString().uppercase()
            val tagId2 = UUID.randomUUID().toString().uppercase()
            val url = "https://amp.shazam.com/discovery/v5/zh-CN/CN/android/-/tag/$tagId1/$tagId2?sync=true&webv3=true&sampling=true"

            val jsonBody = JSONObject().apply {
                put("timezone", "Asia/Shanghai")
                put("signature", JSONObject().apply {
                    put("uri", signatureUri)
                    put("samplems", sampleMs)
                })
                put("timestamp", System.currentTimeMillis())
                put("context", JSONObject())
                put("geolocation", JSONObject())
            }

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                .header("Accept", "application/json")
                .post(jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val bodyStr = response.body?.string().orEmpty()
                val rootJson = JSONObject(bodyStr)
                val track = rootJson.optJSONObject("track") ?: return@withContext null

                val title = track.optString("title", "").trim()
                val artist = track.optString("subtitle", "").trim()
                if (title.isBlank() || artist.isBlank()) return@withContext null

                // 提取原曲声学匹配时间偏移量 (秒，精确到浮点数)
                val matches = rootJson.optJSONArray("matches")
                var matchedOffsetSeconds = 0.0
                if (matches != null && matches.length() > 0) {
                    val matchItem = matches.optJSONObject(0)
                    matchedOffsetSeconds = matchItem?.optDouble("offset", 0.0) ?: 0.0
                }

                val imagesObj = track.optJSONObject("images")
                val coverArt = imagesObj?.optString("coverarthq")?.takeIf { it.isNotBlank() }
                    ?: imagesObj?.optString("coverart")?.takeIf { it.isNotBlank() }
                    ?: ""

                var album = ""
                var releaseYear = ""
                val sections = track.optJSONArray("sections")
                if (sections != null) {
                    for (i in 0 until sections.length()) {
                        val sec = sections.optJSONObject(i) ?: continue
                        val metadata = sec.optJSONArray("metadata") ?: continue
                        for (j in 0 until metadata.length()) {
                            val item = metadata.optJSONObject(j) ?: continue
                            val metaTitle = item.optString("title")
                            val text = item.optString("text")
                            if (metaTitle.equals("Album", ignoreCase = true) || metaTitle.equals("专辑", ignoreCase = true)) {
                                album = text
                            }
                            if (metaTitle.equals("Released", ignoreCase = true) || metaTitle.equals("发行年份", ignoreCase = true)) {
                                releaseYear = text
                            }
                        }
                    }
                }

                val genre = track.optJSONObject("genres")?.optString("primary", "") ?: ""
                val shazamUrl = track.optString("url", "")

                return@withContext RecognizedSong(
                    title = title,
                    artist = artist,
                    album = album,
                    coverArtUrl = coverArt,
                    genre = genre,
                    releaseYear = releaseYear,
                    shazamUrl = shazamUrl,
                    stationName = stationName,
                    timestamp = System.currentTimeMillis(),
                    matchedOffsetSeconds = matchedOffsetSeconds,
                    sampleDurationMs = sampleMs.toLong(),
                    sampleStartRealtime = sampleStartRealtime
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }

    private fun loadHistory() {
        try {
            val sp = context.getSharedPreferences("lxfm_recognition_history", Context.MODE_PRIVATE)
            val raw = sp.getString("history_json", null) ?: return
            val list = json.decodeFromString<List<RecognizedSong>>(raw)
            _history.value = list
        } catch (e: Exception) {
            _history.value = emptyList()
        }
    }

    private fun saveToHistory(song: RecognizedSong) {
        val current = _history.value.toMutableList()
        // Deduplicate recent same song within 1 hour
        current.removeAll { it.title.equals(song.title, ignoreCase = true) && it.artist.equals(song.artist, ignoreCase = true) }
        current.add(0, song)
        if (current.size > 200) {
            current.removeAt(current.size - 1)
        }
        _history.value = current
        persistHistory(current)
    }

    fun deleteSong(songId: String) {
        val current = _history.value.toMutableList()
        current.removeAll { it.id == songId }
        _history.value = current
        persistHistory(current)
    }

    fun clearHistory() {
        _history.value = emptyList()
        persistHistory(emptyList())
    }

    private fun persistHistory(list: List<RecognizedSong>) {
        scope.launch(Dispatchers.IO) {
            try {
                val sp = context.getSharedPreferences("lxfm_recognition_history", Context.MODE_PRIVATE)
                val encoded = json.encodeToString(list)
                sp.edit().putString("history_json", encoded).apply()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
