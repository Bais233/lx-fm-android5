package com.dragon.fm.player

import android.content.Context
import com.dragon.fm.data.model.LiveSubtitleState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 精简版桩：上游在这里跑实时字幕识别，本移植没有 ASR，只保留可读的状态。 */
class LiveStreamSubtitleManager(private val context: Context) {

    val asrModelManager: AsrModelManager = AsrModelManager.getInstance(context)

    private val _subtitleState = MutableStateFlow(LiveSubtitleState())
    val subtitleState: StateFlow<LiveSubtitleState> = _subtitleState.asStateFlow()

    fun onStationChanged() = Unit

    fun toggleSubtitle(captureProcessor: LiveAudioCaptureProcessor?) {
        _subtitleState.value = _subtitleState.value.copy(errorMessage = "这个精简版不带语音识别")
    }

    fun setSubtitleEnabled(enabled: Boolean, captureProcessor: LiveAudioCaptureProcessor?) {
        if (enabled) toggleSubtitle(captureProcessor)
    }

    fun clearSubtitles() {
        _subtitleState.value = LiveSubtitleState()
    }

    fun setFontSize(sizeSp: Float) {
        _subtitleState.value = _subtitleState.value.copy(fontSizeSp = sizeSp.coerceIn(12f, 24f))
    }

    fun copyAllSubtitles() = Unit

    fun copySingleSubtitle(text: String) = Unit
}
