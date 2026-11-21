package com.dragon.fm.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dragon.fm.data.model.LiveSongLyricsState
import com.dragon.fm.data.model.LiveSubtitleState
import com.dragon.fm.data.model.LyricsDisplayMode
import com.dragon.fm.data.model.SubtitleStatus
import com.dragon.fm.ui.theme.DragonCyan
import com.dragon.fm.ui.theme.DragonTheme
import com.dragon.fm.ui.theme.LiveRed

/**
 * 封面模式下的单行歌词条
 * 支持在【实时语音字幕 (ASR)】与【歌曲同步歌词 (LRC)】之间无缝动态切换
 */
@Composable
fun SingleLineLyricBar(
    subtitleState: LiveSubtitleState,
    songLyricState: LiveSongLyricsState = LiveSongLyricsState(),
    displayMode: LyricsDisplayMode = LyricsDisplayMode.AI_SUBTITLE,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onToggleSubtitle: (Boolean) -> Unit = {},
    onToggleDisplayMode: (() -> Unit)? = null
) {
    val theme = DragonTheme
    val isDark = theme.isDark

    val infiniteTransition = rememberInfiniteTransition(label = "singleLyricPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val isSongMode = displayMode == LyricsDisplayMode.SONG_LYRICS && songLyricState.isActive
    val isSpeaking = subtitleState.isEnabled && subtitleState.status == SubtitleStatus.SPEAKING
    val isEnabled = subtitleState.isEnabled || isSongMode

    // 计算当前要展示的单行文字内容（仅保留行首固定音符图标，正文不再重复添加♪前缀）
    val displayText: String = if (isSongMode) {
        val currentLine = songLyricState.currentLineText.trim()
        if (currentLine.isNotBlank()) {
            currentLine
        } else {
            songLyricState.song?.let { "${it.title} - ${it.artist}" } ?: "歌曲伴听中..."
        }
    } else {
        if (!subtitleState.isEnabled) {
            "轻触开启实时字幕 (歌词模式)"
        } else if (subtitleState.currentSentence.isNotBlank()) {
            val raw = subtitleState.currentSentence.trim()
            val clauses = raw.split("，", "。", "！", "？").map { it.trim() }.filter { it.isNotEmpty() }
            clauses.lastOrNull() ?: raw
        } else if (subtitleState.subtitles.isNotEmpty()) {
            subtitleState.subtitles.last().text
        } else {
            "实时字幕监听中..."
        }
    }

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(38.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isDark) Color(0x26000000) else Color(0x38FFFFFF))
            .border(0.5.dp, DragonTheme.glassBorder, RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = rememberRipple(bounded = true, color = if (isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.08f)),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (isSongMode) {
                Icon(
                    imageVector = Icons.Outlined.MusicNote,
                    contentDescription = "歌曲歌词",
                    tint = if (isDark) DragonCyan else theme.primary,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            } else if (isSpeaking) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(LiveRed)
                )
                Spacer(modifier = Modifier.width(6.dp))
            } else if (subtitleState.isEnabled) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(DragonTheme.primary.copy(alpha = pulseAlpha))
                )
                Spacer(modifier = Modifier.width(6.dp))
            } else {
                Icon(
                    imageVector = Icons.Outlined.Subtitles,
                    contentDescription = null,
                    tint = DragonTheme.textSecondary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }

            AnimatedContent(
                targetState = displayText,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(180))
                },
                label = "lyricTextChange",
                modifier = Modifier.weight(1f, fill = false)
            ) { text ->
                Text(
                    text = text.trimEnd('，', '、', ',', ' '),
                    color = when {
                        isSongMode -> if (isDark) DragonCyan else theme.primary
                        isSpeaking -> if (isDark) Color.White else Color(0xFF0F172A)
                        isEnabled -> if (isDark) Color.White.copy(alpha = 0.88f) else Color(0xFF1E293B)
                        else -> DragonTheme.textSecondary
                    },
                    fontSize = if (isEnabled) 15.sp else 13.5.sp,
                    fontWeight = if (isSongMode || isSpeaking) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }

            // 右侧仅在纯实时字幕模式时保留微型关闭图标；歌词模式下彻底去除后置按钮保持极简居中
            if (!isSongMode && subtitleState.isEnabled) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "关闭实时字幕",
                    tint = if (isDark) Color.White.copy(alpha = 0.40f) else Color(0xFF64748B),
                    modifier = Modifier
                        .size(15.dp)
                        .clip(CircleShape)
                        .clickable { onToggleSubtitle(false) }
                )
            }
        }
    }
}
