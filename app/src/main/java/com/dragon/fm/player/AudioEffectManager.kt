package com.dragon.fm.player

import android.content.Context
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.PresetReverb
import android.media.audiofx.Virtualizer
import android.util.Log
import com.dragon.fm.DragonFmApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 录音棚/流行音乐平台级电台音效预设定义
 */
enum class SoundEffectMode(
    val id: String,
    val title: String,
    val shortTitle: String,
    val subtitle: String,
    val tag: String,
    val bassBoostStrength: Short = 0,         // 0 ~ 1000
    val virtualizerStrength: Short = 0,       // 0 ~ 1000
    val reverbPreset: Short = 0,              // PresetReverb preset
    val eqOffsetsMB: List<Short> = listOf(0, 0, 0, 0, 0), // 5 频段增益（毫贝 mB，100mB = 1dB）
    val loudnessTargetGainMB: Int = 0         // 响度动态驱动增强 (0 ~ 400 mB)
) {
    ORIGINAL(
        id = "original",
        title = "原声无损",
        shortTitle = "原声",
        subtitle = "还原电台录音棚纯正声音，真实自然，零音染",
        tag = "Hi-Fi 原汁原味",
        bassBoostStrength = 0,
        virtualizerStrength = 0,
        reverbPreset = PresetReverb.PRESET_NONE,
        eqOffsetsMB = listOf(0, 0, 0, 0, 0),
        loudnessTargetGainMB = 0
    ),
    SPATIAL_3D(
        id = "spatial_3d",
        title = "3D全景环绕",
        shortTitle = "3D全景",
        subtitle = "拓展立体声场边界，声音立体包裹，如临音乐盛宴",
        tag = "沉浸空间",
        bassBoostStrength = 500,
        virtualizerStrength = 1000,
        reverbPreset = PresetReverb.PRESET_SMALLROOM,
        eqOffsetsMB = listOf(400, 200, -300, 700, 1200),
        loudnessTargetGainMB = 180
    ),
    CLEAR_VOCAL(
        id = "clear_vocal",
        title = "清澈人声",
        shortTitle = "清澈人声",
        subtitle = "突出主持人中高频吐字，消除浑浊，听新闻、相声脱口秀绝佳",
        tag = "人声增强",
        bassBoostStrength = 0,
        virtualizerStrength = 0,
        reverbPreset = PresetReverb.PRESET_NONE,
        eqOffsetsMB = listOf(-1200, -600, 1000, 1400, 600),
        loudnessTargetGainMB = 50
    ),
    DEEP_BASS(
        id = "deep_bass",
        title = "澎湃低音",
        shortTitle = "澎湃低音",
        subtitle = "低频强劲下潜，节奏动感有力，听流行金曲与车载电音极致震撼",
        tag = "超重低音",
        bassBoostStrength = 1000,
        virtualizerStrength = 300,
        reverbPreset = PresetReverb.PRESET_NONE,
        eqOffsetsMB = listOf(1400, 1200, 200, -200, -300),
        loudnessTargetGainMB = 260
    ),
    VINTAGE_TUBE(
        id = "vintage_tube",
        title = "复古胆机",
        shortTitle = "复古胆机",
        subtitle = "模拟经典老式收音机与电子管温润质感，柔化高频，温暖耐听",
        tag = "怀旧温暖",
        bassBoostStrength = 500,
        virtualizerStrength = 150,
        reverbPreset = PresetReverb.PRESET_NONE,
        eqOffsetsMB = listOf(600, 1000, 400, -800, -1400),
        loudnessTargetGainMB = 120
    ),
    LIVE_CONCERT(
        id = "live_concert",
        title = "现场LIVE",
        shortTitle = "现场LIVE",
        subtitle = "增强声学空间混响与声场残响，宛若置身音乐厅前排",
        tag = "现场混响",
        bassBoostStrength = 400,
        virtualizerStrength = 950,
        reverbPreset = PresetReverb.PRESET_LARGEHALL,
        eqOffsetsMB = listOf(500, 300, -400, 800, 1100),
        loudnessTargetGainMB = 150
    ),
    CAR_COCKPIT(
        id = "car_cockpit",
        title = "车载座舱",
        shortTitle = "车载座舱",
        subtitle = "专为车载扬声器调校，提升抗风噪动态，声场立体开扬",
        tag = "车载专享",
        bassBoostStrength = 900,
        virtualizerStrength = 800,
        reverbPreset = PresetReverb.PRESET_NONE,
        eqOffsetsMB = listOf(1100, 1200, 400, 800, 600),
        loudnessTargetGainMB = 350
    )
}

/**
 * 全局电台音效控制器
 * 基于 Android 平台硬件/音频中间件 AudioEffect (Equalizer / BassBoost / Virtualizer / LoudnessEnhancer / PresetReverb)
 */
class AudioEffectManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("dragon_fm_audio_fx", Context.MODE_PRIVATE)

    private val _currentMode = MutableStateFlow(loadSavedMode())
    val currentMode: StateFlow<SoundEffectMode> = _currentMode.asStateFlow()

    private var currentSessionId: Int = 0

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var presetReverb: PresetReverb? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null

    init {
        // 加载初始模式
    }

    private fun loadSavedMode(): SoundEffectMode {
        val savedId = prefs.getString("selected_mode_id", SoundEffectMode.ORIGINAL.id)
        return SoundEffectMode.entries.firstOrNull { it.id == savedId } ?: SoundEffectMode.ORIGINAL
    }

    /**
     * 当 ExoPlayer 的 AudioSessionId 发生变化或初始化时调用
     */
    fun attachToAudioSession(sessionId: Int) {
        if (sessionId <= 0) return
        if (sessionId == currentSessionId && equalizer != null) return
        currentSessionId = sessionId
        releaseEffects()
        initEffects(sessionId)
        applyMode(_currentMode.value)
    }

    private fun initEffects(sessionId: Int) {
        try {
            equalizer = Equalizer(0, sessionId).apply { enabled = true }
            Log.i("AudioEffectManager", "Equalizer initialized for session $sessionId")
        } catch (e: Exception) {
            Log.w("AudioEffectManager", "Equalizer not supported on this device/session", e)
        }

        try {
            bassBoost = BassBoost(0, sessionId).apply { enabled = true }
        } catch (e: Exception) {
            Log.w("AudioEffectManager", "BassBoost not supported on this device/session", e)
        }

        try {
            virtualizer = Virtualizer(0, sessionId).apply { enabled = true }
        } catch (e: Exception) {
            Log.w("AudioEffectManager", "Virtualizer not supported on this device/session", e)
        }

        try {
            presetReverb = PresetReverb(0, sessionId).apply { enabled = true }
        } catch (e: Exception) {
            Log.w("AudioEffectManager", "PresetReverb not supported on this device/session", e)
        }

        try {
            loudnessEnhancer = LoudnessEnhancer(sessionId).apply { enabled = true }
        } catch (e: Exception) {
            Log.w("AudioEffectManager", "LoudnessEnhancer not supported on this device/session", e)
        }
    }

    /**
     * 切换音效模式
     */
    fun setMode(mode: SoundEffectMode) {
        _currentMode.value = mode
        prefs.edit().putString("selected_mode_id", mode.id).apply()

        // 若当前未初始化音效句柄，主动获取当前播放器的 session 挂载
        if (equalizer == null && bassBoost == null && virtualizer == null && loudnessEnhancer == null) {
            try {
                val sid = DragonFmApp.instance.radioPlayer.exoPlayer.audioSessionId
                if (sid > 0) {
                    attachToAudioSession(sid)
                    return
                }
            } catch (_: Exception) {}
        }
        applyMode(mode)
    }

    private fun applyMode(mode: SoundEffectMode) {
        val isOriginal = mode == SoundEffectMode.ORIGINAL

        try {
            // 1. 低音增强 (BassBoost)
            bassBoost?.let { bb ->
                if (isOriginal || mode.bassBoostStrength <= 0) {
                    bb.enabled = false
                } else if (bb.strengthSupported) {
                    bb.enabled = true
                    bb.setStrength(mode.bassBoostStrength)
                }
            }
        } catch (e: Exception) {
            Log.w("AudioEffectManager", "Error applying BassBoost", e)
        }

        try {
            // 2. 3D环绕 (Virtualizer)
            virtualizer?.let { v ->
                if (isOriginal || mode.virtualizerStrength <= 0) {
                    v.enabled = false
                } else if (v.strengthSupported) {
                    v.enabled = true
                    v.setStrength(mode.virtualizerStrength)
                }
            }
        } catch (e: Exception) {
            Log.w("AudioEffectManager", "Error applying Virtualizer", e)
        }

        try {
            // 3. 空间混响 (PresetReverb)
            presetReverb?.let { pr ->
                if (isOriginal || mode.reverbPreset == PresetReverb.PRESET_NONE) {
                    pr.enabled = false
                } else {
                    pr.enabled = true
                    pr.preset = mode.reverbPreset
                }
            }
        } catch (e: Exception) {
            Log.w("AudioEffectManager", "Error applying PresetReverb", e)
        }

        try {
            // 4. 响度动态驱动增强 (LoudnessEnhancer) - 极大提升不同音效间的体感对比度
            loudnessEnhancer?.let { le ->
                if (isOriginal || mode.loudnessTargetGainMB <= 0) {
                    le.setTargetGain(0)
                    le.enabled = false
                } else {
                    le.enabled = true
                    le.setTargetGain(mode.loudnessTargetGainMB)
                }
            }
        } catch (e: Exception) {
            Log.w("AudioEffectManager", "Error applying LoudnessEnhancer", e)
        }

        try {
            // 5. 均衡器频段调节 (Equalizer)
            equalizer?.let { eq ->
                if (isOriginal) {
                    val numBands = eq.numberOfBands.toInt()
                    for (i in 0 until numBands) {
                        eq.setBandLevel(i.toShort(), 0)
                    }
                    eq.enabled = false
                } else {
                    eq.enabled = true
                    val numBands = eq.numberOfBands.toInt()
                    val minLevel = eq.bandLevelRange[0]
                    val maxLevel = eq.bandLevelRange[1]

                    for (i in 0 until numBands) {
                        val offset = mode.eqOffsetsMB.getOrElse(i) { 0.toShort() }
                        val clamped = offset.coerceIn(minLevel, maxLevel)
                        eq.setBandLevel(i.toShort(), clamped)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("AudioEffectManager", "Error applying Equalizer", e)
        }
    }

    private fun releaseEffects() {
        try { equalizer?.release() } catch (_: Exception) {}
        try { bassBoost?.release() } catch (_: Exception) {}
        try { virtualizer?.release() } catch (_: Exception) {}
        try { presetReverb?.release() } catch (_: Exception) {}
        try { loudnessEnhancer?.release() } catch (_: Exception) {}
        equalizer = null
        bassBoost = null
        virtualizer = null
        presetReverb = null
        loudnessEnhancer = null
    }

    fun release() {
        releaseEffects()
        currentSessionId = 0
    }
}
