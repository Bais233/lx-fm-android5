package com.dragon.fm.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dragon.fm.data.model.LiveSongLyricsState
import com.dragon.fm.data.model.SubtitleFontSize
import com.dragon.fm.ui.theme.DragonCyan
import com.dragon.fm.ui.theme.DragonTheme

/** Full-screen time-aligned static song lyrics. */
@Composable
fun LiveLyricsCard(
    songLyricState: LiveSongLyricsState,
    modifier: Modifier = Modifier,
    fontSizeOption: SubtitleFontSize = SubtitleFontSize.STANDARD,
    onCloseLyrics: () -> Unit,
    onCycleFontSize: (() -> Unit)? = null
) {
    val isDark = DragonTheme.isDark
    val activeColor = if (isDark) DragonCyan else DragonTheme.primary
    val activeText = if (isDark) Color.White else Color(0xFF0F172A)
    val historyText = if (isDark) Color.White.copy(alpha = 0.42f) else Color(0xFF64748B)
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val listState = rememberLazyListState()
    val interactionSource = remember { MutableInteractionSource() }
    val activeFontSize = (fontSizeOption.activeSp + if (isLandscape) 0f else 2f).sp
    val historyFontSize = fontSizeOption.historySp.sp

    LaunchedEffect(songLyricState.currentLineIndex, songLyricState.lyrics) {
        if (songLyricState.lyrics.isNotEmpty()) {
            listState.animateScrollToItem(songLyricState.currentLineIndex.coerceIn(songLyricState.lyrics.indices))
        }
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize().clip(RoundedCornerShape(20.dp))
            .background(if (isDark) Color(0x22000000) else Color(0x22FFFFFF))
    ) {
        val verticalPadding = maxHeight / 2 - 48.dp
        Column(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                if (onCycleFontSize != null) {
                    IconButton(onClick = onCycleFontSize) {
                        Icon(Icons.Default.FormatSize, "调整歌词字号", tint = activeText)
                    }
                }
                IconButton(onClick = onCloseLyrics) {
                    Icon(Icons.Default.Close, "关闭歌词", tint = activeText)
                }
            }
            when {
                songLyricState.isLoading -> Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(color = activeColor, modifier = Modifier.size(36.dp), strokeWidth = 2.5.dp)
                    Spacer(Modifier.height(14.dp))
                    Text("正在加载时间轴歌词...", color = activeText, textAlign = TextAlign.Center)
                }
                songLyricState.lyrics.isEmpty() -> Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Outlined.MusicNote, null, tint = activeColor, modifier = Modifier.size(38.dp))
                    Spacer(Modifier.height(10.dp))
                    Text(songLyricState.errorMessage ?: "暂无同步歌词", color = activeText, textAlign = TextAlign.Center)
                }
                else -> LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = verticalPadding.coerceAtLeast(12.dp), bottom = verticalPadding.coerceAtLeast(12.dp),
                        start = if (isLandscape) 14.dp else 20.dp, end = if (isLandscape) 14.dp else 20.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(if (isLandscape) 12.dp else 18.dp)
                ) {
                    itemsIndexed(songLyricState.lyrics, key = { index, line -> "lrc_${line.timeMs}_$index" }) { index, line ->
                        val active = index == songLyricState.currentLineIndex
                        Column(modifier = Modifier.fillMaxWidth().clickable(interactionSource, null) { onCloseLyrics() }) {
                            Text(
                                text = line.text,
                                color = if (active) activeText else historyText,
                                fontSize = if (active) activeFontSize else historyFontSize,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                                textAlign = TextAlign.Start
                            )
                            if (!line.translation.isNullOrBlank()) {
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    text = line.translation,
                                    color = if (active) activeText.copy(alpha = 0.75f) else historyText.copy(alpha = 0.6f),
                                    fontSize = historyFontSize,
                                    textAlign = TextAlign.Start
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
