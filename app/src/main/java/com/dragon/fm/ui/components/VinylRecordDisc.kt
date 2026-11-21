package com.dragon.fm.ui.components

import android.graphics.drawable.Drawable
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
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
 * 参考 LynMusic 现代播放器黑胶唱片质感组件 (Vinyl Record Disc)
 *
 * 核心设计：
 * 1. 真实黑胶唱盘：深黑底色盘基 + 多圈细密微刻同心纹理 (Grooves)；
 * 2. 真实旋转光斑：双侧 45°/225° 扇形光影渐变 (Sweep Specular Reflection)；
 * 3. 中心唱片标签 (Center Label)：按唱片黄金比例 (58%) 嵌入圆形电台封面；
 * 4. 真实唱片轴心孔 (Spindle Hole)：中心金属银环 + 穿孔深黑核心；
 * 5. 播放律动：电台播放中平滑旋转 (24s/圈)，暂停时平滑静止；
 * 6. 交互手势：轻触直接进入歌词视图；双击一键红心收藏并迸发动画。
 */
@Composable
fun VinylRecordDisc(
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

    // 累加旋转动画：播放时持续转动（24秒一圈），暂停时平滑定格
    val discRotation = remember { Animatable(0f) }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            discRotation.animateTo(
                targetValue = discRotation.value + 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(24000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                )
            )
        } else {
            discRotation.stop()
        }
    }

    Box(
        modifier = modifier
            .size(coverDimension)
            .shadow(
                elevation = 20.dp,
                shape = CircleShape,
                ambientColor = stationAmbient.copy(alpha = 0.45f),
                spotColor = Color.Black.copy(alpha = 0.70f)
            )
            .clip(CircleShape)
            .pointerInput(station.id) {
                detectTapGestures(
                    onTap = { onTap() },
                    onDoubleTap = { onDoubleTap() }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // 旋转主体：包括黑胶唱片盘基、光斑纹理、同心音轨、中心封面和轴心孔
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationZ = discRotation.value % 360f
                },
            contentAlignment = Alignment.Center
        ) {
            // 1. 黑胶唱片盘基与微刻同心细纹
            Canvas(modifier = Modifier.fillMaxSize()) {
                val radius = size.minDimension / 2f
                val center = Offset(size.width / 2f, size.height / 2f)

                // 盘基深黑微渐变底色
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF202024),
                            Color(0xFF141416),
                            Color(0xFF0C0C0E)
                        ),
                        center = center,
                        radius = radius
                    ),
                    radius = radius,
                    center = center
                )

                // 黑胶唱片旋转光影（对角双侧 45°/225° 哑光扇形高光反光，LynMusic 标志性黑胶质感）
                drawCircle(
                    brush = Brush.sweepGradient(
                        0.00f to Color.Transparent,
                        0.12f to Color.White.copy(alpha = 0.07f),
                        0.25f to Color.Transparent,
                        0.62f to Color.White.copy(alpha = 0.07f),
                        0.75f to Color.Transparent,
                        1.00f to Color.Transparent,
                        center = center
                    ),
                    radius = radius,
                    center = center
                )

                // 同心唱片音轨纹理微刻线（Groove Rings）
                val grooveFractions = floatArrayOf(
                    0.96f, 0.93f, 0.90f, 0.87f, 0.84f, 0.81f,
                    0.78f, 0.75f, 0.72f, 0.69f, 0.66f, 0.63f
                )
                for (frac in grooveFractions) {
                    val ringRadius = radius * frac
                    drawCircle(
                        color = Color.White.copy(alpha = 0.045f),
                        radius = ringRadius,
                        center = center,
                        style = Stroke(width = 0.8.dp.toPx())
                    )
                }

                // 唱片外边缘微光环
                drawCircle(
                    color = Color.White.copy(alpha = 0.12f),
                    radius = radius - 0.5.dp.toPx(),
                    center = center,
                    style = Stroke(width = 1.dp.toPx())
                )
            }

            // 2. 唱片中心标签封面 (Center Album Label，约占盘体 58% 黄金比例)
            val labelSize = coverDimension * 0.58f
            Box(
                modifier = Modifier
                    .size(labelSize)
                    .clip(CircleShape)
                    .background(DragonTheme.surface)
                    .border(
                        width = 2.dp,
                        color = Color(0xFF1B1B1E),
                        shape = CircleShape
                    ),
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
                            modifier = Modifier.size(labelSize * 0.7f)
                        )
                    }
                }
            }

            // 3. 唱片中心穿孔与金属环 (Spindle Hole)
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE4E4E8))
                    .border(0.8.dp, Color(0xFF9E9EA4), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0D0D0F))
                )
            }
        }
    }
}
