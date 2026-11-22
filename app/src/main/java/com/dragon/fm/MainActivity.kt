package com.dragon.fm

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.platform.LocalContext
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.core.view.WindowCompat
import coil.compose.AsyncImage
import com.dragon.fm.data.model.AppBackgroundType
import com.dragon.fm.data.model.BackgroundPresets
import com.dragon.fm.data.model.PlaybackStatus
import com.dragon.fm.data.model.UpdateCheckResult
import com.dragon.fm.ui.components.AppUpdateDialog
import com.dragon.fm.ui.screens.DragonFlowScreen
import com.dragon.fm.ui.screens.LibraryScreen
import com.dragon.fm.ui.screens.RadarExploreScreen
import com.dragon.fm.ui.screens.SettingsScreen
import com.dragon.fm.ui.screens.SleepModeOverlay
import com.dragon.fm.ui.screens.StudioDrawerSheet
import com.dragon.fm.ui.theme.AppearanceSchedule
import com.dragon.fm.ui.theme.DragonFMTheme
import com.dragon.fm.ui.theme.ThemePreset
import com.dragon.fm.ui.viewmodel.MainViewModel
import java.io.File

enum class ScreenRoute {
    FLOW,
    RADAR,
    LIBRARY,
    SETTINGS
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)

        // Enable edge-to-edge full screen experience
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val appearanceModeName by viewModel.appearanceMode.collectAsState()
            val appearanceMode = try { AppearanceSchedule.Mode.valueOf(appearanceModeName) } catch (_: Exception) { AppearanceSchedule.Mode.SYSTEM }
            val dayStartMinutes by viewModel.appearanceDayStart.collectAsState()
            val nightStartMinutes by viewModel.appearanceNightStart.collectAsState()
            val latitude by viewModel.appearanceLatitude.collectAsState()
            val longitude by viewModel.appearanceLongitude.collectAsState()
            var appearanceMinute by remember { mutableLongStateOf(System.currentTimeMillis()) }
            if (appearanceMode == AppearanceSchedule.Mode.SCHEDULE || appearanceMode == AppearanceSchedule.Mode.SUNRISE_SUNSET) {
                androidx.compose.runtime.LaunchedEffect(appearanceMode) {
                    while (true) {
                        appearanceMinute = System.currentTimeMillis()
                        kotlinx.coroutines.delay(60_000L)
                    }
                }
            }
            val currentTheme by viewModel.currentTheme.collectAsState()
            val isFollowSystemTheme by viewModel.isFollowSystemTheme.collectAsState()
            val selectedLightThemeId by viewModel.selectedLightThemeId.collectAsState()
            val selectedDarkThemeId by viewModel.selectedDarkThemeId.collectAsState()
            val isSystemDark = isSystemInDarkTheme()

            val isScheduledDark = AppearanceSchedule.isDark(
                appearanceMode, isSystemDark, currentTheme.isDark, appearanceMinute,
                java.util.TimeZone.getDefault(), dayStartMinutes, nightStartMinutes, latitude, longitude
            )
            val effectiveTheme = remember(currentTheme, isFollowSystemTheme, isScheduledDark, selectedLightThemeId, selectedDarkThemeId) {
                if (isFollowSystemTheme || appearanceMode == AppearanceSchedule.Mode.SCHEDULE || appearanceMode == AppearanceSchedule.Mode.SUNRISE_SUNSET) {
                    if (isScheduledDark) {
                        ThemePreset.fromId(selectedDarkThemeId)
                    } else {
                        ThemePreset.fromId(selectedLightThemeId)
                    }
                } else {
                    currentTheme
                }
            }

            val appBackground by viewModel.appBackground.collectAsState()
            val fontScale by viewModel.fontScale.collectAsState()
            val interfaceScale by viewModel.interfaceScale.collectAsState()
            val customPrimaryColor by viewModel.customPrimaryColor.collectAsState()
            val customFontName by viewModel.customFontName.collectAsState()
            val currentDensity = androidx.compose.ui.platform.LocalDensity.current
            val customDensity = remember(currentDensity, fontScale, interfaceScale) {
                androidx.compose.ui.unit.Density(
                    density = currentDensity.density * interfaceScale,
                    fontScale = currentDensity.fontScale * fontScale
                )
            }
            val customFontFamily = remember(customFontName) {
                viewModel.getCustomFontFile()?.let { file ->
                    try { androidx.compose.ui.text.font.FontFamily(androidx.compose.ui.text.font.Font(file)) } catch (_: Exception) { null }
                }
            }

            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.ui.platform.LocalDensity provides customDensity
            ) {
                DragonFMTheme(
                    preset = effectiveTheme,
                    customPrimaryColor = customPrimaryColor?.let { Color(it) },
                    customFontFamily = customFontFamily
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Background Layer: Default Solid, Selected Fluid Preset, or User Album Wallpaper
                        when (appBackground.type) {
                            AppBackgroundType.DEFAULT -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(effectiveTheme.background)
                                )
                            }
                            AppBackgroundType.PRESET -> {
                                val preset = remember(appBackground.presetId) {
                                    BackgroundPresets.getById(appBackground.presetId)
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(preset.brush)
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(effectiveTheme.background.copy(alpha = appBackground.overlayAlpha))
                                )
                            }
                            AppBackgroundType.CUSTOM -> {
                                val imagePath = appBackground.customImagePath
                                if (!imagePath.isNullOrBlank() && File(imagePath).exists()) {
                                    AsyncImage(
                                        model = File(imagePath),
                                        contentDescription = "Background Wallpaper",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(effectiveTheme.background.copy(alpha = appBackground.overlayAlpha))
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(effectiveTheme.background)
                                    )
                                }
                            }
                        }

                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = Color.Transparent
                        ) {
                            DragonAppContent(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: android.content.Intent?) {
        val rawQuery = intent?.getStringExtra("searchQuery")
        if (!rawQuery.isNullOrBlank()) {
            val searchQuery = try {
                java.net.URLDecoder.decode(rawQuery, "UTF-8")
            } catch (_: Exception) {
                rawQuery
            }
            android.util.Log.d("DragonFM", "handleIntent searchQuery: $searchQuery")
            viewModel.openTagExplore(searchQuery)
            viewModel.navigateTo("RADAR")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DragonAppContent(viewModel: MainViewModel) {
    val context = LocalContext.current
    var lastBackPressTime by remember { mutableLongStateOf(0L) }

    var currentScreen by remember { mutableStateOf(ScreenRoute.FLOW) }
    var showStudioSheet by remember { mutableStateOf(false) }

    val targetRoute by viewModel.targetScreenRoute.collectAsState()
    androidx.compose.runtime.LaunchedEffect(targetRoute) {
        when (targetRoute) {
            "RADAR" -> currentScreen = ScreenRoute.RADAR
            "FLOW" -> currentScreen = ScreenRoute.FLOW
            "LIBRARY" -> currentScreen = ScreenRoute.LIBRARY
            "SETTINGS" -> currentScreen = ScreenRoute.SETTINGS
        }
        if (targetRoute != null) {
            viewModel.clearTargetScreenRoute()
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    val playerState by viewModel.playerState.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val recordingDurationSec by viewModel.recordingDurationSec.collectAsState()
    val sleepTimerSec by viewModel.sleepTimerSeconds.collectAsState()
    val selectedSleepMinutes by viewModel.selectedSleepMinutes.collectAsState()
    val isSleepModeActive by viewModel.isSleepModeActive.collectAsState()
    val selectedScenario by viewModel.selectedScenario.collectAsState()
    val customScenarios by viewModel.customScenarios.collectAsState()
    val deletedDefaultScenarios by viewModel.deletedDefaultScenarios.collectAsState()
    val favorites by viewModel.favoriteStations.collectAsState()
    val playlist by viewModel.playlist.collectAsState()
    val currentTheme by viewModel.currentTheme.collectAsState()
    val scenarioOrder by viewModel.scenarioOrder.collectAsState()
    val fontScale by viewModel.fontScale.collectAsState()

    val showUpdateDialog by viewModel.showUpdateDialog.collectAsState()
    val updateCheckResult by viewModel.updateCheckResult.collectAsState()
    val downloadState by viewModel.downloadState.collectAsState()

    // Robust Android system back button handler
    BackHandler(enabled = true) {
        when {
            isSleepModeActive -> {
                viewModel.toggleSleepMode()
            }
            showStudioSheet -> {
                showStudioSheet = false
            }
            currentScreen != ScreenRoute.FLOW -> {
                when (currentScreen) {
                    ScreenRoute.RADAR -> {
                        viewModel.clearRadarQuery()
                        currentScreen = ScreenRoute.FLOW
                    }
                    ScreenRoute.SETTINGS -> {
                        currentScreen = ScreenRoute.LIBRARY
                    }
                    else -> {
                        currentScreen = ScreenRoute.FLOW
                    }
                }
            }
            else -> {
                val now = System.currentTimeMillis()
                if (now - lastBackPressTime < 2000L) {
                    (context as? Activity)?.finish()
                } else {
                    lastBackPressTime = now
                    Toast.makeText(context, "再按一次返回桌面", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    if (isSleepModeActive) {
        // Bedtime Ultra-Dark Pure Sleep Mode
        SleepModeOverlay(
            playerState = playerState,
            sleepTimerSeconds = sleepTimerSec,
            selectedMinutes = selectedSleepMinutes,
            onTogglePlayPause = { viewModel.togglePlayPause() },
            onSetTimer = { mins -> viewModel.setSleepTimer(mins) },
            onExit = { viewModel.toggleSleepMode() }
        )
    } else {
        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = {
                (fadeIn(animationSpec = tween(240)) + scaleIn(initialScale = 0.96f, animationSpec = tween(240)))
                    .togetherWith(fadeOut(animationSpec = tween(180)))
            },
            label = "ScreenTransition"
        ) { targetScreen ->
            when (targetScreen) {
                ScreenRoute.FLOW -> {
                    DragonFlowScreen(
                        viewModel = viewModel,
                        onOpenRadar = { currentScreen = ScreenRoute.RADAR },
                        onOpenLibrary = { currentScreen = ScreenRoute.LIBRARY },
                        onOpenStudio = { showStudioSheet = true },
                        onToggleSleepMode = { viewModel.toggleSleepMode() },
                        onOpenTag = { tagQuery ->
                            viewModel.openTagExplore(tagQuery)
                            currentScreen = ScreenRoute.RADAR
                        }
                    )
                }
                ScreenRoute.RADAR -> {
                    RadarExploreScreen(
                        viewModel = viewModel,
                        onBack = { currentScreen = ScreenRoute.FLOW }
                    )
                }
                ScreenRoute.LIBRARY -> {
                    LibraryScreen(
                        viewModel = viewModel,
                        onBack = { currentScreen = ScreenRoute.FLOW },
                        onOpenSettings = { currentScreen = ScreenRoute.SETTINGS }
                    )
                }
                ScreenRoute.SETTINGS -> {
                    SettingsScreen(
                        viewModel = viewModel,
                        onBack = { currentScreen = ScreenRoute.LIBRARY }
                    )
                }
            }
        }

        // Studio Bottom Sheet
        if (showStudioSheet) {
            StudioDrawerSheet(
                station = playerState.currentStation,
                isRecording = isRecording,
                recordingDurationSec = recordingDurationSec,
                sleepTimerMinutes = selectedSleepMinutes,
                selectedScenario = selectedScenario,
                customScenarios = customScenarios,
                deletedDefaultScenarios = deletedDefaultScenarios,
                scenarioOrder = scenarioOrder,
                favoriteCount = favorites.size,
                currentTheme = currentTheme,
                onDismiss = {
                    showStudioSheet = false
                },
                onSelectScenario = { scenario ->
                    viewModel.selectScenario(scenario)
                    showStudioSheet = false
                },
                onDeleteCustomScenario = { id ->
                    viewModel.deleteCustomScenario(id)
                },
                onDeleteDefaultScenario = { key ->
                    viewModel.deleteDefaultScenario(key)
                },
                onRestoreDefaultScenarios = {
                    viewModel.restoreDefaultScenarios()
                },
                onUpdateCustomScenario = { id, title, icon ->
                    viewModel.updateCustomScenario(id, title, icon)
                },
                onReorderCustomScenarios = { list ->
                    viewModel.reorderCustomScenarios(list)
                },
                onReorderAllScenarios = { list ->
                    viewModel.reorderAllScenarios(list)
                },
                onCreateCustomScenario = { title, icon ->
                    viewModel.createCustomScenario(title, icon, emptyList())
                },
                onTriggerSpeedSort = {
                    viewModel.sortPlaylistByLatency()
                },
                onSelectTheme = { theme ->
                    viewModel.setTheme(theme)
                },
                onToggleRecording = { viewModel.toggleRecording() },
                onSetSleepTimer = { mins -> viewModel.setSleepTimer(mins) }
            )
        }

        // Self-Update Dialog (Gitee Release/Version Auto-Update)
        if (showUpdateDialog && updateCheckResult is UpdateCheckResult.HasUpdate) {
            val updateInfo = (updateCheckResult as UpdateCheckResult.HasUpdate).info
            AppUpdateDialog(
                updateInfo = updateInfo,
                downloadState = downloadState,
                onStartDownload = { viewModel.startDownloadUpdate(updateInfo) },
                onInstallApk = { file -> viewModel.installApk(file) },
                onDismiss = { viewModel.dismissUpdateDialog() }
            )
        }
    }
}
