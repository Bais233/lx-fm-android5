package com.dragon.fm.player

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import java.nio.ByteBuffer
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 实时广播音频截获处理器（基于 AndroidX Media3 BaseAudioProcessor）
 * 直接截取 ExoPlayer 解码后的纯数字 PCM 音频流，零网络开销，零环境杂音，无需麦克风录音权限。
 * 支持多路监听器分发（实时字幕识别 + 动态实时频谱分析）。
 */
class LiveAudioCaptureProcessor : BaseAudioProcessor() {

    @Volatile
    var isCaptureEnabled: Boolean = false

    private val pcmListeners = CopyOnWriteArrayList<(ByteArray, Int, Int) -> Unit>()

    // 兼容历史单一监听器接口
    var onPcmDataListener: ((ByteArray, Int, Int) -> Unit)? = null
        set(value) {
            field = value
            if (value != null) {
                isCaptureEnabled = true
            }
        }

    fun addPcmListener(listener: (ByteArray, Int, Int) -> Unit) {
        if (!pcmListeners.contains(listener)) {
            pcmListeners.add(listener)
        }
        isCaptureEnabled = true
    }

    fun removePcmListener(listener: (ByteArray, Int, Int) -> Unit) {
        pcmListeners.remove(listener)
    }

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        // 直接透明穿透格式，保持原样音频输出给下游 AudioTrack 正常发声播放
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        val hasListeners = onPcmDataListener != null || pcmListeners.isNotEmpty()
        if (hasListeners && inputAudioFormat.encoding == C.ENCODING_PCM_16BIT) {
            val sampleRate = inputAudioFormat.sampleRate
            val channelCount = inputAudioFormat.channelCount
            if (sampleRate > 0 && channelCount > 0) {
                val frameSize = 2 * channelCount
                val validRemaining = (remaining / frameSize) * frameSize
                if (validRemaining > 0) {
                    val pcmBytes = ByteArray(validRemaining)
                    val originalPosition = inputBuffer.position()
                    inputBuffer.get(pcmBytes, 0, validRemaining)
                    inputBuffer.position(originalPosition)

                    try {
                        onPcmDataListener?.invoke(pcmBytes, sampleRate, channelCount)
                    } catch (_: Exception) {}

                    for (listener in pcmListeners) {
                        try {
                            listener.invoke(pcmBytes, sampleRate, channelCount)
                        } catch (_: Exception) {}
                    }
                }
            }
        }

        // 将音频数据无损传递给下游正常播放
        val outputBuffer = replaceOutputBuffer(remaining)
        outputBuffer.put(inputBuffer)
        outputBuffer.flip()
    }
}
