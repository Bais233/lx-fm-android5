package com.dragon.fm.ui.components

import android.content.Context
import android.os.SystemClock
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dragon.fm.ui.theme.DragonCyan
import com.dragon.fm.ui.theme.DragonTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlin.math.cos
import kotlin.math.sin

enum class RhythmVisualizerMode(val key: String, val label: String) {
    SPECTRUM("spectrum", "均衡频谱"),
    WAVE("wave", "流体波浪"),
    HYBRID("hybrid", "复合极光"),
    PULSE("pulse", "声学光环");

    fun next(): RhythmVisualizerMode {
        val entries = entries
        return entries[(ordinal + 1) % entries.size]
    }

    companion object {
        fun fromKey(key: String): RhythmVisualizerMode {
            return entries.find { it.key == key } ?: SPECTRUM
        }
    }
}

/**
 * LX FM Signature WaveFlow Dynamic Audio Visualizer
 *
 * 核心升级：
 * 接入 ExoPlayer 纯数字 PCM 实时 512 点 FFT 频谱分析数据（spectrumData & peakData）
 * 1. 均衡频谱：24 频段柱状图精确跟随广播音乐实际频率跳动（超重低音、人声、高音分频响应）；
 * 2. 独立高光峰值保持线 (Peak Hold)，真实反映瞬态电平；
 * 3. 流体波浪与声学光环：波幅与光环大小根据真实低频/中频能量实时膨胀与收缩。
 */
@Composable
fun WaveFlowRhythmVisualizer(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    spectrumState: StateFlow<FloatArray>? = null,
    peakState: StateFlow<FloatArray>? = null,
    spectrumData: FloatArray? = null,
    peakData: FloatArray? = null,
    primaryColor: Color = DragonTheme.primary,
    secondaryColor: Color = DragonCyan,
    showModeSelector: Boolean = false
) {
    val collectedSpectrum = spectrumState?.collectAsState()?.value ?: spectrumData
    val collectedPeaks = peakState?.collectAsState()?.value ?: peakData

    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("dragon_fm_prefs", Context.MODE_PRIVATE) }
    var currentMode by remember {
        mutableStateOf(RhythmVisualizerMode.fromKey(prefs.getString("lxfm_rhythm_mode", RhythmVisualizerMode.SPECTRUM.key) ?: "spectrum"))
    }

    var animClock by remember { mutableFloatStateOf(0f) }

    // 辅助流畅时间轴（用于流体波浪相位流动）
    LaunchedEffect(isPlaying) {
        var lastTime = SystemClock.uptimeMillis()
        while (isActive) {
            val now = SystemClock.uptimeMillis()
            val dt = ((now - lastTime).coerceIn(1L, 100L)) / 1000f
            lastTime = now
            val speed = if (isPlaying) 2.2f else 0.5f
            animClock = (animClock + dt * speed) % 6283.18f
            delay(16) // ~60fps
        }
    }

    val density = LocalDensity.current

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable {
                val next = currentMode.next()
                currentMode = next
                prefs.edit().putString("lxfm_rhythm_mode", next.key).apply()
                Toast.makeText(context, "律动模式: ${next.label}", Toast.LENGTH_SHORT).show()
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            if (w <= 0f || h <= 0f) return@Canvas

            when (currentMode) {
                RhythmVisualizerMode.SPECTRUM -> {
                    drawPrecisionSpectrum(
                        w = w,
                        h = h,
                        time = animClock,
                        isPlaying = isPlaying,
                        spectrumData = collectedSpectrum,
                        peakData = collectedPeaks,
                        primaryColor = primaryColor,
                        secondaryColor = secondaryColor,
                        isHybrid = false
                    )
                }
                RhythmVisualizerMode.WAVE -> {
                    drawFluidWaves(
                        w = w,
                        h = h,
                        time = animClock,
                        isPlaying = isPlaying,
                        spectrumData = collectedSpectrum,
                        primaryColor = primaryColor,
                        secondaryColor = secondaryColor
                    )
                }
                RhythmVisualizerMode.HYBRID -> {
                    drawFluidWaves(
                        w = w,
                        h = h,
                        time = animClock,
                        isPlaying = isPlaying,
                        spectrumData = collectedSpectrum,
                        primaryColor = primaryColor,
                        secondaryColor = secondaryColor,
                        alphaScale = 0.50f
                    )
                    drawPrecisionSpectrum(
                        w = w,
                        h = h,
                        time = animClock,
                        isPlaying = isPlaying,
                        spectrumData = collectedSpectrum,
                        peakData = collectedPeaks,
                        primaryColor = primaryColor,
                        secondaryColor = secondaryColor,
                        isHybrid = true
                    )
                }
                RhythmVisualizerMode.PULSE -> {
                    drawAcousticPulse(
                        w = w,
                        h = h,
                        time = animClock,
                        isPlaying = isPlaying,
                        spectrumData = collectedSpectrum,
                        primaryColor = primaryColor,
                        secondaryColor = secondaryColor
                    )
                }
            }
        }

        if (showModeSelector) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 4.dp, top = 2.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.22f))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Text(
                    text = currentMode.label,
                    fontSize = 8.5.sp,
                    color = secondaryColor.copy(alpha = 0.85f),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// 1. Fluid Wave Dynamics (Real Audio Energy Modulated)
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawFluidWaves(
    w: Float,
    h: Float,
    time: Float,
    isPlaying: Boolean,
    spectrumData: FloatArray?,
    primaryColor: Color,
    secondaryColor: Color,
    alphaScale: Float = 1.0f
) {
    val midY = h * 0.58f

    // 从真实音频提取低频（鼓点能量）与中频（人声厚度）
    val bassEnergy = if (isPlaying && spectrumData != null && spectrumData.isNotEmpty()) {
        var sum = 0f
        val count = minOf(6, spectrumData.size)
        for (i in 0 until count) sum += spectrumData[i]
        (sum / count).coerceIn(0.1f, 1.0f)
    } else {
        if (isPlaying) 0.35f else 0.12f
    }

    val midEnergy = if (isPlaying && spectrumData != null && spectrumData.size > 12) {
        var sum = 0f
        for (i in 6 until 14) sum += spectrumData[i]
        (sum / 8).coerceIn(0.1f, 1.0f)
    } else {
        if (isPlaying) 0.30f else 0.10f
    }

    val playFactor = if (isPlaying) 1.0f else 0.22f

    // Layer 1: Ambient filled gradient wave
    val wavePath = Path()
    wavePath.moveTo(0f, h)

    val stepX = 4f
    var x = 0f
    while (x <= w) {
        val p = x / w
        val harmonic1 = sin(p * 6.0f + time * 2.6f) * (h * 0.32f * bassEnergy)
        val harmonic2 = cos(p * 12.0f - time * 1.7f) * (h * 0.20f * midEnergy)
        val y = midY - (harmonic1 + harmonic2) * playFactor
        wavePath.lineTo(x, y.coerceIn(2f, h - 2f))
        x += stepX
    }
    wavePath.lineTo(w, h)
    wavePath.close()

    drawPath(
        path = wavePath,
        brush = Brush.verticalGradient(
            colors = listOf(
                secondaryColor.copy(alpha = 0.40f * alphaScale * (0.6f + bassEnergy * 0.4f)),
                primaryColor.copy(alpha = 0.18f * alphaScale),
                Color.Transparent
            ),
            startY = 0f,
            endY = h
        )
    )

    // Layer 2: Glowing Crest Stroke
    val crestPath = Path()
    x = 0f
    var isFirst = true
    while (x <= w) {
        val p = x / w
        val harmonic1 = sin(p * 8.0f + time * 3.2f) * (h * 0.28f * bassEnergy)
        val harmonic2 = cos(p * 15.0f - time * 2.0f) * (h * 0.18f * midEnergy)
        val y = (midY - (harmonic1 + harmonic2) * playFactor).coerceIn(2f, h - 2f)
        if (isFirst) {
            crestPath.moveTo(x, y)
            isFirst = false
        } else {
            crestPath.lineTo(x, y)
        }
        x += stepX
    }

    drawPath(
        path = crestPath,
        brush = Brush.horizontalGradient(
            colors = listOf(
                secondaryColor.copy(alpha = 0.90f * alphaScale),
                primaryColor.copy(alpha = 0.98f * alphaScale),
                secondaryColor.copy(alpha = 0.90f * alphaScale)
            )
        ),
        style = Stroke(width = 2.5.dp.toPx())
    )
}

// 2. Real Audio Frequency Spectrum Engine with Peak Hold Dots
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPrecisionSpectrum(
    w: Float,
    h: Float,
    time: Float,
    isPlaying: Boolean,
    spectrumData: FloatArray?,
    peakData: FloatArray?,
    primaryColor: Color,
    secondaryColor: Color,
    isHybrid: Boolean
) {
    val barCount = if (spectrumData != null && spectrumData.isNotEmpty()) spectrumData.size else 24
    val barWidth = (w / barCount) * (if (isHybrid) 0.50f else 0.65f)
    val gap = (w / barCount) - barWidth

    val brush = Brush.verticalGradient(
        colors = listOf(
            secondaryColor.copy(alpha = if (isHybrid) 0.95f else 1.0f),
            primaryColor.copy(alpha = if (isHybrid) 0.70f else 0.85f)
        ),
        startY = 0f,
        endY = h
    )

    val minBarHeight = 3.dp.toPx()

    for (i in 0 until barCount) {
        // 从真实频谱数据获取该频段能量
        val rawHeight: Float = if (spectrumData != null && i < spectrumData.size && isPlaying) {
            spectrumData[i].coerceIn(0f, 1f)
        } else if (isPlaying) {
            // 兜底微弱环境微波
            (sin(time * 3f + i * 0.5f) * 0.08f + 0.10f).coerceIn(0.04f, 0.25f)
        } else {
            0f
        }

        val availableHeight = h * (if (isHybrid) 0.76f else 0.92f)
        val barHeight = (rawHeight * availableHeight).coerceIn(minBarHeight, h)

        val x = i * (barWidth + gap) + gap / 2f
        val y = h - barHeight

        drawRoundRect(
            brush = brush,
            topLeft = Offset(x, y),
            size = Size(barWidth, barHeight),
            cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
        )

        // Peak Hold Falling Dot (真实峰值电平保持高光点)
        val peakVal = if (peakData != null && i < peakData.size && isPlaying) {
            peakData[i].coerceIn(0f, 1f)
        } else {
            rawHeight
        }

        if (isPlaying && peakVal > 0.08f) {
            val peakY = (h - (peakVal * availableHeight) - 3.5.dp.toPx()).coerceAtLeast(1f)
            drawRoundRect(
                color = Color.White.copy(alpha = 0.95f),
                topLeft = Offset(x, peakY),
                size = Size(barWidth, 2.dp.toPx()),
                cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
            )
        }
    }
}

// 3. Acoustic Concentric Pulse Halos (Modulated by Real Bass Kick)
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawAcousticPulse(
    w: Float,
    h: Float,
    time: Float,
    isPlaying: Boolean,
    spectrumData: FloatArray?,
    primaryColor: Color,
    secondaryColor: Color
) {
    val center = Offset(w / 2f, h / 2f)
    val maxRadius = minOf(w, h) * 0.48f

    val bassKick = if (isPlaying && spectrumData != null && spectrumData.isNotEmpty()) {
        var sum = 0f
        val count = minOf(4, spectrumData.size)
        for (i in 0 until count) sum += spectrumData[i]
        (sum / count).coerceIn(0.1f, 1.0f)
    } else {
        if (isPlaying) 0.4f else 0.15f
    }

    val playFactor = if (isPlaying) (0.6f + bassKick * 0.6f) else 0.2f

    val ringCount = 3
    for (i in 0 until ringCount) {
        val ringPhase = (time * (1.5f + bassKick * 0.8f) + i * (6.283f / ringCount)) % 6.283f
        val progress = (ringPhase / 6.283f)
        val radius = maxRadius * progress * playFactor
        val alpha = (1f - progress) * 0.50f * (if (isPlaying) 1f else 0.3f)

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    secondaryColor.copy(alpha = alpha * 0.6f),
                    primaryColor.copy(alpha = alpha),
                    Color.Transparent
                ),
                center = center,
                radius = (radius + 12f).coerceAtLeast(1f)
            ),
            center = center,
            radius = radius.coerceAtLeast(2f),
            style = Stroke(width = (2.dp + (bassKick * 1.5f).dp).toPx())
        )
    }
}
