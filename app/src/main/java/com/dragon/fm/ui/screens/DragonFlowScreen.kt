package com.dragon.fm.ui.screens

import com.dragon.fm.data.model.PlayerCoverStyle
import com.dragon.fm.data.model.QuickActionButton
import com.dragon.fm.data.model.SubtitleFontSize
import com.dragon.fm.data.model.SwipeGestureAction
import com.dragon.fm.ui.components.SquareAlbumCover
import com.dragon.fm.ui.components.VinylRecordDisc
import com.dragon.fm.ui.components.LiveLyricsCard
import com.dragon.fm.ui.components.SingleLineLyricBar
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.material.icons.outlined.Equalizer

import android.content.res.Configuration

import androidx.compose.animation.AnimatedVisibility

import androidx.compose.animation.animateColorAsState

import androidx.compose.animation.core.FastOutSlowInEasing

import androidx.compose.animation.core.LinearEasing

import androidx.compose.animation.core.RepeatMode

import androidx.compose.animation.core.animateFloat

import androidx.compose.animation.core.infiniteRepeatable

import androidx.compose.animation.core.rememberInfiniteTransition

import androidx.compose.animation.core.tween

import androidx.compose.animation.fadeIn

import androidx.compose.animation.fadeOut

import androidx.compose.animation.scaleIn

import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.interaction.MutableInteractionSource

import androidx.compose.foundation.BorderStroke

import androidx.compose.foundation.Canvas

import androidx.compose.foundation.ExperimentalFoundationApi

import androidx.compose.foundation.background

import androidx.compose.foundation.border

import androidx.compose.foundation.clickable

import androidx.compose.foundation.combinedClickable

import androidx.compose.foundation.gestures.detectTapGestures

import androidx.compose.foundation.layout.Arrangement

import androidx.compose.foundation.layout.Box

import androidx.compose.foundation.layout.Column

import androidx.compose.foundation.layout.PaddingValues

import androidx.compose.foundation.layout.Row

import androidx.compose.foundation.layout.Spacer

import androidx.compose.foundation.layout.fillMaxHeight

import androidx.compose.foundation.layout.fillMaxSize

import androidx.compose.foundation.layout.fillMaxWidth

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

import androidx.compose.foundation.layout.navigationBarsPadding

import androidx.compose.foundation.layout.padding

import androidx.compose.foundation.layout.size

import androidx.compose.foundation.layout.statusBarsPadding

import androidx.compose.foundation.layout.width

import androidx.compose.foundation.lazy.LazyColumn

import androidx.compose.foundation.lazy.LazyRow

import androidx.compose.foundation.lazy.items

import androidx.compose.foundation.lazy.itemsIndexed

import androidx.compose.foundation.lazy.rememberLazyListState

import androidx.compose.foundation.pager.VerticalPager

import androidx.compose.foundation.pager.rememberPagerState

import androidx.compose.runtime.key

import androidx.compose.foundation.shape.CircleShape

import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material.icons.Icons

import androidx.compose.material.icons.automirrored.outlined.QueueMusic

import androidx.compose.material.icons.filled.Bolt

import androidx.compose.material.icons.filled.Check

import androidx.compose.material.icons.filled.Close


import androidx.compose.material.icons.filled.Favorite

import androidx.compose.material.icons.filled.FavoriteBorder

import androidx.compose.material.icons.filled.FiberManualRecord

import androidx.compose.material.icons.filled.GraphicEq

import androidx.compose.material.icons.filled.Headphones

import androidx.compose.material.icons.filled.Menu

import androidx.compose.material.icons.filled.NightlightRound

import androidx.compose.material.icons.filled.Pause

import androidx.compose.material.icons.filled.PriorityHigh

import androidx.compose.material.icons.filled.Person

import androidx.compose.material.icons.filled.PlayArrow

import androidx.compose.material.icons.filled.Radio

import androidx.compose.material.icons.filled.SkipNext

import androidx.compose.material.icons.filled.SkipPrevious

import androidx.compose.material.icons.filled.Speed

import androidx.compose.material.icons.filled.Refresh

import androidx.compose.material.icons.outlined.BookmarkBorder

import androidx.compose.material.icons.outlined.LocationOn

import androidx.compose.material.icons.outlined.PowerSettingsNew

import androidx.compose.material.icons.outlined.Search

import androidx.compose.material.icons.outlined.TravelExplore

import androidx.compose.material.icons.outlined.Tune

import android.app.Activity

import androidx.compose.ui.platform.LocalContext

import androidx.compose.material3.AlertDialog

import androidx.compose.material3.CircularProgressIndicator

import androidx.compose.material3.Icon

import androidx.compose.material3.IconButton

import androidx.compose.material3.Surface

import androidx.compose.material3.Text

import androidx.compose.material3.TextButton

import androidx.compose.runtime.Composable

import androidx.compose.runtime.LaunchedEffect

import androidx.compose.runtime.collectAsState

import androidx.compose.runtime.getValue

import androidx.compose.runtime.mutableStateMapOf

import androidx.compose.runtime.mutableStateOf

import androidx.compose.runtime.remember

import androidx.compose.runtime.rememberCoroutineScope

import androidx.compose.runtime.rememberUpdatedState

import androidx.compose.runtime.setValue

import androidx.compose.runtime.snapshotFlow

import androidx.compose.ui.Alignment

import androidx.compose.ui.Modifier

import androidx.compose.ui.draw.alpha

import androidx.compose.ui.draw.blur

import androidx.compose.ui.draw.clip

import androidx.compose.ui.draw.drawBehind

import androidx.compose.ui.draw.shadow

import androidx.compose.ui.geometry.CornerRadius

import androidx.compose.ui.geometry.Offset

import androidx.compose.ui.geometry.Size

import androidx.compose.ui.graphics.Brush

import androidx.compose.ui.graphics.Color

import androidx.compose.ui.graphics.graphicsLayer

import androidx.compose.foundation.gestures.detectHorizontalDragGestures

import androidx.compose.ui.input.pointer.pointerInput

import androidx.compose.ui.layout.ContentScale

import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity

import androidx.compose.ui.res.painterResource

import androidx.compose.ui.text.font.FontWeight

import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign

import androidx.compose.ui.unit.Dp

import androidx.compose.ui.unit.dp

import androidx.compose.ui.unit.sp

import coil.compose.AsyncImage

import com.dragon.fm.R

import com.dragon.fm.data.model.ChineseTagHelper

import com.dragon.fm.data.model.PlaybackStatus

import com.dragon.fm.data.model.RadioStation

import com.dragon.fm.ui.components.AddToScenarioBottomSheet


import com.dragon.fm.ui.components.WaveFlowRhythmVisualizer

import androidx.compose.ui.zIndex

import com.dragon.fm.ui.theme.DarkBackground

import com.dragon.fm.ui.theme.DragonCyan

import com.dragon.fm.ui.theme.DragonGold

import com.dragon.fm.ui.theme.DragonTheme

import com.dragon.fm.ui.theme.LiveRed

import com.dragon.fm.ui.theme.StationColorExtractor

import com.dragon.fm.ui.viewmodel.MainViewModel

import kotlinx.coroutines.delay

import kotlinx.coroutines.launch


/**

 * Animated 3-bar Audio Equalizer Waveform indicator for live playing stations

 */

@Composable

fun AudioEqualizerWave(

    isPlaying: Boolean,

    color: Color = DragonCyan,

    modifier: Modifier = Modifier

) {

    if (!isPlaying) {

        Canvas(modifier = modifier.size(width = 13.dp, height = 18.dp)) {

            val barWidth = 2.5.dp.toPx()

            val spacing = 2.5.dp.toPx()

            val cornerRadius = CornerRadius(1.dp.toPx())

            val staticHeights = listOf(6.dp.toPx(), 8.dp.toPx(), 5.dp.toPx())

            val inactiveColor = color.copy(alpha = 0.35f)

            for (i in 0..2) {

                val left = i * (barWidth + spacing)

                val h = staticHeights[i]

                val top = size.height - h

                drawRoundRect(

                    color = inactiveColor,

                    topLeft = Offset(left, top),

                    size = Size(barWidth, h),

                    cornerRadius = cornerRadius

                )

            }

        }

    } else {

        val transition = rememberInfiniteTransition(label = "equalizer")

        val h1 by transition.animateFloat(

            initialValue = 4f,

            targetValue = 16f,

            animationSpec = infiniteRepeatable(

                animation = tween(durationMillis = 380, easing = LinearEasing),

                repeatMode = RepeatMode.Reverse

            ),

            label = "bar1"

        )

        val h2 by transition.animateFloat(

            initialValue = 16f,

            targetValue = 6f,

            animationSpec = infiniteRepeatable(

                animation = tween(durationMillis = 480, easing = LinearEasing),

                repeatMode = RepeatMode.Reverse

            ),

            label = "bar2"

        )

        val h3 by transition.animateFloat(

            initialValue = 8f,

            targetValue = 18f,

            animationSpec = infiniteRepeatable(

                animation = tween(durationMillis = 340, easing = LinearEasing),

                repeatMode = RepeatMode.Reverse

            ),

            label = "bar3"

        )


        Canvas(modifier = modifier.size(width = 13.dp, height = 18.dp)) {

            val barWidth = 2.5.dp.toPx()

            val spacing = 2.5.dp.toPx()

            val cornerRadius = CornerRadius(1.dp.toPx())

            val dynamicHeights = listOf(h1.dp.toPx(), h2.dp.toPx(), h3.dp.toPx())

            for (i in 0..2) {

                val left = i * (barWidth + spacing)

                val h = dynamicHeights[i]

                val top = size.height - h

                drawRoundRect(

                    color = color,

                    topLeft = Offset(left, top),

                    size = Size(barWidth, h),

                    cornerRadius = cornerRadius

                )

            }

        }
    }
}


@Composable

fun DragonFlowScreen(

    viewModel: MainViewModel,

    onOpenRadar: () -> Unit,

    onOpenLibrary: () -> Unit,

    onOpenStudio: () -> Unit,


    onToggleSleepMode: () -> Unit,

    onOpenTag: (String) -> Unit = {}

) {

    val playlist by viewModel.playlist.collectAsState()

    val currentIndex by viewModel.currentIndex.collectAsState()

    val playerState by viewModel.playerState.collectAsState()

    val favoriteIds by viewModel.favoriteIds.collectAsState()

    val isRecording by viewModel.isRecording.collectAsState()

    val recordingDurationSec by viewModel.recordingDurationSec.collectAsState()

    val sleepTimerSec by viewModel.sleepTimerSeconds.collectAsState()

    val selectedScenario by viewModel.selectedScenario.collectAsState()

    val detectedLocation by viewModel.detectedLocation.collectAsState()

    val latencies by viewModel.latencies.collectAsState()

    val isSpeedTesting by viewModel.isSpeedTesting.collectAsState()

    val fontScale by viewModel.fontScale.collectAsState()

    val appBackground by viewModel.appBackground.collectAsState()
    val isDefaultBg = appBackground.type == com.dragon.fm.data.model.AppBackgroundType.DEFAULT

    val isDark = DragonTheme.isDark


    val configuration = LocalConfiguration.current

    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val isTablet = configuration.screenWidthDp >= 600

    val isExpandedLayout = isLandscape || isTablet


    // Ambient extension color cache for stations (sampled from icon/cover art)

    val stationAmbientColors = remember { mutableStateMapOf<String, Color>() }

    val currentStation = playlist.getOrNull(currentIndex)

    val fallbackHarmonic = currentStation?.let {

        remember(it.id, isDark) { StationColorExtractor.getHarmonicColor(it, isDark) }

    } ?: DragonTheme.primary

    val targetAmbientColor = currentStation?.let {

        stationAmbientColors[it.id] ?: fallbackHarmonic

    } ?: DragonTheme.primary


    val animatedAmbientColor by animateColorAsState(

        targetValue = targetAmbientColor,

        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),

        label = "ambientColor"

    )


    var showQuitDialog by remember { mutableStateOf(false) }

    var stationToAddToScenario by remember { mutableStateOf<RadioStation?>(null) }


    Box(modifier = Modifier.fillMaxSize()) {

        if (isExpandedLayout) {

            // Tablet / Foldable / Landscape console mode

            val playerCoverStyle by viewModel.playerCoverStyle.collectAsState()
            val showChannelTags by viewModel.showChannelTags.collectAsState()
            val subtitleFontSizeOption by viewModel.subtitleFontSizeOption.collectAsState()
            val soundEffectMode by viewModel.soundEffectMode.collectAsState()
            LandscapeTabletFlowContent(
                playlist = playlist,
                currentIndex = currentIndex,
                playerState = playerState,
                favoriteIds = favoriteIds,
                isRecording = isRecording,
                recordingDurationSec = recordingDurationSec,
                sleepTimerSec = sleepTimerSec,
                selectedScenario = selectedScenario,
                latencies = latencies,
                animatedAmbientColor = animatedAmbientColor,
                stationAmbientColors = stationAmbientColors,
                isDark = isDark,
                isDefaultBg = isDefaultBg,
                playerCoverStyle = playerCoverStyle,
                showChannelTags = showChannelTags,
                subtitleFontSizeOption = subtitleFontSizeOption,
                soundEffectMode = soundEffectMode,
                onOpenSoundEffect = { viewModel.openSoundEffectSheet() },

                onCycleFontSize = { viewModel.setSubtitleFontSizeOption(subtitleFontSizeOption.next()) },

                onSelectStation = { index -> viewModel.playStationAtIndex(index) },

                onTogglePlayPause = { viewModel.togglePlayPause() },

                onPrevious = { viewModel.previousStation() },

                onNext = { viewModel.nextStation() },

                onToggleFavorite = { station -> viewModel.toggleFavorite(station) },

                onToggleRecording = { viewModel.toggleRecording() },


                onToggleSleepMode = onToggleSleepMode,

                onRunSpeedTest = { viewModel.runPlaylistSpeedTest() },

                onSortByLatency = { viewModel.sortPlaylistByLatency() },


                onOpenStudio = onOpenStudio,

                onOpenRadar = onOpenRadar,

                onOpenLibrary = onOpenLibrary,

                onOpenTag = onOpenTag,

                onOpenQuitDialog = { showQuitDialog = true },

                onRetryPlayback = { viewModel.retryPlayback() },

                onSwitchLine = { lineIdx -> viewModel.switchLine(lineIdx) }

            )

        } else {

            // Smartphone portrait TikTok/Soda Music single-screen swipe flow

            PortraitFlowContent(

                viewModel = viewModel,

                playlist = playlist,

                currentIndex = currentIndex,

                playerState = playerState,

                favoriteIds = favoriteIds,

                isRecording = isRecording,

                recordingDurationSec = recordingDurationSec,

                sleepTimerSec = sleepTimerSec,

                selectedScenario = selectedScenario,

                latencies = latencies,

                isSpeedTesting = isSpeedTesting,

                animatedAmbientColor = animatedAmbientColor,

                stationAmbientColors = stationAmbientColors,

                isDark = isDark,

                isDefaultBg = isDefaultBg,


                fontScale = fontScale,

                onSelectStation = { index -> viewModel.playStationAtIndex(index) },

                onTogglePlayPause = { viewModel.togglePlayPause() },

                onPrevious = { viewModel.previousStation() },

                onNext = { viewModel.nextStation() },

                onToggleFavorite = { station -> viewModel.toggleFavorite(station) },

                onToggleRecording = { viewModel.toggleRecording() },


                onToggleSleepMode = onToggleSleepMode,

                onRunSpeedTest = { viewModel.runPlaylistSpeedTest() },


                onOpenStudio = onOpenStudio,

                onOpenRadar = onOpenRadar,

                onOpenLibrary = onOpenLibrary,

                onOpenTag = onOpenTag,

                onOpenQuitDialog = { showQuitDialog = true },

                onRetryPlayback = { viewModel.retryPlayback() },

                onSwitchLine = { lineIdx -> viewModel.switchLine(lineIdx) },

                onSortByLatency = { viewModel.sortPlaylistByLatency() },

                onRemoveStation = { id -> viewModel.removeStationFromScenario(selectedScenario, id) },

                onMoveStation = { from, to -> viewModel.moveStationInScenario(selectedScenario, from, to) },

                onReorderPlaylist = { list -> viewModel.reorderPlaylist(list) },

                onAddToScenario = { station -> stationToAddToScenario = station }

            )

        }
    }

    val showSoundEffectSheet by viewModel.showSoundEffectSheet.collectAsState()
    val soundEffectMode by viewModel.soundEffectMode.collectAsState()

    if (showSoundEffectSheet) {
        SoundEffectBottomSheet(
            currentMode = soundEffectMode,
            onSelectMode = { mode ->
                viewModel.setSoundEffectMode(mode)
            },
            onDismiss = {
                viewModel.closeSoundEffectSheet()
            }
        )
    }

    if (showQuitDialog) {

        val context = LocalContext.current

        val activity = context as? Activity

        AlertDialog(

            onDismissRequest = { showQuitDialog = false },

            title = {

                Text(

                    text = "退出 LX FM",

                    color = DragonTheme.textPrimary,

                    fontWeight = FontWeight.Bold

                )

            },

            text = {

                Text(

                    text = "确定要停止电台播放并完全退出应用吗？",

                    color = DragonTheme.textSecondary

                )

            },

            confirmButton = {

                TextButton(

                    onClick = {

                        showQuitDialog = false

                        viewModel.quitApplication(activity)

                    }

                ) {

                    Text("退出应用", color = LiveRed, fontWeight = FontWeight.Bold)

                }

            },

            dismissButton = {

                TextButton(onClick = { showQuitDialog = false }) {

                    Text("继续收听", color = DragonTheme.textSecondary)

                }

            },

            containerColor = DragonTheme.surface,

            shape = RoundedCornerShape(16.dp)

        )

    }

    


    stationToAddToScenario?.let { station ->

        val customScenarios by viewModel.customScenarios.collectAsState()

        val scenarioOverrides by viewModel.scenarioStationOverrides.collectAsState()

        AddToScenarioBottomSheet(

            station = station,

            customScenarios = customScenarios,

            scenarioOverrides = scenarioOverrides,

            favoriteIds = favoriteIds,

            onToggleInScenario = { scenarioName ->

                viewModel.toggleStationInScenario(scenarioName, station)

            },

            onCreateScenarioWithStation = { title, iconKey ->

                viewModel.createCustomScenario(title, iconKey, listOf(station))

            },

            onDismiss = { stationToAddToScenario = null }

        )

    }

}


/**

 * Tablet & Landscape Dual-Pane Adaptive Console Layout

 */

@Composable

private fun LandscapeTabletFlowContent(

    playlist: List<RadioStation>,

    currentIndex: Int,

    playerState: com.dragon.fm.data.model.PlayerState,

    favoriteIds: Set<String>,

    isRecording: Boolean,

    recordingDurationSec: Int,

    sleepTimerSec: Int?,

    selectedScenario: String,

    latencies: Map<String, Long>,

    animatedAmbientColor: Color,

    stationAmbientColors: MutableMap<String, Color>,

    isDark: Boolean,

    isDefaultBg: Boolean = true,


    fontScale: Float = 1.0f,

    playerCoverStyle: PlayerCoverStyle = PlayerCoverStyle.VINYL,
    showChannelTags: Boolean = true,
    subtitleFontSizeOption: SubtitleFontSize = SubtitleFontSize.STANDARD,
    soundEffectMode: com.dragon.fm.player.SoundEffectMode = com.dragon.fm.player.SoundEffectMode.ORIGINAL,
    onOpenSoundEffect: () -> Unit = {},


    onCycleFontSize: () -> Unit = {},

    onSelectStation: (Int) -> Unit,

    onTogglePlayPause: () -> Unit,

    onPrevious: () -> Unit,

    onNext: () -> Unit,

    onToggleFavorite: (RadioStation) -> Unit,

    onToggleRecording: () -> Unit,


    onToggleSleepMode: () -> Unit,

    onRunSpeedTest: () -> Unit,

    onSortByLatency: () -> Unit,


    onOpenStudio: () -> Unit,

    onOpenRadar: () -> Unit,

    onOpenLibrary: () -> Unit,

    onOpenTag: (String) -> Unit,

    onOpenQuitDialog: () -> Unit = {},

    onRetryPlayback: () -> Unit = {},

    onSwitchLine: (Int) -> Unit = {}

) {

    val configuration = LocalConfiguration.current

    val screenHeight = configuration.screenHeightDp.dp

    val screenWidth = configuration.screenWidthDp.dp

    val scope = rememberCoroutineScope()

    var showHeartPop by remember { mutableStateOf(false) }


    val currentStation = playlist.getOrNull(currentIndex)

    val latency = currentStation?.let { latencies[it.id] }

    val isPlaying = playerState.status == PlaybackStatus.PLAYING

    val isBuffering = playerState.status == PlaybackStatus.BUFFERING

    val isFav = currentStation?.id?.let { favoriteIds.contains(it) } == true


    val fallbackStationColor = currentStation?.let {

        remember(it.id, isDark) { StationColorExtractor.getHarmonicColor(it, isDark) }

    } ?: animatedAmbientColor

    val stationAmbient = currentStation?.let {

        stationAmbientColors[it.id] ?: fallbackStationColor

    } ?: animatedAmbientColor


    val listState = rememberLazyListState()

    val actualSong = playerState.nowPlayingTitle.takeIf {
        it.isNotBlank() && !it.equals(currentStation?.name, ignoreCase = true) && !it.contains("正在载入", ignoreCase = true)
    } ?: currentStation?.currentProgram?.let { "正在直播：$it" }


    // Smoothly scroll station playlist to current playing station

    LaunchedEffect(currentIndex) {

        if (currentIndex in playlist.indices) {

            val target = (currentIndex - 1).coerceAtLeast(0)

            listState.animateScrollToItem(target)

        }

    }


    Box(

        modifier = Modifier

            .fillMaxSize()

            .then(
                if (isDefaultBg) Modifier.background(DragonTheme.background)
                else Modifier.background(Color.Transparent)
            )
            .pointerInput(Unit) {
                var dragSum = 0f
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (dragSum > 80f) {
                            onOpenStudio()
                        }
                        dragSum = 0f
                    },
                    onDragCancel = { dragSum = 0f },
                    onHorizontalDrag = { _, dragAmount ->
                        if (dragAmount > 0) dragSum += dragAmount
                    }
                )
            }

    ) {

        // Atmospheric dynamic blurred backdrop (64px thumbnail for fluid rendering)

        if (currentStation != null && !currentStation.favicon.isNullOrBlank()) {

            val context = LocalContext.current

            val backdropReq = remember(currentStation.favicon) {

                coil.request.ImageRequest.Builder(context)

                    .data(currentStation.favicon)

                    .size(64, 64)

                    .crossfade(false)

                    .build()

            }

            AsyncImage(

                model = backdropReq,

                contentDescription = null,

                contentScale = ContentScale.Crop,

                modifier = Modifier

                    .fillMaxSize()

                    .blur(20.dp)

                    .alpha(if (isDark) 0.22f else 0.12f)

            )

        }


        // Left Ambient Radial Diffusion Glow (matches the station's cover art)

        Box(

            modifier = Modifier

                .fillMaxSize()

                .drawBehind {

                    drawCircle(

                        brush = Brush.radialGradient(

                            colors = listOf(

                                animatedAmbientColor.copy(alpha = if (isDark) 0.35f else 0.20f),

                                animatedAmbientColor.copy(alpha = if (isDark) 0.10f else 0.05f),

                                Color.Transparent

                            ),

                            center = Offset(size.width * 0.22f, size.height * 0.45f),

                            radius = size.width * 0.45f

                        )

                    )

                }

        )


        val isCompactLandscape = screenHeight < 480.dp


        // Main Dual-Pane Structure

        Row(

            modifier = Modifier

                .fillMaxSize()

                .statusBarsPadding()

                .navigationBarsPadding()

                .padding(

                    horizontal = if (isCompactLandscape) 10.dp else 16.dp,

                    vertical = if (isCompactLandscape) 4.dp else 8.dp

                ),

            horizontalArrangement = Arrangement.spacedBy(if (isCompactLandscape) 10.dp else 16.dp),

            verticalAlignment = Alignment.CenterVertically

        ) {

            // ==========================================

            // LEFT PANE: Immersive Master Turntable & Player (42% Width)

            // ==========================================

            Column(

                modifier = Modifier

                    .weight(0.42f)

                    .fillMaxHeight()

                    .clip(RoundedCornerShape(18.dp))

                    .background(DragonTheme.surface.copy(alpha = if (isDark) 0.55f else 0.75f))

                    .border(0.5.dp, DragonTheme.glassBorder, RoundedCornerShape(18.dp))

                    .padding(

                        horizontal = if (isCompactLandscape) 10.dp else 14.dp,

                        vertical = if (isCompactLandscape) 6.dp else 10.dp

                    ),

                horizontalAlignment = Alignment.CenterHorizontally,

                verticalArrangement = Arrangement.SpaceBetween

            ) {

                // Top Tag Indicator (Tablet / Landscape Mode branding capsule)

                Row(

                    modifier = Modifier.fillMaxWidth(),

                    horizontalArrangement = Arrangement.SpaceBetween,

                    verticalAlignment = Alignment.CenterVertically

                ) {

                    Row(verticalAlignment = Alignment.CenterVertically) {

                        Surface(

                            shape = RoundedCornerShape(8.dp),

                            color = animatedAmbientColor.copy(alpha = 0.18f),

                            border = BorderStroke(0.5.dp, animatedAmbientColor.copy(alpha = 0.4f))

                        ) {

                            Row(

                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),

                                verticalAlignment = Alignment.CenterVertically

                            ) {

                                Box(

                                    modifier = Modifier

                                        .size(5.dp)

                                        .clip(CircleShape)

                                        .background(if (isPlaying) LiveRed else Color.Gray)

                                )

                                Spacer(modifier = Modifier.width(4.dp))

                                Text(

                                    text = if (isCompactLandscape) "LX FM • 横屏沉浸" else "大屏平板 • LX FM 沉浸",

                                    color = DragonTheme.textPrimary,

                                    fontSize = 10.sp,

                                    fontWeight = FontWeight.Bold

                                )

                            }

                        }


                    }


                    Row(verticalAlignment = Alignment.CenterVertically) {

                        // Power Exit quick entry

                        IconButton(

                            onClick = onOpenQuitDialog,

                            modifier = Modifier.size(24.dp)

                        ) {

                            Icon(

                                imageVector = Icons.Outlined.PowerSettingsNew,

                                contentDescription = "Quit",

                                tint = LiveRed.copy(alpha = 0.85f),

                                modifier = Modifier.size(16.dp)

                            )

                        }

                    }

                }


                // Adaptive Album Cover Dimension (compact for landscape phone, expansive for tablet)

                val coverDimension = if (isCompactLandscape) {

                    (screenHeight * 0.22f).coerceIn(64.dp, 82.dp)

                } else {

                    minOf(screenWidth * 0.28f, screenHeight * 0.42f, 260.dp)

                }


                if (currentStation != null) {
                    if (playerCoverStyle == PlayerCoverStyle.SQUARE) {
                        SquareAlbumCover(
                            station = currentStation,
                            coverDimension = coverDimension,
                            isPlaying = isPlaying,
                            stationAmbient = stationAmbient,
                            onTap = {},
                            onDoubleTap = {
                                onToggleFavorite(currentStation)
                                showHeartPop = true
                                scope.launch {
                                    delay(800)
                                    showHeartPop = false
                                }
                            },
                            onCoverLoaded = { drawable ->
                                val color = StationColorExtractor.extractFromDrawable(
                                    currentStation.id,
                                    drawable,
                                    isDark
                                )
                                stationAmbientColors[currentStation.id] = color
                            }
                        )
                    } else {
                        VinylRecordDisc(
                            station = currentStation,
                            coverDimension = coverDimension,
                            isPlaying = isPlaying,
                            stationAmbient = stationAmbient,
                            onTap = {},
                            onDoubleTap = {
                                onToggleFavorite(currentStation)
                                showHeartPop = true
                                scope.launch {
                                    delay(800)
                                    showHeartPop = false
                                }
                            },
                            onCoverLoaded = { drawable ->
                                val color = StationColorExtractor.extractFromDrawable(
                                    currentStation.id,
                                    drawable,
                                    isDark
                                )
                                stationAmbientColors[currentStation.id] = color
                            }
                        )
                    }
                }


                // Track & Station Metadata

                Column(

                    modifier = Modifier.fillMaxWidth(),

                    horizontalAlignment = Alignment.CenterHorizontally

                ) {

                    if (actualSong != null) {

                        Text(

                            text = actualSong,

                            color = DragonTheme.primary,

                            fontSize = if (isCompactLandscape) 10.sp else 12.5.sp,

                            fontWeight = FontWeight.Medium,

                            maxLines = 1,

                            overflow = TextOverflow.Ellipsis

                        )

                        Spacer(modifier = Modifier.height(2.dp))

                    }


                    Text(
                        text = currentStation?.name ?: "LX FM",
                        color = DragonTheme.textPrimary,
                        fontSize = if (isCompactLandscape) 15.sp else 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )


                    Spacer(modifier = Modifier.height(if (isCompactLandscape) 2.dp else 4.dp))


                    if (currentStation != null) {

                        val isError = playerState.status == PlaybackStatus.ERROR

                        val allTags = remember(currentStation.id) { ChineseTagHelper.extractChineseTags(currentStation) }

                        val regionTag = remember(currentStation.id) { allTags.firstOrNull { it.isRegion } }

                        val regionStr = regionTag?.name ?: currentStation.formattedRegion

                        val categoryTags = remember(currentStation.id) { allTags.filter { !it.isRegion } }

                        val regionQuery = regionTag?.searchQuery ?: regionStr


                        if (isCompactLandscape) {

                            Row(

                                verticalAlignment = Alignment.CenterVertically,

                                horizontalArrangement = Arrangement.Center

                            ) {

                                if (isError) {

                                    Surface(

                                        onClick = onRetryPlayback,

                                        shape = RoundedCornerShape(6.dp),

                                        color = LiveRed.copy(alpha = 0.16f),

                                        border = BorderStroke(0.5.dp, LiveRed.copy(alpha = 0.5f))

                                    ) {

                                        Row(

                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp),

                                            verticalAlignment = Alignment.CenterVertically

                                        ) {

                                            Icon(Icons.Default.Refresh, contentDescription = "Retry", tint = LiveRed, modifier = Modifier.size(10.dp))

                                            Spacer(modifier = Modifier.width(2.dp))

                                            Text("重试", color = LiveRed, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)

                                        }

                                    }

                                } else {

                                    Surface(

                                        shape = RoundedCornerShape(6.dp),

                                        color = if (isPlaying) LiveRed.copy(alpha = 0.12f) else DragonTheme.glassSurface,

                                        border = BorderStroke(0.5.dp, if (isPlaying) LiveRed.copy(alpha = 0.35f) else DragonTheme.glassBorder)

                                    ) {

                                        Row(

                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp),

                                            verticalAlignment = Alignment.CenterVertically

                                        ) {

                                            Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(if (isPlaying) LiveRed else Color.Gray))

                                            Spacer(modifier = Modifier.width(3.dp))

                                            Text(if (isBuffering) "缓冲中" else (currentStation.state?.ifBlank { "在线电台" } ?: "在线电台"), color = if (isPlaying) LiveRed else DragonTheme.textSecondary, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)

                                        }

                                    }

                                }


                                Spacer(modifier = Modifier.width(5.dp))


                                Surface(

                                    onClick = { onOpenTag(regionQuery) },

                                    shape = RoundedCornerShape(6.dp),

                                    color = DragonTheme.primary.copy(alpha = 0.12f),

                                    border = BorderStroke(0.5.dp, DragonTheme.primary.copy(alpha = 0.35f))

                                ) {

                                    Row(

                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp),

                                        verticalAlignment = Alignment.CenterVertically

                                    ) {

                                        Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = DragonTheme.primary, modifier = Modifier.size(9.dp))

                                        Spacer(modifier = Modifier.width(2.dp))

                                        Text(regionStr, color = DragonTheme.primary, fontSize = 9.sp, fontWeight = FontWeight.Medium)

                                    }

                                }
                                if (currentStation.hasDedicatedLine || currentStation.effectiveLines.size > 1) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    val lines = currentStation.effectiveLines
                                    val activeIdx = playerState.currentStation?.activeLineIndex ?: currentStation.activeLineIndex
                                    val lineName = playerState.activeLineName ?: lines.getOrNull(activeIdx)?.name ?: (if (currentStation.hasDedicatedLine) "高品质专线" else "默认主线")
                                    Surface(
                                        onClick = {
                                            if (lines.size > 1) {
                                                onSwitchLine((activeIdx + 1) % lines.size)
                                            }
                                        },
                                        shape = RoundedCornerShape(6.dp),
                                        color = DragonTheme.glassSurface,
                                        border = BorderStroke(0.5.dp, DragonTheme.glassBorder)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.5.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Filled.Bolt, contentDescription = null, tint = DragonTheme.textSecondary, modifier = Modifier.size(9.dp))
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text(if (lines.size > 1) "$lineName ▾" else lineName, color = DragonTheme.textPrimary, fontSize = 8.5.sp, fontWeight = FontWeight.Medium)
                                        }
                                    }
                                }

                            }

                        } else {

                            if (isError) {

                                Surface(

                                    onClick = onRetryPlayback,

                                    shape = RoundedCornerShape(8.dp),

                                    color = LiveRed.copy(alpha = 0.16f),

                                    border = BorderStroke(0.5.dp, LiveRed.copy(alpha = 0.5f))

                                ) {

                                    Row(

                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),

                                        verticalAlignment = Alignment.CenterVertically

                                    ) {

                                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Retry", tint = LiveRed, modifier = Modifier.size(12.dp))

                                        Spacer(modifier = Modifier.width(3.dp))

                                        Text("连接受阻 • 点击重试", color = LiveRed, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)

                                    }

                                }

                            } else {

                                Surface(

                                    shape = RoundedCornerShape(8.dp),

                                    color = if (isPlaying) LiveRed.copy(alpha = 0.12f) else DragonTheme.glassSurface,

                                    border = BorderStroke(0.5.dp, if (isPlaying) LiveRed.copy(alpha = 0.35f) else DragonTheme.glassBorder)

                                ) {

                                    Row(

                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),

                                        verticalAlignment = Alignment.CenterVertically

                                    ) {

                                        Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(if (isPlaying) LiveRed else Color.Gray))

                                        Spacer(modifier = Modifier.width(4.dp))

                                        Text(if (isBuffering) "正在缓冲" else "LIVE 直播", color = if (isPlaying) LiveRed else DragonTheme.textSecondary, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)

                                    }

                                }

                            }


                            if (showChannelTags) {
                                Spacer(modifier = Modifier.height(5.dp))

                                Row(

                                    verticalAlignment = Alignment.CenterVertically,

                                    horizontalArrangement = Arrangement.Center

                                ) {

                                    Surface(

                                        onClick = { onOpenTag(regionQuery) },

                                        shape = RoundedCornerShape(6.dp),

                                        color = DragonTheme.primary.copy(alpha = 0.12f),

                                        border = BorderStroke(1.dp, DragonTheme.primary.copy(alpha = 0.35f))

                                    ) {

                                        Row(

                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),

                                            verticalAlignment = Alignment.CenterVertically

                                        ) {

                                            Icon(imageVector = Icons.Outlined.LocationOn, contentDescription = null, tint = DragonTheme.primary, modifier = Modifier.size(10.dp))

                                            Spacer(modifier = Modifier.width(3.dp))

                                            Text(regionStr, color = DragonTheme.primary, fontSize = 10.5.sp, fontWeight = FontWeight.Medium)

                                        }

                                    }
                                    if (currentStation.hasDedicatedLine || currentStation.effectiveLines.size > 1) {
                                        Spacer(modifier = Modifier.width(5.dp))
                                        val lines = currentStation.effectiveLines
                                        val activeIdx = playerState.currentStation?.activeLineIndex ?: currentStation.activeLineIndex
                                        val lineName = playerState.activeLineName ?: lines.getOrNull(activeIdx)?.name ?: (if (currentStation.hasDedicatedLine) "高品质专线" else "默认主线")
                                        Surface(
                                            onClick = {
                                                if (lines.size > 1) {
                                                    onSwitchLine((activeIdx + 1) % lines.size)
                                                }
                                            },
                                            shape = RoundedCornerShape(6.dp),
                                            color = DragonTheme.glassSurface,
                                            border = BorderStroke(0.5.dp, DragonTheme.glassBorder)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Filled.Bolt, contentDescription = null, tint = DragonTheme.textSecondary, modifier = Modifier.size(11.dp))
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text(if (lines.size > 1) "$lineName ▾" else lineName, color = DragonTheme.textPrimary, fontSize = 10.5.sp, fontWeight = FontWeight.Medium)
                                            }
                                        }
                                    }


                                    if (categoryTags.isNotEmpty()) {

                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {

                                            items(categoryTags.take(3)) { tag ->

                                                Surface(

                                                    onClick = { onOpenTag(tag.searchQuery) },

                                                    shape = RoundedCornerShape(6.dp),

                                                    color = DragonTheme.glassSurface,

                                                    border = BorderStroke(0.5.dp, DragonTheme.glassBorder)

                                                ) {

                                                    Text(tag.name, color = DragonTheme.textPrimary.copy(alpha = 0.85f), fontSize = 10.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.5.dp))

                                                }

                                            }

                                        }

                                    }

                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(if (isCompactLandscape) 4.dp else 8.dp))

                // Core Playback Controls (Previous, Large Play/Pause Ring, Next, Favorite)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val sideBtnSize = if (isCompactLandscape) 38.dp else 46.dp
                    val sideIconSize = if (isCompactLandscape) 20.dp else 24.dp
                    val playBtnSize = if (isCompactLandscape) 48.dp else 56.dp
                    val playIconSize = if (isCompactLandscape) 24.dp else 28.dp


                    // Previous Station

                    IconButton(

                        onClick = onPrevious,

                        modifier = Modifier

                            .size(sideBtnSize)

                            .clip(CircleShape)

                            .background(DragonTheme.glassSurface)

                    ) {

                        Icon(

                            imageVector = Icons.Default.SkipPrevious,

                            contentDescription = "Previous Station",

                            tint = DragonTheme.textPrimary,

                            modifier = Modifier.size(sideIconSize)

                        )

                    }


                    // Main Play / Pause Capsule

                    Box(

                        modifier = Modifier

                            .size(playBtnSize)

                            .shadow(8.dp, CircleShape)

                            .clip(CircleShape)

                            .background(

                                Brush.linearGradient(listOf(animatedAmbientColor, DragonTheme.secondary))

                            ),

                        contentAlignment = Alignment.Center

                    ) {

                        IconButton(

                            onClick = onTogglePlayPause,

                            modifier = Modifier.fillMaxSize()

                        ) {

                            val iconColor = if (DragonTheme.isDark) Color(0xFF070B12) else Color.White

                            when {

                                isBuffering -> {

                                    CircularProgressIndicator(

                                        modifier = Modifier.size(18.dp),

                                        color = iconColor,

                                        strokeWidth = 2.dp

                                    )

                                }

                                isPlaying -> {

                                    Icon(

                                        imageVector = Icons.Default.Pause,

                                        contentDescription = "Pause",

                                        tint = iconColor,

                                        modifier = Modifier.size(if (isCompactLandscape) 22.dp else 26.dp)

                                    )

                                }

                                else -> {

                                    Icon(

                                        imageVector = Icons.Default.PlayArrow,

                                        contentDescription = "Play",

                                        tint = iconColor,

                                        modifier = Modifier.size(if (isCompactLandscape) 22.dp else 26.dp)

                                    )

                                }

                            }

                        }

                    }


                    // Next Station

                    IconButton(

                        onClick = onNext,

                        modifier = Modifier

                            .size(sideBtnSize)

                            .clip(CircleShape)

                            .background(DragonTheme.glassSurface)

                    ) {

                        Icon(

                            imageVector = Icons.Default.SkipNext,

                            contentDescription = "Next Station",

                            tint = DragonTheme.textPrimary,

                            modifier = Modifier.size(sideIconSize)

                        )

                    }


                    // Favorite Button

                    IconButton(

                        onClick = { currentStation?.let { onToggleFavorite(it) } },

                        modifier = Modifier

                            .size(sideBtnSize)

                            .clip(CircleShape)

                            .background(DragonTheme.glassSurface)

                    ) {

                        Icon(

                            imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,

                            contentDescription = "Favorite",

                            tint = if (isFav) LiveRed else DragonTheme.textSecondary,

                            modifier = Modifier.size(if (isCompactLandscape) 17.dp else 19.dp)

                        )

                    }

                }


                // Audio Stream Gradient Bar

                Box(

                    modifier = Modifier

                        .fillMaxWidth()

                        .height(2.dp)

                        .clip(RoundedCornerShape(1.dp))

                        .background(Color(0x22334155))

                ) {

                    Box(

                        modifier = Modifier

                            .fillMaxWidth(if (isPlaying) 0.70f else 0.15f)

                            .height(2.dp)

                            .background(

                                Brush.horizontalGradient(

                                    listOf(animatedAmbientColor.copy(alpha = 0.3f), animatedAmbientColor)

                                )

                            )

                    )

                }

            }


            // ==========================================

            // RIGHT PANE: Control Center & Live Station Playlist (58% Width)

            // ==========================================

            Column(
                modifier = Modifier
                    .weight(0.58f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(if (isCompactLandscape) 5.dp else 8.dp)
            ) {
                    // 1. Tool Capsule Bar (Horizontal Scrollable for Quick Operations)
                    LazyRow(

                    horizontalArrangement = Arrangement.spacedBy(8.dp),

                    verticalAlignment = Alignment.CenterVertically,

                    modifier = Modifier.fillMaxWidth()

                ) {

                    // Scenario Picker

                    item {

                        Surface(

                            onClick = onOpenStudio,

                            shape = RoundedCornerShape(14.dp),

                            color = DragonTheme.glassSurface,

                            border = BorderStroke(1.dp, DragonTheme.glassBorder)

                        ) {

                            Row(

                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),

                                verticalAlignment = Alignment.CenterVertically

                            ) {

                                Icon(

                                    imageVector = Icons.Default.Menu,

                                    contentDescription = "Scenario",

                                    tint = DragonTheme.primary,

                                    modifier = Modifier.size(16.dp)

                                )

                                Spacer(modifier = Modifier.width(6.dp))

                                Text(

                                    text = selectedScenario,

                                    color = DragonTheme.textPrimary,

                                    fontSize = 12.sp,

                                    fontWeight = FontWeight.Bold

                                )

                            }

                        }

                    }


                    // Recording

                    item {

                        Surface(

                            onClick = onToggleRecording,

                            shape = RoundedCornerShape(14.dp),

                            color = if (isRecording) LiveRed.copy(alpha = 0.18f) else DragonTheme.glassSurface,

                            border = BorderStroke(1.dp, if (isRecording) LiveRed else DragonTheme.glassBorder)

                        ) {

                            Row(

                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),

                                verticalAlignment = Alignment.CenterVertically

                            ) {

                                Icon(

                                    imageVector = Icons.Default.FiberManualRecord,

                                    contentDescription = "Record",

                                    tint = if (isRecording) LiveRed else DragonTheme.textSecondary,

                                    modifier = Modifier.size(16.dp)

                                )

                                Spacer(modifier = Modifier.width(4.dp))

                                Text(

                                    text = if (isRecording) "${recordingDurationSec}s" else "录音",

                                    color = if (isRecording) LiveRed else DragonTheme.textPrimary,

                                    fontSize = 12.sp

                                )

                            }

                        }

                    }


                    // Sleep Timer

                    item {

                        Surface(

                            onClick = onToggleSleepMode,

                            shape = RoundedCornerShape(14.dp),

                            color = if (sleepTimerSec != null) DragonGold.copy(alpha = 0.18f) else DragonTheme.glassSurface,

                            border = BorderStroke(1.dp, if (sleepTimerSec != null) DragonGold else DragonTheme.glassBorder)

                        ) {

                            Row(

                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),

                                verticalAlignment = Alignment.CenterVertically

                            ) {

                                Icon(

                                    imageVector = Icons.Default.NightlightRound,

                                    contentDescription = "Sleep",

                                    tint = if (sleepTimerSec != null) DragonGold else DragonTheme.textSecondary,

                                    modifier = Modifier.size(15.dp)

                                )

                                Spacer(modifier = Modifier.width(4.dp))

                                Text(

                                    text = if (sleepTimerSec != null) "${sleepTimerSec / 60}分" else "睡眠",

                                    color = if (sleepTimerSec != null) DragonGold else DragonTheme.textPrimary,

                                    fontSize = 12.sp

                                )

                            }

                        }

                    }


                    // Sound Effect Entry (音效模式)
                    item {
                        val isFxActive = soundEffectMode != com.dragon.fm.player.SoundEffectMode.ORIGINAL
                        Surface(
                            onClick = onOpenSoundEffect,
                            shape = RoundedCornerShape(14.dp),
                            color = if (isFxActive) DragonTheme.primary.copy(alpha = 0.18f) else DragonTheme.glassSurface,
                            border = BorderStroke(1.dp, if (isFxActive) DragonTheme.primary else DragonTheme.glassBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Equalizer,
                                    contentDescription = "Sound Effect",
                                    tint = if (isFxActive) DragonTheme.primary else DragonTheme.textSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isFxActive) soundEffectMode.shortTitle else "音效",
                                    color = if (isFxActive) DragonTheme.primary else DragonTheme.textPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = if (isFxActive) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Radar Search Entry

                    item {

                        Surface(

                            onClick = onOpenRadar,

                            shape = RoundedCornerShape(14.dp),

                            color = DragonTheme.glassSurface,

                            border = BorderStroke(1.dp, DragonTheme.glassBorder)

                        ) {

                            Row(

                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),

                                verticalAlignment = Alignment.CenterVertically

                            ) {

                                Icon(

                                    imageVector = Icons.Outlined.Search,

                                    contentDescription = "Search",

                                    tint = DragonCyan,

                                    modifier = Modifier.size(16.dp)

                                )

                                Spacer(modifier = Modifier.width(4.dp))

                                Text(

                                    text = "搜索/发现",

                                    color = DragonTheme.textPrimary,

                                    fontSize = 12.sp

                                )

                            }

                        }

                    }


                    // Library Entry

                    item {

                        Surface(

                            onClick = onOpenLibrary,

                            shape = RoundedCornerShape(14.dp),

                            color = DragonTheme.glassSurface,

                            border = BorderStroke(1.dp, DragonTheme.glassBorder)

                        ) {

                            Row(

                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),

                                verticalAlignment = Alignment.CenterVertically

                            ) {

                                Icon(

                                    imageVector = Icons.Default.Person,

                                    contentDescription = "Library",

                                    tint = DragonTheme.textSecondary,

                                    modifier = Modifier.size(16.dp)

                                )

                                Spacer(modifier = Modifier.width(4.dp))

                                Text(

                                    text = "我的收藏",

                                    color = DragonTheme.textPrimary,

                                    fontSize = 12.sp

                                )

                            }

                        }

                    }

                }


                // Subheader

                Row(

                    modifier = Modifier

                        .fillMaxWidth()

                        .padding(horizontal = 4.dp, vertical = 2.dp),

                    horizontalArrangement = Arrangement.SpaceBetween,

                    verticalAlignment = Alignment.CenterVertically

                ) {

                    Row(verticalAlignment = Alignment.CenterVertically) {

                        Icon(

                            imageVector = Icons.AutoMirrored.Outlined.QueueMusic,

                            contentDescription = null,

                            tint = DragonTheme.primary,

                            modifier = Modifier.size(14.dp)

                        )

                        Spacer(modifier = Modifier.width(5.dp))

                        Text(

                            text = "频道流漫游 • ${playlist.size} 台",

                            color = DragonTheme.textSecondary,

                            fontSize = 12.sp,

                            fontWeight = FontWeight.Medium

                        )

                    }

                }


                // Live Station Playlist LazyColumn

                LazyColumn(

                    state = listState,

                    verticalArrangement = Arrangement.spacedBy(6.dp),

                    modifier = Modifier

                        .fillMaxWidth()

                        .weight(1f)

                ) {

                    itemsIndexed(playlist) { index, station ->

                        val isItemCurrent = index == currentIndex

                        val isItemPlaying = isItemCurrent && isPlaying


                        Surface(

                            onClick = { onSelectStation(index) },

                            shape = RoundedCornerShape(12.dp),

                            color = if (isItemCurrent) DragonTheme.primary.copy(alpha = 0.16f) else DragonTheme.surface.copy(alpha = 0.60f),

                            border = BorderStroke(

                                if (isItemCurrent) 1.dp else 0.5.dp,

                                if (isItemCurrent) DragonTheme.primary.copy(alpha = 0.70f) else DragonTheme.glassBorder

                            ),

                            modifier = Modifier.fillMaxWidth()

                        ) {

                            Row(

                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),

                                verticalAlignment = Alignment.CenterVertically

                            ) {

                                // Station Favicon

                                Box(

                                    modifier = Modifier

                                        .size(38.dp)

                                        .clip(RoundedCornerShape(8.dp))

                                        .background(DragonTheme.surfaceVariant),

                                    contentAlignment = Alignment.Center

                                ) {

                                    if (!station.favicon.isNullOrBlank()) {

                                        AsyncImage(

                                            model = station.favicon,

                                            contentDescription = station.name,

                                            contentScale = ContentScale.Crop,

                                            modifier = Modifier.fillMaxSize()

                                        )

                                    } else {

                                        Icon(

                                            imageVector = Icons.Default.Radio,

                                            contentDescription = "Station",

                                            tint = DragonCyan,

                                            modifier = Modifier.size(18.dp)

                                        )

                                    }

                                }


                                Spacer(modifier = Modifier.width(10.dp))


                                // Station Name & Details

                                Column(modifier = Modifier.weight(1f)) {

                                    Text(

                                        text = station.name,

                                        color = if (isItemCurrent) DragonTheme.primary else DragonTheme.textPrimary,

                                        fontSize = 13.5.sp,

                                        fontWeight = if (isItemCurrent) FontWeight.Bold else FontWeight.Medium,

                                        maxLines = 1,

                                        overflow = TextOverflow.Ellipsis

                                    )

                                    Spacer(modifier = Modifier.height(1.5.dp))

                                    val region = station.state?.takeIf { it.isNotBlank() } ?: station.country

                                    Text(

                                        text = if (station.tags.isNotBlank()) "$region · ${station.tags.take(15)}" else region,

                                        color = DragonTheme.textSecondary,

                                        fontSize = 11.sp,

                                        maxLines = 1,

                                        overflow = TextOverflow.Ellipsis

                                    )

                                }


                                // Right status: Equalizer wave

                                if (isItemCurrent) {

                                    AudioEqualizerWave(

                                        isPlaying = isItemPlaying,

                                        color = DragonTheme.primary,

                                        modifier = Modifier.height(18.dp)

                                    )

                                }

                            }

                        }

                    }

                }
            }
        }


        // Double-tap heart pop up overlay

        AnimatedVisibility(

            visible = showHeartPop,

            enter = scaleIn() + fadeIn(),

            exit = scaleOut() + fadeOut(),

            modifier = Modifier.align(Alignment.Center)

        ) {

            Icon(

                imageVector = Icons.Default.Favorite,

                contentDescription = "Favorite Heart",

                tint = LiveRed,

                modifier = Modifier.size(96.dp)

            )

        }
    }
}


/**

 * Smartphone Portrait TikTok / Soda Music Single Screen Vertical Swipe Flow

 */

@OptIn(ExperimentalFoundationApi::class)

@Composable

private fun PortraitFlowContent(

    viewModel: MainViewModel,

    playlist: List<RadioStation>,

    currentIndex: Int,

    playerState: com.dragon.fm.data.model.PlayerState,

    favoriteIds: Set<String>,

    isRecording: Boolean,

    recordingDurationSec: Int,

    sleepTimerSec: Int?,

    selectedScenario: String,

    latencies: Map<String, Long>,

    isSpeedTesting: Boolean = false,

    animatedAmbientColor: Color,

    stationAmbientColors: MutableMap<String, Color>,

    isDark: Boolean,

    isDefaultBg: Boolean = true,


    fontScale: Float = 1.0f,

    onReorderPlaylist: (List<RadioStation>) -> Unit = {},

    onSelectStation: (Int) -> Unit,

    onTogglePlayPause: () -> Unit,

    onPrevious: () -> Unit = {},

    onNext: () -> Unit = {},

    onToggleFavorite: (RadioStation) -> Unit,

    onToggleRecording: () -> Unit,


    onToggleSleepMode: () -> Unit,

    onRunSpeedTest: () -> Unit,


    onOpenStudio: () -> Unit,

    onOpenRadar: () -> Unit,

    onOpenLibrary: () -> Unit,

    onOpenTag: (String) -> Unit,

    onOpenQuitDialog: () -> Unit = {},

    onRetryPlayback: () -> Unit = {},

    onSwitchLine: (Int) -> Unit = {},

    onSortByLatency: () -> Unit = {},

    onRemoveStation: (String) -> Unit = {},

    onMoveStation: (Int, Int) -> Unit = { _, _ -> },

    onAddToScenario: (RadioStation) -> Unit = {}

) {

    val scope = rememberCoroutineScope()
    val isLyricsModeActive by viewModel.isLyricsModeActive.collectAsState()
    val songLyricState by viewModel.songLyricState.collectAsState()
    val soundEffectMode by viewModel.soundEffectMode.collectAsState()

    val playerCoverStyle by viewModel.playerCoverStyle.collectAsState()
    val showChannelTags by viewModel.showChannelTags.collectAsState()
    val showRhythmVisualizer by viewModel.showRhythmVisualizer.collectAsState()
    val quickActionButtons by viewModel.quickActionButtons.collectAsState()
    val swipeRightAction by viewModel.swipeRightAction.collectAsState()
    val swipeLeftAction by viewModel.swipeLeftAction.collectAsState()
    val subtitleFontSizeOption by viewModel.subtitleFontSizeOption.collectAsState()
    val isPlaying = playerState.status == com.dragon.fm.data.model.PlaybackStatus.PLAYING

    // 响应式屏幕规格与字体尺寸自适应计算
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val screenWidth = configuration.screenWidthDp.dp
    val isTallScreen = screenHeight >= 800.dp
    val isMediumScreen = screenHeight in 710.dp..799.dp
    val isCompactScreen = screenHeight < 710.dp || screenWidth < 360.dp
    val isNarrowPhone = screenWidth < 360.dp

    val systemFontScale = LocalDensity.current.fontScale
    val effectiveFontScale = systemFontScale * fontScale
    val fontScaleFactor = effectiveFontScale.coerceIn(0.85f, 1.25f)
    // 字体放大自适应因子：在用户或系统放大字体时适度微调紧凑按钮字号，防止文本换行挤压换行
    val fontAdaptFactor = (1.0f / effectiveFontScale.coerceAtLeast(1.0f)).coerceIn(0.80f, 1.0f)

    // 1. 底部悬浮控制栏自适应参数（SVG 图标与触控高度全面响应字体缩放设置）
    val basePlaySize = when {
        isTallScreen -> 52.dp
        isMediumScreen -> 46.dp
        else -> 40.dp
    }
    val playButtonSize = (basePlaySize.value * fontScaleFactor.coerceIn(0.92f, 1.15f)).dp
    val basePlayIcon = when {
        isTallScreen -> 26.dp
        isMediumScreen -> 23.dp
        else -> 20.dp
    }
    val playIconSize = (basePlayIcon.value * fontScaleFactor).dp

    val basePrevNextSize = when {
        isTallScreen -> 36.dp
        isMediumScreen -> 32.dp
        else -> 28.dp
    }
    val prevNextButtonSize = (basePrevNextSize.value * fontScaleFactor.coerceIn(0.92f, 1.15f)).dp
    val basePrevNextIcon = when {
        isTallScreen -> 22.dp
        isMediumScreen -> 19.dp
        else -> 17.dp
    }
    val prevNextIconSize = (basePrevNextIcon.value * fontScaleFactor).dp

    val baseNavTabIcon = when {
        isTallScreen -> 19.dp
        isMediumScreen -> 17.dp
        else -> 15.dp
    }
    val navTabIconSize = (baseNavTabIcon.value * fontScaleFactor).dp

    val baseNavTextSp = when {
        isTallScreen -> 11f
        isMediumScreen -> 10f
        else -> 9f
    }
    val navTabTextSize = (baseNavTextSp * fontScaleFactor).sp

    val floatingRowVerticalPad = when {
        isTallScreen -> 5.dp
        isMediumScreen -> 3.5.dp
        else -> 2.5.dp
    }
    val floatingBoxVerticalPad = when {
        isTallScreen -> 8.dp
        isMediumScreen -> 5.dp
        else -> 3.dp
    }
    val floatingBarHeight = playButtonSize + (floatingRowVerticalPad * 2) + (floatingBoxVerticalPad * 2)

    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    val quickButtonCount = quickActionButtons.size.coerceIn(1, 7)

    // 底部留白安全间距：杜绝任何可能导致底部悬浮栏与上方按钮重叠的问题
    val floatingSafetyGap = when {
        quickButtonCount <= 5 -> when {
            isTallScreen -> 16.dp
            isMediumScreen -> 12.dp
            else -> 10.dp
        }
        else -> when {
            isTallScreen -> 12.dp
            isMediumScreen -> 10.dp
            else -> 8.dp
        }
    }
    val floatingBarClearance = navBarBottom + floatingBarHeight + floatingSafetyGap

    // 顶部顶栏高度与离封面的安全间距（彻底解决顶部悬浮条遮挡唱片/方图封面的 BUG）
    val topBarHeight = 48.dp
    val topBarToCoverGap = when {
        isTallScreen -> 16.dp
        isMediumScreen -> 12.dp
        else -> 8.dp
    }
    val topBarClearance = topBarHeight + topBarToCoverGap

    // 2. 常用功能快捷按钮自适应参数：根据按钮数量（如用户精简到5个或全选7个）及字体缩放自适应图标、字体与触控密度
    val baseActionIcon = when {
        quickButtonCount <= 4 -> when {
            isTallScreen -> 24.dp
            isMediumScreen -> 22.dp
            else -> 20.dp
        }
        quickButtonCount <= 5 -> when {
            isTallScreen -> 22.5.dp
            isMediumScreen -> 20.5.dp
            else -> 18.5.dp
        }
        quickButtonCount == 6 -> when {
            isTallScreen -> 20.dp
            isMediumScreen -> 18.5.dp
            else -> 16.5.dp
        }
        else -> when { // 7个全选
            isNarrowPhone -> 15.dp
            isTallScreen -> 18.5.dp
            isMediumScreen -> 16.5.dp
            else -> 15.dp
        }
    }
    val actionIconSize = (baseActionIcon.value * fontScaleFactor).dp

    val baseActionSp = when {
        quickButtonCount <= 4 -> when {
            isTallScreen -> 12.2f
            isMediumScreen -> 11.5f
            else -> 10.8f
        }
        quickButtonCount <= 5 -> when {
            isTallScreen -> 11.5f
            isMediumScreen -> 10.8f
            else -> 10f
        }
        quickButtonCount == 6 -> when {
            isTallScreen -> 10.6f
            isMediumScreen -> 10f
            else -> 9.2f
        }
        else -> when { // 7个全选
            isNarrowPhone -> 8.5f
            isTallScreen -> 10.2f
            isMediumScreen -> 9.5f
            else -> 8.8f
        }
    }
    val actionTextSize = (baseActionSp * fontScaleFactor).sp

    val baseActionMinHeight = when {
        quickButtonCount <= 5 -> when {
            isTallScreen -> 48.dp
            isMediumScreen -> 44.dp
            else -> 38.dp
        }
        quickButtonCount == 6 -> when {
            isTallScreen -> 42.dp
            isMediumScreen -> 38.dp
            else -> 34.dp
        }
        else -> when { // 7个全选
            isTallScreen -> 38.dp
            isMediumScreen -> 34.dp
            else -> 30.dp
        }
    }
    val actionButtonMinHeight = (baseActionMinHeight.value * fontScaleFactor.coerceIn(0.92f, 1.20f)).dp

    val actionVerticalPadding = when {
        quickButtonCount <= 5 -> when {
            isTallScreen -> 5.dp
            isMediumScreen -> 4.dp
            else -> 2.5.dp
        }
        else -> when {
            isTallScreen -> 2.5.dp
            else -> 1.5.dp
        }
    }
    val baseActionSpacing = when {
        quickButtonCount <= 5 -> 3.5.dp
        quickButtonCount == 6 -> 2.5.dp
        else -> 1.5.dp
    }
    val actionIconTextSpacing = (baseActionSpacing.value * fontScaleFactor.coerceIn(0.85f, 1.20f)).dp

    val scenarioSlideOffset = remember { Animatable(0f) }
    var switchedScenarioBanner by remember { mutableStateOf<String?>(null) }

    val safeInitialPage = remember(playlist.size, currentIndex) {
        if (playlist.isNotEmpty()) currentIndex.coerceIn(0, playlist.size - 1) else 0
    }

    val pagerState = key(selectedScenario) {
        rememberPagerState(
            initialPage = safeInitialPage,
            pageCount = { playlist.size }
        )
    }

    var showHeartPop by remember { mutableStateOf(false) }

    var showPlaylistSheet by remember { mutableStateOf(false) }

    val currentStationIndex by rememberUpdatedState(currentIndex)

    val currentPlaylist by rememberUpdatedState(playlist)

    val currentOnSelectStation by rememberUpdatedState(onSelectStation)

    var isProgrammaticScroll by remember { mutableStateOf(false) }

    // Synchronize pager swipe with playback (ONLY on settledPage when NOT programmatic and user is not dragging)

    LaunchedEffect(pagerState) {

        snapshotFlow { pagerState.settledPage }.collect { page ->

            if (!isProgrammaticScroll && page != currentStationIndex && page in currentPlaylist.indices) {

                currentOnSelectStation(page)

            }

        }

    }

    // Synchronize programmatic index changes with pager smoothly (ONLY when user is not dragging)

    LaunchedEffect(currentIndex) {
        if (!pagerState.isScrollInProgress && currentIndex in playlist.indices &&

            (pagerState.currentPage != currentIndex || pagerState.targetPage != currentIndex)

        ) {

            isProgrammaticScroll = true
            try {
                if (kotlin.math.abs(pagerState.currentPage - currentIndex) > 1) {

                    pagerState.scrollToPage(currentIndex)

                } else {

                    pagerState.animateScrollToPage(currentIndex)

                }
            } finally {
                isProgrammaticScroll = false
            }

        }

    }

    // When playlist changes (e.g. mode / scenario switch), instantly sync pager to target index

    LaunchedEffect(playlist) {

        if (currentIndex in playlist.indices && pagerState.currentPage != currentIndex) {

            isProgrammaticScroll = true
            try {
                pagerState.scrollToPage(currentIndex)
            } finally {
                isProgrammaticScroll = false
            }

        }

    }


    Box(

        modifier = Modifier

            .fillMaxSize()

            .then(
                if (isDefaultBg) Modifier.background(DragonTheme.background)
                else Modifier.background(Color.Transparent)
            )
            .pointerInput(swipeRightAction, swipeLeftAction) {
                var dragTotal = 0f
                detectHorizontalDragGestures(
                    onDragStart = {
                        dragTotal = 0f
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        dragTotal += dragAmount
                        scope.launch {
                            scenarioSlideOffset.snapTo(dragTotal * 0.45f)
                        }
                    },
                    onDragEnd = {
                        scope.launch {
                            val threshold = 90f
                            if (dragTotal > threshold) {
                                // 向右滑动动作
                                when (swipeRightAction) {
                                    SwipeGestureAction.OPEN_STUDIO -> {
                                        scenarioSlideOffset.animateTo(0f, spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMedium))
                                        onOpenStudio()
                                    }
                                    SwipeGestureAction.OPEN_EXPLORE -> {
                                        scenarioSlideOffset.animateTo(0f, spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMedium))
                                        onOpenRadar()
                                    }
                                    SwipeGestureAction.OPEN_MY -> {
                                        scenarioSlideOffset.animateTo(0f, spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMedium))
                                        onOpenLibrary()
                                    }
                                    SwipeGestureAction.OPEN_PLAYLIST -> {
                                        scenarioSlideOffset.animateTo(0f, spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMedium))
                                        showPlaylistSheet = true
                                    }
                                    SwipeGestureAction.PREV_SCENARIO -> {
                                        scenarioSlideOffset.animateTo(400f, tween(160, easing = FastOutLinearInEasing))
                                        val newScenario = viewModel.switchToPreviousScenario()
                                        switchedScenarioBanner = newScenario
                                        scenarioSlideOffset.snapTo(-400f)
                                        scenarioSlideOffset.animateTo(0f, spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow))
                                        delay(2200)
                                        if (switchedScenarioBanner == newScenario) {
                                            switchedScenarioBanner = null
                                        }
                                    }
                                    SwipeGestureAction.NEXT_SCENARIO -> {
                                        scenarioSlideOffset.animateTo(400f, tween(160, easing = FastOutLinearInEasing))
                                        val newScenario = viewModel.switchToNextScenario()
                                        switchedScenarioBanner = newScenario
                                        scenarioSlideOffset.snapTo(-400f)
                                        scenarioSlideOffset.animateTo(0f, spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow))
                                        delay(2200)
                                        if (switchedScenarioBanner == newScenario) {
                                            switchedScenarioBanner = null
                                        }
                                    }
                                    SwipeGestureAction.NONE -> {
                                        scenarioSlideOffset.animateTo(0f, spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMedium))
                                    }
                                }
                            } else if (dragTotal < -threshold) {
                                // 向左滑动动作
                                when (swipeLeftAction) {
                                    SwipeGestureAction.OPEN_MY -> {
                                        scenarioSlideOffset.animateTo(0f, spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMedium))
                                        onOpenLibrary()
                                    }
                                    SwipeGestureAction.OPEN_STUDIO -> {
                                        scenarioSlideOffset.animateTo(0f, spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMedium))
                                        onOpenStudio()
                                    }
                                    SwipeGestureAction.OPEN_EXPLORE -> {
                                        scenarioSlideOffset.animateTo(0f, spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMedium))
                                        onOpenRadar()
                                    }
                                    SwipeGestureAction.OPEN_PLAYLIST -> {
                                        scenarioSlideOffset.animateTo(0f, spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMedium))
                                        showPlaylistSheet = true
                                    }
                                    SwipeGestureAction.NEXT_SCENARIO -> {
                                        scenarioSlideOffset.animateTo(-400f, tween(160, easing = FastOutLinearInEasing))
                                        val newScenario = viewModel.switchToNextScenario()
                                        switchedScenarioBanner = newScenario
                                        scenarioSlideOffset.snapTo(400f)
                                        scenarioSlideOffset.animateTo(0f, spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow))
                                        delay(2200)
                                        if (switchedScenarioBanner == newScenario) {
                                            switchedScenarioBanner = null
                                        }
                                    }
                                    SwipeGestureAction.PREV_SCENARIO -> {
                                        scenarioSlideOffset.animateTo(-400f, tween(160, easing = FastOutLinearInEasing))
                                        val newScenario = viewModel.switchToPreviousScenario()
                                        switchedScenarioBanner = newScenario
                                        scenarioSlideOffset.snapTo(400f)
                                        scenarioSlideOffset.animateTo(0f, spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow))
                                        delay(2200)
                                        if (switchedScenarioBanner == newScenario) {
                                            switchedScenarioBanner = null
                                        }
                                    }
                                    SwipeGestureAction.NONE -> {
                                        scenarioSlideOffset.animateTo(0f, spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMedium))
                                    }
                                }
                            } else {
                                scenarioSlideOffset.animateTo(0f, spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMedium))
                            }
                            dragTotal = 0f
                        }
                    },
                    onDragCancel = {
                        scope.launch {
                            scenarioSlideOffset.animateTo(0f, spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMedium))
                        }
                        dragTotal = 0f
                    }
                )
            }

    ) {

        // Dynamic Ambient Radial Lighting

        Box(

            modifier = Modifier

                .fillMaxSize()

                .drawBehind {

                    drawCircle(

                        brush = Brush.radialGradient(

                            colors = listOf(

                                animatedAmbientColor.copy(alpha = if (isDark) 0.38f else 0.22f),

                                animatedAmbientColor.copy(alpha = if (isDark) 0.12f else 0.06f),

                                Color.Transparent

                            ),

                            center = Offset(size.width / 2f, size.height * 0.28f),

                            radius = size.width * 0.90f

                        )

                    )

                }

        )


        // Vertical ambient wash extending color downwards smoothly

        Box(

            modifier = Modifier

                .fillMaxWidth()

                .height(420.dp)

                .background(

                    Brush.verticalGradient(

                        colors = listOf(

                            animatedAmbientColor.copy(alpha = if (isDark) 0.28f else 0.16f),

                            animatedAmbientColor.copy(alpha = if (isDark) 0.08f else 0.04f),

                            Color.Transparent

                        )

                    )

                )

        )


        // Vertical Pager (TikTok / Soda Music style swipe-to-switch stations)

        val pagerFlingBehavior = androidx.compose.foundation.pager.PagerDefaults.flingBehavior(
            state = pagerState,
            snapPositionalThreshold = 0.15f
        )

        VerticalPager(

            state = pagerState,
            beyondBoundsPageCount = 1,
            flingBehavior = pagerFlingBehavior,
            key = { page -> playlist.getOrNull(page)?.id ?: page },
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { translationX = scenarioSlideOffset.value }

        ) { page ->

            val station = playlist.getOrNull(page)

            if (station != null) {

                val isCurrent = page == currentIndex

                val isPlaying = isCurrent && playerState.status == PlaybackStatus.PLAYING

                val isBuffering = isCurrent && playerState.status == PlaybackStatus.BUFFERING

                val latency = latencies[station.id]


                val defaultHarmonic = remember(station.id, isDark) {

                    StationColorExtractor.getHarmonicColor(station, isDark)

                }

                val stationAmbient = stationAmbientColors[station.id] ?: defaultHarmonic


                Box(

                    modifier = Modifier.fillMaxSize()

                ) {

                    // Atmospheric Blurred Backdrop (Lightweight 64px downsampled thumbnail for 60fps+ swipe)

                    if (!station.favicon.isNullOrBlank()) {

                        val context = LocalContext.current

                        val backdropReq = remember(station.favicon) {

                            coil.request.ImageRequest.Builder(context)

                                .data(station.favicon)

                                .size(64, 64)

                                .crossfade(false)

                                .build()

                        }

                        AsyncImage(

                            model = backdropReq,

                            contentDescription = null,

                            contentScale = ContentScale.Crop,

                            modifier = Modifier

                                .fillMaxSize()

                                .blur(20.dp)

                                .alpha(if (isDark) 0.25f else 0.15f)

                        )

                    }


                    // Dynamic Station Ambient Glow Mesh (Radiates the station's harmonic color)

                    Box(

                        modifier = Modifier

                            .fillMaxSize()

                            .drawBehind {

                                // Primary upper halo centered behind album art

                                drawCircle(

                                    brush = Brush.radialGradient(

                                        colors = listOf(

                                            stationAmbient.copy(alpha = if (isDark) 0.52f else 0.35f),

                                            stationAmbient.copy(alpha = if (isDark) 0.18f else 0.10f),

                                            Color.Transparent

                                        ),

                                        center = Offset(size.width * 0.50f, size.height * 0.25f),

                                        radius = size.width * 0.92f

                                    )

                                )

                                // Secondary accent wash on upper-right

                                drawCircle(

                                    brush = Brush.radialGradient(

                                        colors = listOf(

                                            stationAmbient.copy(alpha = if (isDark) 0.26f else 0.15f),

                                            Color.Transparent

                                        ),

                                        center = Offset(size.width * 0.85f, size.height * 0.10f),

                                        radius = size.width * 0.55f

                                    )

                                )

                            }

                    )


                    // Scrim Gradient (Keep top translucent so ambient glow shines through)

                    Box(

                        modifier = Modifier

                            .fillMaxSize()

                            .background(

                                Brush.verticalGradient(

                                    colors = listOf(

                                        Color.Transparent,

                                        DragonTheme.background.copy(alpha = if (isDark) 0.38f else 0.25f),

                                        DragonTheme.background.copy(alpha = if (isDark) 0.88f else 0.78f),

                                        DragonTheme.background

                                    )

                                )

                            )

                    )


                    val actualPlaying = playerState.nowPlayingTitle.takeIf {
                        it.isNotBlank() &&
                        !it.equals(station.name, ignoreCase = true) &&
                        !it.contains("正在载入", ignoreCase = true)
                    } ?: station.currentProgram?.let { "正在直播：$it" }

                    val visualizerHeight = if (showRhythmVisualizer) (if (isTallScreen) 34.dp else 26.dp) else 0.dp
                    val subtitleBarHeight = if (songLyricState.song != null) (if (isTallScreen) 34.dp else 28.dp) else 0.dp
                    val actualPlayingHeight = if (actualPlaying != null) (if (isTallScreen) 18.dp else 16.dp) else 0.dp
                    val stationTitleHeight = if (isTallScreen) 28.dp else 24.dp
                    val liveCapsuleRowHeight = if (isTallScreen) 26.dp else 22.dp
                    val tagsRowHeight = if (showChannelTags) (if (isTallScreen) 26.dp else 22.dp) else 0.dp
                    val actionRowHeight = actionButtonMinHeight + (actionVerticalPadding * 2)

                    val internalHeroSpacersTotal = when {
                        isTallScreen -> 36.dp
                        isMediumScreen -> 24.dp
                        else -> 16.dp
                    }

                    val totalNonCoverHeight = (
                        topBarClearance +
                        statusBarTop +
                        floatingBarClearance +
                        subtitleBarHeight +
                        actualPlayingHeight +
                        stationTitleHeight +
                        liveCapsuleRowHeight +
                        tagsRowHeight +
                        visualizerHeight +
                        actionRowHeight +
                        internalHeroSpacersTotal +
                        16.dp /* 确保弹性留白空间，杜绝任何元素溢出裁剪 */
                    )

                    val maxCoverAvailable = (screenHeight - totalNonCoverHeight).coerceAtLeast(140.dp)
                    val maxCoverByWidth = (screenWidth - (if (isNarrowPhone) 36.dp else 48.dp)).coerceAtLeast(140.dp)
                    val targetCover = when {
                        screenHeight >= 860.dp -> if (!showChannelTags && !showRhythmVisualizer) 325.dp else 295.dp
                        screenHeight >= 800.dp -> if (!showChannelTags && !showRhythmVisualizer) 305.dp else 275.dp
                        screenHeight >= 740.dp -> if (!showChannelTags && !showRhythmVisualizer) 280.dp else 255.dp
                        screenHeight >= 680.dp -> if (!showChannelTags && !showRhythmVisualizer) 245.dp else 220.dp
                        else -> 180.dp
                    }
                    val coverDimension = minOf(maxCoverAvailable, maxCoverByWidth, targetCover)

                    if (isLyricsModeActive) {
                        // 沉浸式全屏同步歌词瀑布流 (汽水音乐 / Apple Music 风格)
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = if (screenWidth < 360.dp) 12.dp else 18.dp)
                                .statusBarsPadding()
                                .padding(
                                    top = if (isTallScreen) 16.dp else 12.dp,
                                    bottom = floatingBarClearance
                                ),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            LiveLyricsCard(
                                songLyricState = songLyricState,
                                fontSizeOption = subtitleFontSizeOption,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                onCloseLyrics = { viewModel.setLyricsModeActive(false) },
                                onCycleFontSize = {
                                    val next = subtitleFontSizeOption.next()
                                    viewModel.setSubtitleFontSizeOption(next)
                                    viewModel.showToast("字幕字号已设为: ${next.title}")
                                },
                            )

                            // 底部：沉浸模式下隐藏操作按钮与标签，保留超纤细动感律动光带
                            if (showRhythmVisualizer) {
                                Spacer(modifier = Modifier.height(4.dp))
                                WaveFlowRhythmVisualizer(
                                    isPlaying = isPlaying,
                                    spectrumState = viewModel.audioSpectrum,
                                    peakState = viewModel.audioPeaks,
                                    primaryColor = DragonTheme.primary,
                                    secondaryColor = DragonCyan,
                                    showModeSelector = false,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(20.dp)
                                        .padding(horizontal = 16.dp)
                                )
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = if (screenWidth < 360.dp) 12.dp else 18.dp)
                                .statusBarsPadding()
                                .padding(
                                    top = topBarClearance,
                                    bottom = floatingBarClearance
                                ),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val topBreathingWeight = if (!showChannelTags && !showRhythmVisualizer) 0.30f else 0.12f
                            Spacer(
                                modifier = Modifier
                                    .weight(topBreathingWeight, fill = true)
                            )

                            // ===============================================
                            // 1. TOP HERO: Album Art / Turntable (Independent flexible node)
                            // ===============================================
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(coverDimension)
                                    .align(Alignment.CenterHorizontally)
                            ) {

                                // Ambient Halo Aura (Luminous pulse extending beyond cover boundaries)

                                val haloPulse by if (isPlaying) {

                                    val haloTransition = rememberInfiniteTransition(label = "halo")

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

                                    remember { mutableStateOf(0.5f) }

                                }


                                Box(

                                    modifier = Modifier

                                        .size(coverDimension + 40.dp)

                                        .graphicsLayer {

                                            alpha = if (isPlaying) haloPulse else 0.40f

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


                                if (playerCoverStyle == PlayerCoverStyle.SQUARE) {
                                    SquareAlbumCover(
                                        station = station,
                                        coverDimension = coverDimension,
                                        isPlaying = isPlaying,
                                        stationAmbient = stationAmbient,
                                        onTap = { viewModel.toggleLyricsMode() },
                                        onDoubleTap = {
                                            onToggleFavorite(station)
                                            showHeartPop = true
                                            scope.launch {
                                                delay(800)
                                                showHeartPop = false
                                            }
                                        },
                                        onCoverLoaded = { drawable ->
                                            val color = StationColorExtractor.extractFromDrawable(
                                                station.id,
                                                drawable,
                                                isDark
                                            )
                                            stationAmbientColors[station.id] = color
                                        }
                                    )
                                } else {
                                    VinylRecordDisc(
                                        station = station,
                                        coverDimension = coverDimension,
                                        isPlaying = isPlaying,
                                        stationAmbient = stationAmbient,
                                        onTap = { viewModel.toggleLyricsMode() },
                                        onDoubleTap = {
                                            onToggleFavorite(station)
                                            showHeartPop = true
                                            scope.launch {
                                                delay(800)
                                                showHeartPop = false
                                            }
                                        },
                                        onCoverLoaded = { drawable ->
                                            val color = StationColorExtractor.extractFromDrawable(
                                                station.id,
                                                drawable,
                                                isDark
                                            )
                                            stationAmbientColors[station.id] = color
                                        }
                                    )
                                }

                                }

                            // Flexible breathing room between album cover and metadata
                            val coverToInfoWeight = if (!showChannelTags && !showRhythmVisualizer) 0.40f else 0.22f
                            Spacer(
                                modifier = Modifier
                                    .weight(coverToInfoWeight, fill = true)
                            )

                            if (songLyricState.song != null) {
                                SingleLineLyricBar(
                                    songLyricState = songLyricState,
                                    onClick = {
                                        viewModel.toggleLyricsMode()
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth(0.92f)
                                        .align(Alignment.CenterHorizontally)
                                )
                                Spacer(modifier = Modifier.height(if (isTallScreen) 10.dp else 6.dp))
                            }

                            // Station Metadata Block
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.Start
                            ) {

                            // Actual playing song (ONLY displayed if genuine broadcast metadata exists, NO 24-hour filler!)
                            if (actualPlaying != null) {
                                Text(
                                    text = actualPlaying,
                                    color = DragonTheme.primary,
                                    fontSize = if (isTallScreen) 13.sp else 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(if (isTallScreen) 4.dp else 2.dp))
                            }

                            // Main Station Title
                            Text(
                                text = station.name,
                                color = DragonTheme.textPrimary,
                                fontSize = if (isTallScreen) 22.sp else if (isMediumScreen) 20.sp else 18.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(if (isTallScreen) 8.dp else 4.dp))


                            // Live Status Pulse Pill + Region & Chinese Tags (only animate when actively playing)

                            val pulseAlpha by if (isPlaying) {

                                val infiniteTransition = rememberInfiniteTransition(label = "livePulse")

                                infiniteTransition.animateFloat(

                                    initialValue = 1f,

                                    targetValue = 0.35f,

                                    animationSpec = infiniteRepeatable(

                                        animation = tween(durationMillis = 850, easing = FastOutSlowInEasing),

                                        repeatMode = RepeatMode.Reverse

                                    ),

                                    label = "livePulseAlpha"

                                )

                            } else {

                                remember { mutableStateOf(1f) }

                            }


                            val isError = isCurrent && playerState.status == PlaybackStatus.ERROR


                            // 1. LIVE Status Breathing Capsule (Independent row between title and tags)

                            Row(

                                verticalAlignment = Alignment.CenterVertically,

                                modifier = Modifier.fillMaxWidth()

                            ) {

                                if (isError) {

                                    Surface(

                                        onClick = onRetryPlayback,

                                        shape = RoundedCornerShape(8.dp),

                                        color = LiveRed.copy(alpha = 0.16f),

                                        border = BorderStroke(0.5.dp, LiveRed.copy(alpha = 0.5f))

                                    ) {

                                        Row(

                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),

                                            verticalAlignment = Alignment.CenterVertically

                                        ) {

                                            Icon(

                                                imageVector = Icons.Default.Refresh,

                                                contentDescription = "Retry",

                                                tint = LiveRed,

                                                modifier = Modifier.size(13.dp)

                                            )

                                            Spacer(modifier = Modifier.width(4.dp))

                                            Text(

                                                text = "连接受阻 • 点击重试",

                                                color = LiveRed,

                                                fontSize = 11.sp,

                                                fontWeight = FontWeight.SemiBold

                                            )

                                        }

                                    }

                                } else {

                                    Surface(

                                        shape = RoundedCornerShape(8.dp),

                                        color = if (isPlaying) LiveRed.copy(alpha = 0.14f) else DragonTheme.glassSurface,

                                        border = BorderStroke(0.5.dp, if (isPlaying) LiveRed.copy(alpha = 0.40f) else DragonTheme.glassBorder)

                                    ) {

                                        Row(

                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp),

                                            verticalAlignment = Alignment.CenterVertically

                                        ) {

                                            Box(

                                                modifier = Modifier

                                                    .size(6.dp)

                                                    .clip(CircleShape)

                                                    .graphicsLayer {

                                                        alpha = if (isPlaying) pulseAlpha else 1f

                                                    }

                                                    .background(if (isPlaying) LiveRed else Color.Gray)

                                            )

                                            Spacer(modifier = Modifier.width(5.dp))

                                            Text(

                                                text = if (isBuffering) "正在缓冲" else "LIVE 直播",

                                                color = if (isPlaying) LiveRed else DragonTheme.textSecondary,

                                                maxLines = 1,

                                                softWrap = false,

                                                fontSize = 11.sp,

                                                fontWeight = FontWeight.SemiBold

                                            )

                                        }

                                    }

                                }

                                if (station.hasDedicatedLine || station.effectiveLines.size > 1) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    val lines = station.effectiveLines
                                    val isCur = (page == currentIndex && playerState.currentStation?.id == station.id)
                                    val activeIdx = if (isCur) (playerState.currentStation?.activeLineIndex ?: station.activeLineIndex) else station.activeLineIndex
                                    val lineName = if (isCur && playerState.activeLineName != null) playerState.activeLineName!! else (lines.getOrNull(activeIdx)?.name ?: (if (station.hasDedicatedLine) "高品质专线" else "默认主线"))
                                    Surface(
                                        onClick = {
                                            if (lines.size > 1) {
                                                onSwitchLine((activeIdx + 1) % lines.size)
                                            }
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        color = DragonTheme.glassSurface,
                                        border = BorderStroke(0.5.dp, DragonTheme.glassBorder)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Bolt,
                                                contentDescription = null,
                                                tint = DragonTheme.textSecondary,
                                                modifier = Modifier.size(11.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = if (lines.size > 1) "$lineName ▾" else lineName,
                                                color = DragonTheme.textPrimary,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                Surface(

                                    onClick = { showPlaylistSheet = true },

                                    shape = RoundedCornerShape(8.dp),

                                    color = DragonTheme.glassSurface,

                                    border = BorderStroke(0.5.dp, DragonTheme.glassBorder)

                                ) {

                                    Row(

                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp),

                                        verticalAlignment = Alignment.CenterVertically

                                    ) {

                                        Icon(

                                            imageVector = Icons.AutoMirrored.Outlined.QueueMusic,

                                            contentDescription = "Queue",

                                            tint = DragonTheme.textSecondary,

                                            modifier = Modifier.size(12.dp)

                                        )

                                        Spacer(modifier = Modifier.width(4.dp))

                                        Text(

                                            text = "列表 • ${page + 1}/${playlist.size}", maxLines = 1, softWrap = false,

                                            color = DragonTheme.textPrimary,

                                            fontSize = 11.sp,

                                            fontWeight = FontWeight.Medium

                                        )

                                    }

                                }

                            }


                            if (showChannelTags) {
                                Spacer(modifier = Modifier.height(if (isTallScreen) 8.dp else 6.dp))

                                // 2. Region & Aligned Category Tags Row
                                val allTags = remember(station.id) { ChineseTagHelper.extractChineseTags(station) }
                                val regionTag = remember(station.id) { allTags.firstOrNull { it.isRegion } }
                                val regionStr = regionTag?.name ?: station.formattedRegion
                                val categoryTags = remember(station.id) { allTags.filter { !it.isRegion } }
                                val regionQuery = regionTag?.searchQuery ?: regionStr

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    if (regionStr.isNotBlank()) {
                                        Surface(
                                            onClick = { onOpenTag(regionQuery) },
                                            shape = RoundedCornerShape(8.dp),
                                            color = DragonTheme.primary.copy(alpha = 0.14f),
                                            border = BorderStroke(0.5.dp, DragonTheme.primary.copy(alpha = 0.40f))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.LocationOn,
                                                    contentDescription = null,
                                                    tint = DragonTheme.primary,
                                                    modifier = Modifier.size(11.dp)
                                                )
                                                Spacer(modifier = Modifier.width(3.5.dp))
                                                Text(
                                                    text = regionStr,
                                                    color = DragonTheme.primary,
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }

                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        items(categoryTags.take(4)) { tag ->
                                            Surface(
                                                onClick = { onOpenTag(tag.searchQuery) },
                                                shape = RoundedCornerShape(8.dp),
                                                color = DragonTheme.glassSurface,
                                                border = BorderStroke(0.5.dp, DragonTheme.glassBorder)
                                            ) {
                                                Text(
                                                    text = tag.name,
                                                    color = DragonTheme.textPrimary.copy(alpha = 0.85f),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Normal,
                                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                        }

                        // Proportional middle spacer between hero content and action controls
                        val middleWeight = if (!showChannelTags && !showRhythmVisualizer) 0.60f else 0.45f
                        Spacer(modifier = Modifier.weight(middleWeight, fill = true))

                        // ===============================================
                        // 2. BOTTOM ACTION & STREAM BAR GROUP
                        // ===============================================
                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (showRhythmVisualizer) {
                                WaveFlowRhythmVisualizer(
                                    isPlaying = isPlaying,
                                    spectrumState = viewModel.audioSpectrum,
                                    peakState = viewModel.audioPeaks,
                                    primaryColor = DragonTheme.primary,
                                    secondaryColor = DragonTheme.primary.copy(alpha = 0.82f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(if (isTallScreen) 34.dp else 26.dp)
                                        .padding(horizontal = 16.dp)
                                )
                                Spacer(modifier = Modifier.height(if (isTallScreen) 14.dp else if (isMediumScreen) 10.dp else 6.dp))
                            }

                            // Dynamic Customizable Actions Row (7 Adaptive Function Buttons)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 2.dp, vertical = actionVerticalPadding),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val isFav = favoriteIds.contains(station.id)
                                val voteDisplay = if (station.votes > 1000) "${station.votes / 1000}k" else "${station.votes}"
                                val isFxActive = soundEffectMode != com.dragon.fm.player.SoundEffectMode.ORIGINAL

                                for (btn in quickActionButtons) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .defaultMinSize(minHeight = actionButtonMinHeight),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        when (btn) {
                                            QuickActionButton.FAVORITE -> {
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.Center,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .combinedClickable(
                                                            onClick = { onToggleFavorite(station) },
                                                            onLongClick = { onAddToScenario(station) }
                                                        )
                                                        .padding(vertical = actionVerticalPadding, horizontal = 0.5.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                                        contentDescription = "Favorite",
                                                        tint = if (isFav) LiveRed else DragonTheme.textSecondary,
                                                        modifier = Modifier.size(actionIconSize)
                                                    )
                                                    Spacer(modifier = Modifier.height(actionIconTextSpacing))
                                                    Text(
                                                        text = voteDisplay,
                                                        color = DragonTheme.textSecondary,
                                                        fontSize = actionTextSize,
                                                        fontWeight = FontWeight.Medium,
                                                        maxLines = 1,
                                                        softWrap = false,
                                                        overflow = TextOverflow.Ellipsis,
                                                        textAlign = TextAlign.Center
                                                    )
                                                }
                                            }
                                            QuickActionButton.RECORD -> {
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.Center,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .clickable { onToggleRecording() }
                                                        .padding(vertical = actionVerticalPadding, horizontal = 0.5.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.FiberManualRecord,
                                                        contentDescription = "Record",
                                                        tint = if (isRecording && isCurrent) LiveRed else DragonTheme.textSecondary,
                                                        modifier = Modifier.size(actionIconSize)
                                                    )
                                                    Spacer(modifier = Modifier.height(actionIconTextSpacing))
                                                    Text(
                                                        text = if (isRecording && isCurrent) "${recordingDurationSec}s" else "录音",
                                                        color = if (isRecording && isCurrent) LiveRed else DragonTheme.textSecondary,
                                                        fontSize = actionTextSize,
                                                        fontWeight = FontWeight.Medium,
                                                        maxLines = 1,
                                                        softWrap = false,
                                                        overflow = TextOverflow.Ellipsis,
                                                        textAlign = TextAlign.Center
                                                    )
                                                }
                                            }
                                            QuickActionButton.SLEEP -> {
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.Center,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .clickable { onToggleSleepMode() }
                                                        .padding(vertical = actionVerticalPadding, horizontal = 0.5.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.NightlightRound,
                                                        contentDescription = "Sleep",
                                                        tint = if (sleepTimerSec != null) DragonGold else DragonTheme.textSecondary,
                                                        modifier = Modifier.size(actionIconSize)
                                                    )
                                                    Spacer(modifier = Modifier.height(actionIconTextSpacing))
                                                    Text(
                                                        text = if (sleepTimerSec != null) "${sleepTimerSec / 60}分" else "睡眠",
                                                        color = if (sleepTimerSec != null) DragonGold else DragonTheme.textSecondary,
                                                        fontSize = actionTextSize,
                                                        fontWeight = FontWeight.Medium,
                                                        maxLines = 1,
                                                        softWrap = false,
                                                        overflow = TextOverflow.Ellipsis,
                                                        textAlign = TextAlign.Center
                                                    )
                                                }
                                            }
                                            QuickActionButton.SOUND_EFFECT -> {
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.Center,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .clickable { viewModel.openSoundEffectSheet() }
                                                        .padding(vertical = actionVerticalPadding, horizontal = 0.5.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.Equalizer,
                                                        contentDescription = "Sound Effect",
                                                        tint = if (isFxActive) DragonTheme.primary else DragonTheme.textSecondary,
                                                        modifier = Modifier.size(actionIconSize)
                                                    )
                                                    Spacer(modifier = Modifier.height(actionIconTextSpacing))
                                                    val effectTextSize = if (isFxActive && soundEffectMode.shortTitle.length > 2) (actionTextSize.value - 1f).coerceAtLeast(7.5f).sp else actionTextSize
                                                    Text(
                                                        text = if (isFxActive) soundEffectMode.shortTitle else "音效",
                                                        color = if (isFxActive) DragonTheme.primary else DragonTheme.textSecondary,
                                                        fontSize = effectTextSize,
                                                        fontWeight = if (isFxActive) FontWeight.Bold else FontWeight.Medium,
                                                        maxLines = 1,
                                                        softWrap = false,
                                                        overflow = TextOverflow.Ellipsis,
                                                        textAlign = TextAlign.Center
                                                    )
                                                }
                                            }
                                            QuickActionButton.PLAYLIST -> {
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.Center,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .clickable { showPlaylistSheet = true }
                                                        .padding(vertical = actionVerticalPadding, horizontal = 0.5.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.AutoMirrored.Outlined.QueueMusic,
                                                        contentDescription = "Playlist",
                                                        tint = DragonTheme.primary,
                                                        modifier = Modifier.size(actionIconSize)
                                                    )
                                                    Spacer(modifier = Modifier.height(actionIconTextSpacing))
                                                    Text(
                                                        text = "列表",
                                                        color = DragonTheme.primary,
                                                        fontSize = actionTextSize,
                                                        fontWeight = FontWeight.Medium,
                                                        maxLines = 1,
                                                        softWrap = false,
                                                        overflow = TextOverflow.Ellipsis,
                                                        textAlign = TextAlign.Center
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Bottom safety breathing space above the floating dock
                        val bottomSafetyWeight = if (!showChannelTags && !showRhythmVisualizer) 0.35f else 0.18f
                        Spacer(
                            modifier = Modifier
                                .weight(bottomSafetyWeight, fill = true)
                        )

                        } // end else (normal mode)

                    }

                }

            }

        }


        // Top Navigation Bar (≡ 模式选择 on left, 🔍 Search on right)
        if (!isLyricsModeActive) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .statusBarsPadding()
            ) {

            Row(

                modifier = Modifier

                    .fillMaxWidth()

                    .padding(horizontal = 16.dp, vertical = 6.dp),

                horizontalArrangement = Arrangement.SpaceBetween,

                verticalAlignment = Alignment.CenterVertically

            ) {

                val cleanScenario = selectedScenario.replace(Regex("[\\p{So}\\p{Sk}]"), "").trim()

                Row(

                    modifier = Modifier

                        .clip(RoundedCornerShape(12.dp))

                        .background(DragonTheme.glassSurface)

                        .border(1.dp, DragonTheme.glassBorder, RoundedCornerShape(12.dp))

                        .clickable { onOpenStudio() }

                        .padding(horizontal = 10.dp, vertical = 6.dp),

                    verticalAlignment = Alignment.CenterVertically

                ) {

                    Icon(

                        imageVector = Icons.Outlined.Tune,

                        contentDescription = "Mode Menu",

                        tint = DragonTheme.primary,

                        modifier = Modifier.size(18.dp)

                    )

                    Spacer(modifier = Modifier.width(7.dp))

                    Text(

                        text = cleanScenario,

                        color = DragonTheme.textPrimary,

                        fontSize = 15.sp,

                        fontWeight = FontWeight.Bold

                    )

                }


                Surface(

                    shape = RoundedCornerShape(12.dp),

                    color = DragonTheme.glassSurface,

                    border = BorderStroke(1.dp, DragonTheme.glassBorder)

                ) {

                    Row(

                        verticalAlignment = Alignment.CenterVertically,

                        modifier = Modifier.padding(horizontal = 2.dp, vertical = 2.dp)

                    ) {

                        Box(

                            modifier = Modifier

                                .size(32.dp)

                                .clip(RoundedCornerShape(9.dp))

                                .clickable { onOpenRadar() },

                            contentAlignment = Alignment.Center

                        ) {

                            Icon(

                                imageVector = Icons.Outlined.Search,

                                contentDescription = "Search",

                                tint = DragonTheme.textPrimary,

                                modifier = Modifier.size(17.dp)

                            )

                        }

                        Box(

                            modifier = Modifier

                                .width(1.dp)

                                .height(14.dp)

                                .background(DragonTheme.glassBorder)

                        )

                        Box(

                            modifier = Modifier

                                .size(32.dp)

                                .clip(RoundedCornerShape(9.dp))

                                .clickable { onOpenQuitDialog() },

                            contentAlignment = Alignment.Center

                        ) {

                            Icon(

                                imageVector = Icons.Outlined.PowerSettingsNew,

                                contentDescription = "Quit",

                                tint = LiveRed.copy(alpha = 0.88f),

                                modifier = Modifier.size(17.dp)

                            )

                        }

                    }
                }
            }
        }


        // Bottom Navigation Bar (🎧 发现 | ⚪ 播放键 | 👤 我的)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = if (isNarrowPhone) 12.dp else 20.dp, vertical = floatingBoxVerticalPad)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .background(if (DragonTheme.isDark) Color(0xF2090B10) else DragonTheme.surface.copy(alpha = 0.94f))
                    .border(
                        1.dp,
                        DragonTheme.glassBorder,
                        RoundedCornerShape(26.dp)
                    )
                    .padding(horizontal = if (isNarrowPhone) 8.dp else 12.dp, vertical = floatingRowVerticalPad),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                // 1) 发现 Tab
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onOpenRadar() }
                        .padding(horizontal = if (isNarrowPhone) 6.dp else 8.dp, vertical = if (isTallScreen) 4.dp else 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.TravelExplore,
                        contentDescription = "Discover",
                        tint = DragonTheme.primary,
                        modifier = Modifier.size(navTabIconSize)
                    )
                    Spacer(modifier = Modifier.height(1.5.dp))
                    Text(
                        text = "发现",
                        color = DragonTheme.primary,
                        fontSize = navTabTextSize,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }


                // 2) 切上一台
                IconButton(
                    onClick = onPrevious,
                    enabled = playlist.size > 1,
                    modifier = Modifier.size(prevNextButtonSize)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        tint = if (playlist.size > 1) DragonTheme.textPrimary else DragonTheme.textSecondary.copy(alpha = 0.35f),
                        modifier = Modifier.size(prevNextIconSize)
                    )
                }


                // 3) 居中核心播放/暂停键
                Box(
                    modifier = Modifier
                        .size(playButtonSize)
                        .shadow(4.dp, CircleShape)
                        .clip(CircleShape)
                        .background(DragonTheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = onTogglePlayPause,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        val iconColor = if (DragonTheme.isDark) Color(0xFF070B12) else Color.White
                        when (playerState.status) {
                            PlaybackStatus.BUFFERING -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(playIconSize * 0.85f),
                                    color = iconColor,
                                    strokeWidth = 2.5.dp
                                )
                            }
                            PlaybackStatus.PLAYING -> {
                                Icon(
                                    imageVector = Icons.Default.Pause,
                                    contentDescription = "Pause",
                                    tint = iconColor,
                                    modifier = Modifier.size(playIconSize)
                                )
                            }
                            else -> {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Play",
                                    tint = iconColor,
                                    modifier = Modifier.size(playIconSize)
                                )
                            }
                        }
                    }
                }


                // 4) 切下一台
                IconButton(
                    onClick = onNext,
                    enabled = playlist.size > 1,
                    modifier = Modifier.size(prevNextButtonSize)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = if (playlist.size > 1) DragonTheme.textPrimary else DragonTheme.textSecondary.copy(alpha = 0.35f),
                        modifier = Modifier.size(prevNextIconSize)
                    )
                }


                // 5) 我的 Tab
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onOpenLibrary() }
                        .padding(horizontal = if (isNarrowPhone) 6.dp else 8.dp, vertical = if (isTallScreen) 4.dp else 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.BookmarkBorder,
                        contentDescription = "My Library",
                        tint = DragonTheme.textSecondary,
                        modifier = Modifier.size(navTabIconSize)
                    )
                    Spacer(modifier = Modifier.height(1.5.dp))
                    Text(
                        text = "我的",
                        color = DragonTheme.textSecondary,
                        fontSize = navTabTextSize,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        softWrap = false
                    )
                }

            }
        }


        // Double-tap heart pop up overlay

        AnimatedVisibility(

            visible = showHeartPop,

            enter = scaleIn() + fadeIn(),

            exit = scaleOut() + fadeOut(),

            modifier = Modifier.align(Alignment.Center)

        ) {

            Column(

                horizontalAlignment = Alignment.CenterHorizontally,

                verticalArrangement = Arrangement.Center,

                modifier = Modifier

                    .clip(RoundedCornerShape(24.dp))

                    .background(Color.Black.copy(alpha = 0.65f))

                    .padding(horizontal = 24.dp, vertical = 18.dp)

            ) {

                Icon(

                    imageVector = Icons.Default.Favorite,

                    contentDescription = "Favorite Heart",

                    tint = LiveRed,

                    modifier = Modifier.size(76.dp)

                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(

                    text = "已加入收藏",

                    color = Color.White,

                    fontSize = 14.sp,

                    fontWeight = FontWeight.SemiBold

                )

            }

        }


        if (showPlaylistSheet) {

            PlaylistDrawerSheet(

                playlist = playlist,

                currentIndex = currentIndex,

                isPlaying = playerState.status == PlaybackStatus.PLAYING,

                selectedScenario = selectedScenario,

                latencies = latencies,

                isSpeedTesting = isSpeedTesting,

                fontScale = fontScale,

                onSelectStation = { idx ->

                    onSelectStation(idx)

                    scope.launch {

                        pagerState.scrollToPage(idx)

                    }

                },

                onRunSpeedTest = onRunSpeedTest,

                onSortByLatency = onSortByLatency,

                onRemoveStation = onRemoveStation,

                onMoveStation = onMoveStation,

                onReorderPlaylist = onReorderPlaylist,

                onAddToScenario = onAddToScenario,

                onDismiss = { showPlaylistSheet = false }
            )
        }
    }
}
