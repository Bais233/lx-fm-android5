package com.dragon.fm.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dragon.fm.player.AsrModelState
import com.dragon.fm.player.AsrModelType
import com.dragon.fm.ui.theme.DragonCyan
import com.dragon.fm.ui.theme.DragonGold
import com.dragon.fm.ui.theme.DragonTheme
import com.dragon.fm.ui.theme.LiveRed

/**
 * 方案 A 离线语音识别模型按需下载与准备弹窗
 * 提供【极速精简中文版（默认推荐 29MB）】与【中英混合双语版（190MB）】两种选择。
 * 保持初始安装包仅约 16MB 原生轻盈。
 */
@Composable
fun AsrModelDownloadDialog(
    modelState: AsrModelState,
    currentModelType: AsrModelType = AsrModelType.COMPACT_ZH,
    onDismiss: () -> Unit,
    onSelectModelType: (AsrModelType) -> Unit = {},
    onStartDownload: (AsrModelType) -> Unit,
    onCancelDownload: () -> Unit
) {
    val theme = DragonTheme
    val isDark = theme.isDark
    var selectedType by remember(currentModelType) { mutableStateOf(currentModelType) }

    Dialog(
        onDismissRequest = {
            if (modelState !is AsrModelState.Downloading) {
                onDismiss()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = modelState !is AsrModelState.Downloading,
            dismissOnClickOutside = modelState !is AsrModelState.Downloading,
            usePlatformDefaultWidth = false
        )
    ) {
        val configuration = androidx.compose.ui.platform.LocalConfiguration.current
        val screenHeight = configuration.screenHeightDp.dp
        val maxDialogHeight = (screenHeight - 64.dp).coerceAtLeast(360.dp)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = theme.surface,
                border = BorderStroke(1.dp, DragonTheme.glassBorder),
                shadowElevation = 16.dp,
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .heightIn(max = maxDialogHeight)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // 1. 固定头部 (永远在顶部，不随内容滚动)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(DragonCyan.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Subtitles,
                                    contentDescription = null,
                                    tint = DragonCyan,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "电台实时字幕",
                                    color = theme.textPrimary,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "端侧离线语音识别 (100% 本地运算)",
                                    color = theme.textSecondary,
                                    fontSize = 11.5.sp
                                )
                            }
                        }

                        if (modelState !is AsrModelState.Downloading) {
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "关闭",
                                    tint = theme.textSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // 2. 根据状态呈现内容
                    when (modelState) {
                        is AsrModelState.Downloading -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 16.dp)
                            ) {
                                val animatedProgress by animateFloatAsState(
                                    targetValue = modelState.progress,
                                    animationSpec = tween(300, easing = FastOutSlowInEasing),
                                    label = "DownloadProgress"
                                )

                                val downloadedMb = String.format("%.1f", modelState.downloadedBytes / (1024f * 1024f))
                                val totalMb = String.format("%.1f", modelState.totalBytes / (1024f * 1024f))
                                val percent = (animatedProgress * 100).toInt()

                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "正在下载 ${modelState.modelType.title}...",
                                        color = theme.textPrimary,
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "接入 ModelScope 阿里云官方高速镜像 · 纯本地运算",
                                        color = theme.textSecondary,
                                        fontSize = 11.5.sp
                                    )

                                    Spacer(modifier = Modifier.height(18.dp))

                                    LinearProgressIndicator(
                                        progress = { animatedProgress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                        color = DragonCyan,
                                        trackColor = DragonCyan.copy(alpha = 0.2f),
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "$downloadedMb MB / $totalMb MB",
                                            color = theme.textSecondary,
                                            fontSize = 11.5.sp
                                        )
                                        Text(
                                            text = "$percent%",
                                            color = DragonCyan,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(18.dp))

                                    OutlinedButton(
                                        onClick = onCancelDownload,
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(0.8.dp, theme.textSecondary.copy(alpha = 0.35f)),
                                        modifier = Modifier.fillMaxWidth(0.6f)
                                    ) {
                                        Text(
                                            text = "取消下载",
                                            color = theme.textSecondary,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }

                        is AsrModelState.Error -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = LiveRed,
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "下载遇到问题",
                                    color = LiveRed,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = modelState.message,
                                    color = theme.textSecondary,
                                    fontSize = 12.sp
                                )

                                Spacer(modifier = Modifier.height(18.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = onDismiss,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("取消", color = theme.textSecondary)
                                    }

                                    Button(
                                        onClick = { onStartDownload(selectedType) },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = DragonCyan),
                                        modifier = Modifier.weight(1.2f)
                                    ) {
                                        Text("重试下载", color = Color.Black, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        else -> { // NotDownloaded or Ready: 呈现模型列表与固定操作按钮
                            val isReady = modelState is AsrModelState.Ready && modelState.modelType == selectedType

                            // 可滚动内容区域（模型选项列表 + 特性说明卡片）
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f, fill = false)
                                    .verticalScroll(rememberScrollState())
                                    .padding(horizontal = 20.dp)
                            ) {
                                Text(
                                    text = "选择离线语音识别模型",
                                    color = theme.textPrimary,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    AsrModelType.entries.forEach { modelType ->
                                        val isSelected = selectedType == modelType
                                        val badgeColor = when (modelType) {
                                            AsrModelType.COMPACT_ZH -> DragonGold
                                            AsrModelType.BILINGUAL_ZH_EN -> DragonCyan
                                        }

                                        val isThisModelReady = modelState is AsrModelState.Ready && modelState.modelType == modelType

                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isSelected) DragonCyan.copy(alpha = 0.12f) else theme.glassSurface,
                                            border = BorderStroke(
                                                width = if (isSelected) 1.5.dp else 0.8.dp,
                                                color = if (isSelected) DragonCyan else theme.glassBorder
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable {
                                                    selectedType = modelType
                                                    onSelectModelType(modelType)
                                                }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                RadioButton(
                                                    selected = isSelected,
                                                    onClick = {
                                                        selectedType = modelType
                                                        onSelectModelType(modelType)
                                                    },
                                                    colors = RadioButtonDefaults.colors(selectedColor = DragonCyan)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Text(
                                                            text = modelType.title,
                                                            color = theme.textPrimary,
                                                            fontSize = 13.5.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            softWrap = false,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis,
                                                            modifier = Modifier.weight(1f)
                                                        )
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Surface(
                                                            shape = RoundedCornerShape(5.dp),
                                                            color = if (isThisModelReady) DragonCyan.copy(alpha = 0.16f) else badgeColor.copy(alpha = 0.14f),
                                                            border = BorderStroke(0.5.dp, if (isThisModelReady) DragonCyan.copy(alpha = 0.45f) else badgeColor.copy(alpha = 0.45f))
                                                        ) {
                                                            val badgeText = when {
                                                                isThisModelReady -> "已就绪 · ${modelType.estimatedSizeMB} MB"
                                                                modelType == AsrModelType.COMPACT_ZH -> "推荐 · 约 29 MB"
                                                                modelType == AsrModelType.BILINGUAL_ZH_EN -> "双语 · 约 190 MB"
                                                                else -> "${modelType.estimatedSizeMB} MB"
                                                            }
                                                            Text(
                                                                text = badgeText,
                                                                color = if (isThisModelReady) DragonCyan else badgeColor,
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Medium,
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                                softWrap = false,
                                                                maxLines = 1
                                                            )
                                                        }
                                                    }
                                                    Spacer(modifier = Modifier.height(3.dp))
                                                    Text(
                                                        text = modelType.subtitle,
                                                        color = theme.textSecondary,
                                                        fontSize = 11.sp,
                                                        lineHeight = 15.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Feature Highlight Card
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = theme.glassSurface.copy(alpha = 0.4f),
                                    border = BorderStroke(0.5.dp, theme.glassBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = "• 接入魔搭 (ModelScope) 阿里国内官方高速源，下载极速稳定\n" +
                                                    "• 100% 本地运算，零云端 API 与流量消耗，隐私安全\n" +
                                                    "• 纯数字音频流抓取直接识别播音原声，无环境杂音\n" +
                                                    "• 首次下载自动配置当前芯片轻量核心（压缩仅约 1.6 MB）\n" +
                                                    "• 随时可在「设置 → 存储与缓存」中切换或清除",
                                            color = theme.textSecondary,
                                            fontSize = 10.5.sp,
                                            lineHeight = 15.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            // 3. 底部固定操作按钮（永远可见、不被裁剪）
                            HorizontalDivider(
                                color = theme.glassBorder.copy(alpha = 0.5f),
                                thickness = 0.5.dp
                            )
                            val configuration = androidx.compose.ui.platform.LocalConfiguration.current
                            val fontScale = androidx.compose.ui.platform.LocalDensity.current.fontScale
                            val isCompactScreen = configuration.screenHeightDp < 700 || configuration.screenWidthDp < 360
                            val btnVerticalPad = if (isCompactScreen) 8.dp else 12.dp
                            val btnTextSize = if (fontScale > 1.15f || isCompactScreen) 11.5.sp else 12.5.sp

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = btnVerticalPad),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = onDismiss,
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(0.8.dp, theme.textSecondary.copy(alpha = 0.4f)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "暂不使用",
                                        color = theme.textSecondary,
                                        fontSize = btnTextSize,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Button(
                                    onClick = {
                                        if (isReady) {
                                            onDismiss()
                                        } else {
                                            onStartDownload(selectedType)
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = DragonCyan
                                    ),
                                    modifier = Modifier.weight(1.4f)
                                ) {
                                    Icon(
                                        imageVector = if (isReady) Icons.Default.CheckCircle else Icons.Default.CloudDownload,
                                        contentDescription = null,
                                        tint = Color.Black,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isReady) "立即体验" else "立即下载 (${selectedType.estimatedSizeMB}MB)",
                                        color = Color.Black,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = btnTextSize,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
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
