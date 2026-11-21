package com.dragon.fm.data.model

enum class SubtitleStatus {
    IDLE,        // 未开启
    CONNECTING,  // 正在准备音频转写通道
    LISTENING,   // 正在监听广播伴听中（等待主播发话）
    SPEAKING,    // 正在实时出字（检测到语音）
    ERROR        // 异常提示
}

data class SubtitleItem(
    val id: Long = System.currentTimeMillis(),
    val text: String,
    val timeStr: String,
    val isFinal: Boolean = true
)

data class LiveSubtitleState(
    val isEnabled: Boolean = false,
    val status: SubtitleStatus = SubtitleStatus.IDLE,
    val currentSentence: String = "",
    val subtitles: List<SubtitleItem> = emptyList(),
    val errorMessage: String? = null,
    val fontSizeSp: Float = 16f
)
