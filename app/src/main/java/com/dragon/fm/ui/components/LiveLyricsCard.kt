package com.dragon.fm.ui.components

import android.content.res.Configuration
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.delay
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dragon.fm.data.model.LiveSongLyricsState
import com.dragon.fm.data.model.LiveSubtitleState
import com.dragon.fm.data.model.LyricsDisplayMode
import com.dragon.fm.data.model.RecognitionState
import com.dragon.fm.data.model.SubtitleFontSize
import com.dragon.fm.data.model.SubtitleStatus
import com.dragon.fm.ui.theme.DragonCyan
import com.dragon.fm.ui.theme.DragonTheme
import com.dragon.fm.ui.theme.LiveRed

/**
 * 沉浸式全屏歌词/字幕瀑布流视图 (参考汽水音乐与 Apple Music 风格)
 * 深度集成：
 * 1. 【实时语音字幕 (ASR)】：主播讲话口播实时流式转写
 * 2. 【歌曲同步歌词 (LRC)】：听歌识曲命中后基于毫秒级 Offset 自动同步滚动
 * 3. 顶部胶囊支持一键在两套方案间自由切换
 */
@Composable
fun LiveLyricsCard(
    subtitleState: LiveSubtitleState,
    stationName: String,
    modifier: Modifier = Modifier,
    songLyricState: LiveSongLyricsState = LiveSongLyricsState(),
    displayMode: LyricsDisplayMode = LyricsDisplayMode.AI_SUBTITLE,
    onSwitchDisplayMode: (LyricsDisplayMode) -> Unit = {},
    currentProgram: String? = null,
    fontSizeOption: SubtitleFontSize = SubtitleFontSize.STANDARD,
    onCloseLyrics: () -> Unit,
    onToggleSubtitle: (Boolean) -> Unit = {},
    onClearSubtitles: () -> Unit,
    onCopyAll: () -> Unit,
    onCopySingle: (String) -> Unit = {},
    onAdjustFontSize: (Float) -> Unit = {},
    onCycleFontSize: (() -> Unit)? = null,
    recognitionState: RecognitionState? = null,
    onTriggerRecognition: () -> Unit = {}
) {
    val theme = DragonTheme
    val isDark = theme.isDark
    val listState = rememberLazyListState()

    // 动态对比度调色盘
    val activeTextColor = if (isDark) Color.White else Color(0xFF0F172A)
    val historyTextColor = if (isDark) Color.White.copy(alpha = 0.42f) else Color(0xFF64748B)
    val emptyTitleColor = if (isDark) Color.White.copy(alpha = 0.88f) else Color(0xFF0F172A)
    val emptySubColor = if (isDark) Color.White.copy(alpha = 0.50f) else Color(0xFF64748B)
    val pillBgColor = if (isDark) Color(0x66000000) else Color(0xCCF1F5F9)
    val pillBorderColor = if (isDark) Color.White.copy(alpha = 0.14f) else Color(0x1F000000)
    val pillTextColor = if (isDark) Color.White.copy(alpha = 0.90f) else Color(0xFF0F172A)

    // 呼吸脉冲动效
    val infiniteTransition = rememberInfiniteTransition(label = "LyricsGlow")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Glow"
    )

    val isSongMode = displayMode == LyricsDisplayMode.SONG_LYRICS

    // 计算 ASR 句子项数
    val currentSentence = subtitleState.currentSentence.trim()
    val streamClauses = remember(currentSentence) {
        if (currentSentence.isBlank()) emptyList()
        else currentSentence.split(Regex("(?<=[，。？！])"))
            .map { it.trim().trimEnd('，', '、', ',', ' ') }
            .filter { it.isNotEmpty() }
    }

    val activeAccentColor = if (isDark) DragonCyan else theme.primary
    val interactionSource = remember { MutableInteractionSource() }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(20.dp))
            .background(if (isDark) Color(0x22000000) else Color(0x22FFFFFF))
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                // 点击歌词空白区域返回封面
                onCloseLyrics()
            }
    ) {
        val configuration = LocalConfiguration.current
        val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE || maxWidth > maxHeight

        // 字号自适应响应当前字号选项，在横屏模式下进行合理比例折算，防止超大字号在横屏溢出
        val activeFontSize = if (isLandscape) (fontSizeOption.activeSp * 0.78f).coerceAtLeast(16f).sp else fontSizeOption.activeSp.sp
        val historyFontSize = if (isLandscape) (fontSizeOption.historySp * 0.80f).coerceAtLeast(13f).sp else fontSizeOption.historySp.sp
        val activeLineHeight = if (isLandscape) (activeFontSize.value * 1.35f).sp else (fontSizeOption.activeSp * 1.45f).sp
        val historyLineHeight = if (isLandscape) (historyFontSize.value * 1.4f).sp else (fontSizeOption.historySp * 1.5f).sp
        val lineSpacing = if (isLandscape) 10.dp else 22.dp

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 1. 顶部沉浸信息条
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = if (isLandscape) 4.dp else 8.dp)
            ) {
                // 上排：电台/歌曲标题与一键返回封面按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 12.dp)
                    ) {
                        val headerTitle = if (isSongMode && songLyricState.song != null) {
                            songLyricState.song.title
                        } else {
                            stationName.ifBlank { "电台直播" }
                        }
                        Text(
                            text = headerTitle,
                            color = activeTextColor,
                            fontSize = if (isLandscape) 14.sp else 17.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        val subText = if (isSongMode && songLyricState.song != null) {
                            "${songLyricState.song.artist} · 歌曲歌词同步"
                        } else {
                            currentProgram?.takeIf { it.isNotBlank() } ?: "AI 实时字幕伴听"
                        }
                        Text(
                            text = subText,
                            color = emptySubColor,
                            fontSize = if (isLandscape) 10.sp else 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // 返回封面按钮
                    Surface(
                        onClick = onCloseLyrics,
                        shape = RoundedCornerShape(999.dp),
                        color = if (isDark) Color(0x66000000) else Color(0xEEFFFFFF),
                        border = BorderStroke(0.8.dp, activeAccentColor.copy(alpha = 0.5f)),
                        modifier = Modifier.clip(RoundedCornerShape(999.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "返回封面",
                                tint = pillTextColor,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "封面",
                                color = pillTextColor,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(if (isLandscape) 2.dp else 6.dp))

                // 下排：模式切换器 / 伴听指示灯 (左) 与快捷操作组 (右：字号调节、复制、清空)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 顶部始终展示模式自由切换双联胶囊（歌曲歌词 / 语音转写）
                    val isRecognizing = recognitionState is RecognitionState.Sampling ||
                            recognitionState is RecognitionState.Fingerprinting ||
                            recognitionState is RecognitionState.QueryingCloud

                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = pillBgColor,
                        border = BorderStroke(0.6.dp, pillBorderColor)
                    ) {
                        Row(modifier = Modifier.padding(2.dp), verticalAlignment = Alignment.CenterVertically) {
                            // Tab 1: 歌曲歌词 (点击直接切到歌词，若未识别且未在识曲中，则自动触发听歌识曲)
                            Surface(
                                onClick = {
                                    onSwitchDisplayMode(LyricsDisplayMode.SONG_LYRICS)
                                    if (!songLyricState.isActive && !isRecognizing) {
                                        onTriggerRecognition()
                                    }
                                },
                                shape = RoundedCornerShape(999.dp),
                                color = if (isSongMode) activeAccentColor.copy(alpha = 0.28f) else Color.Transparent
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isRecognizing && isSongMode) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(11.dp),
                                            strokeWidth = 1.6.dp,
                                            color = activeAccentColor
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Outlined.MusicNote,
                                            contentDescription = null,
                                            tint = if (isSongMode) activeAccentColor else pillTextColor,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = if (isRecognizing && isSongMode) "识曲中" else "歌曲歌词",
                                        color = if (isSongMode) activeAccentColor else pillTextColor,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSongMode) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }

                            // Tab 2: 语音转写
                            val isSpeaking = subtitleState.status == SubtitleStatus.SPEAKING
                            Surface(
                                onClick = { onSwitchDisplayMode(LyricsDisplayMode.AI_SUBTITLE) },
                                shape = RoundedCornerShape(999.dp),
                                color = if (!isSongMode) activeAccentColor.copy(alpha = 0.28f) else Color.Transparent
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .clip(CircleShape)
                                            .background(if (isSpeaking) LiveRed else activeAccentColor)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "语音转写",
                                        color = if (!isSongMode) activeAccentColor else pillTextColor,
                                        fontSize = 11.sp,
                                        fontWeight = if (!isSongMode) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }

                    // 右侧工具胶囊组
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 字号调节
                        if (onCycleFontSize != null) {
                            Surface(
                                onClick = onCycleFontSize,
                                shape = RoundedCornerShape(999.dp),
                                color = pillBgColor,
                                border = BorderStroke(0.5.dp, pillBorderColor),
                                modifier = Modifier.clip(RoundedCornerShape(999.dp))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FormatSize,
                                        contentDescription = "字号",
                                        tint = pillTextColor,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    val sizeLabel = when (fontSizeOption) {
                                        SubtitleFontSize.COMPACT -> "小号"
                                        SubtitleFontSize.STANDARD -> "标准"
                                        SubtitleFontSize.LARGE -> "大号"
                                        SubtitleFontSize.EXTRA_LARGE -> "特大"
                                    }
                                    Text(
                                        text = sizeLabel,
                                        color = pillTextColor,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        // 复制
                        Surface(
                            onClick = onCopyAll,
                            shape = CircleShape,
                            color = pillBgColor,
                            border = BorderStroke(0.5.dp, pillBorderColor),
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "复制全部",
                                    tint = pillTextColor,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }

                        // 清空 (仅在转写模式下展示)
                        if (!isSongMode) {
                            Surface(
                                onClick = onClearSubtitles,
                                shape = CircleShape,
                                color = pillBgColor,
                                border = BorderStroke(0.5.dp, pillBorderColor),
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteSweep,
                                        contentDescription = "清空",
                                        tint = pillTextColor.copy(alpha = 0.8f),
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. 核心歌词/字幕瀑布流渲染区 (精准视口垂直居中自适应)
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawWithContent {
                        drawContent()
                        drawRect(
                            brush = Brush.verticalGradient(
                                0.00f to Color.Transparent,
                                (if (isLandscape) 0.12f else 0.07f) to Color.Black,
                                (if (isLandscape) 0.88f else 0.93f) to Color.Black,
                                1.00f to Color.Transparent
                            ),
                            blendMode = BlendMode.DstIn
                        )
                    }
            ) {
                val density = LocalDensity.current
                val viewportHeight = maxHeight
                // 确保首尾项都能完美垂直居中：上下内边距设为视口高度的一半（扣除半行文字行高）
                val verticalCenterPadding = (viewportHeight / 2 - (if (isLandscape) 18.dp else 24.dp)).coerceAtLeast(40.dp)

                // 1. 歌曲歌词自动平滑居中滚动 (精准像素级定位在屏幕正中央)
                LaunchedEffect(songLyricState.currentLineIndex, isSongMode, fontSizeOption, isLandscape) {
                    if (isSongMode && songLyricState.currentLineIndex >= 0 && songLyricState.lyrics.isNotEmpty()) {
                        val targetIndex = songLyricState.currentLineIndex
                        delay(20) // 等待测量布局渲染
                        val viewportHeightPx = listState.layoutInfo.viewportSize.height
                        val targetCenterY = viewportHeightPx / 2
                        val beforePadding = listState.layoutInfo.beforeContentPadding
                        val visibleItem = listState.layoutInfo.visibleItemsInfo.find { it.index == targetIndex }
                        if (visibleItem != null) {
                            val itemCenterY = visibleItem.offset + visibleItem.size / 2 + beforePadding
                            val diff = itemCenterY - targetCenterY
                            if (kotlin.math.abs(diff) > 2) {
                                listState.animateScrollBy(diff.toFloat(), animationSpec = tween(320, easing = FastOutSlowInEasing))
                            }
                        } else {
                            listState.scrollToItem(targetIndex)
                            delay(20)
                            val postItem = listState.layoutInfo.visibleItemsInfo.find { it.index == targetIndex }
                            if (postItem != null) {
                                val itemCenterY = postItem.offset + postItem.size / 2 + listState.layoutInfo.beforeContentPadding
                                val diff = itemCenterY - targetCenterY
                                if (kotlin.math.abs(diff) > 2) {
                                    listState.animateScrollBy(diff.toFloat(), animationSpec = tween(200))
                                }
                            }
                        }
                    }
                }

                // 2. ASR 实时语音字幕自动平滑居中滚动 (正在识别说出的句子始终保持在屏幕正中央)
                val activeAsrIndex = if (currentSentence.isNotBlank()) {
                    (subtitleState.subtitles.size + streamClauses.size - 1).coerceAtLeast(0)
                } else if (subtitleState.subtitles.isNotEmpty()) {
                    subtitleState.subtitles.lastIndex
                } else {
                    -1
                }

                LaunchedEffect(activeAsrIndex, isSongMode, fontSizeOption, isLandscape, streamClauses.size) {
                    if (!isSongMode && activeAsrIndex >= 0) {
                        delay(20)
                        val viewportHeightPx = listState.layoutInfo.viewportSize.height
                        val targetCenterY = viewportHeightPx / 2
                        val beforePadding = listState.layoutInfo.beforeContentPadding
                        val visibleItem = listState.layoutInfo.visibleItemsInfo.find { it.index == activeAsrIndex }
                        if (visibleItem != null) {
                            val itemCenterY = visibleItem.offset + visibleItem.size / 2 + beforePadding
                            val diff = itemCenterY - targetCenterY
                            if (kotlin.math.abs(diff) > 4) {
                                listState.animateScrollBy(diff.toFloat(), animationSpec = tween(280, easing = FastOutSlowInEasing))
                            }
                        } else {
                            listState.scrollToItem(activeAsrIndex)
                            delay(20)
                            val postItem = listState.layoutInfo.visibleItemsInfo.find { it.index == activeAsrIndex }
                            if (postItem != null) {
                                val itemCenterY = postItem.offset + postItem.size / 2 + listState.layoutInfo.beforeContentPadding
                                val diff = itemCenterY - targetCenterY
                                if (kotlin.math.abs(diff) > 4) {
                                    listState.animateScrollBy(diff.toFloat(), animationSpec = tween(180))
                                }
                            }
                        }
                    }
                }

                // 3. ASR 长句流式更新时（跨行膨胀）微调居中
                LaunchedEffect(currentSentence.length / 10) {
                    if (!isSongMode && activeAsrIndex >= 0 && currentSentence.isNotBlank()) {
                        delay(20)
                        val viewportHeightPx = listState.layoutInfo.viewportSize.height
                        val targetCenterY = viewportHeightPx / 2
                        val beforePadding = listState.layoutInfo.beforeContentPadding
                        val visibleItem = listState.layoutInfo.visibleItemsInfo.find { it.index == activeAsrIndex }
                        if (visibleItem != null) {
                            val itemCenterY = visibleItem.offset + visibleItem.size / 2 + beforePadding
                            val diff = itemCenterY - targetCenterY
                            if (kotlin.math.abs(diff) > 8) {
                                listState.animateScrollBy(diff.toFloat(), animationSpec = tween(180))
                            }
                        }
                    }
                }

                if (isSongMode) {
                    // ================= 歌曲同步歌词视图 (LRC) =================
                    val isRecognizing = recognitionState is RecognitionState.Sampling ||
                            recognitionState is RecognitionState.Fingerprinting ||
                            recognitionState is RecognitionState.QueryingCloud

                    if (isRecognizing || songLyricState.isLoading) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(36.dp),
                                color = activeAccentColor,
                                strokeWidth = 2.5.dp
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = if (songLyricState.isLoading) "正在匹配《${songLyricState.song?.title ?: "歌曲"}》时间轴歌词..." else "正在听歌识曲识别当前广播...",
                                color = emptyTitleColor,
                                fontSize = if (isLandscape) 13.sp else 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "电台音频流采样匹配中，请稍候",
                                color = emptySubColor,
                                fontSize = 11.5.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else if (songLyricState.lyrics.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.MusicNote,
                                contentDescription = null,
                                tint = activeAccentColor.copy(alpha = 0.6f),
                                modifier = Modifier.size(38.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = songLyricState.errorMessage ?: "未识别到当前歌曲歌词",
                                color = emptyTitleColor,
                                fontSize = if (isLandscape) 13.sp else 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "电台可能处于主持人口播、新闻或广告中",
                                color = emptySubColor,
                                fontSize = 11.5.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    onClick = onTriggerRecognition,
                                    shape = RoundedCornerShape(999.dp),
                                    color = activeAccentColor.copy(alpha = 0.18f),
                                    border = BorderStroke(1.dp, activeAccentColor.copy(alpha = 0.6f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Outlined.MusicNote, null, tint = activeAccentColor, modifier = Modifier.size(13.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = "开始听歌识曲",
                                            color = activeAccentColor,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Surface(
                                    onClick = { onSwitchDisplayMode(LyricsDisplayMode.AI_SUBTITLE) },
                                    shape = RoundedCornerShape(999.dp),
                                    color = pillBgColor,
                                    border = BorderStroke(0.6.dp, pillBorderColor)
                                ) {
                                    Text(
                                        text = "切回语音转写",
                                        color = pillTextColor,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    detectTapGestures { onCloseLyrics() }
                                },
                            contentPadding = PaddingValues(
                                top = verticalCenterPadding,
                                bottom = verticalCenterPadding,
                                start = if (isLandscape) 14.dp else 20.dp,
                                end = if (isLandscape) 14.dp else 20.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(lineSpacing)
                        ) {
                            itemsIndexed(
                                items = songLyricState.lyrics,
                                key = { idx, line -> "lrc_${line.timeMs}_$idx" }
                            ) { index, line ->
                                val isActiveLine = index == songLyricState.currentLineIndex

                                if (isActiveLine) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable(interactionSource = interactionSource, indication = null) { onCloseLyrics() }
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(3.5.dp)
                                                .height(if (isLandscape) 18.dp else 24.dp)
                                                .clip(RoundedCornerShape(999.dp))
                                                .background(activeAccentColor.copy(alpha = pulseAlpha))
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = line.text,
                                                color = activeTextColor,
                                                fontSize = activeFontSize,
                                                lineHeight = activeLineHeight,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Start
                                            )
                                            if (!line.translation.isNullOrBlank()) {
                                                Spacer(modifier = Modifier.height(3.dp))
                                                Text(
                                                    text = line.translation,
                                                    color = activeTextColor.copy(alpha = 0.75f),
                                                    fontSize = historyFontSize,
                                                    lineHeight = historyLineHeight,
                                                    fontWeight = FontWeight.Normal,
                                                    textAlign = TextAlign.Start
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable(interactionSource = interactionSource, indication = null) { onCloseLyrics() }
                                    ) {
                                        Text(
                                            text = line.text,
                                            color = historyTextColor,
                                            fontSize = historyFontSize,
                                            lineHeight = historyLineHeight,
                                            fontWeight = FontWeight.Normal,
                                            textAlign = TextAlign.Start
                                        )
                                        if (!line.translation.isNullOrBlank()) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = line.translation,
                                                color = historyTextColor.copy(alpha = 0.60f),
                                                fontSize = (historyFontSize.value - 2).coerceAtLeast(10f).sp,
                                                lineHeight = historyLineHeight,
                                                fontWeight = FontWeight.Normal,
                                                textAlign = TextAlign.Start
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // ================= 实时语音转写视图 (ASR) =================
                    if (subtitleState.subtitles.isEmpty() && currentSentence.isBlank()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.GraphicEq,
                                contentDescription = null,
                                tint = activeAccentColor.copy(alpha = pulseAlpha),
                                modifier = Modifier.size(if (isLandscape) 28.dp else 42.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "正在实时伴听电台直播...",
                                color = emptyTitleColor,
                                fontSize = if (isLandscape) 14.sp else 17.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "主播口播、音乐歌词或新闻访谈将逐行居中滚动呈现",
                                color = emptySubColor,
                                fontSize = if (isLandscape) 11.sp else 12.5.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Surface(
                                onClick = onCloseLyrics,
                                shape = RoundedCornerShape(999.dp),
                                color = pillBgColor,
                                border = BorderStroke(0.5.dp, pillBorderColor)
                            ) {
                                Text(
                                    text = "点击空白处随时返回封面",
                                    color = emptySubColor,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    detectTapGestures { onCloseLyrics() }
                                },
                            contentPadding = PaddingValues(
                                top = verticalCenterPadding,
                                bottom = verticalCenterPadding,
                                start = if (isLandscape) 12.dp else 18.dp,
                                end = if (isLandscape) 12.dp else 18.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(lineSpacing)
                        ) {
                            // 1. 历史句子行
                            itemsIndexed(
                                items = subtitleState.subtitles,
                                key = { _, item -> item.id }
                            ) { index, item ->
                                val isLatestHistory = index == subtitleState.subtitles.lastIndex && currentSentence.isBlank()

                                if (isLatestHistory) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable(interactionSource = interactionSource, indication = null) { onCloseLyrics() }
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(3.5.dp)
                                                .height(if (isLandscape) 16.dp else 22.dp)
                                                .clip(RoundedCornerShape(999.dp))
                                                .background(activeAccentColor.copy(alpha = pulseAlpha))
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = item.text.trimEnd('，', '、', ',', ' '),
                                            color = activeTextColor,
                                            fontSize = activeFontSize,
                                            lineHeight = activeLineHeight,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Start,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                } else {
                                    Text(
                                        text = item.text.trimEnd('，', '、', ',', ' '),
                                        color = historyTextColor,
                                        fontSize = historyFontSize,
                                        lineHeight = historyLineHeight,
                                        fontWeight = FontWeight.Normal,
                                        textAlign = TextAlign.Start,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable(interactionSource = interactionSource, indication = null) { onCloseLyrics() }
                                    )
                                }
                            }

                            // 2. 当前流式正在识别中的句子
                            if (currentSentence.isNotBlank()) {
                                if (streamClauses.size > 1) {
                                    itemsIndexed(
                                        items = streamClauses.subList(0, streamClauses.size - 1),
                                        key = { idx, _ -> "stream_sub_$idx" }
                                    ) { _, clause ->
                                        Text(
                                            text = clause.trimEnd('，', '、', ',', ' '),
                                            color = historyTextColor,
                                            fontSize = historyFontSize,
                                            lineHeight = historyLineHeight,
                                            fontWeight = FontWeight.Normal,
                                            textAlign = TextAlign.Start,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable(interactionSource = interactionSource, indication = null) { onCloseLyrics() }
                                        )
                                    }
                                }

                                val activeText = (streamClauses.lastOrNull() ?: currentSentence).trimEnd('，', '、', ',', ' ')
                                item(key = "stream_active_clause") {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable(interactionSource = interactionSource, indication = null) { onCloseLyrics() }
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(3.5.dp)
                                                .height(if (isLandscape) 16.dp else 22.dp)
                                                .clip(RoundedCornerShape(999.dp))
                                                .background(activeAccentColor.copy(alpha = pulseAlpha))
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = activeText,
                                            color = activeTextColor,
                                            fontSize = activeFontSize,
                                            lineHeight = activeLineHeight,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Start,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
