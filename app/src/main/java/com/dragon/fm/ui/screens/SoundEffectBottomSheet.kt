package com.dragon.fm.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Equalizer
import androidx.compose.material.icons.outlined.Festival
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Radio
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Speaker
import androidx.compose.material.icons.outlined.SurroundSound
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dragon.fm.player.SoundEffectMode
import com.dragon.fm.ui.theme.DragonCyan
import com.dragon.fm.ui.theme.DragonTheme

/**
 * 网易云音乐级电台声学音效选择弹窗
 * 支持 7 大主流电台场景音效预设，实时硬件级生效并记忆保存
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoundEffectBottomSheet(
    currentMode: SoundEffectMode,
    onSelectMode: (SoundEffectMode) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val theme = DragonTheme
    val isDark = theme.isDark

    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val waveScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wave"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = if (isDark) Color(0xFF11141D) else Color(0xFFFFFFFF),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(width = 38.dp, height = 4.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(theme.textSecondary.copy(alpha = 0.25f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // 1. 顶部标题栏与关闭键
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(theme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Equalizer,
                            contentDescription = "音效",
                            tint = theme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "声音实验室 · 场景音效",
                            color = theme.textPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "7 种硬件级场景声场调校，专为广播流深度定制",
                            color = theme.textSecondary,
                            fontSize = 11.5.sp
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "关闭",
                        tint = theme.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // 2. 音效卡片列表
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(SoundEffectMode.entries, key = { it.id }) { mode ->
                    val isSelected = mode == currentMode

                    val itemIcon: ImageVector = when (mode) {
                        SoundEffectMode.ORIGINAL -> Icons.Outlined.GraphicEq
                        SoundEffectMode.SPATIAL_3D -> Icons.Outlined.SurroundSound
                        SoundEffectMode.CLEAR_VOCAL -> Icons.Outlined.RecordVoiceOver
                        SoundEffectMode.DEEP_BASS -> Icons.Outlined.Speaker
                        SoundEffectMode.VINTAGE_TUBE -> Icons.Outlined.Radio
                        SoundEffectMode.LIVE_CONCERT -> Icons.Outlined.Festival
                        SoundEffectMode.CAR_COCKPIT -> Icons.Outlined.DirectionsCar
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = when {
                            isSelected && isDark -> Color(0xFF1B2332)
                            isSelected && !isDark -> theme.primary.copy(alpha = 0.08f)
                            isDark -> Color(0xFF161A24)
                            else -> Color(0xFFF8FAFC)
                        },
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = when {
                                isSelected -> theme.primary
                                isDark -> Color(0xFF232B3E)
                                else -> Color(0xFFE2E8F0)
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                onSelectMode(mode)
                                Toast.makeText(context, "已开启「${mode.title}」音效", Toast.LENGTH_SHORT).show()
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 模式图标
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) theme.primary.copy(alpha = 0.20f)
                                        else if (isDark) Color(0xFF222838) else Color(0xFFE2E8F0)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = itemIcon,
                                    contentDescription = mode.title,
                                    tint = if (isSelected) theme.primary else theme.textSecondary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            // 标题 + 标签 + 描述
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = mode.title,
                                        color = if (isSelected) theme.primary else theme.textPrimary,
                                        fontSize = 15.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (isSelected) theme.primary.copy(alpha = 0.16f) else theme.surfaceVariant,
                                        border = BorderStroke(
                                            0.5.dp,
                                            if (isSelected) theme.primary.copy(alpha = 0.5f) else theme.glassBorder
                                        )
                                    ) {
                                        Text(
                                            text = mode.tag,
                                            color = if (isSelected) theme.primary else theme.textSecondary,
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(3.dp))

                                Text(
                                    text = mode.subtitle,
                                    color = if (isSelected) theme.textPrimary.copy(alpha = 0.85f) else theme.textSecondary,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            // 右侧选中状态圆标
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(theme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = Color.White,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(if (isDark) Color(0xFF222838) else Color(0xFFE2E8F0))
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
