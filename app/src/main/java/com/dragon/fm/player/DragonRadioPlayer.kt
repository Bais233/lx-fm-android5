package com.dragon.fm.player

import com.dragon.fm.DragonFmApp

import android.animation.ValueAnimator
import android.content.Context
import android.net.Uri
import android.os.SystemClock
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.dragon.fm.data.model.PlaybackStatus
import com.dragon.fm.data.model.PlayerState
import com.dragon.fm.data.model.RadioStation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class DragonRadioPlayer(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Main + Job())

    // High performance OkHttpClient with warm connection pool, resilient timeouts, and auto-redirect caching
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS) // Resilient against minor radio broadcast jitters
        .connectionPool(okhttp3.ConnectionPool(12, 60, TimeUnit.SECONDS))
        .retryOnConnectionFailure(true)
        .followRedirects(true)
        .followSslRedirects(true)
        .addInterceptor { chain ->
            val req = chain.request()
            val resp = chain.proceed(req)
            val originalUrl = req.url.toString()
            val finalUrl = resp.request.url.toString()
            if (finalUrl != originalUrl && resp.isSuccessful) {
                resolvedUrlCache[originalUrl] = finalUrl
                Log.d(TAG, "[RedirectInterceptor] Auto-cached redirect: $originalUrl -> $finalUrl")
            }
            resp
        }
        .build()

    private val httpDataSourceFactory = OkHttpDataSource.Factory(okHttpClient)
        .setUserAgent("LXFM/1.0.0 (Linux; Android; ExoPlayer)")
        .setDefaultRequestProperties(mapOf("Icy-MetaData" to "1"))

    // Optimized LoadControl for ultra-snappy live audio streaming
    private val loadControl = DefaultLoadControl.Builder()
        .setBufferDurationsMs(
            1500, // minBufferMs: stable buffer floor
            5000, // maxBufferMs: conservative max to avoid memory & bandwidth waste
            200,  // bufferForPlaybackMs: rapid 0.2s start
            800   // bufferForPlaybackAfterRebufferMs: robust rebuffer recovery
        )
        .setPrioritizeTimeOverSizeThresholds(true)
        .setBackBuffer(0, false) // 0 backbuffer saves memory on continuous radio
        .build()

    val liveAudioCaptureProcessor = LiveAudioCaptureProcessor()

    private val renderersFactory = object : DefaultRenderersFactory(context) {
        override fun buildAudioSink(
            context: Context,
            enableFloatOutput: Boolean,
            enableAudioTrackPlaybackParams: Boolean
        ): AudioSink {
            return DefaultAudioSink.Builder(context)
                .setEnableFloatOutput(false)
                .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                .setAudioProcessors(arrayOf(liveAudioCaptureProcessor))
                .build()
        }
    }

    val exoPlayer: ExoPlayer = ExoPlayer.Builder(context, renderersFactory)
        .setMediaSourceFactory(
            DefaultMediaSourceFactory(context)
                .setDataSourceFactory(httpDataSourceFactory)
        )
        .setLoadControl(loadControl)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .setUsage(C.USAGE_MEDIA)
                .build(),
            true
        )
        .setHandleAudioBecomingNoisy(true)
        .setWakeMode(C.WAKE_MODE_NETWORK)
        .build()

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    // Pre-resolved redirect URL cache
    private val resolvedUrlCache = ConcurrentHashMap<String, String>()

    private var currentSessionId = 0L
    private var retryCount = 0
    private var retryJob: Job? = null
    private var timeoutWatchdogJob: Job? = null
    private var warmUpJob: Job? = null
    private var volumeAnimator: ValueAnimator? = null
    private var activeStationId: String? = null
    private var playStartTimeMs: Long = 0L

    var onStationRefreshedListener: ((RadioStation) -> Unit)? = null

    init {
        setupPlayerListener()
    }

    private fun cancelVolumeAnimation() {
        volumeAnimator?.removeAllListeners()
        volumeAnimator?.cancel()
        volumeAnimator = null
    }

    private fun isEventStale(mediaItem: MediaItem? = exoPlayer.currentMediaItem): Boolean {
        val targetId = activeStationId ?: return true
        val eventId = mediaItem?.mediaId ?: return true
        return eventId != targetId
    }

    private fun setupPlayerListener() {
        exoPlayer.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                try {
                    DragonFmApp.instance.audioEffectManager.attachToAudioSession(audioSessionId)
                } catch (_: Exception) {}
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (isEventStale()) return
                when (playbackState) {
                    Player.STATE_BUFFERING -> {
                        if (exoPlayer.playWhenReady) {
                            _playerState.value = _playerState.value.copy(
                                status = PlaybackStatus.BUFFERING,
                                errorMessage = null
                            )
                            val current = _playerState.value.currentStation
                            if (current != null && timeoutWatchdogJob == null) {
                                startTimeoutWatchdog(currentSessionId, current, isMidStream = true)
                            }
                        }
                    }
                    Player.STATE_READY -> {
                        timeoutWatchdogJob?.cancel()
                        timeoutWatchdogJob = null
                        retryJob?.cancel()
                        retryJob = null
                        retryCount = 0

                        val isPlaying = exoPlayer.playWhenReady
                        val elapsed = SystemClock.elapsedRealtime() - playStartTimeMs
                        val currentStation = _playerState.value.currentStation
                        val currentStationName = currentStation?.name ?: "Unknown"
                        Log.i(TAG, "[FirstAudioReady] Station: $currentStationName, TTFB/FirstFrame: ${elapsed}ms")

                        currentStation?.id?.let {
                            com.dragon.fm.data.local.StationHealthManager.markHealthy(it)
                        }

                        val sid = exoPlayer.audioSessionId
                        if (sid > 0) {
                            try {
                                DragonFmApp.instance.audioEffectManager.attachToAudioSession(sid)
                            } catch (_: Exception) {}
                        }

                        _playerState.value = _playerState.value.copy(
                            status = if (isPlaying) PlaybackStatus.PLAYING else PlaybackStatus.PAUSED,
                            errorMessage = null
                        )

                        // Smooth anti-pop fade in when playback begins (guard against duplicate runs)
                        if (isPlaying && (volumeAnimator == null || !volumeAnimator!!.isRunning)) {
                            fadeInVolume()
                        }
                    }
                    Player.STATE_ENDED -> {
                        timeoutWatchdogJob?.cancel()
                        timeoutWatchdogJob = null
                        _playerState.value = _playerState.value.copy(status = PlaybackStatus.PAUSED)
                    }
                    Player.STATE_IDLE -> {
                        if (_playerState.value.status != PlaybackStatus.ERROR &&
                            _playerState.value.status != PlaybackStatus.BUFFERING) {
                            _playerState.value = _playerState.value.copy(status = PlaybackStatus.IDLE)
                        }
                    }
                }
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                if (isEventStale()) return
                Log.d(TAG, "[Player] onPlayWhenReadyChanged: playWhenReady=$playWhenReady, reason=$reason, state=${exoPlayer.playbackState}")
                if (!playWhenReady) {
                    timeoutWatchdogJob?.cancel()
                    timeoutWatchdogJob = null
                    retryJob?.cancel()
                    retryJob = null
                    _playerState.value = _playerState.value.copy(status = PlaybackStatus.PAUSED)
                } else {
                    when (exoPlayer.playbackState) {
                        Player.STATE_READY -> {
                            _playerState.value = _playerState.value.copy(status = PlaybackStatus.PLAYING)
                            if (exoPlayer.volume < 1.0f && (volumeAnimator == null || !volumeAnimator!!.isRunning)) {
                                fadeInVolume()
                            }
                        }
                        Player.STATE_BUFFERING -> {
                            _playerState.value = _playerState.value.copy(status = PlaybackStatus.BUFFERING)
                            val current = _playerState.value.currentStation
                            if (current != null && timeoutWatchdogJob == null) {
                                startTimeoutWatchdog(currentSessionId, current, isMidStream = true)
                            }
                        }
                        Player.STATE_IDLE -> {
                            val current = _playerState.value.currentStation
                            if (current != null) {
                                playStation(current)
                            }
                        }
                    }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isEventStale()) return
                if (exoPlayer.playbackState == Player.STATE_READY) {
                    _playerState.value = _playerState.value.copy(
                        status = if (isPlaying) PlaybackStatus.PLAYING else PlaybackStatus.PAUSED
                    )
                    if (isPlaying && exoPlayer.volume < 1.0f && (volumeAnimator == null || !volumeAnimator!!.isRunning)) {
                        fadeInVolume()
                    }
                } else if (!isPlaying && !exoPlayer.playWhenReady) {
                    _playerState.value = _playerState.value.copy(status = PlaybackStatus.PAUSED)
                }
            }

            override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                if (isEventStale()) return
                val title = mediaMetadata.title?.toString()
                    ?: mediaMetadata.displayTitle?.toString()
                    ?: mediaMetadata.artist?.toString().orEmpty()
                if (title.isNotBlank()) {
                    _playerState.value = _playerState.value.copy(nowPlayingTitle = title)
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                if (isEventStale()) return
                val current = _playerState.value.currentStation ?: return
                if (current.id != activeStationId) return

                timeoutWatchdogJob?.cancel()
                timeoutWatchdogJob = null
                retryJob?.cancel()

                // Fast recovery for HLS live window expiration (resync directly to live edge)
                if (error.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
                    Log.i(TAG, "[SelfHealing] Behind live window on ${current.name}, seeking to live edge")
                    try {
                        exoPlayer.seekToDefaultPosition()
                        exoPlayer.prepare()
                        return
                    } catch (_: Exception) {}
                }

                // Evict potentially stale or invalid redirect URL from cache
                resolvedUrlCache.remove(current.playUrl)

                // 1. Multi-line failover: if current line failed and alternative lines exist, failover immediately
                val activeLineIdx = current.activeLineIndex
                val lines = current.effectiveLines
                if (lines.size > 1 && (activeLineIdx + 1) < lines.size) {
                    val nextIdx = activeLineIdx + 1
                    val nextLine = lines[nextIdx]
                    Log.w(TAG, "[Failover] Line $activeLineIdx failed on ${current.name}, auto-switching to line $nextIdx (${nextLine.name})")
                    _playerState.value = _playerState.value.copy(
                        status = PlaybackStatus.BUFFERING,
                        nowPlayingTitle = "正在无感切换备用线路 (${nextLine.name})..."
                    )
                    val switched = current.copy(activeLineIndex = nextIdx)
                    playStation(switched, isRetry = true)
                    return
                }

                if (retryCount < MAX_AUTO_RETRIES) {
                    retryCount++
                    Log.w(TAG, "[SelfHealing] Error on ${current.name} (${error.errorCodeName}), auto-retry $retryCount/$MAX_AUTO_RETRIES with dynamic stream refresh")
                    _playerState.value = _playerState.value.copy(
                        status = PlaybackStatus.BUFFERING,
                        nowPlayingTitle = "正在刷新最新广播专线 ($retryCount/$MAX_AUTO_RETRIES)..."
                    )
                    val expectedSessionId = currentSessionId
                    retryJob = scope.launch {
                        val freshStation = com.dragon.fm.data.local.DedicatedSourceManager.resolveFreshStationAsync(current, forceRefresh = true)
                        if (expectedSessionId == currentSessionId && activeStationId == current.id) {
                            if (freshStation.playUrl != current.playUrl) {
                                Log.i(TAG, "[SelfHealing] Successfully refreshed live stream for ${freshStation.name}: ${freshStation.playUrl}")
                                withContext(Dispatchers.Main) {
                                    onStationRefreshedListener?.invoke(freshStation)
                                }
                                playStation(freshStation, isRetry = true)
                            } else {
                                delay(600L)
                                playStation(current, isRetry = true)
                            }
                        }
                    }
                } else {
                    Log.e(TAG, "[SelfHealing] Station ${current.name} retries exhausted, marking error: ${error.message}")
                    com.dragon.fm.data.local.StationHealthManager.markUnhealthy(current.id)
                    try {
                        exoPlayer.stop()
                    } catch (_: Exception) {}
                    _playerState.value = _playerState.value.copy(
                        status = PlaybackStatus.ERROR,
                        errorMessage = "该电台暂时无法连接，可点击重试或切换其他频道"
                    )
                }
            }
        })
    }

    companion object {
        private const val TAG = "DragonPlayer"
        private const val MAX_AUTO_RETRIES = 2
        var onAutoSkipListener: (() -> Unit)? = null
    }

    /**
     * Pre-resolves DNS and HTTP redirects for upcoming or adjacent radio stations
     * to eliminate TTFB delays when swiping channels.
     * Uses debounce and job cancellation to prevent network saturation during rapid swiping.
     */
    fun warmUpAdjacentStations(stations: List<RadioStation>) {
        if (stations.isEmpty()) return
        warmUpJob?.cancel()
        warmUpJob = scope.launch(Dispatchers.IO) {
            delay(500L) // Debounce rapid swiping to conserve bandwidth for active stream
            for (st in stations) {
                try {
                    val rawUrl = st.playUrl
                    if (rawUrl.isBlank()) continue
                    val uri = Uri.parse(rawUrl)
                    val host = uri.host ?: continue

                    // 1. Pre-warm DNS cache
                    Dns.SYSTEM.lookup(host)

                    // 2. Pre-resolve HTTP 301/302 redirects if not already cached
                    if (!resolvedUrlCache.containsKey(rawUrl)) {
                        var resolved = false
                        val headReq = Request.Builder()
                            .url(rawUrl)
                            .header("User-Agent", "DragonFM/1.0.0 (Linux; Android; ExoPlayer)")
                            .head()
                            .build()
                        try {
                            okHttpClient.newCall(headReq).execute().use { resp ->
                                val finalUrl = resp.request.url.toString()
                                if (finalUrl != rawUrl && resp.isSuccessful) {
                                    resolvedUrlCache[rawUrl] = finalUrl
                                    resolved = true
                                    Log.d(TAG, "[PreResolve] Cached redirect for ${st.name}: $rawUrl -> $finalUrl")
                                }
                            }
                        } catch (_: Exception) {
                            // Head request might not be supported on some shoutcast/CDN servers; fallback below
                        }

                        // Fallback to lightweight Range GET if HEAD failed or was unsupported
                        if (!resolved && !resolvedUrlCache.containsKey(rawUrl)) {
                            try {
                                val getReq = Request.Builder()
                                    .url(rawUrl)
                                    .header("User-Agent", "DragonFM/1.0.0 (Linux; Android; ExoPlayer)")
                                    .header("Range", "bytes=0-1")
                                    .build()
                                okHttpClient.newCall(getReq).execute().use { resp ->
                                    val finalUrl = resp.request.url.toString()
                                    if (finalUrl != rawUrl && (resp.isSuccessful || resp.code == 206)) {
                                        resolvedUrlCache[rawUrl] = finalUrl
                                        Log.d(TAG, "[PreResolve] Cached redirect via GET for ${st.name}: $rawUrl -> $finalUrl")
                                    }
                                }
                            } catch (_: Exception) {}
                        }
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "[PreResolve] Pre-warm failed for ${st.name}: ${e.message}")
                }
            }
        }
    }

    private fun startTimeoutWatchdog(sessionId: Long, station: RadioStation, isMidStream: Boolean = false) {
        timeoutWatchdogJob?.cancel()
        timeoutWatchdogJob = scope.launch {
            delay(6500L) // 6.5s connection / rebuffer watchdog
            if (sessionId == currentSessionId && activeStationId == station.id && _playerState.value.status == PlaybackStatus.BUFFERING) {
                Log.w(TAG, "[Watchdog] Connection timeout for ${station.name} (isMidStream=$isMidStream)")
                resolvedUrlCache.remove(station.playUrl)

                // 1. Multi-line failover on timeout
                val activeLineIdx = station.activeLineIndex
                val lines = station.effectiveLines
                if (lines.size > 1 && (activeLineIdx + 1) < lines.size) {
                    val nextIdx = activeLineIdx + 1
                    val nextLine = lines[nextIdx]
                    Log.w(TAG, "[Watchdog Failover] Line $activeLineIdx timed out on ${station.name}, switching to line $nextIdx (${nextLine.name})")
                    _playerState.value = _playerState.value.copy(
                        status = PlaybackStatus.BUFFERING,
                        nowPlayingTitle = "连接超时，正在切换备用线路 (${nextLine.name})..."
                    )
                    val switched = station.copy(activeLineIndex = nextIdx)
                    playStation(switched, isRetry = true)
                    return@launch
                }

                if (retryCount < MAX_AUTO_RETRIES) {
                    retryCount++
                    Log.w(TAG, "[Watchdog] Attempting auto-retry $retryCount/$MAX_AUTO_RETRIES with dynamic stream refresh")
                    _playerState.value = _playerState.value.copy(
                        status = PlaybackStatus.BUFFERING,
                        nowPlayingTitle = "正在刷新最新广播专线 ($retryCount/$MAX_AUTO_RETRIES)..."
                    )
                    scope.launch {
                        val freshStation = com.dragon.fm.data.local.DedicatedSourceManager.resolveFreshStationAsync(station, forceRefresh = true)
                        if (sessionId == currentSessionId && activeStationId == station.id) {
                            if (freshStation.playUrl != station.playUrl) {
                                Log.i(TAG, "[Watchdog] Successfully refreshed live stream for ${freshStation.name}")
                                withContext(Dispatchers.Main) {
                                    onStationRefreshedListener?.invoke(freshStation)
                                }
                                playStation(freshStation, isRetry = true)
                            } else {
                                playStation(station, isRetry = true)
                            }
                        }
                    }
                } else {
                    try {
                        exoPlayer.stop()
                    } catch (_: Exception) {}
                    _playerState.value = _playerState.value.copy(
                        status = PlaybackStatus.ERROR,
                        errorMessage = "电台连接超时，请点击重试或切换频道"
                    )
                }
            }
        }
    }

    fun playStation(station: RadioStation, isRetry: Boolean = false) {
        val targetStation = if (!isRetry) {
            val fresh = com.dragon.fm.data.local.DedicatedSourceManager.resolveFreshStation(station)
            if (fresh.playUrl != station.playUrl) {
                Log.i(TAG, "[PlayResolve] Proactively refreshed stream for ${fresh.name}")
                onStationRefreshedListener?.invoke(fresh)
            }
            fresh
        } else {
            station
        }

        // 核心自愈：若起播前检测到防盗链时效性地址已过期，直接毫秒级异步拉取最新官方授权流，杜绝 403 阻断
        if (!isRetry && com.dragon.fm.data.local.DedicatedSourceManager.isStreamUrlExpired(targetStation.playUrl)) {
            val thisSessionId = ++currentSessionId
            activeStationId = targetStation.id
            _playerState.value = _playerState.value.copy(
                status = PlaybackStatus.BUFFERING,
                currentStation = targetStation,
                nowPlayingTitle = "正在获取最新官方直播专线..."
            )
            scope.launch {
                val freshStation = com.dragon.fm.data.local.DedicatedSourceManager.resolveFreshStationAsync(targetStation, forceRefresh = true)
                if (thisSessionId == currentSessionId && activeStationId == targetStation.id) {
                    withContext(Dispatchers.Main) {
                        if (freshStation.playUrl != targetStation.playUrl) {
                            onStationRefreshedListener?.invoke(freshStation)
                        }
                    }
                    // 传入 isRetry = true，坚决阻断任何可能的递归自旋
                    playStation(freshStation, isRetry = true)
                }
            }
            return
        }

        retryJob?.cancel()
        retryJob = null
        timeoutWatchdogJob?.cancel()
        timeoutWatchdogJob = null

        // Ensure playback service is running and active
        DragonMediaSessionService.ensureRunning(context)

        val thisSessionId = ++currentSessionId
        activeStationId = targetStation.id
        if (!isRetry) {
            retryCount = 0
        }
        playStartTimeMs = SystemClock.elapsedRealtime()

        // Zero-pop: immediately mute hardware volume and cancel prior animation before stopping old stream
        cancelVolumeAnimation()
        exoPlayer.volume = 0f

        val finalPlayUrl = resolvedUrlCache[targetStation.playUrl] ?: targetStation.playUrl
        if (finalPlayUrl.isBlank()) {
            Log.w(TAG, "[Switch] Station ${targetStation.name} has empty stream URL, aborting playback safely")
            _playerState.value = _playerState.value.copy(
                currentStation = targetStation,
                status = PlaybackStatus.ERROR,
                errorMessage = "该电台暂无有效音频直播流",
                nowPlayingTitle = "该电台暂无有效音频直播流"
            )
            return
        }
        Log.i(TAG, "[Switch] Station: ${targetStation.name} (${targetStation.id}), session: $thisSessionId, isRetry: $isRetry, url: $finalPlayUrl")

        val artworkUri = targetStation.favicon?.takeIf {
            it.isNotBlank() && (it.startsWith("http://") || it.startsWith("https://") || it.startsWith("content://"))
        }?.let { Uri.parse(it) }

        val mediaItem = MediaItem.Builder()
            .setMediaId(targetStation.id)
            .setUri(Uri.parse(finalPlayUrl))
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(targetStation.name)
                    .setArtist(targetStation.formattedRegion)
                    .apply {
                        if (artworkUri != null) {
                            setArtworkUri(artworkUri)
                        }
                    }
                    .build()
            )
            .build()

        val activeLine = targetStation.currentLine
        val lineName = activeLine?.name ?: (if (targetStation.hasDedicatedLine) "官方专线" else "默认主线")
        val initialTitle = targetStation.currentProgram?.let { "正在直播：$it" }
            ?: if (isRetry) "正在连接 $lineName..." else "正在载入电台流..."

        _playerState.value = _playerState.value.copy(
            currentStation = targetStation,
            status = PlaybackStatus.BUFFERING,
            nowPlayingTitle = initialTitle,
            errorMessage = null,
            activeLineName = lineName,
            availableLines = targetStation.effectiveLines
        )

        // Resilient Stream Watchdog (handles initial connection and reconnection timeouts)
        startTimeoutWatchdog(thisSessionId, targetStation, isMidStream = false)

        try {
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
        } catch (e: Exception) {
            timeoutWatchdogJob?.cancel()
            _playerState.value = _playerState.value.copy(
                status = PlaybackStatus.ERROR,
                errorMessage = "该电台流暂无法播放: ${e.message}"
            )
        }
    }

    fun prepareStation(station: RadioStation) {
        val targetStation = com.dragon.fm.data.local.DedicatedSourceManager.resolveFreshStation(station)
        if (targetStation.playUrl != station.playUrl) {
            onStationRefreshedListener?.invoke(targetStation)
        }

        retryJob?.cancel()
        retryJob = null
        timeoutWatchdogJob?.cancel()
        timeoutWatchdogJob = null

        DragonMediaSessionService.ensureRunning(context)

        val thisSessionId = ++currentSessionId
        activeStationId = targetStation.id
        retryCount = 0

        cancelVolumeAnimation()
        exoPlayer.volume = 1f

        val finalPlayUrl = resolvedUrlCache[targetStation.playUrl] ?: targetStation.playUrl
        val artworkUri = targetStation.favicon?.takeIf {
            it.isNotBlank() && (it.startsWith("http://") || it.startsWith("https://") || it.startsWith("content://"))
        }?.let { Uri.parse(it) }

        val mediaItem = MediaItem.Builder()
            .setMediaId(targetStation.id)
            .setUri(Uri.parse(finalPlayUrl))
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(targetStation.name)
                    .setArtist(targetStation.formattedRegion)
                    .apply {
                        if (artworkUri != null) {
                            setArtworkUri(artworkUri)
                        }
                    }
                    .build()
            )
            .build()

        _playerState.value = _playerState.value.copy(
            currentStation = targetStation,
            status = PlaybackStatus.PAUSED,
            nowPlayingTitle = "准备就绪",
            errorMessage = null
        )

        try {
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.playWhenReady = false
        } catch (_: Exception) {}
    }

    fun retryCurrentStation() {
        val current = _playerState.value.currentStation ?: return
        resolvedUrlCache.remove(current.playUrl)
        retryCount = 0
        playStation(current, isRetry = false)
    }

    /**
     * Switch playback to a specific line index for the current station (e.g. 0 for 官方专线, 1 for 公网备用)
     */
    fun switchLine(lineIndex: Int) {
        val current = _playerState.value.currentStation ?: return
        val lines = current.effectiveLines
        if (lineIndex in lines.indices && lineIndex != current.activeLineIndex) {
            resolvedUrlCache.remove(current.playUrl)
            retryCount = 0
            val switched = current.copy(activeLineIndex = lineIndex)
            playStation(switched, isRetry = false)
        }
    }

    fun play() {
        cancelVolumeAnimation()
        DragonMediaSessionService.ensureRunning(context)
        if (exoPlayer.playbackState == Player.STATE_IDLE && _playerState.value.currentStation != null) {
            playStation(_playerState.value.currentStation!!)
        } else {
            exoPlayer.volume = 0f
            exoPlayer.play()
            fadeInVolume(180L)
        }
    }

    fun pause() {
        timeoutWatchdogJob?.cancel()
        timeoutWatchdogJob = null
        retryJob?.cancel()
        retryJob = null

        if (exoPlayer.playbackState == Player.STATE_READY && exoPlayer.volume > 0f) {
            // Fast 60ms mute ramp before pause to eliminate sudden cut-off pop
            cancelVolumeAnimation()
            val anim = ValueAnimator.ofFloat(exoPlayer.volume, 0.0f).apply {
                duration = 60
                addUpdateListener {
                    exoPlayer.volume = it.animatedValue as Float
                }
                addListener(object : android.animation.AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: android.animation.Animator) {
                        exoPlayer.pause()
                        exoPlayer.volume = 1.0f
                        _playerState.value = _playerState.value.copy(status = PlaybackStatus.PAUSED)
                    }
                })
            }
            volumeAnimator = anim
            anim.start()
        } else {
            // Paused while buffering or zero volume: pause immediately
            cancelVolumeAnimation()
            exoPlayer.pause()
            exoPlayer.volume = 1.0f
            _playerState.value = _playerState.value.copy(status = PlaybackStatus.PAUSED)
        }
    }

    fun togglePlayPause() {
        if (exoPlayer.playWhenReady) {
            pause()
        } else {
            play()
        }
    }

    fun updateFavoriteStatus(stationId: String, isFavorite: Boolean) {
        val current = _playerState.value.currentStation
        if (current?.id == stationId) {
            _playerState.value = _playerState.value.copy(
                currentStation = current.copy(isFavorite = isFavorite)
            )
        }
    }

    fun updateStationName(stationId: String, newName: String) {
        val current = _playerState.value.currentStation
        if (current?.id == stationId) {
            _playerState.value = _playerState.value.copy(
                currentStation = current.copy(name = newName)
            )
        }
    }

    fun updateNowPlayingTitle(title: String) {
        if (title.isNotBlank()) {
            _playerState.value = _playerState.value.copy(nowPlayingTitle = title)
        }
    }

    fun stop() {
        timeoutWatchdogJob?.cancel()
        timeoutWatchdogJob = null
        retryJob?.cancel()
        retryJob = null
        warmUpJob?.cancel()
        warmUpJob = null
        activeStationId = null

        cancelVolumeAnimation()
        exoPlayer.volume = 0f

        exoPlayer.stop()
        exoPlayer.clearMediaItems()
        _playerState.value = _playerState.value.copy(status = PlaybackStatus.IDLE)
    }

    private fun fadeInVolume(durationMs: Long = 180L) {
        cancelVolumeAnimation()
        val startVol = exoPlayer.volume
        if (startVol >= 1.0f) return
        val anim = ValueAnimator.ofFloat(startVol, 1.0f).apply {
            duration = durationMs
            addUpdateListener {
                exoPlayer.volume = it.animatedValue as Float
            }
            start()
        }
        volumeAnimator = anim
    }

    fun fadeOutAndPause(onComplete: () -> Unit = {}) {
        cancelVolumeAnimation()
        val animator = ValueAnimator.ofFloat(exoPlayer.volume, 0.0f).apply {
            duration = 1500
            addUpdateListener {
                exoPlayer.volume = it.animatedValue as Float
            }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    exoPlayer.pause()
                    exoPlayer.volume = 1.0f
                    _playerState.value = _playerState.value.copy(status = PlaybackStatus.PAUSED)
                    onComplete()
                }
            })
            start()
        }
        volumeAnimator = animator
    }

    fun release() {
        stop()
        exoPlayer.release()
    }
}
