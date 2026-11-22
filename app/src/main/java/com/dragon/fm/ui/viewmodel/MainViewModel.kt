package com.dragon.fm.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dragon.fm.DragonFmApp
import com.dragon.fm.data.api.IpLocationService
import com.dragon.fm.data.api.RadioBrowserApi
import com.dragon.fm.data.local.FavoritesRepository
import com.dragon.fm.data.local.PresetsData
import com.dragon.fm.data.local.DedicatedSourceManager
import com.dragon.fm.data.model.PlaybackStatus
import com.dragon.fm.data.model.PlayerState
import com.dragon.fm.data.model.RadioStation
import com.dragon.fm.data.model.StreamLine
import com.dragon.fm.data.model.RecognitionMode
import com.dragon.fm.data.model.RecognitionState
import com.dragon.fm.data.model.RecognizedSong
import com.dragon.fm.data.model.LiveSongLyricsState
import com.dragon.fm.data.model.LyricsDisplayMode
import com.dragon.fm.player.DragonRadioPlayer
import com.dragon.fm.player.MusicRecognitionManager
import com.dragon.fm.player.RecordedFile
import com.dragon.fm.player.RecorderManager
import com.dragon.fm.player.SleepTimerManager
import com.dragon.fm.player.AsrModelType
import com.dragon.fm.player.AsrModelState
import com.dragon.fm.ui.theme.ThemePreset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.dragon.fm.data.api.AppUpdateManager
import com.dragon.fm.data.local.ExclusionFilterManager
import com.dragon.fm.data.model.AppUpdateInfo
import com.dragon.fm.data.model.CustomScenarioMode
import com.dragon.fm.data.model.AppBackupData
import com.dragon.fm.data.model.BackupRestoreSummary
import com.dragon.fm.data.model.DownloadState
import com.dragon.fm.data.model.PlayerCoverStyle
import com.dragon.fm.data.model.QuickActionButton
import com.dragon.fm.data.model.SubtitleFontSize
import com.dragon.fm.data.model.SwipeGestureAction
import com.dragon.fm.data.model.UpdateCheckResult
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as DragonFmApp
    private val prefs = application.getSharedPreferences("dragon_fm_prefs", Context.MODE_PRIVATE)
    val player: DragonRadioPlayer = app.radioPlayer
    val favoritesRepo: FavoritesRepository = app.favoritesRepository
    val recorderManager: RecorderManager = app.recorderManager
    val recognitionManager: MusicRecognitionManager = app.musicRecognitionManager
    val subtitleManager: com.dragon.fm.player.LiveStreamSubtitleManager = app.subtitleManager
    val audioSpectrumManager: com.dragon.fm.player.AudioSpectrumManager = app.audioSpectrumManager
    val audioSpectrum: StateFlow<FloatArray> = audioSpectrumManager.spectrumBands
    val audioPeaks: StateFlow<FloatArray> = audioSpectrumManager.spectrumPeaks
    val audioEffectManager: com.dragon.fm.player.AudioEffectManager = app.audioEffectManager
    val soundEffectMode: StateFlow<com.dragon.fm.player.SoundEffectMode> = audioEffectManager.currentMode

    private val _showSoundEffectSheet = MutableStateFlow(false)
    val showSoundEffectSheet: StateFlow<Boolean> = _showSoundEffectSheet.asStateFlow()

    fun openSoundEffectSheet() { _showSoundEffectSheet.value = true }
    fun closeSoundEffectSheet() { _showSoundEffectSheet.value = false }
    fun setSoundEffectMode(mode: com.dragon.fm.player.SoundEffectMode) {
        audioEffectManager.setMode(mode)
    }

    val sleepTimer: SleepTimerManager = app.sleepTimerManager
    val localRecordingPlayer = com.dragon.fm.player.LocalRecordingPlayer(application)
    private val api = RadioBrowserApi()
    val exclusionManager = ExclusionFilterManager(application)
    private val json = Json { ignoreUnknownKeys = true }

    val playerState: StateFlow<PlayerState> = player.playerState
    val favoriteIds: StateFlow<Set<String>> = favoritesRepo.favoriteIds
    val favoriteStations: StateFlow<List<RadioStation>> = favoritesRepo.favoriteStations
    val historyList: StateFlow<List<RadioStation>> = favoritesRepo.history
    val historyStations: StateFlow<List<RadioStation>> = historyList
    val customStations: StateFlow<List<RadioStation>> = favoritesRepo.customStations
    val recordedFiles: StateFlow<List<RecordedFile>> = recorderManager.recordingsList
    val playingRecordedFile: StateFlow<RecordedFile?> = localRecordingPlayer.currentPlayingFile
    val isLocalRecordingPlaying: StateFlow<Boolean> = localRecordingPlayer.isPlaying
    val localRecordingPositionMs: StateFlow<Long> = localRecordingPlayer.currentPositionMs
    val localRecordingDurationMs: StateFlow<Long> = localRecordingPlayer.durationMs
    val localRecordingSpeed: StateFlow<Float> = localRecordingPlayer.playbackSpeed
    val recognitionState: StateFlow<RecognitionState> = recognitionManager.state
    val recognitionHistory: StateFlow<List<RecognizedSong>> = recognitionManager.history

    // Live AI Subtitles & Lyrics Flow State
    val subtitleState = subtitleManager.subtitleState
    private val _isLyricsModeActive = MutableStateFlow(false)
    val isLyricsModeActive: StateFlow<Boolean> = _isLyricsModeActive.asStateFlow()

    // Live Synchronized Song Lyrics
    val songLyricManager = com.dragon.fm.player.LiveSongLyricManager(viewModelScope)
    val songLyricState: StateFlow<LiveSongLyricsState> = songLyricManager.lyricsState

    private val _lyricsDisplayMode = MutableStateFlow(LyricsDisplayMode.AI_SUBTITLE)
    val lyricsDisplayMode: StateFlow<LyricsDisplayMode> = _lyricsDisplayMode.asStateFlow()

    fun setLyricsDisplayMode(mode: LyricsDisplayMode) {
        _lyricsDisplayMode.value = mode
    }

    fun toggleLyricsDisplayMode() {
        _lyricsDisplayMode.value = if (_lyricsDisplayMode.value == LyricsDisplayMode.SONG_LYRICS) {
            LyricsDisplayMode.AI_SUBTITLE
        } else {
            LyricsDisplayMode.SONG_LYRICS
        }
    }

    // Navigation routing support
    private val _targetScreenRoute = MutableStateFlow<String?>(null)
    val targetScreenRoute: StateFlow<String?> = _targetScreenRoute.asStateFlow()

    fun navigateTo(route: String) {
        _targetScreenRoute.value = route
    }

    fun clearTargetScreenRoute() {
        _targetScreenRoute.value = null
    }

    // Offline Sherpa-ONNX ASR Model Management
    val asrModelState = subtitleManager.asrModelManager.modelState
    val currentAsrModelType: StateFlow<AsrModelType> = subtitleManager.asrModelManager.currentModelType
    private val _showAsrDownloadDialog = MutableStateFlow(false)
    val showAsrDownloadDialog: StateFlow<Boolean> = _showAsrDownloadDialog.asStateFlow()

    fun openAsrDownloadDialog() {
        _showAsrDownloadDialog.value = true
    }

    fun closeAsrDownloadDialog() {
        _showAsrDownloadDialog.value = false
    }

    fun setAsrModelType(type: AsrModelType) {
        subtitleManager.asrModelManager.setSelectedModelType(type)
    }

    fun startDownloadAsrModel(modelType: AsrModelType? = null) {
        val target = modelType ?: subtitleManager.asrModelManager.currentModelType.value
        subtitleManager.asrModelManager.startDownload(target) { success ->
            if (success) {
                _showAsrDownloadDialog.value = false
                setLyricsModeActive(true)
            }
        }
    }

    fun cancelDownloadAsrModel() {
        subtitleManager.asrModelManager.cancelDownload()
    }

    fun deleteAsrModel(deleteEngineAlso: Boolean = false): Boolean {
        if (_isLyricsModeActive.value) {
            setLyricsModeActive(false)
        }
        return subtitleManager.asrModelManager.deleteModel(deleteEngineAlso)
    }

    private var lastLyricsToggleTime = 0L

    fun toggleLyricsMode() {
        val now = System.currentTimeMillis()
        if (now - lastLyricsToggleTime < 400L) return
        lastLyricsToggleTime = now

        if (!_isLyricsModeActive.value) {
            if (!subtitleManager.asrModelManager.isModelReady()) {
                _showAsrDownloadDialog.value = true
            } else {
                setLyricsModeActive(true)
            }
        } else {
            setLyricsModeActive(false)
        }
    }

    fun setLyricsModeActive(active: Boolean) {
        if (active) {
            if (!subtitleManager.asrModelManager.isModelReady()) {
                _showAsrDownloadDialog.value = true
                return
            }
            _isLyricsModeActive.value = true
            subtitleManager.setSubtitleEnabled(true, player.liveAudioCaptureProcessor)
        } else {
            // 切回封面模式，保持底层语音转写继续运行，供封面下方单行歌词条实时展示
            _isLyricsModeActive.value = false
        }
    }

    fun setSubtitleEnabled(enabled: Boolean) {
        if (!enabled) {
            _isLyricsModeActive.value = false
            subtitleManager.setSubtitleEnabled(false, player.liveAudioCaptureProcessor)
        } else {
            if (!subtitleManager.asrModelManager.isModelReady()) {
                _showAsrDownloadDialog.value = true
                return
            }
            subtitleManager.setSubtitleEnabled(true, player.liveAudioCaptureProcessor)
        }
    }

    fun onStationChangedForSubtitle() {
        subtitleManager.onStationChanged()
        songLyricManager.onStationChanged()
        _lyricsDisplayMode.value = LyricsDisplayMode.AI_SUBTITLE
    }

    fun toggleSubtitleEnabled() {
        if (subtitleState.value.isEnabled) {
            setSubtitleEnabled(false)
        } else {
            setSubtitleEnabled(true)
        }
    }

    fun clearSubtitles() {
        subtitleManager.clearSubtitles()
    }

    fun setSubtitleFontSize(sizeSp: Float) {
        subtitleManager.setFontSize(sizeSp)
    }

    fun copyAllSubtitles() {
        subtitleManager.copyAllSubtitles()
    }

    fun copySingleSubtitle(text: String) {
        subtitleManager.copySingleSubtitle(text)
    }

    // 1. 播放页自定义设置 (封面样式、频道标签、律动频谱、功能按钮)
    private val _playerCoverStyle = MutableStateFlow(
        PlayerCoverStyle.fromId(prefs.getString("pref_player_cover_style", PlayerCoverStyle.VINYL.id) ?: PlayerCoverStyle.VINYL.id)
    )
    val playerCoverStyle: StateFlow<PlayerCoverStyle> = _playerCoverStyle.asStateFlow()

    fun setPlayerCoverStyle(style: PlayerCoverStyle) {
        _playerCoverStyle.value = style
        prefs.edit().putString("pref_player_cover_style", style.id).apply()
    }

    private val _showChannelTags = MutableStateFlow(
        prefs.getBoolean("pref_show_channel_tags", true)
    )
    val showChannelTags: StateFlow<Boolean> = _showChannelTags.asStateFlow()

    fun setShowChannelTags(show: Boolean) {
        _showChannelTags.value = show
        prefs.edit().putBoolean("pref_show_channel_tags", show).apply()
    }

    private val _showRhythmVisualizer = MutableStateFlow(
        prefs.getBoolean("pref_show_rhythm_visualizer", true)
    )
    val showRhythmVisualizer: StateFlow<Boolean> = _showRhythmVisualizer.asStateFlow()

    fun setShowRhythmVisualizer(show: Boolean) {
        _showRhythmVisualizer.value = show
        prefs.edit().putBoolean("pref_show_rhythm_visualizer", show).apply()
    }

    private val _quickActionButtons = MutableStateFlow(loadQuickActionButtons())
    val quickActionButtons: StateFlow<List<QuickActionButton>> = _quickActionButtons.asStateFlow()

    private fun loadQuickActionButtons(): List<QuickActionButton> {
        val raw = prefs.getString("pref_quick_actions", null)
        if (raw.isNullOrBlank()) return QuickActionButton.defaultList
        return raw.split(",").mapNotNull { QuickActionButton.fromId(it.trim()) }.ifEmpty { QuickActionButton.defaultList }
    }

    fun setQuickActionButtons(buttons: List<QuickActionButton>) {
        _quickActionButtons.value = buttons
        prefs.edit().putString("pref_quick_actions", buttons.joinToString(",") { it.id }).apply()
    }

    fun toggleQuickActionButton(action: QuickActionButton) {
        val current = _quickActionButtons.value.toMutableList()
        if (current.contains(action)) {
            if (current.size > 1) { // 至少保留一个
                current.remove(action)
            }
        } else {
            current.add(action)
        }
        setQuickActionButtons(current)
    }

    fun resetQuickActionButtons() {
        setQuickActionButtons(QuickActionButton.defaultList)
    }

    // 2. 左右滑屏手势动作设置 (支持右滑打开场景抽屉/发现页/切换场景，左滑打开「我的」/播放列表等)
    private val _swipeRightAction = MutableStateFlow(
        SwipeGestureAction.fromId(prefs.getString("pref_swipe_right_action", SwipeGestureAction.OPEN_STUDIO.id) ?: SwipeGestureAction.OPEN_STUDIO.id, SwipeGestureAction.OPEN_STUDIO)
    )
    val swipeRightAction: StateFlow<SwipeGestureAction> = _swipeRightAction.asStateFlow()

    fun setSwipeRightAction(action: SwipeGestureAction) {
        _swipeRightAction.value = action
        prefs.edit().putString("pref_swipe_right_action", action.id).apply()
    }

    private val _swipeLeftAction = MutableStateFlow(
        SwipeGestureAction.fromId(prefs.getString("pref_swipe_left_action", SwipeGestureAction.OPEN_MY.id) ?: SwipeGestureAction.OPEN_MY.id, SwipeGestureAction.OPEN_MY)
    )
    val swipeLeftAction: StateFlow<SwipeGestureAction> = _swipeLeftAction.asStateFlow()

    fun setSwipeLeftAction(action: SwipeGestureAction) {
        _swipeLeftAction.value = action
        prefs.edit().putString("pref_swipe_left_action", action.id).apply()
    }

    // 3. 实时字幕与歌词设置 (总开关与字号调节)
    private val _showLiveSubtitles = MutableStateFlow(
        prefs.getBoolean("pref_show_live_subtitles", true)
    )
    val showLiveSubtitles: StateFlow<Boolean> = _showLiveSubtitles.asStateFlow()

    fun setShowLiveSubtitles(show: Boolean) {
        _showLiveSubtitles.value = show
        prefs.edit().putBoolean("pref_show_live_subtitles", show).apply()
        if (!show) {
            setSubtitleEnabled(false)
        }
    }

    private val _subtitleFontSizeOption = MutableStateFlow(
        SubtitleFontSize.fromId(prefs.getString("pref_subtitle_font_size", SubtitleFontSize.STANDARD.id) ?: SubtitleFontSize.STANDARD.id)
    )
    val subtitleFontSizeOption: StateFlow<SubtitleFontSize> = _subtitleFontSizeOption.asStateFlow()

    fun setSubtitleFontSizeOption(option: SubtitleFontSize) {
        _subtitleFontSizeOption.value = option
        prefs.edit().putString("pref_subtitle_font_size", option.id).apply()
        setSubtitleFontSize(option.activeSp)
    }

    // 5. 是否开启版本更新检测提醒设置 (默认开启，关闭后不自动弹窗提醒)
    private val _enableUpdateReminder = MutableStateFlow(
        prefs.getBoolean("pref_enable_update_reminder", true)
    )
    val enableUpdateReminder: StateFlow<Boolean> = _enableUpdateReminder.asStateFlow()

    fun setEnableUpdateReminder(enable: Boolean) {
        _enableUpdateReminder.value = enable
        prefs.edit().putBoolean("pref_enable_update_reminder", enable).apply()
    }

    // 调试模式（隐藏开发者选项，默认关闭）：开启后输出详细日志并展示开发者面板
    private val _debugModeEnabled = MutableStateFlow(prefs.getBoolean("pref_debug_mode", false))
    val debugModeEnabled: StateFlow<Boolean> = _debugModeEnabled.asStateFlow()

    fun setDebugModeEnabled(enable: Boolean) {
        _debugModeEnabled.value = enable
        prefs.edit().putBoolean("pref_debug_mode", enable).apply()
    }

    // Music recognition dialog visibility
    private val _showRecognitionDialog = MutableStateFlow(false)
    val showRecognitionDialog: StateFlow<Boolean> = _showRecognitionDialog.asStateFlow()

    private val _currentRecognitionMode = MutableStateFlow(RecognitionMode.RADIO_STREAM)
    val currentRecognitionMode: StateFlow<RecognitionMode> = _currentRecognitionMode.asStateFlow()

    // Exclusion filter states
    val isDomesticOnly: StateFlow<Boolean> = exclusionManager.isDomesticOnly
    val blockedStations: StateFlow<Map<String, String>> = exclusionManager.blockedStations
    val blockedTags: StateFlow<Set<String>> = exclusionManager.blockedTags

    val isRecording: StateFlow<Boolean> = recorderManager.isRecording
    val recordingDurationSec: StateFlow<Int> = recorderManager.recordingDurationSec
    val sleepTimerSeconds: StateFlow<Int?> = sleepTimer.remainingSeconds
    val selectedSleepMinutes: StateFlow<Int?> = sleepTimer.selectedMinutes

    private val _playlist = MutableStateFlow<List<RadioStation>>(PresetsData.defaultStations)
    val playlist: StateFlow<List<RadioStation>> = _playlist.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    // Scenarios (汽水音乐场景与分类标签)
    private val _selectedScenario = MutableStateFlow("精选推荐")
    val selectedScenario: StateFlow<String> = _selectedScenario.asStateFlow()

    // Scenario Station Overrides & Customization (场景内电台收藏、删除与排序)
    private val _scenarioStationOverrides = MutableStateFlow<Map<String, List<RadioStation>>>(emptyMap())
    val scenarioStationOverrides: StateFlow<Map<String, List<RadioStation>>> = _scenarioStationOverrides.asStateFlow()

    // Overall Scenario Order (支持全场景展示排序调整)
    private val _scenarioOrder = MutableStateFlow<List<String>>(emptyList())
    val scenarioOrder: StateFlow<List<String>> = _scenarioOrder.asStateFlow()

    // Custom Scenario Modes
    private val _customScenarios = MutableStateFlow<List<CustomScenarioMode>>(emptyList())
    val customScenarios: StateFlow<List<CustomScenarioMode>> = _customScenarios.asStateFlow()

    // Deleted Default Scenario Keys (allows user to remove default modes and restore them)
    private val _deletedDefaultScenarios = MutableStateFlow<Set<String>>(emptySet())
    val deletedDefaultScenarios: StateFlow<Set<String>> = _deletedDefaultScenarios.asStateFlow()

    // Infinite Random Roaming deduplication queue
    private val recentRoamingIds = java.util.ArrayDeque<String>(200)

    private val _detectedLocation = MutableStateFlow("本地电台")
    val detectedLocation: StateFlow<String> = _detectedLocation.asStateFlow()

    private val _detectedProvince = MutableStateFlow<String?>("河南")
    val detectedProvince: StateFlow<String?> = _detectedProvince.asStateFlow()

    private val _detectedCity = MutableStateFlow<String?>("郑州")
    val detectedCity: StateFlow<String?> = _detectedCity.asStateFlow()

    // Pure Sleep Mode state (纯净睡眠模式)
    private val _isSleepModeActive = MutableStateFlow(false)
    val isSleepModeActive: StateFlow<Boolean> = _isSleepModeActive.asStateFlow()

    // Explore & Search State
    private val _exploreStations = MutableStateFlow<List<RadioStation>>(emptyList())
    val exploreStations: StateFlow<List<RadioStation>> = _exploreStations.asStateFlow()

    private val _radarSearchQuery = MutableStateFlow("")
    val radarSearchQuery: StateFlow<String> = _radarSearchQuery.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    // High-speed in-memory cache for explore tabs & provinces (tag/province -> list)
    private val exploreStationCache = java.util.concurrent.ConcurrentHashMap<String, List<RadioStation>>()

    private val _currentExploreTag = MutableStateFlow("全国精选")
    val currentExploreTag: StateFlow<String> = _currentExploreTag.asStateFlow()

    private val _currentExploreGroupIndex = MutableStateFlow(0)
    val currentExploreGroupIndex: StateFlow<Int> = _currentExploreGroupIndex.asStateFlow()

    fun setExploreTag(tag: String) {
        _currentExploreTag.value = tag
    }

    fun setExploreGroupIndex(index: Int) {
        _currentExploreGroupIndex.value = index
    }

    fun clearExploreCache() {
        exploreStationCache.clear()
    }

    // Speed test state & low latency sorting
    val latencies: StateFlow<Map<String, Long>> = com.dragon.fm.player.StationSpeedTester.latencies
    val isSpeedTesting: StateFlow<Boolean> = com.dragon.fm.player.StationSpeedTester.isTesting

    private val _isSortByLatency = MutableStateFlow(false)
    val isSortByLatency: StateFlow<Boolean> = _isSortByLatency.asStateFlow()

    // Multi-style Theme Engine (跟随系统 / 11款精美浅色与深色主题)
    private val _isFollowSystemTheme = MutableStateFlow(prefs.getBoolean("pref_follow_system_theme", true))
    val isFollowSystemTheme: StateFlow<Boolean> = _isFollowSystemTheme.asStateFlow()

    private val _selectedLightThemeId = MutableStateFlow(
        prefs.getString("pref_selected_light_theme", ThemePreset.PORCELAIN_JADE.id) ?: ThemePreset.PORCELAIN_JADE.id
    )
    val selectedLightThemeId: StateFlow<String> = _selectedLightThemeId.asStateFlow()

    private val _selectedDarkThemeId = MutableStateFlow(
        prefs.getString("pref_selected_dark_theme", ThemePreset.SODA_OBSIDIAN.id) ?: ThemePreset.SODA_OBSIDIAN.id
    )
    val selectedDarkThemeId: StateFlow<String> = _selectedDarkThemeId.asStateFlow()

    private val _currentTheme = MutableStateFlow(
        ThemePreset.fromId(prefs.getString("saved_theme_id", ThemePreset.PORCELAIN_JADE.id) ?: ThemePreset.PORCELAIN_JADE.id)
    )
    val currentTheme: StateFlow<ThemePreset> = _currentTheme.asStateFlow()

    fun setFollowSystemTheme(enabled: Boolean) {
        _isFollowSystemTheme.value = enabled
        prefs.edit().putBoolean("pref_follow_system_theme", enabled).apply()
    }

    fun setTheme(theme: ThemePreset) {
        _currentTheme.value = theme
        prefs.edit().putString("saved_theme_id", theme.id).apply()
        if (theme.isDark) {
            _selectedDarkThemeId.value = theme.id
            prefs.edit().putString("pref_selected_dark_theme", theme.id).apply()
        } else {
            _selectedLightThemeId.value = theme.id
            prefs.edit().putString("pref_selected_light_theme", theme.id).apply()
        }
    }

    // App Background Wallpapers & Presets
    private val _appBackground = MutableStateFlow(
        com.dragon.fm.data.model.AppBackgroundState(
            type = try {
                com.dragon.fm.data.model.AppBackgroundType.valueOf(
                    prefs.getString("pref_bg_type", com.dragon.fm.data.model.AppBackgroundType.DEFAULT.name)
                        ?: com.dragon.fm.data.model.AppBackgroundType.DEFAULT.name
                )
            } catch (_: Exception) { com.dragon.fm.data.model.AppBackgroundType.DEFAULT },
            presetId = prefs.getString("pref_bg_preset_id", "twilight") ?: "twilight",
            customImagePath = prefs.getString("pref_bg_custom_path", null),
            overlayAlpha = prefs.getFloat("pref_bg_overlay_alpha", 0.65f)
        )
    )
    val appBackground: StateFlow<com.dragon.fm.data.model.AppBackgroundState> = _appBackground.asStateFlow()

    fun setBackgroundType(type: com.dragon.fm.data.model.AppBackgroundType) {
        _appBackground.value = _appBackground.value.copy(type = type)
        prefs.edit().putString("pref_bg_type", type.name).apply()
    }

    fun setBackgroundPreset(presetId: String) {
        _appBackground.value = _appBackground.value.copy(
            type = com.dragon.fm.data.model.AppBackgroundType.PRESET,
            presetId = presetId
        )
        prefs.edit()
            .putString("pref_bg_type", com.dragon.fm.data.model.AppBackgroundType.PRESET.name)
            .putString("pref_bg_preset_id", presetId)
            .apply()
    }

    fun setCustomBackgroundImage(uriString: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                val inputStream = context.contentResolver.openInputStream(android.net.Uri.parse(uriString))
                val targetFile = java.io.File(context.filesDir, "custom_bg_wallpaper.jpg")
                inputStream?.use { input ->
                    targetFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                val path = targetFile.absolutePath
                _appBackground.value = _appBackground.value.copy(
                    type = com.dragon.fm.data.model.AppBackgroundType.CUSTOM,
                    customImagePath = path
                )
                prefs.edit()
                    .putString("pref_bg_type", com.dragon.fm.data.model.AppBackgroundType.CUSTOM.name)
                    .putString("pref_bg_custom_path", path)
                    .apply()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun resetBackgroundToDefault() {
        _appBackground.value = _appBackground.value.copy(type = com.dragon.fm.data.model.AppBackgroundType.DEFAULT)
        prefs.edit().putString("pref_bg_type", com.dragon.fm.data.model.AppBackgroundType.DEFAULT.name).apply()
    }

    // Stream Source & Line Preference (智能极速模式 / 纯净官方专线 / 全球公网广播)
    private val _streamSourceMode = MutableStateFlow(
        com.dragon.fm.data.model.StreamSourceMode.values().find {
            it.code == prefs.getString("pref_stream_source_mode", com.dragon.fm.data.model.StreamSourceMode.HYBRID.code)
        } ?: com.dragon.fm.data.model.StreamSourceMode.HYBRID
    )
    val streamSourceMode: StateFlow<com.dragon.fm.data.model.StreamSourceMode> = _streamSourceMode.asStateFlow()

    fun setStreamSourceMode(mode: com.dragon.fm.data.model.StreamSourceMode) {
        _streamSourceMode.value = mode
        prefs.edit().putString("pref_stream_source_mode", mode.code).apply()
        DedicatedSourceManager.currentSourceMode = mode
        api.clearCache()
        DedicatedSourceManager.clearCache()
        exploreStationCache.clear()
        loadExploreInitial(forceRefresh = true)
        selectScenario(selectedScenario.value)
    }

    // User Region Customization (支持手动指定省份与二级市)
    private val _userSelectedProvince = MutableStateFlow<String?>(prefs.getString("saved_user_province", null))
    val userSelectedProvince: StateFlow<String?> = _userSelectedProvince.asStateFlow()

    private val _userSelectedCity = MutableStateFlow<String?>(prefs.getString("saved_user_city", null))
    val userSelectedCity: StateFlow<String?> = _userSelectedCity.asStateFlow()

    private val _isCustomRegionEnabled = MutableStateFlow(prefs.getBoolean("saved_user_custom_region", false))
    val isCustomRegionEnabled: StateFlow<Boolean> = _isCustomRegionEnabled.asStateFlow()

    fun setUserRegion(province: String, city: String) {
        _userSelectedProvince.value = province
        _userSelectedCity.value = city
        _isCustomRegionEnabled.value = true
        prefs.edit()
            .putString("saved_user_province", province)
            .putString("saved_user_city", city)
            .putBoolean("saved_user_custom_region", true)
            .apply()

        val display = if (city.isBlank() || city == "全城通用" || city == "全省通用" || city == "全区通用") province else "$province · $city"
        _detectedLocation.value = "本地 ($display)"

        if (_selectedScenario.value.contains("本地")) {
            loadLocalScenarioStations(province, city)
        }
    }

    fun resetToAutoLocation() {
        _userSelectedProvince.value = null
        _userSelectedCity.value = null
        _isCustomRegionEnabled.value = false
        prefs.edit()
            .remove("saved_user_province")
            .remove("saved_user_city")
            .putBoolean("saved_user_custom_region", false)
            .apply()
        detectLocalLocation()
    }

    // Fast Buffer Setting (0.25s 极速缓冲模式)
    private val _isFastBuffer = MutableStateFlow(prefs.getBoolean("pref_fast_buffer", true))
    val isFastBuffer: StateFlow<Boolean> = _isFastBuffer.asStateFlow()

    fun setFastBuffer(enabled: Boolean) {
        _isFastBuffer.value = enabled
        prefs.edit().putBoolean("pref_fast_buffer", enabled).apply()
    }

    // Auto-play on launch setting (启动时自动播放)
    private val _isAutoPlayOnLaunch = MutableStateFlow(prefs.getBoolean("pref_auto_play_on_launch", true))
    val isAutoPlayOnLaunch: StateFlow<Boolean> = _isAutoPlayOnLaunch.asStateFlow()

    fun setAutoPlayOnLaunch(enabled: Boolean) {
        _isAutoPlayOnLaunch.value = enabled
        prefs.edit().putBoolean("pref_auto_play_on_launch", enabled).apply()
    }

    // Font Scale setting (界面字体大小调节: 0.88f 紧凑, 1.0f 标准, 1.12f 舒适, 1.25f 特大)
    private val _fontScale = MutableStateFlow(prefs.getFloat("pref_font_scale", 1.0f))
    val fontScale: StateFlow<Float> = _fontScale.asStateFlow()

    fun setFontScale(scale: Float) {
        val clamped = scale.coerceIn(0.85f, 1.30f)
        _fontScale.value = clamped
        prefs.edit().putFloat("pref_font_scale", clamped).apply()
    }

    // Playback Memory Setting (记忆最后收听电台、模式与播放列表)
    private val _isPlaybackMemoryEnabled = MutableStateFlow(prefs.getBoolean("pref_playback_memory", true))
    val isPlaybackMemoryEnabled: StateFlow<Boolean> = _isPlaybackMemoryEnabled.asStateFlow()

    fun setPlaybackMemoryEnabled(enabled: Boolean) {
        _isPlaybackMemoryEnabled.value = enabled
        prefs.edit().putBoolean("pref_playback_memory", enabled).apply()
        if (!enabled) {
            prefs.edit()
                .remove("saved_last_station")
                .remove("saved_last_scenario")
                .remove("saved_last_playlist")
                .remove("saved_last_index")
                .apply()
        } else {
            val curList = _playlist.value
            val idx = _currentIndex.value
            if (idx in curList.indices) {
                savePlaybackMemory(curList[idx], _selectedScenario.value, curList, idx)
            }
        }
    }

    private fun savePlaybackMemory(
        station: RadioStation,
        scenario: String,
        playlist: List<RadioStation>,
        index: Int
    ) {
        if (!_isPlaybackMemoryEnabled.value) return
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val stationJson = json.encodeToString(station)
                val trimmedPlaylist = playlist.take(120)
                val playlistJson = json.encodeToString(trimmedPlaylist)
                prefs.edit()
                    .putString("saved_last_station", stationJson)
                    .putString("saved_last_scenario", scenario)
                    .putString("saved_last_playlist", playlistJson)
                    .putInt("saved_last_index", index)
                    .apply()
            } catch (_: Exception) {}
        }
    }

    private fun updateSavedPlaylistIfMemoryEnabled() {
        if (!_isPlaybackMemoryEnabled.value) return
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val curList = _playlist.value
                val curIndex = _currentIndex.value
                val curStation = curList.getOrNull(curIndex) ?: return@launch
                val stationJson = json.encodeToString(curStation)
                val trimmedPlaylist = curList.take(120)
                val playlistJson = json.encodeToString(trimmedPlaylist)
                prefs.edit()
                    .putString("saved_last_station", stationJson)
                    .putString("saved_last_scenario", _selectedScenario.value)
                    .putString("saved_last_playlist", playlistJson)
                    .putInt("saved_last_index", curIndex)
                    .apply()
            } catch (_: Exception) {}
        }
    }

    // Image Cache Size Calculation & Clearing
    private val _imageCacheSizeBytes = MutableStateFlow(0L)
    val imageCacheSizeBytes: StateFlow<Long> = _imageCacheSizeBytes.asStateFlow()

    fun refreshCacheSize() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val dir = java.io.File(getApplication<Application>().cacheDir, "image_cache")
            val size = if (dir.exists()) {
                dir.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
            } else 0L
            _imageCacheSizeBytes.value = size
        }
    }

    fun clearImageCache(): Long {
        val dir = java.io.File(getApplication<Application>().cacheDir, "image_cache")
        val size = if (dir.exists()) {
            dir.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
        } else 0L
        if (dir.exists()) {
            dir.deleteRecursively()
            dir.mkdirs()
        }
        _imageCacheSizeBytes.value = 0L
        return size
    }

    // Gitee In-App Update Engine
    val updateManager = AppUpdateManager(application)

    private val _updateCheckResult = MutableStateFlow<UpdateCheckResult>(UpdateCheckResult.Idle)
    val updateCheckResult: StateFlow<UpdateCheckResult> = _updateCheckResult.asStateFlow()

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    private val _showUpdateDialog = MutableStateFlow(false)
    val showUpdateDialog: StateFlow<Boolean> = _showUpdateDialog.asStateFlow()

    init {
        // Initialize stream source mode
        DedicatedSourceManager.currentSourceMode = _streamSourceMode.value

        // When fresh stream lines are fetched in background, sync into playlist, scenarios, and favorites asynchronously
        DedicatedSourceManager.onPoolRefreshedListener = {
            viewModelScope.launch(Dispatchers.IO) {
                val curPlaylist = _playlist.value
                val freshPlaylist = DedicatedSourceManager.refreshStationList(curPlaylist)
                val playlistChanged = freshPlaylist != curPlaylist

                val curScenarios = _customScenarios.value
                var scenariosChanged = false
                val newScenarios = curScenarios.map { sc ->
                    val refreshed = DedicatedSourceManager.refreshStationList(sc.stations)
                    if (refreshed != sc.stations) {
                        scenariosChanged = true
                        sc.copy(stations = refreshed)
                    } else sc
                }

                val curOverrides = _scenarioStationOverrides.value
                var overridesChanged = false
                val newOverrides = curOverrides.mapValues { (_, stations) ->
                    val refreshed = DedicatedSourceManager.refreshStationList(stations)
                    if (refreshed != stations) {
                        overridesChanged = true
                        refreshed
                    } else stations
                }

                if (playlistChanged || scenariosChanged || overridesChanged) {
                    withContext(Dispatchers.Main) {
                        if (playlistChanged) {
                            _playlist.value = freshPlaylist
                        }
                        if (scenariosChanged) {
                            _customScenarios.value = newScenarios
                            saveCustomScenariosToPrefs()
                        }
                        if (overridesChanged) {
                            _scenarioStationOverrides.value = newOverrides
                            saveScenarioStationOverridesToPrefs()
                        }
                    }
                }
                favoritesRepo.refreshFavorites()
            }
        }

        // Disable auto-skip on radio error to prevent unexpected channel jumping
        DragonRadioPlayer.onAutoSkipListener = null
        player.onStationRefreshedListener = { freshStation ->
            updateStationEverywhere(freshStation)
        }
        com.dragon.fm.player.DragonMediaSessionService.onSkipPreviousListener = {
            viewModelScope.launch {
                previousStation()
            }
        }
        com.dragon.fm.player.DragonMediaSessionService.onSkipNextListener = {
            viewModelScope.launch {
                nextStation()
            }
        }

        loadScenarioOrder()
        loadCustomScenarios()
        loadScenarioStationOverrides()
        loadDeletedDefaultScenarios()

        // 启动静默检查应用版本更新（在用户开启更新提醒时自动弹出更新卡片）
        viewModelScope.launch {
            kotlinx.coroutines.delay(1200)
            if (_enableUpdateReminder.value) {
                checkForUpdates(isManual = false)
            }
        }

        viewModelScope.launch {
            val memoryEnabled = prefs.getBoolean("pref_playback_memory", true)
            var restored = false

            if (memoryEnabled) {
                val savedStationJson = prefs.getString("saved_last_station", null)
                val savedScenario = prefs.getString("saved_last_scenario", null)
                val savedPlaylistJson = prefs.getString("saved_last_playlist", null)
                val savedIndex = prefs.getInt("saved_last_index", 0)

                if (!savedStationJson.isNullOrBlank()) {
                    try {
                        val lastStation = json.decodeFromString<RadioStation>(savedStationJson)
                        val lastPlaylist = if (!savedPlaylistJson.isNullOrBlank()) {
                            json.decodeFromString<List<RadioStation>>(savedPlaylistJson)
                        } else emptyList()

                        val filteredPlaylist = exclusionManager.filterStations(lastPlaylist).toMutableList()
                        val freshPlaylist = DedicatedSourceManager.refreshStationList(filteredPlaylist).toMutableList()

                        // Determine target index
                        var targetIndex = -1
                        if (savedIndex in freshPlaylist.indices && freshPlaylist[savedIndex].id == lastStation.id) {
                            targetIndex = savedIndex
                        } else {
                            targetIndex = freshPlaylist.indexOfFirst { it.id == lastStation.id }
                        }

                        // If not found in filtered list but not blocked, insert back at saved position
                        if (targetIndex == -1 && !exclusionManager.isStationBlocked(lastStation)) {
                            val insertPos = savedIndex.coerceIn(0, freshPlaylist.size)
                            val freshLastStation = DedicatedSourceManager.resolveFreshStation(lastStation)
                            freshPlaylist.add(insertPos, freshLastStation)
                            targetIndex = insertPos
                        }

                        if (freshPlaylist.isNotEmpty() && targetIndex in freshPlaylist.indices) {
                            val scenarioName = savedScenario ?: "精选推荐"
                            _selectedScenario.value = scenarioName
                            _playlist.value = freshPlaylist
                            if (_isAutoPlayOnLaunch.value) {
                                playStationAtIndex(targetIndex)
                            } else {
                                prepareStationAtIndex(targetIndex)
                            }
                            restored = true
                            reconcilePlaylistInWarmup(freshPlaylist)

                            // If restored to default featured scenario, complement online China stations in background
                            if (scenarioName.contains("精选") || scenarioName.contains("推荐")) {
                                expandPlaylistWithChinaStations()
                            }
                        }
                    } catch (_: Exception) {
                        restored = false
                    }
                }
            }

            if (!restored) {
                _playlist.value = exclusionManager.filterStations(PresetsData.defaultStations)
                if (_playlist.value.isNotEmpty()) {
                    if (_isAutoPlayOnLaunch.value) {
                        playStationAtIndex(0)
                    } else {
                        prepareStationAtIndex(0)
                    }
                }
                expandPlaylistWithChinaStations()
            }
        }

        // Set initial location from custom region or auto IP
        if (_isCustomRegionEnabled.value && !_userSelectedProvince.value.isNullOrBlank()) {
            val p = _userSelectedProvince.value ?: ""
            val c = _userSelectedCity.value ?: ""
            val display = if (c.isBlank() || c == "全城通用" || c == "全省通用" || c == "全区通用") p else c
            _detectedLocation.value = "本地 ($display)"
        } else {
            detectLocalLocation()
        }
        refreshCacheSize()

        // Background check for updates 2.5s after app startup
        viewModelScope.launch {
            kotlinx.coroutines.delay(2500)
            val res = updateManager.checkUpdate(isManual = false)
            _updateCheckResult.value = res
            if (res is UpdateCheckResult.HasUpdate) {
                _showUpdateDialog.value = true
            }
        }

        // Listen to music recognition results to sync Now Playing track & trigger floating banner / failure toasts
        viewModelScope.launch {
            recognitionState.collect { state ->
                when (state) {
                    is RecognitionState.Success -> {
                        applyRecognizedSongToNowPlaying(state.song)
                        _floatingRecognizedSong.value = state.song
                        // 识别成功后自动启动时间轴歌词同步并切换为歌曲歌词模式
                        songLyricManager.startSync(state.song)
                        _lyricsDisplayMode.value = LyricsDisplayMode.SONG_LYRICS
                        showToast("已识别: 《${state.song.title}》 - ${state.song.artist}")
                        delay(3500)
                        if (recognitionState.value is RecognitionState.Success) {
                            recognitionManager.resetState()
                        }
                    }
                    is RecognitionState.NoMatch -> {
                        val tip = state.message.ifBlank { "当前广播未识别到歌曲（可能正处于主持人口播、新闻或广告）" }
                        showToast(tip)
                        delay(2500)
                        if (recognitionState.value is RecognitionState.NoMatch) {
                            recognitionManager.resetState()
                        }
                    }
                    is RecognitionState.Error -> {
                        val tip = if (state.message.contains("采样数据不足") || state.message.contains("有效音频长度")) {
                            "电台流采样数据不足，请确保电台播放流畅后重试"
                        } else {
                            "识曲未完成: ${state.message}"
                        }
                        showToast(tip)
                        delay(2500)
                        if (recognitionState.value is RecognitionState.Error) {
                            recognitionManager.resetState()
                        }
                    }
                    else -> {}
                }
            }
        }

        // 监听歌曲歌词同步状态，当歌曲自然播放完毕时自动切回实时语音字幕伴听模式
        viewModelScope.launch {
            songLyricState.collect { lState ->
                if (!lState.isActive && _lyricsDisplayMode.value == LyricsDisplayMode.SONG_LYRICS && lState.errorMessage == "歌曲播放完毕") {
                    _lyricsDisplayMode.value = LyricsDisplayMode.AI_SUBTITLE
                }
            }
        }

        // Prewarm high-speed explore cache and instant stations on startup
        prewarmExploreCache()
    }

    var debugVersionTapCount by mutableStateOf(0)
        private set

    /** 连续点击版本行 5 次进入开发者模式（一次性解锁） */
    fun onVersionRowTapped() {
        if (_debugModeEnabled.value) return
        debugVersionTapCount += 1
        if (debugVersionTapCount >= 5) {
            debugVersionTapCount = 0
            setDebugModeEnabled(true)
        }
    }

    val currentVersionName: String get() = updateManager.getCurrentVersionName()
    val currentVersionCode: Int get() = updateManager.getCurrentVersionCode()

    fun checkForUpdates(isManual: Boolean = false) {
        if (!isManual && !_enableUpdateReminder.value) return
        viewModelScope.launch {
            _updateCheckResult.value = UpdateCheckResult.Checking
            val result = updateManager.checkUpdate(isManual = isManual)
            _updateCheckResult.value = result
            if (result is UpdateCheckResult.HasUpdate) {
                _showUpdateDialog.value = true
            }
        }
    }

    fun startDownloadUpdate(info: AppUpdateInfo) {
        viewModelScope.launch {
            _downloadState.value = DownloadState.Downloading(0f, 0L, 0L)
            val result = updateManager.downloadApk(info) { progress, cur, total ->
                _downloadState.value = DownloadState.Downloading(progress, cur, total)
            }
            result.fold(
                onSuccess = { file ->
                    _downloadState.value = DownloadState.Completed(file)
                    updateManager.installApk(file)
                },
                onFailure = { err ->
                    _downloadState.value = DownloadState.Error(err.message ?: "下载失败")
                }
            )
        }
    }

    fun installApk(file: java.io.File) {
        updateManager.installApk(file)
    }

    fun dismissUpdateDialog() {
        _showUpdateDialog.value = false
    }

    private fun loadScenarioStationOverrides() {
        val raw = prefs.getString("saved_scenario_stations", null)
        android.util.Log.d("DragonScene", "loadScenarioStationOverrides raw: $raw")
        if (raw.isNullOrBlank()) return
        try {
            val decoded: Map<String, List<RadioStation>> = json.decodeFromString(raw)
            _scenarioStationOverrides.value = decoded
            android.util.Log.d("DragonScene", "loadScenarioStationOverrides SUCCESS size: ${decoded.size}")
        } catch (e: Exception) {
            android.util.Log.e("DragonScene", "loadScenarioStationOverrides ERROR", e)
        }
    }

    private fun saveScenarioStationOverridesToPrefs() {
        val snapshot = _scenarioStationOverrides.value
        android.util.Log.d("DragonScene", "saveScenarioStationOverridesToPrefs keys: ${snapshot.keys}")
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val encoded = json.encodeToString(snapshot)
                val ok = prefs.edit().putString("saved_scenario_stations", encoded).commit()
                android.util.Log.d("DragonScene", "saveScenarioStationOverridesToPrefs committed: $ok length: ${encoded.length}")
            } catch (e: Exception) {
                android.util.Log.e("DragonScene", "saveScenarioStationOverridesToPrefs ERROR", e)
            }
        }
    }

    private fun loadScenarioOrder() {
        val raw = prefs.getString("saved_scenario_order", null)
        android.util.Log.d("DragonScene", "loadScenarioOrder raw: $raw")
        if (raw.isNullOrBlank()) return
        try {
            val decoded: List<String> = json.decodeFromString(raw)
            _scenarioOrder.value = decoded
            android.util.Log.d("DragonScene", "loadScenarioOrder SUCCESS size: ${decoded.size}")
        } catch (e: Exception) {
            android.util.Log.e("DragonScene", "loadScenarioOrder ERROR", e)
        }
    }

    fun reorderAllScenarios(orderedKeys: List<String>) {
        _scenarioOrder.value = orderedKeys
        // 同步对 _customScenarios 重新排序，防止不同界面或重启后自定义场景顺序脱节
        val currentCustom = _customScenarios.value
        val sortedCustom = currentCustom.sortedBy { sc ->
            val idx = orderedKeys.indexOf(sc.title)
            if (idx >= 0) idx else 999
        }
        _customScenarios.value = sortedCustom
        saveCustomScenariosToPrefs()

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val ok = prefs.edit().putString("saved_scenario_order", json.encodeToString(orderedKeys)).commit()
                android.util.Log.d("DragonScene", "reorderAllScenarios saved order committed: $ok")
            } catch (e: Exception) {
                android.util.Log.e("DragonScene", "reorderAllScenarios ERROR", e)
            }
        }
    }

    private fun loadCustomScenarios() {
        val raw = prefs.getString("saved_custom_scenarios", null)
        android.util.Log.d("DragonScene", "loadCustomScenarios raw: $raw")
        if (raw.isNullOrBlank()) return
        try {
            val decoded: List<CustomScenarioMode> = json.decodeFromString(raw)
            val filtered = decoded.filterNot {
                it.id.contains("canton", ignoreCase = true) ||
                it.title.contains("canton", ignoreCase = true) ||
                it.title.contains("粤语", ignoreCase = true)
            }
            // 依据已保存的场景全局展示顺序排序，确保每次启动顺序完全一致
            val order = _scenarioOrder.value
            val sorted = if (order.isNotEmpty()) {
                filtered.sortedBy { sc ->
                    val idx = order.indexOf(sc.title)
                    if (idx >= 0) idx else 999
                }
            } else {
                filtered
            }
            _customScenarios.value = sorted
            android.util.Log.d("DragonScene", "loadCustomScenarios SUCCESS size: ${sorted.size}")
            if (filtered.size != decoded.size) {
                saveCustomScenariosToPrefs()
            }
        } catch (e: Exception) {
            android.util.Log.e("DragonScene", "loadCustomScenarios ERROR", e)
        }
    }

    private fun saveCustomScenariosToPrefs() {
        val snapshot = _customScenarios.value
        android.util.Log.d("DragonScene", "saveCustomScenariosToPrefs size: ${snapshot.size}")
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val encoded = json.encodeToString(snapshot)
                val ok = prefs.edit().putString("saved_custom_scenarios", encoded).commit()
                android.util.Log.d("DragonScene", "saveCustomScenariosToPrefs committed: $ok, length: ${encoded.length}")
            } catch (e: Exception) {
                android.util.Log.e("DragonScene", "saveCustomScenariosToPrefs ERROR", e)
            }
        }
    }

    /**
     * Propagates a freshly resolved station across all active state holders:
     * active playlist, favorites repository, custom scenarios, and scenario station overrides.
     */
    fun updateStationEverywhere(freshStation: RadioStation) {
        // 1. Update current playlist
        val curList = _playlist.value.toMutableList()
        val pIdx = curList.indexOfFirst { it.id == freshStation.id }
        if (pIdx != -1) {
            curList[pIdx] = freshStation
            _playlist.value = curList
        }

        // 2. Update favorites and history repository
        favoritesRepo.updateStation(freshStation)

        // 3. Update custom scenarios
        val curScenarios = _customScenarios.value.toMutableList()
        var scenariosChanged = false
        for (i in curScenarios.indices) {
            val sc = curScenarios[i]
            val sIdx = sc.stations.indexOfFirst { it.id == freshStation.id }
            if (sIdx != -1) {
                val scList = sc.stations.toMutableList()
                scList[sIdx] = freshStation
                curScenarios[i] = sc.copy(stations = scList)
                scenariosChanged = true
            }
        }
        if (scenariosChanged) {
            _customScenarios.value = curScenarios
            saveCustomScenariosToPrefs()
        }

        // 4. Update builtin scenario overrides
        val overrides = _scenarioStationOverrides.value.toMutableMap()
        var overridesChanged = false
        for ((key, stList) in overrides) {
            val sIdx = stList.indexOfFirst { it.id == freshStation.id }
            if (sIdx != -1) {
                val newList = stList.toMutableList()
                newList[sIdx] = freshStation
                overrides[key] = newList
                overridesChanged = true
            }
        }
        if (overridesChanged) {
            _scenarioStationOverrides.value = overrides
            saveScenarioStationOverridesToPrefs()
        }

        // 5. Update playback memory if active
        updateSavedPlaylistIfMemoryEnabled()
    }

    /**
     * Asynchronously validates and warms up fresh broadcast stream URLs for a playlist in the background.
     * Only checks stations whose streams are actually expired to prevent startup storms.
     */
    private fun reconcilePlaylistInWarmup(stations: List<RadioStation>) {
        if (stations.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            val expired = stations.filter { DedicatedSourceManager.isStreamUrlExpired(it.playUrl) }
            if (expired.isEmpty()) return@launch
            for (st in expired) {
                val fresh = DedicatedSourceManager.resolveFreshStationAsync(st)
                if (fresh.playUrl != st.playUrl) {
                    withContext(Dispatchers.Main) {
                        updateStationEverywhere(fresh)
                    }
                }
            }
        }
    }

    fun saveCustomScenario(
        title: String,
        iconEmoji: String,
        query: String = "",
        replaceId: String? = null,
        initialStation: RadioStation? = null
    ): String {
        val current = _customScenarios.value.toMutableList()
        val id = replaceId ?: "mode_${System.currentTimeMillis()}"
        val existingIndex = current.indexOfFirst { it.id == id }
        val stations = if (existingIndex >= 0) {
            val list = current[existingIndex].stations.toMutableList()
            if (initialStation != null && list.none { it.id == initialStation.id }) {
                list.add(0, initialStation)
            }
            list
        } else {
            if (initialStation != null) listOf(initialStation) else emptyList()
        }

        val newMode = CustomScenarioMode(
            id = id,
            title = title,
            iconEmoji = iconEmoji,
            query = query,
            stations = stations
        )

        if (existingIndex >= 0) {
            current[existingIndex] = newMode
        } else {
            current.add(0, newMode)
            val curOrder = _scenarioOrder.value.toMutableList()
            if (curOrder.isNotEmpty() && !curOrder.contains(title)) {
                val favIdx = curOrder.indexOf("我的收藏")
                val insertIdx = if (favIdx >= 0) favIdx + 1 else 0
                curOrder.add(insertIdx.coerceIn(0, curOrder.size), title)
                _scenarioOrder.value = curOrder
                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        val ok = prefs.edit().putString("saved_scenario_order", json.encodeToString(curOrder)).commit()
                        android.util.Log.d("DragonScene", "saveCustomScenario order committed: $ok")
                    } catch (e: Exception) {
                        android.util.Log.e("DragonScene", "saveCustomScenario order ERROR", e)
                    }
                }
            }
        }
        _customScenarios.value = current
        saveCustomScenariosToPrefs()
        return id
    }

    fun createCustomScenario(
        title: String,
        iconKey: String,
        initialStations: List<RadioStation> = emptyList()
    ): String {
        val id = saveCustomScenario(
            title = title,
            iconEmoji = iconKey,
            initialStation = initialStations.firstOrNull()
        )
        if (initialStations.size > 1) {
            val current = _customScenarios.value.toMutableList()
            val idx = current.indexOfFirst { it.id == id }
            if (idx >= 0) {
                current[idx] = current[idx].copy(stations = initialStations)
                _customScenarios.value = current
                saveCustomScenariosToPrefs()
            }
        }
        return id
    }

    fun updateCustomScenario(id: String, newTitle: String, newIconKey: String) {
        val current = _customScenarios.value.toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index >= 0) {
            val oldTitle = current[index].title
            val item = current[index]
            current[index] = item.copy(title = newTitle, iconEmoji = newIconKey)
            _customScenarios.value = current
            saveCustomScenariosToPrefs()

            val curOrder = _scenarioOrder.value.toMutableList()
            val orderIdx = curOrder.indexOf(oldTitle)
            if (orderIdx >= 0) {
                curOrder[orderIdx] = newTitle
                _scenarioOrder.value = curOrder
                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        val ok = prefs.edit().putString("saved_scenario_order", json.encodeToString(curOrder)).commit()
                        android.util.Log.d("DragonScene", "saveCustomScenario order committed: $ok")
                    } catch (e: Exception) {
                        android.util.Log.e("DragonScene", "saveCustomScenario order ERROR", e)
                    }
                }
            }
        }
    }

    fun reorderCustomScenarios(newList: List<CustomScenarioMode>) {
        _customScenarios.value = newList
        saveCustomScenariosToPrefs()
    }

    fun deleteCustomScenario(id: String) {
        val current = _customScenarios.value.toMutableList()
        val match = current.find { it.id == id }
        val titleToDelete = match?.title
        current.removeAll { it.id == id }
        _customScenarios.value = current
        saveCustomScenariosToPrefs()

        if (titleToDelete != null) {
            val curOrder = _scenarioOrder.value.toMutableList()
            if (curOrder.remove(titleToDelete)) {
                _scenarioOrder.value = curOrder
                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        val ok = prefs.edit().putString("saved_scenario_order", json.encodeToString(curOrder)).commit()
                        android.util.Log.d("DragonScene", "saveCustomScenario order committed: $ok")
                    } catch (e: Exception) {
                        android.util.Log.e("DragonScene", "saveCustomScenario order ERROR", e)
                    }
                }
            }
        }
    }

    private fun loadDeletedDefaultScenarios() {
        val set = prefs.getStringSet("deleted_default_scenarios", emptySet()) ?: emptySet()
        android.util.Log.d("DragonScene", "loadDeletedDefaultScenarios set: $set")
        _deletedDefaultScenarios.value = set
    }

    fun deleteDefaultScenario(key: String) {
        val current = _deletedDefaultScenarios.value.toMutableSet()
        current.add(key)
        _deletedDefaultScenarios.value = current
        val ok = prefs.edit().putStringSet("deleted_default_scenarios", HashSet(current)).commit()
        android.util.Log.d("DragonScene", "deleteDefaultScenario key: $key, committed: $ok, total: ${current.size}")
    }

    fun restoreDefaultScenarios() {
        _deletedDefaultScenarios.value = emptySet()
        val ok = prefs.edit().remove("deleted_default_scenarios").commit()
        android.util.Log.d("DragonScene", "restoreDefaultScenarios committed: $ok")
    }

    // ================= Local Backup & Restore (本地场景、收藏与自定义音乐源备份恢复) =================
    private val backupJson = Json { ignoreUnknownKeys = true; prettyPrint = true }

    /**
     * 生成完整的个性化配置备份 JSON 字符串
     */
    fun createBackupJson(): String {
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
        val backup = AppBackupData(
            version = 1,
            appName = "Dragon FM",
            appVersion = "v$currentVersionName (Build $currentVersionCode)",
            timestamp = System.currentTimeMillis(),
            exportDate = dateFormat.format(java.util.Date()),
            customScenarios = _customScenarios.value,
            scenarioStationOverrides = _scenarioStationOverrides.value,
            scenarioOrder = _scenarioOrder.value,
            deletedDefaultScenarios = _deletedDefaultScenarios.value.toList(),
            favoriteStations = favoritesRepo.favoriteStations.value,
            customStations = favoritesRepo.customStations.value,
            customNames = favoritesRepo.customNames.value,
            historyStations = favoritesRepo.history.value
        )
        return backupJson.encodeToString(backup)
    }

    /**
     * 将个性化备份 JSON 写入指定 SAF Uri
     */
    fun exportBackupToUri(uri: android.net.Uri): Boolean {
        return try {
            val content = createBackupJson()
            getApplication<Application>().contentResolver.openOutputStream(uri)?.use { os ->
                os.write(content.toByteArray(Charsets.UTF_8))
                os.flush()
            }
            true
        } catch (e: Exception) {
            android.util.Log.e("DragonBackup", "exportBackupToUri failed", e)
            false
        }
    }

    /**
     * 从指定 SAF Uri 读取并恢复个性化备份
     */
    fun importBackupFromUri(uri: android.net.Uri, onComplete: (BackupRestoreSummary) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val jsonString = getApplication<Application>().contentResolver.openInputStream(uri)?.use { stream ->
                    stream.bufferedReader(Charsets.UTF_8).readText()
                }
                if (jsonString.isNullOrBlank()) {
                    withContext(Dispatchers.Main) {
                        onComplete(BackupRestoreSummary(success = false, message = "读取备份文件失败或文件内容为空"))
                    }
                    return@launch
                }

                val backup: AppBackupData = json.decodeFromString(jsonString)
                val summary = restoreBackupData(backup)
                withContext(Dispatchers.Main) {
                    refreshCurrentScenarioPlaylistQuietly()
                    onComplete(summary)
                }
            } catch (e: Exception) {
                android.util.Log.e("DragonBackup", "importBackupFromUri failed", e)
                withContext(Dispatchers.Main) {
                    onComplete(BackupRestoreSummary(success = false, message = "恢复失败: 备份文件格式错误或已损坏 (${e.localizedMessage})"))
                }
            }
        }
    }

    /**
     * 安全无声刷新当前场景的播放列表，不打断用户当前的播放状态
     */
    private fun refreshCurrentScenarioPlaylistQuietly() {
        val cleanScenario = _selectedScenario.value.replace(Regex("[\\p{So}\\p{Sk}]"), "").trim()
        if (cleanScenario == "我的收藏" || cleanScenario.contains("收藏")) {
            _playlist.value = favoritesRepo.favoriteStations.value
            return
        }
        val customMatch = _customScenarios.value.find { 
            val cleanTitle = it.title.replace(Regex("[\\p{So}\\p{Sk}]"), "").trim()
            cleanTitle == cleanScenario || it.id == _selectedScenario.value || it.title == _selectedScenario.value
        }
        if (customMatch != null && customMatch.stations.isNotEmpty()) {
            _playlist.value = customMatch.stations
            return
        }
        val overriddenList = _scenarioStationOverrides.value[cleanScenario]
        if (!overriddenList.isNullOrEmpty()) {
            _playlist.value = overriddenList
        }
    }

    /**
     * 解析并应用个性化备份数据
     */
    fun restoreBackupData(backup: AppBackupData): BackupRestoreSummary {
        var scenarioCount = 0
        var favoriteCount = 0
        var customStationCount = 0
        var historyCount = 0

        // 1. 恢复场景排序
        if (backup.scenarioOrder.isNotEmpty()) {
            _scenarioOrder.value = backup.scenarioOrder
            prefs.edit().putString("saved_scenario_order", json.encodeToString(backup.scenarioOrder)).commit()
        }

        // 2. 恢复自定义场景
        if (backup.customScenarios.isNotEmpty()) {
            _customScenarios.value = backup.customScenarios
            prefs.edit().putString("saved_custom_scenarios", json.encodeToString(backup.customScenarios)).commit()
            scenarioCount += backup.customScenarios.size
        }

        // 3. 恢复内置场景覆盖
        if (backup.scenarioStationOverrides.isNotEmpty()) {
            _scenarioStationOverrides.value = backup.scenarioStationOverrides
            prefs.edit().putString("saved_scenario_stations", json.encodeToString(backup.scenarioStationOverrides)).commit()
            scenarioCount += backup.scenarioStationOverrides.size
        }

        // 4. 恢复已删除默认场景
        if (backup.deletedDefaultScenarios.isNotEmpty()) {
            val set = backup.deletedDefaultScenarios.toSet()
            _deletedDefaultScenarios.value = set
            prefs.edit().putStringSet("deleted_default_scenarios", HashSet(set)).commit()
        }

        // 5. 恢复收藏、自定义电台、别名映射及历史记录
        favoritesRepo.importBackupData(
            newFavorites = backup.favoriteStations.ifEmpty { null },
            newCustomStations = backup.customStations.ifEmpty { null },
            newCustomNames = backup.customNames.ifEmpty { null },
            newHistory = backup.historyStations.ifEmpty { null }
        )
        favoriteCount = backup.favoriteStations.size
        customStationCount = backup.customStations.size
        historyCount = backup.historyStations.size

        return BackupRestoreSummary(
            success = true,
            scenarioCount = scenarioCount,
            favoriteCount = favoriteCount,
            customStationCount = customStationCount,
            historyCount = historyCount,
            message = "恢复成功！已导入 $scenarioCount 个场景配置、$favoriteCount 个收藏电台、$customStationCount 个自定义音乐源"
        )
    }

    fun getBaseStationsForScenario(cleanName: String): List<RadioStation> {
        return when {
            cleanName.contains("交通") || cleanName.contains("路况") -> PresetsData.trafficStations
            cleanName.contains("通勤") -> PresetsData.commuteStations
            cleanName.contains("助眠") -> PresetsData.sleepStations
            cleanName.contains("专注") -> PresetsData.focusStations
            cleanName.contains("新闻") -> PresetsData.newsStations
            cleanName.contains("摇滚") -> PresetsData.rockStations
            cleanName.contains("经典") -> PresetsData.classicStations
            cleanName.contains("本地") -> {
                val prov = if (_isCustomRegionEnabled.value) _userSelectedProvince.value else _detectedProvince.value
                val city = if (_isCustomRegionEnabled.value) _userSelectedCity.value else _detectedCity.value
                val list = mutableListOf<RadioStation>()
                if (!city.isNullOrBlank()) list.addAll(PresetsData.findStationsByCity(city))
                if (!prov.isNullOrBlank()) {
                    val cityIds = list.map { it.id }.toSet()
                    list.addAll(PresetsData.findStationsByProvince(prov).filter { it.id !in cityIds })
                }
                if (list.isEmpty()) list.addAll(PresetsData.nationalStations)
                list
            }
            else -> PresetsData.featuredStations
        }
    }

    fun getScenariosContainingStation(stationId: String): Set<String> {
        val set = mutableSetOf<String>()
        // 1. Custom scenarios
        _customScenarios.value.forEach { mode ->
            if (mode.stations.any { it.id == stationId }) {
                set.add(mode.title)
            }
        }
        // 2. Builtin scenario overrides
        _scenarioStationOverrides.value.forEach { (name, list) ->
            if (list.any { it.id == stationId }) {
                set.add(name)
            }
        }
        // 3. Favorites mode
        if (favoritesRepo.isFavorite(stationId)) {
            set.add("我的收藏")
        }
        return set
    }

    fun toggleStationInScenario(scenarioName: String, station: RadioStation): Boolean {
        val cleanName = scenarioName.replace(Regex("[\\p{So}\\p{Sk}]"), "").trim()
        val isAlreadyIn = getScenariosContainingStation(station.id).contains(cleanName)
        if (isAlreadyIn) {
            removeStationFromScenario(cleanName, station.id)
            return false
        } else {
            addStationToScenario(cleanName, station)
            return true
        }
    }

    fun addStationToScenario(scenarioName: String, station: RadioStation) {
        val cleanName = scenarioName.replace(Regex("[\\p{So}\\p{Sk}]"), "").trim()
        if (cleanName == "我的收藏" || cleanName.contains("收藏")) {
            if (!favoritesRepo.isFavorite(station.id)) {
                favoritesRepo.toggleFavorite(station)
            }
            if (_selectedScenario.value.contains("收藏")) {
                _playlist.value = favoritesRepo.favoriteStations.value
            }
            return
        }

        val customIndex = _customScenarios.value.indexOfFirst {
            val title = it.title.replace(Regex("[\\p{So}\\p{Sk}]"), "").trim()
            title == cleanName || it.id == scenarioName
        }

        if (customIndex >= 0) {
            val list = _customScenarios.value.toMutableList()
            val mode = list[customIndex]
            val newStations = mode.stations.toMutableList()
            newStations.removeAll { it.id == station.id }
            newStations.add(0, station)
            list[customIndex] = mode.copy(stations = newStations)
            _customScenarios.value = list
            saveCustomScenariosToPrefs()

            if (_selectedScenario.value == mode.title || _selectedScenario.value == cleanName) {
                _playlist.value = newStations
                updateSavedPlaylistIfMemoryEnabled()
            }
        } else {
            val currentOverrides = _scenarioStationOverrides.value.toMutableMap()
            val existingList = (currentOverrides[cleanName] ?: getBaseStationsForScenario(cleanName)).toMutableList()
            existingList.removeAll { it.id == station.id }
            existingList.add(0, station)
            currentOverrides[cleanName] = existingList
            _scenarioStationOverrides.value = currentOverrides
            saveScenarioStationOverridesToPrefs()

            if (_selectedScenario.value == cleanName) {
                _playlist.value = existingList
                updateSavedPlaylistIfMemoryEnabled()
            }
        }

        // Also sync into favorites so heart lights up
        if (!favoritesRepo.isFavorite(station.id)) {
            favoritesRepo.toggleFavorite(station)
        }
    }

    fun removeStationFromCurrentPlaylist(stationId: String) {
        val updated = _playlist.value.filter { it.id != stationId }
        _playlist.value = updated
        val curIdx = _currentIndex.value.coerceIn(0, (updated.size - 1).coerceAtLeast(0))
        if (updated.isNotEmpty()) {
            _currentIndex.value = curIdx
        }
        val cleanName = _selectedScenario.value.replace(Regex("[\\p{So}\\p{Sk}]"), "").trim()
        removeStationFromScenario(cleanName, stationId)
        updateSavedPlaylistIfMemoryEnabled()
    }

    fun removeStationFromScenario(scenarioName: String, stationId: String) {
        val cleanName = scenarioName.replace(Regex("[\\p{So}\\p{Sk}]"), "").trim()
        val cleanSelected = _selectedScenario.value.replace(Regex("[\\p{So}\\p{Sk}]"), "").trim()

        // 无论何种场景，当前活动播放列表同步过滤删除
        val updated = _playlist.value.filter { it.id != stationId }
        _playlist.value = updated
        val curIdx = _currentIndex.value.coerceIn(0, (updated.size - 1).coerceAtLeast(0))
        if (updated.isNotEmpty()) {
            _currentIndex.value = curIdx
        }
        updateSavedPlaylistIfMemoryEnabled()

        if (cleanName == "我的收藏" || cleanName.contains("收藏") || cleanSelected.contains("收藏")) {
            favoritesRepo.removeFavorite(stationId)
            return
        }

        val customIndex = _customScenarios.value.indexOfFirst {
            val title = it.title.replace(Regex("[\\p{So}\\p{Sk}]"), "").trim()
            title == cleanName || it.id == scenarioName || title == cleanSelected
        }

        if (customIndex >= 0) {
            val list = _customScenarios.value.toMutableList()
            val mode = list[customIndex]
            val newStations = mode.stations.filter { it.id != stationId }
            list[customIndex] = mode.copy(stations = newStations)
            _customScenarios.value = list
            saveCustomScenariosToPrefs()
        }

        // 同时也对 _scenarioStationOverrides 全键名彻底清理，防止切换场景时被旧预设或缓存唤醒
        val currentOverrides = _scenarioStationOverrides.value.toMutableMap()
        val baseList = currentOverrides[cleanName] ?: currentOverrides[cleanSelected] ?: currentOverrides[scenarioName] ?: getBaseStationsForScenario(cleanName)
        val existingList = baseList.filter { it.id != stationId }
        currentOverrides[cleanName] = existingList
        currentOverrides[cleanSelected] = existingList
        currentOverrides[scenarioName] = existingList
        _scenarioStationOverrides.value = currentOverrides
        saveScenarioStationOverridesToPrefs()
    }

    fun moveStationInScenario(scenarioName: String, fromIndex: Int, toIndex: Int) {
        val cleanName = scenarioName.replace(Regex("[\\p{So}\\p{Sk}]"), "").trim()
        val currentList = _playlist.value.toMutableList()
        if (fromIndex in currentList.indices && toIndex in currentList.indices && fromIndex != toIndex) {
            val item = currentList.removeAt(fromIndex)
            currentList.add(toIndex, item)
            _playlist.value = currentList

            val cur = _currentIndex.value
            _currentIndex.value = when {
                cur == fromIndex -> toIndex
                fromIndex < toIndex && cur in (fromIndex + 1)..toIndex -> cur - 1
                fromIndex > toIndex && cur in toIndex until fromIndex -> cur + 1
                else -> cur
            }

            val customIndex = _customScenarios.value.indexOfFirst {
                it.title.replace(Regex("[\\p{So}\\p{Sk}]"), "").trim() == cleanName
            }
            if (customIndex >= 0) {
                val list = _customScenarios.value.toMutableList()
                list[customIndex] = list[customIndex].copy(stations = currentList)
                _customScenarios.value = list
                saveCustomScenariosToPrefs()
            } else {
                val currentOverrides = _scenarioStationOverrides.value.toMutableMap()
                currentOverrides[cleanName] = currentList
                _scenarioStationOverrides.value = currentOverrides
                saveScenarioStationOverridesToPrefs()
            }
            updateSavedPlaylistIfMemoryEnabled()
        }
    }

    fun setDomesticOnly(enabled: Boolean) {
        exclusionManager.setDomesticOnly(enabled)
        val filtered = exclusionManager.filterStations(_playlist.value)
        _playlist.value = filtered.ifEmpty { exclusionManager.filterStations(PresetsData.defaultStations) }
    }

    fun blockStation(station: RadioStation) {
        exclusionManager.blockStation(station)
        val filtered = exclusionManager.filterStations(_playlist.value)
        _playlist.value = filtered.ifEmpty { exclusionManager.filterStations(PresetsData.defaultStations) }
    }

    fun blockStationByName(name: String) {
        exclusionManager.blockStationByName(name)
        val filtered = exclusionManager.filterStations(_playlist.value)
        _playlist.value = filtered.ifEmpty { exclusionManager.filterStations(PresetsData.defaultStations) }
    }

    fun unblockStation(stationId: String) {
        exclusionManager.unblockStation(stationId)
    }

    fun clearAllBlockedStations() {
        exclusionManager.clearAllBlockedStations()
    }

    fun blockTag(tag: String) {
        exclusionManager.blockTag(tag)
        val filtered = exclusionManager.filterStations(_playlist.value)
        _playlist.value = filtered.ifEmpty { exclusionManager.filterStations(PresetsData.defaultStations) }
    }

    fun unblockTag(tag: String) {
        exclusionManager.unblockTag(tag)
    }

    fun removeFavorite(stationId: String) {
        favoritesRepo.removeFavorite(stationId)
    }

    fun pinFavorite(stationId: String) {
        favoritesRepo.pinFavorite(stationId)
    }

    fun removeFromHistory(stationId: String) {
        favoritesRepo.removeFromHistory(stationId)
    }

    fun clearHistory() {
        favoritesRepo.clearHistory()
    }

    private fun expandPlaylistWithChinaStations() {
        viewModelScope.launch {
            api.getChinaStations(limit = 80).onSuccess { chinaList ->
                val filteredChina = exclusionManager.filterStations(chinaList)
                if (filteredChina.isNotEmpty()) {
                    val existingIds = _playlist.value.map { it.id }.toSet()
                    val existingNames = _playlist.value.map { it.name.trim() }.toSet()
                    val newStations = filteredChina.filter { it.id !in existingIds && it.name.trim() !in existingNames }
                    if (newStations.isNotEmpty()) {
                        _playlist.value = _playlist.value + newStations
                        updateSavedPlaylistIfMemoryEnabled()
                    }
                }
            }
        }
    }

    private fun detectLocalLocation() {
        viewModelScope.launch {
            val loc = IpLocationService.detectLocation()
            if (loc != null) {
                _detectedProvince.value = loc.province
                _detectedCity.value = loc.city
                val label = if (loc.city.isNotBlank() && loc.city != loc.province) {
                    "${loc.province} · ${loc.city}"
                } else {
                    loc.province
                }
                _detectedLocation.value = "本地 ($label)"

                // If user is currently listening to local scenario and not on custom region, reload local stations!
                if (_selectedScenario.value.contains("本地") && !_isCustomRegionEnabled.value) {
                    loadLocalScenarioStations(loc.province, loc.city)
                }
            } else {
                _detectedLocation.value = "本地电台"
            }
        }
    }

    fun loadLocalScenarioStations(targetProvince: String?, targetCity: String?) {
        val prov = targetProvince?.trim()
            ?.removeSuffix("省")?.removeSuffix("市")?.removeSuffix("自治区")?.removeSuffix("特别行政区")
        val ct = targetCity?.trim()
            ?.removeSuffix("市")?.removeSuffix("区")?.removeSuffix("县")?.removeSuffix("地区")
            ?.takeIf { it != "全城通用" && it != "全省通用" && it != "全区通用" }

        val resultStations = mutableListOf<RadioStation>()

        if (!ct.isNullOrBlank()) {
            // Level 1: City level (范围由小到大: 最优先展示本地地级市相关广播，如郑州新闻、郑州音乐)
            val cityStations = PresetsData.findStationsByCity(ct)
            resultStations.addAll(cityStations)
        }

        if (!prov.isNullOrBlank()) {
            // Level 2: Province level (其次展示省份广播，如河南广播，严格排除国内其他省市广播)
            val existingIds = resultStations.map { it.id }.toSet()
            val provinceStations = PresetsData.findStationsByProvince(prov).filter { it.id !in existingIds }
            resultStations.addAll(provinceStations)
        }

        // Level 3: Fallback if neither city nor province broadcasts exist
        if (resultStations.isEmpty()) {
            // 如没有郑州且没有河南广播，或没有选择地区，就从国家广播展示 (CNR, CCTV)
            resultStations.addAll(PresetsData.nationalStations)
        }

        val filtered = exclusionManager.filterStations(resultStations)
        _playlist.value = filtered
        if (filtered.isNotEmpty()) {
            playStationAtIndex(0)
            updateSavedPlaylistIfMemoryEnabled()
        }

        // High-availability dedicated stream integration: fetch province dedicated channels and merge redundant lines
        viewModelScope.launch {
            val dedicatedList = if (!prov.isNullOrBlank()) {
                DedicatedSourceManager.getStationsByProvince(prov)
            } else {
                DedicatedSourceManager.getNationalStations()
            }
            if (dedicatedList.isNotEmpty()) {
                val merged = DedicatedSourceManager.mergeWithDedicatedStations(_playlist.value, dedicatedList)
                _playlist.value = exclusionManager.filterStations(merged)
                updateSavedPlaylistIfMemoryEnabled()
            }

            if (!prov.isNullOrBlank()) {
                api.getChinaStationsByProvince(prov, limit = 40).onSuccess { list ->
                    val cleanOnline = exclusionManager.filterStations(list).filter { st ->
                        val text = "${st.name} ${st.tags} ${st.state ?: ""}".lowercase()
                        text.contains(prov.lowercase()) || (!ct.isNullOrBlank() && text.contains(ct.lowercase()))
                    }
                    if (cleanOnline.isNotEmpty()) {
                        val currentList = _playlist.value
                        val currentIds = currentList.map { it.id }.toSet()
                        val toAdd = cleanOnline.filter { it.id !in currentIds }
                        if (toAdd.isNotEmpty()) {
                            val combined = currentList + toAdd
                            val finalMerged = if (dedicatedList.isNotEmpty()) {
                                DedicatedSourceManager.mergeWithDedicatedStations(combined, dedicatedList)
                            } else combined
                            _playlist.value = exclusionManager.filterStations(finalMerged)
                            updateSavedPlaylistIfMemoryEnabled()
                        }
                    }
                }
            }
        }
    }

    fun startRandomRoaming() {
        viewModelScope.launch {
            _selectedScenario.value = "随机漫游"
            _isSearching.value = true

            // Local preset pool shuffled without recently played
            val localPool = PresetsData.allStations.filter { it.id !in recentRoamingIds }.shuffled()
            val initialBatch = (if (localPool.isNotEmpty()) localPool.take(20) else PresetsData.defaultStations).toMutableList()

            _playlist.value = exclusionManager.filterStations(initialBatch)
            if (_playlist.value.isNotEmpty()) {
                playStationAtIndex(0)
            }
            _isSearching.value = false

            // Append online diverse batch ahead of user
            appendRandomRoamingBatch()
        }
    }

    fun appendRandomRoamingBatch() {
        viewModelScope.launch {
            val tagsPool = listOf("流行", "rock", "jazz", "民谣", "ambient", "古典", "chill", "classic", "hits", "cpop")
            val randomTag = tagsPool.random()
            val domestic = exclusionManager.isDomesticOnly.value

            val cloudResult = if (domestic) {
                api.searchStations(randomTag, limit = 40)
            } else {
                api.getStationsByTag(randomTag, limit = 40)
            }

            val cloudList = cloudResult.getOrNull()?.let { exclusionManager.filterStations(it) } ?: emptyList()
            val currentIds = _playlist.value.map { it.id }.toSet()

            // Merge unplayed presets + cloud stations
            val candidatePool = (PresetsData.allStations.filter { it.id !in recentRoamingIds && it.id !in currentIds } +
                    cloudList.filter { it.id !in recentRoamingIds && it.id !in currentIds }).shuffled().take(15)

            if (candidatePool.isNotEmpty()) {
                _playlist.value = _playlist.value + candidatePool
            }
        }
    }

    fun playFavoritesPlaylist(startStation: RadioStation? = null) {
        val favs = favoriteStations.value
        if (favs.isNotEmpty()) {
            _selectedScenario.value = "我的收藏"
            val freshFavs = DedicatedSourceManager.refreshStationList(favs)
            val targetIndex = if (startStation != null) {
                freshFavs.indexOfFirst { it.id == startStation.id }.coerceAtLeast(0)
            } else 0
            _currentIndex.value = targetIndex
            _playlist.value = freshFavs
            playStationAtIndex(targetIndex)
            reconcilePlaylistInWarmup(freshFavs)
        } else {
            _selectedScenario.value = "精选推荐"
            val defaultList = exclusionManager.filterStations(PresetsData.defaultStations)
            _currentIndex.value = 0
            _playlist.value = defaultList
            playStationAtIndex(0)
        }
    }

    fun playHistoryPlaylist(startStation: RadioStation? = null) {
        val hist = historyStations.value
        if (hist.isNotEmpty()) {
            _selectedScenario.value = "收听足迹"
            val freshHist = DedicatedSourceManager.refreshStationList(hist)
            val targetIndex = if (startStation != null) {
                freshHist.indexOfFirst { it.id == startStation.id }.coerceAtLeast(0)
            } else 0
            _currentIndex.value = targetIndex
            _playlist.value = freshHist
            playStationAtIndex(targetIndex)
            reconcilePlaylistInWarmup(freshHist)
        } else if (startStation != null) {
            playStation(startStation)
        }
    }

    fun getAllAvailableScenarios(): List<String> {
        val builtIn = listOf(
            "精选推荐", "潮流通勤", "路况交通", "晚间助眠", "咖啡专注",
            "实时新闻", "华语经典", "摇滚动感", "环球视界", "本地电台", "我的收藏"
        )
        val custom = _customScenarios.value.map { it.title }
        val all = (custom + builtIn).distinct()
        val order = _scenarioOrder.value
        return if (order.isNotEmpty()) {
            all.sortedBy { key ->
                val idx = order.indexOf(key)
                if (idx >= 0) idx else 999
            }
        } else {
            all
        }
    }

    fun switchToNextScenario(): String {
        val list = getAllAvailableScenarios()
        val cur = _selectedScenario.value
        val curIdx = list.indexOfFirst { it == cur || it.contains(cur) || cur.contains(it) }.coerceAtLeast(0)
        val nextIdx = (curIdx + 1) % list.size
        val target = list[nextIdx]
        selectScenario(target)
        return target
    }

    fun switchToPreviousScenario(): String {
        val list = getAllAvailableScenarios()
        val cur = _selectedScenario.value
        val curIdx = list.indexOfFirst { it == cur || it.contains(cur) || cur.contains(it) }.coerceAtLeast(0)
        val prevIdx = if (curIdx - 1 < 0) list.size - 1 else curIdx - 1
        val target = list[prevIdx]
        selectScenario(target)
        return target
    }

    fun selectScenario(scenario: String) {
        val cleanScenario = scenario.replace(Regex("[\\p{So}\\p{Sk}]"), "").trim()
        _selectedScenario.value = cleanScenario

        if (cleanScenario == "我的收藏" || cleanScenario.contains("收藏")) {
            playFavoritesPlaylist()
            return
        }

        // Check if custom scenario
        val customMatch = _customScenarios.value.find { 
            val cleanTitle = it.title.replace(Regex("[\\p{So}\\p{Sk}]"), "").trim()
            cleanTitle == cleanScenario || it.id == scenario || it.title == scenario
        }
        if (customMatch != null) {
            if (customMatch.stations.isNotEmpty()) {
                val blockedMap = exclusionManager.blockedStations.value
                val filtered = customMatch.stations.filterNot { 
                    blockedMap.containsKey(it.id) || blockedMap.values.any { bName -> bName.equals(it.name, ignoreCase = true) }
                }
                if (filtered.isNotEmpty()) {
                    val freshStations = DedicatedSourceManager.refreshStationList(filtered)
                    _playlist.value = freshStations
                    playStationAtIndex(0)
                    reconcilePlaylistInWarmup(freshStations)
                    return
                }
            }
            viewModelScope.launch {
                _isSearching.value = true
                val query = customMatch.query.ifBlank { customMatch.title }.trim()
                // 1. Instant local preset matching with query (guaranteed fast zero-latency response)
                val localMatches = exclusionManager.filterStations(PresetsData.findStationsByKeyword(query))
                if (localMatches.isNotEmpty()) {
                    _playlist.value = localMatches
                    playStationAtIndex(0)
                }

                // 2. Online search with domestic filter respected
                val isDomestic = exclusionManager.isDomesticOnly.value
                api.searchStations(query, isDomestic = isDomestic, limit = 60).onSuccess { list ->
                    val filtered = exclusionManager.filterStations(list)
                    val combined = (localMatches + filtered).distinctBy { it.id }
                    if (combined.isNotEmpty()) {
                        _playlist.value = combined
                        updateSavedPlaylistIfMemoryEnabled()
                        if (localMatches.isEmpty()) {
                            playStationAtIndex(0)
                        }
                    }
                }
                _isSearching.value = false
            }
            return
        }

        // Check if builtin scenario has custom user overrides
        val overriddenList = _scenarioStationOverrides.value[cleanScenario]
        if (!overriddenList.isNullOrEmpty()) {
            val blockedMap = exclusionManager.blockedStations.value
            val filtered = overriddenList.filterNot { 
                blockedMap.containsKey(it.id) || blockedMap.values.any { bName -> bName.equals(it.name, ignoreCase = true) }
            }
            if (filtered.isNotEmpty()) {
                val freshStations = DedicatedSourceManager.refreshStationList(filtered)
                _playlist.value = freshStations
                playStationAtIndex(0)
                reconcilePlaylistInWarmup(freshStations)
                return
            }
        }

        when {
            cleanScenario.contains("随机漫游") || cleanScenario == "每日发现" -> {
                startRandomRoaming()
            }
            cleanScenario.contains("路况") || cleanScenario.contains("交通") -> {
                _playlist.value = exclusionManager.filterStations(PresetsData.trafficStations)
                playStationAtIndex(0)
                viewModelScope.launch {
                    val isDomestic = exclusionManager.isDomesticOnly.value
                    api.searchStations("交通", isDomestic = isDomestic, limit = 40).onSuccess { list ->
                        val clean = exclusionManager.filterStations(list).filter {
                            val lower = (it.name + " " + it.tags).lowercase()
                            lower.contains("交通") || lower.contains("路况") || lower.contains("traffic")
                        }
                        if (clean.isNotEmpty()) {
                            val existingIds = _playlist.value.map { it.id }.toSet()
                            _playlist.value = _playlist.value + clean.filter { it.id !in existingIds }
                            updateSavedPlaylistIfMemoryEnabled()
                        }
                    }
                }
            }
            cleanScenario.contains("潮流通勤") || cleanScenario.contains("通勤") -> {
                _playlist.value = exclusionManager.filterStations(PresetsData.commuteStations)
                playStationAtIndex(0)
                viewModelScope.launch {
                    val isDomestic = exclusionManager.isDomesticOnly.value
                    api.searchStations("流行", isDomestic = isDomestic, limit = 40).onSuccess { list ->
                        val clean = exclusionManager.filterStations(list).filter {
                            val lower = (it.name + " " + it.tags).lowercase()
                            !lower.contains("新闻") && !lower.contains("news") && !lower.contains("助眠") &&
                            !lower.contains("交通") && !lower.contains("路况")
                        }
                        if (clean.isNotEmpty()) {
                            val existingIds = _playlist.value.map { it.id }.toSet()
                            _playlist.value = _playlist.value + clean.filter { it.id !in existingIds }
                            updateSavedPlaylistIfMemoryEnabled()
                        }
                    }
                }
            }
            cleanScenario.contains("晚间助眠") || cleanScenario.contains("助眠") -> {
                _playlist.value = exclusionManager.filterStations(PresetsData.sleepStations)
                playStationAtIndex(0)
                viewModelScope.launch {
                    val isDomestic = exclusionManager.isDomesticOnly.value
                    api.searchStations("轻音乐", isDomestic = isDomestic, limit = 40).onSuccess { list ->
                        val clean = exclusionManager.filterStations(list).filter {
                            val lower = (it.name + " " + it.tags).lowercase()
                            !lower.contains("新闻") && !lower.contains("news") && !lower.contains("rock") &&
                            !lower.contains("戏曲") && !lower.contains("京剧") && !lower.contains("talk")
                        }
                        if (clean.isNotEmpty()) {
                            val existingIds = _playlist.value.map { it.id }.toSet()
                            _playlist.value = _playlist.value + clean.filter { it.id !in existingIds }
                            updateSavedPlaylistIfMemoryEnabled()
                        }
                    }
                }
            }
            cleanScenario.contains("咖啡专注") || cleanScenario.contains("专注") -> {
                _playlist.value = exclusionManager.filterStations(PresetsData.focusStations)
                playStationAtIndex(0)
                viewModelScope.launch {
                    api.getStationsByTag("jazz", limit = 40).onSuccess { list ->
                        val clean = exclusionManager.filterStations(list).filter {
                            val lower = (it.name + " " + it.tags).lowercase()
                            !lower.contains("新闻") && !lower.contains("news") && !lower.contains("talk")
                        }
                        if (clean.isNotEmpty()) {
                            val existingIds = _playlist.value.map { it.id }.toSet()
                            _playlist.value = _playlist.value + clean.filter { it.id !in existingIds }
                            updateSavedPlaylistIfMemoryEnabled()
                        }
                    }
                }
            }
            cleanScenario.contains("资讯谈话") || cleanScenario.contains("实时新闻") || cleanScenario.contains("新闻") -> {
                _playlist.value = exclusionManager.filterStations(PresetsData.newsStations)
                playStationAtIndex(0)
                viewModelScope.launch {
                    val isDomestic = exclusionManager.isDomesticOnly.value
                    api.searchStations("新闻", isDomestic = isDomestic, limit = 40).onSuccess { list ->
                        val clean = exclusionManager.filterStations(list)
                        if (clean.isNotEmpty()) {
                            val existingIds = _playlist.value.map { it.id }.toSet()
                            _playlist.value = _playlist.value + clean.filter { it.id !in existingIds }
                            updateSavedPlaylistIfMemoryEnabled()
                        }
                    }
                }
            }
            cleanScenario.contains("摇滚") -> {
                _playlist.value = exclusionManager.filterStations(PresetsData.rockStations)
                playStationAtIndex(0)
                viewModelScope.launch {
                    api.searchStations("rock", limit = 40).onSuccess { list ->
                        val clean = exclusionManager.filterStations(list).filter {
                            val lower = (it.name + " " + it.tags).lowercase()
                            !lower.contains("新闻") && !lower.contains("news") && !lower.contains("talk") &&
                                    !lower.contains("助眠") && !lower.contains("ambient")
                        }
                        if (clean.isNotEmpty()) {
                            val existingIds = _playlist.value.map { it.id }.toSet()
                            _playlist.value = _playlist.value + clean.filter { it.id !in existingIds }
                            updateSavedPlaylistIfMemoryEnabled()
                        }
                    }
                }
            }
            cleanScenario.contains("经典") -> {
                _playlist.value = exclusionManager.filterStations(PresetsData.classicStations)
                playStationAtIndex(0)
                viewModelScope.launch {
                    val isDomestic = exclusionManager.isDomesticOnly.value
                    api.searchStations("经典", isDomestic = isDomestic, limit = 40).onSuccess { list ->
                        val clean = exclusionManager.filterStations(list).filter {
                            val lower = (it.name + " " + it.tags).lowercase()
                            !lower.contains("新闻") && !lower.contains("news")
                        }
                        if (clean.isNotEmpty()) {
                            val existingIds = _playlist.value.map { it.id }.toSet()
                            _playlist.value = _playlist.value + clean.filter { it.id !in existingIds }
                            updateSavedPlaylistIfMemoryEnabled()
                        }
                    }
                }
            }
            cleanScenario.contains("环球") -> {
                _playlist.value = exclusionManager.filterStations(PresetsData.worldStations)
                playStationAtIndex(0)
            }
            cleanScenario.contains("本地") -> {
                val customEnabled = _isCustomRegionEnabled.value
                val prov = if (customEnabled) _userSelectedProvince.value else _detectedProvince.value
                val city = if (customEnabled) _userSelectedCity.value else _detectedCity.value
                loadLocalScenarioStations(prov, city)
            }
            else -> {
                // "精选推荐"
                _playlist.value = exclusionManager.filterStations(PresetsData.featuredStations)
                playStationAtIndex(0)
                expandPlaylistWithChinaStations()
            }
        }
    }

    fun selectCountry(country: String) {
        viewModelScope.launch {
            _selectedScenario.value = country
            _isSearching.value = true
            api.getStationsByCountry(country, limit = 100).onSuccess { list ->
                if (list.isNotEmpty()) {
                    _playlist.value = list
                    playStationAtIndex(0)
                }
            }
            _isSearching.value = false
        }
    }

    fun toggleSleepMode() {
        _isSleepModeActive.value = !_isSleepModeActive.value
        if (_isSleepModeActive.value && sleepTimerSeconds.value == null) {
            // Auto start 30 min timer on sleep mode by default
            setSleepTimer(30)
        }
    }

    fun playStationAtIndex(index: Int) {
        val list = _playlist.value
        if (index in list.indices) {
            localRecordingPlayer.stop()
            _currentIndex.value = index
            val station = list[index]
            val freshStation = DedicatedSourceManager.resolveFreshStation(station)
            onStationChangedForSubtitle()
            val isFav = favoritesRepo.isFavorite(freshStation.id)
            val updated = favoritesRepo.applyCustomName(freshStation.copy(isFavorite = isFav))
            if (freshStation.playUrl != station.playUrl) {
                val updatedList = list.toMutableList()
                updatedList[index] = updated
                _playlist.value = updatedList
                updateStationEverywhere(updated)
            }
            player.playStation(updated)
            favoritesRepo.addToHistory(updated)

            // Deduplicate in recent roaming queue
            recentRoamingIds.remove(station.id)
            recentRoamingIds.addLast(station.id)
            if (recentRoamingIds.size > 200) {
                recentRoamingIds.removeFirst()
            }

            // Infinite Roaming: Automatically extend playlist ahead when near end
            if (_selectedScenario.value.contains("随机漫游") && index >= list.size - 4) {
                appendRandomRoamingBatch()
            }

            // Warm up adjacent stations (i-1 and i+1) to slash channel-switch TTFB latency
            val prevStation = list.getOrNull(index - 1)
            val nextStation = list.getOrNull(index + 1)
            player.warmUpAdjacentStations(listOfNotNull(prevStation, nextStation))

            // Save playback memory (station, scenario, playlist, index)
            savePlaybackMemory(updated, _selectedScenario.value, list, index)
        }
    }

    fun prepareStationAtIndex(index: Int) {
        val list = _playlist.value
        if (index in list.indices) {
            _currentIndex.value = index
            val station = list[index]
            val freshStation = DedicatedSourceManager.resolveFreshStation(station)
            onStationChangedForSubtitle()
            val isFav = favoritesRepo.isFavorite(freshStation.id)
            val updated = favoritesRepo.applyCustomName(freshStation.copy(isFavorite = isFav))
            if (freshStation.playUrl != station.playUrl) {
                val updatedList = list.toMutableList()
                updatedList[index] = updated
                _playlist.value = updatedList
                updateStationEverywhere(updated)
            }
            player.prepareStation(updated)
            favoritesRepo.addToHistory(updated)

            val prevStation = list.getOrNull(index - 1)
            val nextStation = list.getOrNull(index + 1)
            player.warmUpAdjacentStations(listOfNotNull(prevStation, nextStation))

            savePlaybackMemory(updated, _selectedScenario.value, list, index)
        }
    }

    fun renameFavoriteStation(stationId: String, newName: String?) {
        favoritesRepo.setCustomStationName(stationId, newName)
        val currentList = _playlist.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == stationId }
        if (index != -1) {
            val updatedStation = favoritesRepo.applyCustomName(currentList[index])
            currentList[index] = updatedStation
            _playlist.value = currentList
            player.updateStationName(stationId, updatedStation.name)
        }
    }

    fun reorderPlaylist(newList: List<RadioStation>) {
        _playlist.value = newList
        val curStation = playerState.value.currentStation
        if (curStation != null) {
            val newIdx = newList.indexOfFirst { it.id == curStation.id }
            if (newIdx != -1) {
                _currentIndex.value = newIdx
            }
        }

        val curScenario = _selectedScenario.value
        val cleanName = curScenario.replace(Regex("[\\p{So}\\p{Sk}]"), "").trim()

        if (curScenario.contains("收藏") || cleanName == "我的收藏") {
            favoritesRepo.reorderFavorites(newList)
        } else {
            val customIndex = _customScenarios.value.indexOfFirst {
                val title = it.title.replace(Regex("[\\p{So}\\p{Sk}]"), "").trim()
                title == cleanName || it.id == curScenario || it.title == curScenario
            }
            if (customIndex >= 0) {
                val list = _customScenarios.value.toMutableList()
                list[customIndex] = list[customIndex].copy(stations = newList)
                _customScenarios.value = list
                saveCustomScenariosToPrefs()
            } else {
                val currentOverrides = _scenarioStationOverrides.value.toMutableMap()
                currentOverrides[cleanName] = newList
                currentOverrides[curScenario] = newList
                _scenarioStationOverrides.value = currentOverrides
                saveScenarioStationOverridesToPrefs()
            }
        }

        updateSavedPlaylistIfMemoryEnabled()
    }

    fun playStation(station: RadioStation) {
        val freshStation = DedicatedSourceManager.resolveFreshStation(station)
        val list = _playlist.value.toMutableList()
        var targetIndex = list.indexOfFirst { it.id == freshStation.id }
        if (targetIndex == -1) {
            list.add(freshStation)
            targetIndex = list.lastIndex
        } else {
            list[targetIndex] = freshStation
        }
        _currentIndex.value = targetIndex
        _playlist.value = list
        playStationAtIndex(targetIndex)
    }

    /**
     * Seamlessly adopts the entire explore/search list into the vertical flow pager
     */
    fun playStationFromExplore(station: RadioStation) {
        val exploreList = _exploreStations.value
        if (exploreList.isNotEmpty()) {
            _playlist.value = exploreList
            val targetIndex = exploreList.indexOfFirst { it.id == station.id }
            playStationAtIndex(if (targetIndex >= 0) targetIndex else 0)
        } else {
            playStation(station)
        }
    }

    fun nextStation() {
        val list = _playlist.value
        if (list.isNotEmpty()) {
            if (_selectedScenario.value.contains("随机漫游") && _currentIndex.value >= list.size - 2) {
                appendRandomRoamingBatch()
            }
            val next = (_currentIndex.value + 1) % list.size
            playStationAtIndex(next)
        }
    }

    fun previousStation() {
        val list = _playlist.value
        if (list.isNotEmpty()) {
            val prev = if (_currentIndex.value - 1 < 0) list.size - 1 else _currentIndex.value - 1
            playStationAtIndex(prev)
        }
    }

    fun togglePlayPause() {
        player.togglePlayPause()
    }

    fun toggleFavoriteCurrent() {
        val current = playerState.value.currentStation ?: return
        toggleFavorite(current)
    }

    fun toggleFavorite(station: RadioStation) {
        val isNowFav = favoritesRepo.toggleFavorite(station)
        player.updateFavoriteStatus(station.id, isNowFav)
        val list = _playlist.value
        val idx = list.indexOfFirst { it.id == station.id }
        if (idx >= 0) {
            val updated = list.toMutableList()
            updated[idx] = updated[idx].copy(isFavorite = isNowFav)
            _playlist.value = updated
        }
    }

    fun toggleRecording() {
        val current = playerState.value.currentStation ?: return
        if (recorderManager.isRecording.value) {
            recorderManager.stopRecording()
        } else {
            recorderManager.startRecording(current)
        }
    }

    // ================= Music Recognition (听歌识曲) =================
    private val _floatingRecognizedSong = MutableStateFlow<RecognizedSong?>(null)
    val floatingRecognizedSong: StateFlow<RecognizedSong?> = _floatingRecognizedSong.asStateFlow()

    fun showToast(message: String) {
        viewModelScope.launch(Dispatchers.Main) {
            try {
                android.widget.Toast.makeText(getApplication(), message, android.widget.Toast.LENGTH_SHORT).show()
            } catch (_: Exception) {}
        }
    }

    fun dismissFloatingRecognizedSong() {
        _floatingRecognizedSong.value = null
    }

    /**
     * 极简听歌识曲切换：
     * 无需弹窗、无需麦克风环境拾音，直接捕获播放器原生音频流；
     * 状态完全反馈在界面按钮自身（识别中/已识别/重试）。
     */
    fun toggleMusicRecognition() {
        val curState = recognitionState.value
        if (curState is RecognitionState.Sampling || curState is RecognitionState.Fingerprinting || curState is RecognitionState.QueryingCloud) {
            recognitionManager.resetState()
            showToast("已停止识曲")
            return
        }

        val station = playerState.value.currentStation
        if (playerState.value.status != PlaybackStatus.PLAYING || station == null) {
            showToast("当前广播未播放，请先播放电台")
            return
        }

        recognitionManager.resetState()
        _currentRecognitionMode.value = RecognitionMode.RADIO_STREAM
        recognitionManager.recognizeCurrentStation(station, player.liveAudioCaptureProcessor)
    }

    fun startSilentRecognition() {
        toggleMusicRecognition()
    }

    fun openMusicRecognition(preferredMode: RecognitionMode? = null) {
        toggleMusicRecognition()
    }

    fun closeMusicRecognition() {
        _showRecognitionDialog.value = false
        recognitionManager.resetState()
    }

    fun startMusicRecognition(mode: RecognitionMode) {
        toggleMusicRecognition()
    }

    fun applyRecognizedSongToNowPlaying(song: RecognizedSong) {
        val title = "${song.title} - ${song.artist}"
        player.updateNowPlayingTitle(title)
    }

    fun deleteRecognizedSong(id: String) {
        recognitionManager.deleteSong(id)
    }

    fun clearRecognitionHistory() {
        recognitionManager.clearHistory()
    }

    fun setSleepTimer(minutes: Int) {
        if (minutes <= 0) {
            sleepTimer.cancelTimer()
        } else {
            sleepTimer.startTimer(minutes)
        }
    }

    private fun prewarmExploreCache() {
        viewModelScope.launch(Dispatchers.IO) {
            // 1. Prewarm "全国精选"
            val initialPresets = exclusionManager.filterStations(PresetsData.defaultStations)
            val nationalDedicated = DedicatedSourceManager.getCachedNationalStations().ifEmpty {
                DedicatedSourceManager.getNationalStations()
            }
            val mergedPresets = if (nationalDedicated.isNotEmpty()) {
                DedicatedSourceManager.mergeWithDedicatedStations(initialPresets, nationalDedicated)
            } else initialPresets
            exploreStationCache["全国精选"] = mergedPresets
            if (_exploreStations.value.isEmpty()) {
                _exploreStations.value = mergedPresets
            }

            // 2. Prewarm top categories from local presets
            val categoriesToPrewarm = listOf(
                "央视伴音" to listOf("CCTV", "央视", "伴音"),
                "国家广播" to listOf("CNR", "CRI", "中国之声", "环球资讯", "华语环球"),
                "热门流行" to listOf("流行", "音乐", "金曲", "潮声", "pop", "cpop"),
                "实时新闻" to listOf("新闻", "资讯", "联播", "权威", "news"),
                "交通出行" to listOf("交通", "路况", "畅通", "交广", "出行", "traffic"),
                "经典老歌" to listOf("经典", "岁月", "典藏", "老歌", "金曲", "oldies"),
                "轻音助眠" to listOf("助眠", "轻音乐", "舒缓", "纯净", "冥想", "ambient", "chill"),
                "流行音乐" to listOf("流行", "音乐", "金曲", "动感", "飞扬", "pop", "cpop"),
                "新闻资讯" to listOf("新闻", "资讯", "权威", "早新闻", "联播", "news"),
                "交通路况" to listOf("交通", "路况", "交广", "畅通", "出行", "traffic"),
                "财经经济" to listOf("财经", "经济", "股市", "商业", "第一财经", "finance"),
                "华语经典" to listOf("经典", "岁月", "典藏", "老歌", "华语", "oldies"),
                "文艺戏剧" to listOf("文艺", "戏曲", "梨园", "剧场", "京剧", "百家"),
                "粤语名声" to listOf("粤语", "南粤", "大湾区", "珠江", "香港", "cantopop")
            )
            for ((catTitle, terms) in categoriesToPrewarm) {
                val matches = exclusionManager.filterStations(PresetsData.findStationsByKeywords(terms))
                val dedicatedMatches = DedicatedSourceManager.searchCachedDedicatedStations(terms)
                val merged = if (dedicatedMatches.isNotEmpty()) {
                    DedicatedSourceManager.mergeWithDedicatedStations(matches, dedicatedMatches)
                } else matches
                if (merged.isNotEmpty()) {
                    exploreStationCache[catTitle] = merged
                }
            }

            // 3. Prewarm user's local province if available
            val userProv = _userSelectedProvince.value ?: _detectedProvince.value
            if (!userProv.isNullOrBlank()) {
                val provMatches = exclusionManager.filterStations(PresetsData.findStationsByProvince(userProv))
                if (provMatches.isNotEmpty()) {
                    exploreStationCache[userProv] = provMatches
                }
            }
        }
    }

    fun loadExploreInitial(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _currentExploreTag.value = "全国精选"

            // Check cache first for 0ms instantaneous load
            if (!forceRefresh) {
                val cached = exploreStationCache["全国精选"]
                if (!cached.isNullOrEmpty()) {
                    _exploreStations.value = cached
                    _isSearching.value = false
                    return@launch
                }
            }

            // Phase 1: Instant local display (< 5ms)
            val initialPresets = exclusionManager.filterStations(PresetsData.defaultStations)
            val nationalDedicated = DedicatedSourceManager.getCachedNationalStations()
            val mergedPresets = if (nationalDedicated.isNotEmpty()) {
                DedicatedSourceManager.mergeWithDedicatedStations(initialPresets, nationalDedicated)
            } else initialPresets

            if (mergedPresets.isNotEmpty()) {
                _exploreStations.value = mergedPresets
                exploreStationCache["全国精选"] = mergedPresets
                _isSearching.value = false
            } else {
                _isSearching.value = true
            }

            // Phase 2: If national dedicated wasn't cached, fetch asynchronously
            if (nationalDedicated.isEmpty() && DedicatedSourceManager.currentSourceMode != com.dragon.fm.data.model.StreamSourceMode.PUBLIC_ONLY) {
                val freshNational = DedicatedSourceManager.getNationalStations()
                if (freshNational.isNotEmpty()) {
                    val updated = DedicatedSourceManager.mergeWithDedicatedStations(_exploreStations.value, freshNational)
                    _exploreStations.value = updated
                    exploreStationCache["全国精选"] = updated
                    _isSearching.value = false
                }
            }

            // Phase 3: Cloud enrichment (only if mode allows public stations)
            if (DedicatedSourceManager.currentSourceMode != com.dragon.fm.data.model.StreamSourceMode.DEDICATED_ONLY) {
                api.getChinaStations(limit = 100).onSuccess { cloudList ->
                    val filtered = exclusionManager.filterStations(cloudList)
                    if (filtered.isNotEmpty()) {
                        val current = _exploreStations.value
                        val existingIds = current.map { it.id }.toSet()
                        val combined = current + filtered.filter { it.id !in existingIds }
                        val finalMerged = if (nationalDedicated.isNotEmpty()) {
                            DedicatedSourceManager.mergeWithDedicatedStations(combined, nationalDedicated)
                        } else combined
                        _exploreStations.value = finalMerged
                        exploreStationCache["全国精选"] = finalMerged
                    }
                }
            }
            _isSearching.value = false
        }
    }

    fun searchByRealTag(tagItem: com.dragon.fm.data.model.RealExploreTagItem) {
        viewModelScope.launch {
            _currentExploreTag.value = tagItem.title
            _radarSearchQuery.value = ""

            // Check cache first for 0ms instantaneous switch!
            val cached = exploreStationCache[tagItem.title]
            if (!cached.isNullOrEmpty()) {
                _exploreStations.value = cached
                _isSearching.value = false
                return@launch
            }

            when {
                tagItem.isProvince -> {
                    fetchProvinceStations(tagItem.title)
                }
                tagItem.isCountry -> {
                    fetchCountryStations(tagItem.searchTerms.firstOrNull() ?: tagItem.title)
                }
                tagItem.title == "全国精选" -> {
                    loadExploreInitial()
                }
                else -> {
                    // 1. Instant local preset matching with multi-term coverage (guaranteed 0ms result)
                    val localMatches = exclusionManager.filterStations(
                        PresetsData.findStationsByKeywords(tagItem.searchTerms)
                    )
                    val dedicatedMatches = DedicatedSourceManager.searchCachedDedicatedStations(tagItem.searchTerms)
                    val mergedLocal = if (dedicatedMatches.isNotEmpty()) {
                        DedicatedSourceManager.mergeWithDedicatedStations(localMatches, dedicatedMatches)
                    } else localMatches

                    if (mergedLocal.isNotEmpty()) {
                        _exploreStations.value = mergedLocal
                        exploreStationCache[tagItem.title] = mergedLocal
                        // If we already have items, don't block the UI with a full-screen spinner
                        _isSearching.value = false
                    } else {
                        _exploreStations.value = emptyList()
                        _isSearching.value = true
                    }

                    // 2. Query cloud stations only if mode is HYBRID or PUBLIC_ONLY
                    if (DedicatedSourceManager.currentSourceMode != com.dragon.fm.data.model.StreamSourceMode.DEDICATED_ONLY) {
                        val isDomestic = exclusionManager.isDomesticOnly.value
                        api.searchStationsWithTerms(tagItem.searchTerms, isDomestic = isDomestic, limit = 80).onSuccess { cloudList ->
                            val filteredCloud = exclusionManager.filterStations(cloudList)
                            val combined = (mergedLocal + filteredCloud).distinctBy { it.id }
                            val finalMerged = if (dedicatedMatches.isNotEmpty()) {
                                DedicatedSourceManager.mergeWithDedicatedStations(combined, dedicatedMatches)
                            } else combined
                            if (finalMerged.isNotEmpty()) {
                                _exploreStations.value = finalMerged
                                exploreStationCache[tagItem.title] = finalMerged
                            }
                        }
                    }
                    _isSearching.value = false
                }
            }
        }
    }

    /**
     * Strict keyword matching logic ensuring search results ONLY contain stations
     * genuinely relevant to the search query (name, state, city mapping, tags).
     * Prevents previous/unrelated stations from contaminating search results.
     */
    fun isStationStrictlyMatchingKeyword(station: RadioStation, query: String): Boolean {
        val cleanQ = query.trim().lowercase()
        if (cleanQ.isBlank()) return false

        val nameLower = station.name.lowercase()
        // 1. Station name direct substring match (e.g. "西安音乐广播" contains "西安")
        if (nameLower.contains(cleanQ)) return true

        // 2. Multi-word tokens match (e.g. "西安 音乐" -> all words present in station name)
        val tokens = cleanQ.split("\\s+".toRegex()).filter { it.isNotBlank() }
        if (tokens.size > 1 && tokens.all { nameLower.contains(it) }) return true

        // 3. Normalized name match (e.g. ignoring prefixes like CNR/FM/AM/广播电视台)
        val normSt = DedicatedSourceManager.normalizeName(station.name).lowercase()
        val normQ = DedicatedSourceManager.normalizeName(cleanQ).lowercase()
        if (normQ.isNotBlank() && normSt.contains(normQ)) return true

        // 4. Region/State direct match (e.g. searching "陕西" matches state "陕西")
        val stateLower = station.state?.lowercase().orEmpty()
        if (stateLower.isNotBlank() && stateLower.contains(cleanQ)) return true

        // 5. City-to-Province relation:
        // If user searched a city like "西安", match stations whose state/city is Xi'an
        val cityProv = DedicatedSourceManager.cityToProvinceMap[query.trim()]
        if (cityProv != null && stateLower.contains(cleanQ)) return true

        // 6. Explicit tag match (e.g. tag token matches query)
        val tags = station.tags.split(",", " ", ";", "，", "；").map { it.trim().lowercase() }
        if (tags.any { it == cleanQ }) return true

        return false
    }

    private fun sortStationsByRelevance(stations: List<RadioStation>, query: String): List<RadioStation> {
        val cleanQ = query.trim().lowercase()
        val normQ = DedicatedSourceManager.normalizeName(cleanQ).lowercase()
        return stations.sortedWith(
            compareByDescending<RadioStation> { st ->
                val nameLower = st.name.trim().lowercase()
                when {
                    nameLower == cleanQ -> 100
                    nameLower.startsWith(cleanQ) -> 80
                    nameLower.contains(cleanQ) -> 60
                    normQ.isNotBlank() && DedicatedSourceManager.normalizeName(st.name).lowercase().contains(normQ) -> 40
                    st.state?.lowercase()?.contains(cleanQ) == true -> 20
                    else -> 10
                }
            }.thenByDescending {
                it.hasDedicatedLine || it.lines.any { l -> l.isStable }
            }.thenByDescending {
                it.votes
            }
        )
    }

    fun searchOnline(query: String) {
        val cleanQ = query.trim()
        if (cleanQ.isBlank()) return
        viewModelScope.launch {
            val cacheKey = "query:$cleanQ"
            val cached = exploreStationCache[cacheKey]
            if (!cached.isNullOrEmpty()) {
                _exploreStations.value = cached
                _isSearching.value = false
                return@launch
            }

            _isSearching.value = true
            val isDomestic = exclusionManager.isDomesticOnly.value

            // Step 1: Instant local preset matching (zero latency display)
            // Filter strictly by keyword so old/unrelated presets are never included
            val rawLocal = PresetsData.findStationsByKeyword(cleanQ)
            val localMatches = exclusionManager.filterStations(rawLocal)
                .filter { isStationStrictlyMatchingKeyword(it, cleanQ) }

            // Immediately set to local matches (or empty) to completely clear previous unrelated stations
            _exploreStations.value = localMatches
            if (localMatches.isNotEmpty()) {
                _isSearching.value = false
            }

            // Step 2: Query dedicated high-availability official streams
            val rawDedicated = DedicatedSourceManager.searchDedicatedStations(cleanQ, _userSelectedProvince.value ?: _detectedProvince.value)
            val dedicatedMatches = exclusionManager.filterStations(rawDedicated)
                .filter { isStationStrictlyMatchingKeyword(it, cleanQ) }

            // Merge local matches with dedicated matches (DO NOT use _exploreStations.value as base!)
            val localAndDedicated = if (dedicatedMatches.isNotEmpty()) {
                if (localMatches.isNotEmpty()) {
                    DedicatedSourceManager.mergeWithDedicatedStations(localMatches, dedicatedMatches)
                } else {
                    dedicatedMatches
                }
            } else {
                localMatches
            }.filter { isStationStrictlyMatchingKeyword(it, cleanQ) }

            if (localAndDedicated.isNotEmpty()) {
                val sorted = sortStationsByRelevance(localAndDedicated, cleanQ)
                _exploreStations.value = sorted
                _isSearching.value = false
            }

            // Step 3: Query cloud stations with domestic scoping (only if mode allows)
            if (DedicatedSourceManager.currentSourceMode != com.dragon.fm.data.model.StreamSourceMode.DEDICATED_ONLY) {
                api.searchStations(cleanQ, isDomestic = isDomestic, limit = 100).onSuccess { cloudList ->
                    val filteredCloud = exclusionManager.filterStations(cloudList)
                        .filter { isStationStrictlyMatchingKeyword(it, cleanQ) }

                    // Merge filtered cloud results and local matches with dedicated matches
                    val allBases = (localMatches + filteredCloud).distinctBy { it.id }
                    val combined = if (dedicatedMatches.isNotEmpty()) {
                        DedicatedSourceManager.mergeWithDedicatedStations(allBases, dedicatedMatches)
                    } else {
                        (localAndDedicated + filteredCloud).distinctBy { it.id }
                    }

                    // Strict final filtering against the search keyword
                    val finalStrict = exclusionManager.filterStations(combined)
                        .filter { isStationStrictlyMatchingKeyword(it, cleanQ) }

                    val sorted = sortStationsByRelevance(finalStrict, cleanQ)
                    if (sorted.isNotEmpty()) {
                        _exploreStations.value = sorted
                        exploreStationCache[cacheKey] = sorted
                    }
                }.onFailure {
                    // Cloud query failed; preserve local/dedicated matches if any
                }
            }

            val finalResult = _exploreStations.value
            if (finalResult.isNotEmpty()) {
                exploreStationCache[cacheKey] = finalResult
            }
            _isSearching.value = false
        }
    }

    fun openTagExplore(tagQuery: String) {
        _radarSearchQuery.value = tagQuery
        _currentExploreTag.value = tagQuery
        searchOnline(tagQuery)
    }

    fun clearRadarQuery() {
        _radarSearchQuery.value = ""
    }

    fun fetchTagStations(tag: String) {
        viewModelScope.launch {
            val cached = exploreStationCache[tag]
            if (!cached.isNullOrEmpty()) {
                _exploreStations.value = cached
                _isSearching.value = false
                return@launch
            }
            _isSearching.value = true
            val localMatches = exclusionManager.filterStations(PresetsData.findStationsByKeyword(tag))
            if (localMatches.isNotEmpty()) {
                _exploreStations.value = localMatches
                _isSearching.value = false
            }

            api.getStationsByTag(tag, limit = 100).onSuccess { cloudList ->
                val filteredCloud = exclusionManager.filterStations(cloudList)
                val combined = (localMatches + filteredCloud).distinctBy { it.id }
                if (combined.isNotEmpty()) {
                    _exploreStations.value = combined
                    exploreStationCache[tag] = combined
                }
            }
            _isSearching.value = false
        }
    }

    fun fetchCountryStations(country: String) {
        viewModelScope.launch {
            val cached = exploreStationCache[country]
            if (!cached.isNullOrEmpty()) {
                _exploreStations.value = cached
                _isSearching.value = false
                return@launch
            }
            _isSearching.value = true
            api.getStationsByCountry(country, limit = 100).onSuccess {
                val filtered = exclusionManager.filterStations(it)
                _exploreStations.value = filtered
                exploreStationCache[country] = filtered
            }
            _isSearching.value = false
        }
    }

    fun fetchProvinceStations(province: String) {
        viewModelScope.launch {
            // Check cache first for 0ms switch
            val cached = exploreStationCache[province]
            if (!cached.isNullOrEmpty()) {
                _exploreStations.value = cached
                _isSearching.value = false
                return@launch
            }

            // Step 1: Instant local preset matching for province (< 5ms)
            val localMatches = exclusionManager.filterStations(PresetsData.findStationsByProvince(province))
            val cachedDedicated = DedicatedSourceManager.getCachedStationsByProvince(province)
            val initial = if (cachedDedicated.isNotEmpty()) {
                DedicatedSourceManager.mergeWithDedicatedStations(localMatches, cachedDedicated)
            } else localMatches

            if (initial.isNotEmpty()) {
                _exploreStations.value = initial
                _isSearching.value = false
            } else {
                _isSearching.value = true
            }

            // Step 2: Fetch dedicated official streams for province (domestic, ~100-200ms)
            var currentList = initial
            if (cachedDedicated.isEmpty() && DedicatedSourceManager.currentSourceMode != com.dragon.fm.data.model.StreamSourceMode.PUBLIC_ONLY) {
                val dedicated = DedicatedSourceManager.getStationsByProvince(province)
                if (dedicated.isNotEmpty()) {
                    currentList = DedicatedSourceManager.mergeWithDedicatedStations(currentList, dedicated)
                    _exploreStations.value = exclusionManager.filterStations(currentList)
                    exploreStationCache[province] = _exploreStations.value
                    _isSearching.value = false
                }
            }

            // Step 3: Query province cloud stations (only if not DEDICATED_ONLY)
            if (DedicatedSourceManager.currentSourceMode != com.dragon.fm.data.model.StreamSourceMode.DEDICATED_ONLY) {
                api.getChinaStationsByProvince(province, limit = 80).onSuccess { cloudList ->
                    val filteredCloud = exclusionManager.filterStations(cloudList)
                    val combined = (currentList + filteredCloud).distinctBy { it.id }
                    if (combined.isNotEmpty()) {
                        _exploreStations.value = combined
                        exploreStationCache[province] = combined
                    }
                }
            }
            if (_exploreStations.value.isNotEmpty()) {
                exploreStationCache[province] = _exploreStations.value
            }
            _isSearching.value = false
        }
    }

    fun switchLine(lineIndex: Int) {
        player.switchLine(lineIndex)
        val cur = playerState.value.currentStation ?: return
        val curList = _playlist.value
        val idx = curList.indexOfFirst { it.id == cur.id }
        if (idx != -1) {
            val updated = curList[idx].copy(activeLineIndex = lineIndex)
            val updatedList = curList.toMutableList()
            updatedList[idx] = updated
            _playlist.value = updatedList
        }
        val exploreList = _exploreStations.value
        val exploreIdx = exploreList.indexOfFirst { it.id == cur.id }
        if (exploreIdx != -1) {
            val updatedExplore = exploreList.toMutableList()
            updatedExplore[exploreIdx] = updatedExplore[exploreIdx].copy(activeLineIndex = lineIndex)
            _exploreStations.value = updatedExplore
        }
        val targetLineName = cur.effectiveLines.getOrNull(lineIndex)?.name ?: "线路 ${lineIndex + 1}"
        showToast("已切换至: $targetLineName")
    }

    fun retryPlayback() {
        player.retryCurrentStation()
    }

    fun quitApplication(activity: android.app.Activity?) {
        player.stop()
        sleepTimer.cancelTimer()
        recorderManager.stopRecording()
        com.dragon.fm.player.DragonMediaSessionService.stopPlaybackAndService(getApplication())
        activity?.finishAffinity()
    }

    fun runSpeedTest() {
        viewModelScope.launch {
            val list = if (_exploreStations.value.isNotEmpty()) _exploreStations.value else _playlist.value
            com.dragon.fm.player.StationSpeedTester.batchTestStations(list)
            if (_isSortByLatency.value) {
                sortPlaylistByLatency()
            }
        }
    }

    fun runPlaylistSpeedTest() {
        viewModelScope.launch {
            val list = _playlist.value
            com.dragon.fm.player.StationSpeedTester.batchTestStations(list)
            sortPlaylistByLatency()
        }
    }

    fun toggleSortByLatency() {
        val next = !_isSortByLatency.value
        _isSortByLatency.value = next
        if (next) {
            viewModelScope.launch {
                val list = _playlist.value
                com.dragon.fm.player.StationSpeedTester.batchTestStations(list)
                sortPlaylistByLatency()
            }
        }
    }

    fun sortPlaylistByLatency() {
        val lMap = latencies.value
        val sorted = _playlist.value.sortedWith(
            compareBy(
                { val l = lMap[it.id]; if (l != null && l > 0) 0 else 1 },
                { lMap[it.id] ?: Long.MAX_VALUE }
            )
        )
        _playlist.value = sorted
    }

    fun sortExploreByLatency() {
        val lMap = latencies.value
        val sorted = _exploreStations.value.sortedWith(
            compareBy(
                { val l = lMap[it.id]; if (l != null && l > 0) 0 else 1 },
                { lMap[it.id] ?: Long.MAX_VALUE }
            )
        )
        _exploreStations.value = sorted
    }

    fun addCustomStation(name: String, url: String) {
        val station = RadioStation(
            id = "custom-${System.currentTimeMillis()}",
            name = name,
            url = url,
            urlResolved = url,
            country = "自定义频段",
            tags = "User Stream",
            frequencyMhz = 95.5f
        )
        favoritesRepo.addCustomStation(station)
        playStation(station)
    }

    fun deleteRecordedFile(file: RecordedFile) {
        if (playingRecordedFile.value?.file?.absolutePath == file.file.absolutePath) {
            localRecordingPlayer.stop()
        }
        recorderManager.deleteRecording(file.file)
    }

    fun playRecordedFile(file: RecordedFile) {
        player.pause()
        localRecordingPlayer.playFile(file)
    }

    fun toggleRecordedFilePlayPause() {
        localRecordingPlayer.togglePlayPause()
    }

    fun seekRecordedFile(posMs: Long) {
        localRecordingPlayer.seekTo(posMs)
    }

    fun setRecordedFileSpeed(speed: Float) {
        localRecordingPlayer.setSpeed(speed)
    }

    fun cycleRecordedFileSpeed(): Float {
        return localRecordingPlayer.cycleSpeed()
    }

    fun stopRecordedFile() {
        localRecordingPlayer.stop()
    }

    fun refreshRecordings() {
        recorderManager.refreshRecordings()
    }

    override fun onCleared() {
        super.onCleared()
        localRecordingPlayer.release()
    }
}
