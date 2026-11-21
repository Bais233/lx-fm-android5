package com.dragon.fm.ui.components

import android.graphics.drawable.Drawable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.dragon.fm.R
import com.dragon.fm.data.model.RadioStation
import com.dragon.fm.ui.theme.DragonTheme

/**
 * 经典四方圆角大图封面展示组件 (Square Album Cover)
 *
 * 特性：
 * 1. 大尺寸四方圆角卡片，配备流光环境氛围光晕（Halo Aura）；
 * 2. 播放中根据电台主调环境色产生柔和呼吸脉冲动效；
 * 3. 轻触进入全屏歌词流；双击快速红心收藏电台。
 */
@Composable
fun SquareAlbumCover(
    station: RadioStation,
    coverDimension: Dp,
    isPlaying: Boolean,
    stationAmbient: Color,
    modifier: Modifier = Modifier,
    onTap: () -> Unit,
    onDoubleTap: () -> Unit,
    onCoverLoaded: (Drawable) -> Unit = {}
) {
    val isDark = DragonTheme.isDark

    val haloPulse by if (isPlaying) {
        val haloTransition = rememberInfiniteTransition(label = "squareHalo")
        haloTransition.animateFloat(
            initialValue = 0.85f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(2200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse"
        )
    } else {
        remember { mutableStateOf(0.45f) }
    }

    Box(
        modifier = modifier.size(coverDimension),
        contentAlignment = Alignment.Center
    ) {
        // 1. 背景流光氛围光环 (Ambient Halo)
        Box(
            modifier = Modifier
                .size(coverDimension + 36.dp)
                .graphicsLayer {
                    alpha = if (isPlaying) haloPulse else 0.35f
                }
                .drawBehind {
                    drawRoundRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                stationAmbient.copy(alpha = if (isDark) 0.52f else 0.38f),
                                stationAmbient.copy(alpha = if (isDark) 0.22f else 0.14f),
                                Color.Transparent
                            ),
                            radius = size.width * 0.65f
                        ),
                        cornerRadius = CornerRadius(36.dp.toPx(), 36.dp.toPx())
                    )
                }
        )

        // 2. 主封面主体卡片
        Box(
            modifier = Modifier
                .fillMaxSize()
                .shadow(
                    elevation = 18.dp,
                    shape = RoundedCornerShape(22.dp),
                    ambientColor = stationAmbient.copy(alpha = 0.50f),
                    spotColor = Color.Black.copy(alpha = 0.65f)
                )
                .clip(RoundedCornerShape(22.dp))
                .background(DragonTheme.surface)
                .border(
                    width = 0.8.dp,
                    color = if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(22.dp)
                )
                .pointerInput(station.id) {
                    detectTapGestures(
                        onTap = { onTap() },
                        onDoubleTap = { onDoubleTap() }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            if (!station.favicon.isNullOrBlank()) {
                AsyncImage(
                    model = station.favicon,
                    contentDescription = station.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    onSuccess = { state ->
                        onCoverLoaded(state.result.drawable)
                    }
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    stationAmbient.copy(alpha = 0.40f),
                                    DragonTheme.surface
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.app_icon),
                        contentDescription = "App Icon",
                        tint = Color.Unspecified,
                        modifier = Modifier.size(coverDimension * 0.55f)
                    )
                }
            }
        }
    }
}
