package com.dragon.fm.data.model

import kotlinx.serialization.Serializable

@Serializable
data class LyricLine(
    val timeMs: Long,
    val text: String,
    val translation: String? = null
)

/**
 * 实时同步歌词状态模型
 */
data class LiveSongLyricsState(
    val isActive: Boolean = false,
    val song: RecognizedSong? = null,
    val currentPositionMs: Long = 0L,
    val totalDurationMs: Long = 0L,
    val currentLineIndex: Int = -1,
    val currentLineText: String = "",
    val currentTranslation: String? = null,
    val lyrics: List<LyricLine> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
