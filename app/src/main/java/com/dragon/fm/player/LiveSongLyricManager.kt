package com.dragon.fm.player

import android.os.SystemClock
import com.dragon.fm.data.model.LiveSongLyricsState
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

/**
 * 广播音乐实时同步歌词引擎
 * 基于声学指纹时间偏移量 (Offset) 与本地墙上时钟自动推进，
 * 彻底解决广播直播流无内置进度条时的歌词同步与自动滚动难题。
 */
class LiveSongLyricManager(private val scope: CoroutineScope) {

    private val _lyricsState = MutableStateFlow(LiveSongLyricsState())
    val lyricsState: StateFlow<LiveSongLyricsState> = _lyricsState.asStateFlow()

    private var tickerJob: Job? = null
    private var offsetAdjustmentMs: Long = 0L

    /**
     * 当识别出歌曲后，启动歌词拉取与高精度自动同步时钟
     */
    fun startSync(song: RecognizedSong) {
        tickerJob?.cancel()
        offsetAdjustmentMs = 0L

        _lyricsState.value = LiveSongLyricsState(
            isActive = true,
            song = song,
            isLoading = true
        )

        tickerJob = scope.launch(Dispatchers.IO) {
            try {
                val lyricResult = LyricRepository.fetchLyrics(song.title, song.artist, song.album)
                if (lyricResult == null || lyricResult.lines.isEmpty()) {
                    _lyricsState.value = _lyricsState.value.copy(
                        isLoading = false,
                        errorMessage = "未找到带时间轴的滚动歌词"
                    )
                    return@launch
                }

                // 计算当前时刻原曲的精确播放进度（毫秒）
                val nowRealtime = SystemClock.elapsedRealtime()
                val elapsedSinceSample = if (song.sampleStartRealtime > 0) {
                    (nowRealtime - song.sampleStartRealtime).coerceAtLeast(0L)
                } else {
                    0L
                }

                val baseOffsetMs = (song.matchedOffsetSeconds * 1000).toLong()
                var currentTrackMs = baseOffsetMs + elapsedSinceSample
                val totalDurationMs = lyricResult.durationMs

                if (currentTrackMs >= totalDurationMs) {
                    _lyricsState.value = _lyricsState.value.copy(
                        isActive = false,
                        isLoading = false,
                        errorMessage = "该歌曲在电台中已播毕"
                    )
                    return@launch
                }

                val lines = lyricResult.lines
                val tickInterval = 80L

                // 启动微秒级高平滑 Ticker
                while (isActive && (currentTrackMs + offsetAdjustmentMs) < totalDurationMs) {
                    val adjustedMs = (currentTrackMs + offsetAdjustmentMs).coerceAtLeast(0L)

                    // 倒序匹配当前活跃行
                    val activeIndex = lines.indexOfLast { it.timeMs <= adjustedMs }.coerceAtLeast(0)
                    val activeLine = lines.getOrNull(activeIndex)

                    _lyricsState.value = LiveSongLyricsState(
                        isActive = true,
                        song = song,
                        currentPositionMs = adjustedMs,
                        totalDurationMs = totalDurationMs,
                        currentLineIndex = activeIndex,
                        currentLineText = activeLine?.text.orEmpty(),
                        currentTranslation = activeLine?.translation,
                        lyrics = lines,
                        isLoading = false,
                        errorMessage = null
                    )

                    delay(tickInterval)
                    currentTrackMs += tickInterval
                }

                // 歌曲自然播放结束，淡出并恢复空闲状态
                if (isActive) {
                    _lyricsState.value = _lyricsState.value.copy(
                        isActive = false,
                        errorMessage = "歌曲播放完毕"
                    )
                }
            } catch (e: Exception) {
                if (isActive) {
                    _lyricsState.value = _lyricsState.value.copy(
                        isLoading = false,
                        errorMessage = "歌词同步发生异常: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    /**
     * 微调歌词时间轴 (支持 ±0.5s 调节广播延迟)
     */
    fun adjustOffset(deltaMs: Long) {
        offsetAdjustmentMs += deltaMs
    }

    /**
     * 电台切台或用户手动停止时清除歌词
     */
    fun onStationChanged() {
        stop()
    }

    fun stop() {
        tickerJob?.cancel()
        tickerJob = null
        offsetAdjustmentMs = 0L
        _lyricsState.value = LiveSongLyricsState()
    }
}
