package com.dragon.fm.player

import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 专业级电台实时音频频谱分析器 (Real-time Audio Spectrum FFT Engine)
 *
 * 核心技术：
 * 1. 直接截取 ExoPlayer 解码后的 16-bit PCM 数字音频流（零延迟、零杂音、免麦克风权限）；
 * 2. 512 点快速傅里叶变换 (Cooley-Tukey Radix-2 FFT) + Hann 窗函数消除频谱泄漏；
 * 3. 24 个对数频率通道（Bark/Mel 听觉刻度），精准覆盖 Sub-Bass(40Hz) 到 Treble(16kHz)；
 * 4. 专业弹道学平滑算法：瞬态极速冲击 (Attack ~0.80)，指数重力衰减 (Decay ~0.86)；
 * 5. 独立高光峰值保持 (Peak Hold & Gravity Drop)，还原先锋调音台与录音棚专业频谱仪质感。
 */
class AudioSpectrumManager(
    val bandCount: Int = 24
) {
    private val fftSize = 512
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _spectrumBands = MutableStateFlow(FloatArray(bandCount))
    val spectrumBands: StateFlow<FloatArray> = _spectrumBands.asStateFlow()

    private val _spectrumPeaks = MutableStateFlow(FloatArray(bandCount))
    val spectrumPeaks: StateFlow<FloatArray> = _spectrumPeaks.asStateFlow()

    // 预分配计算缓冲区，避免 GC 抖动
    private val pcmSampleBuffer = FloatArray(fftSize)
    private var pcmBufferWritePos = 0

    private val fftReal = FloatArray(fftSize)
    private val fftImag = FloatArray(fftSize)
    private val magnitudes = FloatArray(fftSize / 2)

    private val currentBands = FloatArray(bandCount)
    private val currentPeaks = FloatArray(bandCount)
    private val peakHoldFrames = IntArray(bandCount)

    // 预计算 Hann 窗与三角函数查找表
    private val window = FloatArray(fftSize)
    private val cosTable = FloatArray(fftSize / 2)
    private val sinTable = FloatArray(fftSize / 2)
    private val bitRevTable = IntArray(fftSize)

    // 频段范围映射 (binStart, binEnd)
    private val bandBinRanges = Array(bandCount) { IntArray(2) }

    @Volatile
    private var lastPcmTimestamp = 0L
    private var lastEmitTime = 0L

    init {
        // 1. 预计算位反转索引
        var j = 0
        for (i in 0 until fftSize) {
            bitRevTable[i] = j
            var bit = fftSize shr 1
            while (bit and j != 0) {
                j = j xor bit
                bit = bit shr 1
            }
            j = j xor bit
        }

        // 2. 预计算旋转因子与 Hann 窗
        for (i in 0 until fftSize / 2) {
            val angle = -2.0 * Math.PI * i / fftSize
            cosTable[i] = cos(angle).toFloat()
            sinTable[i] = sin(angle).toFloat()
        }
        for (i in 0 until fftSize) {
            window[i] = (0.5 * (1.0 - cos(2.0 * Math.PI * i / (fftSize - 1)))).toFloat()
        }

        // 3. 预计算对数频率分区 (40Hz ~ 16000Hz 对应 44.1kHz 采样率下的 bin 分布)
        updateBandRanges(44100)

        // 4. 后台衰减守卫线程：当音频暂停或静音超过 120ms 时，使频谱自然平滑归零
        scope.launch {
            while (isActive) {
                delay(33) // ~30fps 衰减轮询
                val now = SystemClock.uptimeMillis()
                if (now - lastPcmTimestamp > 120L) {
                    var hasEnergy = false
                    for (b in 0 until bandCount) {
                        if (currentBands[b] > 0.005f || currentPeaks[b] > 0.005f) {
                            currentBands[b] = (currentBands[b] * 0.75f).coerceAtLeast(0f)
                            currentPeaks[b] = (currentPeaks[b] - 0.03f).coerceAtLeast(0f)
                            hasEnergy = true
                        } else {
                            currentBands[b] = 0f
                            currentPeaks[b] = 0f
                        }
                    }
                    if (hasEnergy) {
                        _spectrumBands.value = currentBands.copyOf()
                        _spectrumPeaks.value = currentPeaks.copyOf()
                    }
                }
            }
        }
    }

    private var currentSampleRate = 44100
    private fun updateBandRanges(sampleRate: Int) {
        if (sampleRate <= 0) return
        currentSampleRate = sampleRate
        val binHz = sampleRate.toFloat() / fftSize
        val minFreq = 40.0
        val maxFreq = min(16000.0, sampleRate / 2.0 - 500.0).coerceAtLeast(8000.0)
        val logMin = log10(minFreq)
        val logMax = log10(maxFreq)

        for (i in 0 until bandCount) {
            val fLow = 10.0.pow(logMin + (logMax - logMin) * (i.toDouble() / bandCount))
            val fHigh = 10.0.pow(logMin + (logMax - logMin) * ((i + 1).toDouble() / bandCount))

            val binStart = max(1, (fLow / binHz).toInt())
            val binEnd = min(fftSize / 2, max(binStart + 1, (fHigh / binHz).toInt() + 1))

            bandBinRanges[i][0] = binStart
            bandBinRanges[i][1] = binEnd
        }
    }

    private fun Double.pow(exp: Double): Double = Math.pow(this, exp)

    /**
     * 接收 ExoPlayer 数字 PCM 流并实时进行 FFT 分析
     */
    fun processPcm(pcmBytes: ByteArray, sampleRate: Int, channelCount: Int) {
        if (pcmBytes.isEmpty() || channelCount <= 0) return
        lastPcmTimestamp = SystemClock.uptimeMillis()

        if (sampleRate != currentSampleRate && sampleRate > 0) {
            updateBandRanges(sampleRate)
        }

        val bytesPerSample = 2 // 16-bit PCM
        val frameBytes = bytesPerSample * channelCount
        val totalFrames = pcmBytes.size / frameBytes
        if (totalFrames <= 0) return

        var byteIdx = 0
        for (f in 0 until totalFrames) {
            // 解析 16-bit 单声道或立体声平均样本
            var sumSample = 0f
            for (ch in 0 until channelCount) {
                val b0 = pcmBytes[byteIdx].toInt() and 0xFF
                val b1 = pcmBytes[byteIdx + 1].toInt()
                val s16 = (b1 shl 8) or b0
                sumSample += s16.toShort().toFloat() / 32768f
                byteIdx += 2
            }
            val monoSample = sumSample / channelCount

            pcmSampleBuffer[pcmBufferWritePos++] = monoSample

            // 当缓冲区填满 512 个采样点，执行一次快速傅里叶分析
            if (pcmBufferWritePos >= fftSize) {
                runFftAnalysis()
                // 重叠滑动 50% (Hop size = 256)，捕获更高的瞬态冲击动态
                System.arraycopy(pcmSampleBuffer, fftSize / 2, pcmSampleBuffer, 0, fftSize / 2)
                pcmBufferWritePos = fftSize / 2
            }
        }
    }

    /**
     * 执行 512 点 FFT 计算并映射至 24 频段弹道学平滑
     */
    private fun runFftAnalysis() {
        // 1. 加窗与位反转
        for (i in 0 until fftSize) {
            val rev = bitRevTable[i]
            fftReal[rev] = pcmSampleBuffer[i] * window[i]
            fftImag[rev] = 0f
        }

        // 2. Cooley-Tukey Radix-2 蝶形运算
        var len = 2
        while (len <= fftSize) {
            val halfLen = len shr 1
            val step = fftSize / len
            var k = 0
            while (k < fftSize) {
                for (j in 0 until halfLen) {
                    val idx = j * step
                    val c = cosTable[idx]
                    val s = sinTable[idx]
                    val uR = fftReal[k + j]
                    val uI = fftImag[k + j]
                    val vR = fftReal[k + j + halfLen] * c - fftImag[k + j + halfLen] * s
                    val vI = fftReal[k + j + halfLen] * s + fftImag[k + j + halfLen] * c

                    fftReal[k + j] = uR + vR
                    fftImag[k + j] = uI + vI
                    fftReal[k + j + halfLen] = uR - vR
                    fftImag[k + j + halfLen] = uI - vI
                }
                k += len
            }
            len = len shl 1
        }

        // 3. 计算各频点线性幅度
        val halfFft = fftSize / 2
        val scale = 2.0f / fftSize
        for (i in 0 until halfFft) {
            val r = fftReal[i]
            val im = fftImag[i]
            magnitudes[i] = sqrt(r * r + im * im) * scale
        }

        // 4. 将 256 频点能量汇聚至 24 个对数频段，并进行人耳听觉等响度曲线补偿
        for (b in 0 until bandCount) {
            val startBin = bandBinRanges[b][0]
            val endBin = bandBinRanges[b][1]

            var maxMag = 0f
            var avgMag = 0f
            val count = endBin - startBin
            for (bin in startBin until endBin) {
                val m = magnitudes[bin]
                if (m > maxMag) maxMag = m
                avgMag += m
            }
            avgMag = if (count > 0) avgMag / count else maxMag

            // 综合最大值与平均值（保证鼓点瞬态冲击同时保留音色厚度）
            val rawEnergy = (maxMag * 0.7f + avgMag * 0.3f)

            // 高频能量在自然声学中衰减较多，引入对数高频提升增益 (Treble Boost Curve)
            val frequencyGain = 1.0f + (b.toFloat() / bandCount) * 1.8f
            val compensated = rawEnergy * frequencyGain * 5.2f

            // 对数压缩映射至 0.0 ~ 1.0
            val target = if (compensated > 0.001f) {
                (log10(1f + compensated * 9f)).coerceIn(0f, 1f)
            } else {
                0f
            }

            // 5. 弹道学平滑 (Ballistics: Fast Attack, Smooth Exponential Decay)
            if (target > currentBands[b]) {
                currentBands[b] = currentBands[b] + (target - currentBands[b]) * 0.82f // 极速响应打击乐/节拍
            } else {
                currentBands[b] = currentBands[b] * 0.86f // 柔和下落，杜绝突兀闪烁
            }

            // 6. 峰值保持与重力坠落 (Peak Hold with Gravity)
            if (currentBands[b] >= currentPeaks[b]) {
                currentPeaks[b] = currentBands[b]
                peakHoldFrames[b] = 6 // 保持 6 帧高光
            } else {
                if (peakHoldFrames[b] > 0) {
                    peakHoldFrames[b]--
                } else {
                    currentPeaks[b] = (currentPeaks[b] - 0.024f).coerceAtLeast(0f)
                }
            }
        }

        // 7. 发送最新的频谱与峰值数据 (限速 ~33fps，杜绝过高频率刷新导致 Compose 界面掉帧卡死)
        val now = SystemClock.uptimeMillis()
        if (now - lastEmitTime >= 30L) {
            lastEmitTime = now
            _spectrumBands.value = currentBands.copyOf()
            _spectrumPeaks.value = currentPeaks.copyOf()
        }
    }
}
