package com.dragon.fm.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.DashboardCustomize
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Swipe
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import com.dragon.fm.data.model.AppBackgroundType
import com.dragon.fm.data.model.BackgroundPresets
import com.dragon.fm.data.model.BackgroundPreset
import com.dragon.fm.data.model.PlaybackStatus
import com.dragon.fm.data.model.PlayerCoverStyle
import com.dragon.fm.data.model.QuickActionButton
import com.dragon.fm.data.model.SubtitleFontSize
import com.dragon.fm.data.model.SwipeGestureAction
import com.dragon.fm.data.model.StreamSourceMode
import com.dragon.fm.data.model.UpdateCheckResult
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dragon.fm.data.model.ChinaRegionData
import com.dragon.fm.data.model.ProvinceRegion
import com.dragon.fm.ui.theme.DragonCyan
import com.dragon.fm.ui.theme.DragonGold
import com.dragon.fm.ui.theme.DragonTheme
import com.dragon.fm.ui.theme.LiveRed
import com.dragon.fm.ui.theme.ThemePreset
import com.dragon.fm.ui.viewmodel.MainViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    val isDomesticOnly by viewModel.isDomesticOnly.collectAsState()
    val isFastBuffer by viewModel.isFastBuffer.collectAsState()
    val isPlaybackMemoryEnabled by viewModel.isPlaybackMemoryEnabled.collectAsState()
    val blockedStations by viewModel.blockedStations.collectAsState()
    val blockedTags by viewModel.blockedTags.collectAsState()
    val userProvince by viewModel.userSelectedProvince.collectAsState()
    val userCity by viewModel.userSelectedCity.collectAsState()
    val isCustomRegion by viewModel.isCustomRegionEnabled.collectAsState()
    val detectedProvince by viewModel.detectedProvince.collectAsState()
    val detectedCity by viewModel.detectedCity.collectAsState()
    val imageCacheBytes by viewModel.imageCacheSizeBytes.collectAsState()
    val historyStations by viewModel.historyStations.collectAsState()
    val currentTheme by viewModel.currentTheme.collectAsState()
    val isFollowSystemTheme by viewModel.isFollowSystemTheme.collectAsState()
    val selectedLightThemeId by viewModel.selectedLightThemeId.collectAsState()
    val selectedDarkThemeId by viewModel.selectedDarkThemeId.collectAsState()
    val displayedThemeId = if (appearanceMode == "MANUAL") currentTheme.id else if (DragonTheme.isDark) selectedDarkThemeId else selectedLightThemeId
    val appBackground by viewModel.appBackground.collectAsState()
    val isAutoPlayOnLaunch by viewModel.isAutoPlayOnLaunch.collectAsState()
    val fontScale by viewModel.fontScale.collectAsState()
    val appearanceMode by viewModel.appearanceMode.collectAsState()
    val appearanceDay by viewModel.appearanceDayStart.collectAsState()
    val appearanceNight by viewModel.appearanceNightStart.collectAsState()
    val appearanceLatitude by viewModel.appearanceLatitude.collectAsState()
    val appearanceLongitude by viewModel.appearanceLongitude.collectAsState()
    var appearanceDayText by remember { mutableStateOf("") }
    var appearanceNightText by remember { mutableStateOf("") }
    var appearanceLatitudeText by remember { mutableStateOf("") }
    var appearanceLongitudeText by remember { mutableStateOf("") }
    LaunchedEffect(appearanceDay, appearanceNight) {
        appearanceDayText = formatAppearanceTime(appearanceDay)
        appearanceNightText = formatAppearanceTime(appearanceNight)
    }
    LaunchedEffect(appearanceLatitude, appearanceLongitude) {
        appearanceLatitudeText = appearanceLatitude.toString()
        appearanceLongitudeText = appearanceLongitude.toString()
    }
    val interfaceScale by viewModel.interfaceScale.collectAsState()
    val customPrimaryColor by viewModel.customPrimaryColor.collectAsState()
    val customFontName by viewModel.customFontName.collectAsState()
    val fontPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val error = viewModel.importCustomFont(uri)
            Toast.makeText(context, error ?: "字体已应用", Toast.LENGTH_LONG).show()
        }
    }
    val streamSourceMode by viewModel.streamSourceMode.collectAsState()

    val wallpaperPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.setCustomBackgroundImage(it.toString())
            Toast.makeText(context, "自定义壁纸已应用", Toast.LENGTH_SHORT).show()
        }
    }

    val playerCoverStyle by viewModel.playerCoverStyle.collectAsState()
    val showChannelTags by viewModel.showChannelTags.collectAsState()
    val showRhythmVisualizer by viewModel.showRhythmVisualizer.collectAsState()
    val quickActionButtons by viewModel.quickActionButtons.collectAsState()
    val swipeRightAction by viewModel.swipeRightAction.collectAsState()
    val swipeLeftAction by viewModel.swipeLeftAction.collectAsState()
    val subtitleFontSizeOption by viewModel.subtitleFontSizeOption.collectAsState()
    val enableUpdateReminder by viewModel.enableUpdateReminder.collectAsState()

    var showSwipeRightDialog by remember { mutableStateOf(false) }
    var showSwipeLeftDialog by remember { mutableStateOf(false) }
    var showQuickActionsDialog by remember { mutableStateOf(false) }
    var showSubtitleFontSizeDialog by remember { mutableStateOf(false) }

    var showRegionPicker by remember { mutableStateOf(false) }
    var showStreamSourceDialog by remember { mutableStateOf(false) }
    var showBlockedStationsDialog by remember { mutableStateOf(false) }
    var showBlockedTagsDialog by remember { mutableStateOf(false) }
    var showAddCustomDialog by remember { mutableStateOf(false) }
    var showClearHistoryConfirm by remember { mutableStateOf(false) }
    var showQuitConfirm by remember { mutableStateOf(false) }

    val updateCheckResult by viewModel.updateCheckResult.collectAsState()
    val debugModeEnabled by viewModel.debugModeEnabled.collectAsState()
    val debugTapCount = viewModel.debugVersionTapCount
    val playerDebugState by viewModel.playerState.collectAsState()

    LaunchedEffect(debugTapCount) {
        if (debugTapCount in 2..4) {
            Toast.makeText(context, "再点 " + (5 - debugTapCount) + " 次开启开发者模式", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(updateCheckResult) {
        when (val res = updateCheckResult) {
            is UpdateCheckResult.UpToDate -> {
                if (res.isManual) {
                    Toast.makeText(context, "当前已是最新版本 (v${res.currentVersion})", Toast.LENGTH_SHORT).show()
                }
            }
            is UpdateCheckResult.Error -> {
                if (res.isManual) {
                    Toast.makeText(context, "检测更新失败: ${res.message}", Toast.LENGTH_SHORT).show()
                }
            }
            is UpdateCheckResult.HasUpdate -> {
                if (res.isManual) {
                    Toast.makeText(context, "发现新版本: v${res.info.versionName}", Toast.LENGTH_SHORT).show()
                }
            }
            else -> {}
        }
    }

    val openGitHubRepo = {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Bais233/lx-fm-android5"))
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "请在浏览器访问: https://github.com/Bais233/lx-fm-android5", Toast.LENGTH_LONG).show()
        }
    }

    val createBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            val success = viewModel.exportBackupToUri(uri)
            if (success) {
                Toast.makeText(context, "个性化数据已成功导出备份！", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "导出备份失败，请检查存储或重试", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val openBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importBackupFromUri(uri) { summary ->
                Toast.makeText(context, summary.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.refreshCacheSize()
    }

    val regionDisplayText = if (isCustomRegion && !userProvince.isNullOrBlank()) {
        val cityPart = if (userCity.isNullOrBlank() || userCity == "全城通用" || userCity == "全省通用" || userCity == "全区通用") "" else " · $userCity"
        "$userProvince$cityPart (自定义)"
    } else {
        val detectedText = if (!detectedProvince.isNullOrBlank()) {
            val cityStr = if (!detectedCity.isNullOrBlank() && detectedCity != detectedProvince) "-$detectedCity" else ""
            "$detectedProvince$cityStr"
        } else {
            "跟随网络"
        }
        "自动定位: $detectedText"
    }

    val cacheDisplay = if (imageCacheBytes > 0) {
        String.format("%.1f MB", imageCacheBytes / (1024f * 1024f))
    } else {
        "0.0 MB"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (appBackground.type == AppBackgroundType.DEFAULT) Modifier.background(DragonTheme.background)
                else Modifier.background(Color.Transparent)
            )
            .statusBarsPadding()
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = DragonTheme.textPrimary
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Column {
                Text(
                    text = "系统与偏好设置",
                    color = DragonTheme.textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "广播定位、播放体验与个性化管理",
                    color = DragonTheme.textSecondary,
                    fontSize = 11.5.sp
                )
            }
        }

        // Settings Body
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
        ) {
            // Section 1: Region & Localization
            item {
                SettingsSectionHeader(title = "地区与广播定位", icon = Icons.Default.LocationOn)
                Spacer(modifier = Modifier.height(8.dp))
                SettingsCard {
                    // Region Picker Entry
                    SettingsActionRow(
                        title = "我的偏好地区 (支持选择二级市)",
                        subtitle = regionDisplayText,
                        actionLabel = "更换地区",
                        onClick = { showRegionPicker = true }
                    )

                    HorizontalDivider(color = DragonTheme.glassBorder, thickness = 0.5.dp)

                    // Domestic Only Switch
                    SettingsSwitchRow(
                        title = "仅加载国内电台",
                        subtitle = "自动过滤海外频段，专注收听央广、省市及各地方高品质广播",
                        checked = isDomesticOnly,
                        onCheckedChange = { viewModel.setDomesticOnly(it) }
                    )
                }
            }

            // Section 2: Audio & Playback Engine
            item {
                SettingsSectionHeader(title = "播放与音频引擎", icon = Icons.Default.Speed)
                Spacer(modifier = Modifier.height(8.dp))
                SettingsCard {
                    SettingsActionRow(
                        title = "直播源与线路偏好",
                        subtitle = when (streamSourceMode) {
                            StreamSourceMode.HYBRID -> "智能极速模式 · 官方专线优先，公网自动容灾"
                            StreamSourceMode.DEDICATED_ONLY -> "纯净官方专线 · 仅使用官方高可靠极速专线"
                            StreamSourceMode.PUBLIC_ONLY -> "全球公网广播 · 仅使用开放公共广播源"
                        },
                        actionLabel = when (streamSourceMode) {
                            StreamSourceMode.HYBRID -> "智能极速"
                            StreamSourceMode.DEDICATED_ONLY -> "纯净专线"
                            StreamSourceMode.PUBLIC_ONLY -> "全球公网"
                        },
                        onClick = { showStreamSourceDialog = true }
                    )

                    HorizontalDivider(color = DragonTheme.glassBorder, thickness = 0.5.dp)

                    SettingsSwitchRow(
                        title = "0.25s 极速起播优化",
                        subtitle = "超低缓冲延迟配置，切台如转动物理收音机旋钮般灵敏无顿挫",
                        checked = isFastBuffer,
                        onCheckedChange = { viewModel.setFastBuffer(it) }
                    )

                    HorizontalDivider(color = DragonTheme.glassBorder, thickness = 0.5.dp)

                    SettingsSwitchRow(
                        title = "启动时自动播放电台",
                        subtitle = "开启后打开应用立即播放电台；关闭后应用准备就绪但不立即发出声音",
                        checked = isAutoPlayOnLaunch,
                        onCheckedChange = { viewModel.setAutoPlayOnLaunch(it) }
                    )

                    HorizontalDivider(color = DragonTheme.glassBorder, thickness = 0.5.dp)

                    SettingsSwitchRow(
                        title = "记忆收听进度与播放列表",
                        subtitle = "下次打开应用时自动恢复最后收听的电台频道、模式场景与播放队列",
                        checked = isPlaybackMemoryEnabled,
                        onCheckedChange = { viewModel.setPlaybackMemoryEnabled(it) }
                    )

                    HorizontalDivider(color = DragonTheme.glassBorder, thickness = 0.5.dp)

                    SettingsInfoRow(
                        title = "音频设备拔出自动暂停",
                        subtitle = "拔出耳机或断开车载蓝牙时静默暂停，避免公共场合声音外放",
                        statusText = "已开启"
                    )
                }
            }

            // Section 3: Content Filtering & Blacklist
            item {
                SettingsSectionHeader(title = "内容过滤与电台屏蔽", icon = Icons.Default.Block)
                Spacer(modifier = Modifier.height(8.dp))
                SettingsCard {
                    SettingsActionRow(
                        title = "已屏蔽电台黑名单",
                        subtitle = "管理已拉黑的电台 (${blockedStations.size} 个)，漫游时将自动跳过",
                        actionLabel = "查看管理",
                        onClick = { showBlockedStationsDialog = true }
                    )

                    HorizontalDivider(color = DragonTheme.glassBorder, thickness = 0.5.dp)

                    SettingsActionRow(
                        title = "屏蔽分类与关键词标签",
                        subtitle = "包含所选标签的电台将自动跳过 (${blockedTags.size} 个标签)",
                        actionLabel = "查看管理",
                        onClick = { showBlockedTagsDialog = true }
                    )
                }
            }

            // Section 4: Themes & Modes
            item {
                SettingsSectionHeader(title = "界面与个性化", icon = Icons.Default.Tune)
                Spacer(modifier = Modifier.height(8.dp))
                SettingsCard {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("外观切换", color = DragonTheme.textPrimary, fontSize = 14.5.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        val modes = listOf("SYSTEM" to "跟随系统", "MANUAL" to "固定主题", "SCHEDULE" to "定时切换", "SUNRISE_SUNSET" to "日出日落")
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            modes.forEach { (mode, label) ->
                                Surface(
                                    onClick = { viewModel.setAppearanceMode(mode) },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (appearanceMode == mode) DragonTheme.primary.copy(alpha = 0.18f) else DragonTheme.glassSurface,
                                    border = BorderStroke(1.dp, if (appearanceMode == mode) DragonTheme.primary else DragonTheme.glassBorder)
                                ) { Text(label, color = if (appearanceMode == mode) DragonTheme.primary else DragonTheme.textPrimary, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) }
                            }
                        }
                        if (appearanceMode == "SCHEDULE") {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("日间开始 / 夜间开始（24 小时制，支持跨午夜）", color = DragonTheme.textSecondary, fontSize = 12.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(value = appearanceDayText, onValueChange = { appearanceDayText = it }, label = { Text("日间，如 07:00") }, singleLine = true, modifier = Modifier.weight(1f))
                                OutlinedTextField(value = appearanceNightText, onValueChange = { appearanceNightText = it }, label = { Text("夜间，如 19:00") }, singleLine = true, modifier = Modifier.weight(1f))
                            }
                            TextButton(onClick = {
                                val day = parseAppearanceTime(appearanceDayText)
                                val night = parseAppearanceTime(appearanceNightText)
                                if (day == null || night == null) Toast.makeText(context, "请输入有效时间，例如 07:00", Toast.LENGTH_SHORT).show()
                                else { viewModel.setAppearanceSchedule(day, night); Toast.makeText(context, "切换时间已保存", Toast.LENGTH_SHORT).show() }
                            }) { Text("保存切换时间") }
                        }
                        if (appearanceMode == "SUNRISE_SUNSET") {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("手动填写经纬度，离线估算日出日落；不获取位置、不联网。", color = DragonTheme.textSecondary, fontSize = 12.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(value = appearanceLatitudeText, onValueChange = { appearanceLatitudeText = it }, label = { Text("纬度，如 31.23") }, singleLine = true, modifier = Modifier.weight(1f))
                                OutlinedTextField(value = appearanceLongitudeText, onValueChange = { appearanceLongitudeText = it }, label = { Text("经度，如 121.47") }, singleLine = true, modifier = Modifier.weight(1f))
                            }
                            TextButton(onClick = {
                                val lat = appearanceLatitudeText.toDoubleOrNull()
                                val lon = appearanceLongitudeText.toDoubleOrNull()
                                if (lat == null || lon == null || lat !in -90.0..90.0 || lon !in -180.0..180.0) Toast.makeText(context, "纬度范围 -90 至 90，经度范围 -180 至 180", Toast.LENGTH_SHORT).show()
                                else { viewModel.setAppearanceLocation(lat, lon); Toast.makeText(context, "位置已保存，仅用于日出日落", Toast.LENGTH_SHORT).show() }
                            }) { Text("保存位置") }
                        }
                    }

                    HorizontalDivider(color = DragonTheme.glassBorder, thickness = 0.5.dp)

                    // 2. Light Theme Presets (专为播放器打造的高颜值浅色主题)
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.BrightnessAuto,
                                contentDescription = null,
                                tint = DragonGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "高颜值浅色主题",
                                color = DragonTheme.textPrimary,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "清爽通透 · 护眼声学",
                                color = DragonTheme.textSecondary,
                                fontSize = 11.5.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            for (theme in ThemePreset.lightPresets) {
                                val isSelected = theme.id == displayedThemeId
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) DragonTheme.primary.copy(alpha = 0.15f) else DragonTheme.glassSurface,
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) DragonTheme.primary else DragonTheme.glassBorder
                                    ),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { viewModel.setTheme(theme) }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clip(CircleShape)
                                                .background(theme.indicatorColor)
                                                .border(1.dp, DragonTheme.glassBorder, CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = theme.title,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) DragonTheme.primary else DragonTheme.textPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = DragonTheme.glassBorder, thickness = 0.5.dp)

                    // 3. Dark Theme Presets (沉浸深色主题)
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = null,
                                tint = DragonCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "沉浸深色主题",
                                color = DragonTheme.textPrimary,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "纯粹深空 · 极客黑曜",
                                color = DragonTheme.textSecondary,
                                fontSize = 11.5.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            for (theme in ThemePreset.darkPresets) {
                                val isSelected = theme.id == displayedThemeId
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) DragonTheme.primary.copy(alpha = 0.15f) else DragonTheme.glassSurface,
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) DragonTheme.primary else DragonTheme.glassBorder
                                    ),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { viewModel.setTheme(theme) }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clip(CircleShape)
                                                .background(theme.indicatorColor)
                                                .border(1.dp, DragonTheme.glassBorder, CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = theme.title,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) DragonTheme.primary else DragonTheme.textPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = DragonTheme.glassBorder, thickness = 0.5.dp)


                    HorizontalDivider(color = DragonTheme.glassBorder, thickness = 0.5.dp)
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("自选强调色", color = DragonTheme.textPrimary, fontSize = 14.5.sp, fontWeight = FontWeight.Bold)
                        Text("改变按钮、选中状态与播放强调色；保留主题背景。", color = DragonTheme.textSecondary, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(0xFF0284C7, 0xFF059669, 0xFFD97706, 0xFFE11D48, 0xFF8B5CF6, 0xFF0891B2, 0xFF4F46E5, 0xFF78716C).forEach { color ->
                                Surface(onClick = { viewModel.setCustomPrimaryColor(color) }, modifier = Modifier.size(30.dp), shape = CircleShape, color = Color(color), border = BorderStroke(if (customPrimaryColor == color) 3.dp else 1.dp, if (customPrimaryColor == color) DragonTheme.textPrimary else DragonTheme.glassBorder)) {}
                            }
                            TextButton(onClick = { viewModel.setCustomPrimaryColor(null) }) { Text("恢复主题色") }
                        }
                    }
                    HorizontalDivider(color = DragonTheme.glassBorder, thickness = 0.5.dp)
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("界面大小", color = DragonTheme.textPrimary, fontSize = 14.5.sp, fontWeight = FontWeight.Bold)
                        Text("当前 " + (interfaceScale * 100).toInt() + "%，调整布局尺寸；字号可独立设置。", color = DragonTheme.textSecondary, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("紧凑" to 0.85f, "标准" to 1f, "舒适" to 1.10f, "较大" to 1.20f).forEach { (label, scale) ->
                                Surface(onClick = { viewModel.setInterfaceScale(scale) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp), color = if (interfaceScale == scale) DragonTheme.primary.copy(alpha = 0.18f) else DragonTheme.glassSurface) {
                                    Text(label, fontSize = 12.sp, color = if (interfaceScale == scale) DragonTheme.primary else DragonTheme.textPrimary, modifier = Modifier.padding(vertical = 9.dp, horizontal = 8.dp))
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = DragonTheme.glassBorder, thickness = 0.5.dp)
                    SettingsActionRow(
                        title = "自定义字体",
                        subtitle = customFontName ?: "选择 TTF / OTF 字体；保存在应用内部，无需存储权限",
                        actionLabel = if (customFontName == null) "选择字体" else "更换字体",
                        onClick = { fontPickerLauncher.launch(arrayOf("font/ttf", "font/otf", "application/x-font-ttf", "application/x-font-otf", "application/octet-stream")) }
                    )
                    if (customFontName != null) {
                        TextButton(onClick = { viewModel.clearCustomFont() }, modifier = Modifier.padding(horizontal = 10.dp)) { Text("恢复系统字体") }
                    }

                    // 4. Background & Custom Wallpaper Settings
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Wallpaper,
                                contentDescription = null,
                                tint = DragonTheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "背景与个性化壁纸",
                                color = DragonTheme.textPrimary,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = when (appBackground.type) {
                                AppBackgroundType.DEFAULT -> "当前：默认主题纯色底色"
                                AppBackgroundType.PRESET -> "当前预设：${BackgroundPresets.getById(appBackground.presetId).title}"
                                AppBackgroundType.CUSTOM -> "当前：自定义相册壁纸"
                            },
                            color = DragonTheme.textSecondary,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Custom Wallpaper Actions
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Pick from Album
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (appBackground.type == AppBackgroundType.CUSTOM) DragonTheme.primary.copy(alpha = 0.18f) else DragonTheme.glassSurface,
                                border = BorderStroke(
                                    1.dp,
                                    if (appBackground.type == AppBackgroundType.CUSTOM) DragonTheme.primary else DragonTheme.glassBorder
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { wallpaperPickerLauncher.launch("image/*") }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(vertical = 9.dp, horizontal = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AddPhotoAlternate,
                                        contentDescription = null,
                                        tint = if (appBackground.type == AppBackgroundType.CUSTOM) DragonTheme.primary else DragonTheme.textPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "相册选图壁纸",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (appBackground.type == AppBackgroundType.CUSTOM) DragonTheme.primary else DragonTheme.textPrimary
                                    )
                                }
                            }

                            // Restore Solid Default
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (appBackground.type == AppBackgroundType.DEFAULT) DragonTheme.primary.copy(alpha = 0.18f) else DragonTheme.glassSurface,
                                border = BorderStroke(
                                    1.dp,
                                    if (appBackground.type == AppBackgroundType.DEFAULT) DragonTheme.primary else DragonTheme.glassBorder
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        viewModel.resetBackgroundToDefault()
                                        Toast.makeText(context, "已恢复纯色默认背景", Toast.LENGTH_SHORT).show()
                                    }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(vertical = 9.dp, horizontal = 8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(DragonTheme.background)
                                            .border(1.dp, DragonTheme.glassBorder, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "恢复纯色背景",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (appBackground.type == AppBackgroundType.DEFAULT) DragonTheme.primary else DragonTheme.textPrimary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Preset fluid backgrounds
                        Text(
                            text = "内置精选流光背景预设：",
                            color = DragonTheme.textSecondary,
                            fontSize = 11.5.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            for (preset in BackgroundPresets.PRESETS) {
                                val isSelected = appBackground.type == AppBackgroundType.PRESET && appBackground.presetId == preset.id
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) DragonTheme.primary.copy(alpha = 0.2f) else DragonTheme.glassSurface,
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) DragonTheme.primary else DragonTheme.glassBorder
                                    ),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            viewModel.setBackgroundPreset(preset.id)
                                        }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clip(CircleShape)
                                                .background(preset.brush)
                                                .border(1.dp, DragonTheme.glassBorder, CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = preset.title,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) DragonTheme.primary else DragonTheme.textPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = DragonTheme.glassBorder, thickness = 0.5.dp)

                    // Font Size Scale Adjustment
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "界面字体大小调节",
                                color = DragonTheme.textPrimary,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            val currentScaleName = when {
                                fontScale <= 0.90f -> "紧凑 (88%)"
                                fontScale <= 1.05f -> "标准 (100%)"
                                fontScale <= 1.18f -> "舒适 (112%)"
                                else -> "特大 (125%)"
                            }
                            Text(
                                text = currentScaleName,
                                color = DragonTheme.primary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "完美自适应不同屏幕尺寸与横竖屏模式，防止文字溢出或卡片截断",
                            color = DragonTheme.textSecondary,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        val fontOptions = listOf(
                            Triple("紧凑", 0.88f, "88%"),
                            Triple("标准", 1.00f, "100%"),
                            Triple("舒适", 1.12f, "112%"),
                            Triple("特大", 1.25f, "125%")
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            fontOptions.forEach { (label, scaleVal, pct) ->
                                val isSelected = kotlin.math.abs(fontScale - scaleVal) < 0.05f
                                Surface(
                                    onClick = { viewModel.setFontScale(scaleVal) },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) DragonTheme.primary.copy(alpha = 0.16f) else DragonTheme.glassSurface,
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) DragonTheme.primary else DragonTheme.glassBorder
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) DragonTheme.primary else DragonTheme.textPrimary
                                        )
                                        Text(
                                            text = pct,
                                            fontSize = 10.sp,
                                            color = if (isSelected) DragonTheme.primary else DragonTheme.textSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = DragonTheme.glassBorder, thickness = 0.5.dp)

                    SettingsActionRow(
                        title = "添加自定义网络广播流",
                        subtitle = "手动添加支持 M3U8、MP3、AAC 格式的私有或小众电台",
                        actionLabel = "添加流",
                        onClick = { showAddCustomDialog = true }
                    )

                    HorizontalDivider(color = DragonTheme.glassBorder, thickness = 0.5.dp)

                    SettingsActionRow(
                        title = "恢复全部默认收听场景",
                        subtitle = "若误删除了「潮流通勤」「晚间助眠」等预设模式，可一键重置",
                        actionLabel = "一键恢复",
                        onClick = {
                            viewModel.restoreDefaultScenarios()
                            Toast.makeText(context, "所有预设收听模式已重置恢复", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            // Section: Player UI Customization
            item {
                SettingsSectionHeader(title = "播放界面自定义", icon = Icons.Default.DashboardCustomize)
                Spacer(modifier = Modifier.height(8.dp))
                SettingsCard {
                    // 1. 封面样式切换 (经典黑胶 vs 四方大图)
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Album,
                                contentDescription = null,
                                tint = DragonTheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "封面展示样式",
                                color = DragonTheme.textPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            PlayerCoverStyle.entries.forEach { style ->
                                val isSelected = style == playerCoverStyle
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { viewModel.setPlayerCoverStyle(style) },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) DragonTheme.primary.copy(alpha = 0.16f) else DragonTheme.glassSurface,
                                    border = BorderStroke(
                                        if (isSelected) 1.5.dp else 0.5.dp,
                                        if (isSelected) DragonTheme.primary else DragonTheme.glassBorder
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = style.title,
                                            color = if (isSelected) DragonTheme.primary else DragonTheme.textPrimary,
                                            fontSize = 13.5.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = style.description,
                                            color = DragonTheme.textSecondary,
                                            fontSize = 10.5.sp,
                                            lineHeight = 14.sp,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = DragonTheme.glassBorder, thickness = 0.5.dp)

                    // 2. 频道标签展示开关
                    SettingsSwitchRow(
                        title = "展示频道与分类标签",
                        subtitle = "在播放页电台主标题下方展示所属地域与流派分类标签",
                        checked = showChannelTags,
                        onCheckedChange = { viewModel.setShowChannelTags(it) }
                    )

                    HorizontalDivider(color = DragonTheme.glassBorder, thickness = 0.5.dp)

                    // 3. 动态律动频谱动效小部件
                    SettingsSwitchRow(
                        title = "动态律动频谱小部件",
                        subtitle = "随广播音乐节奏实时起伏跳动的 WaveFlow 频谱小部件，轻触可切换波形、极光与频谱模式",
                        checked = showRhythmVisualizer,
                        onCheckedChange = { viewModel.setShowRhythmVisualizer(it) }
                    )

                    HorizontalDivider(color = DragonTheme.glassBorder, thickness = 0.5.dp)

                    // 4. 下方快捷功能按钮勾选与排序
                    SettingsActionRow(
                        title = "下方快捷功能按钮自定义",
                        subtitle = "已启用 ${quickActionButtons.size} 个功能按钮（支持勾选展示与自由排序）",
                        actionLabel = "自定义",
                        onClick = { showQuickActionsDialog = true }
                    )

                }
            }

            // Section: Gestures & Swipe Control
            item {
                SettingsSectionHeader(title = "手势与滑屏操作", icon = Icons.Default.Swipe)
                Spacer(modifier = Modifier.height(8.dp))
                SettingsCard {
                    SettingsActionRow(
                        title = "向右滑动动作",
                        subtitle = "当前设定: ${swipeRightAction.title} · ${swipeRightAction.description}",
                        actionLabel = "更换",
                        onClick = { showSwipeRightDialog = true }
                    )

                    HorizontalDivider(color = DragonTheme.glassBorder, thickness = 0.5.dp)

                    SettingsActionRow(
                        title = "向左滑动动作",
                        subtitle = "当前设定: ${swipeLeftAction.title} · ${swipeLeftAction.description}",
                        actionLabel = "更换",
                        onClick = { showSwipeLeftDialog = true }
                    )
                }
            }

            // Section: Live Subtitles & Lyrics
            item {
                SettingsSectionHeader(title = "歌词", icon = Icons.Default.Subtitles)
                Spacer(modifier = Modifier.height(8.dp))
                SettingsCard {
                    SettingsActionRow(
                        title = "歌词字体大小",
                        subtitle = "当前字号: ${subtitleFontSizeOption.title} · 适配深浅色模式",
                        actionLabel = "调整",
                        onClick = { showSubtitleFontSizeDialog = true }
                    )
                }
            }

            // Section 5: Storage & Cache Management
            item {
                SettingsSectionHeader(title = "存储与缓存管理", icon = Icons.Default.Storage)
                Spacer(modifier = Modifier.height(8.dp))
                SettingsCard {
                    SettingsActionRow(
                        title = "电台台标与封面缓存",
                        subtitle = "当前磁盘占用：$cacheDisplay (采用 LRU 自动清理)",
                        actionLabel = "清理缓存",
                        onClick = {
                            val freed = viewModel.clearImageCache()
                            val freedMb = String.format("%.1f MB", freed / (1024f * 1024f))
                            Toast.makeText(context, "已清理 $freedMb 图片缓存", Toast.LENGTH_SHORT).show()
                        }
                    )

                    HorizontalDivider(color = DragonTheme.glassBorder, thickness = 0.5.dp)


                    SettingsActionRow(
                        title = "收听历史足迹",
                        subtitle = "已记录 ${historyStations.size} 个电台（上限 60 条，约 15 KB）",
                        actionLabel = "清空足迹",
                        onClick = { showClearHistoryConfirm = true }
                    )
                }
                Spacer(modifier = Modifier.height(18.dp))
            }

            // Section 5.5: Data Backup & Restore (个性化数据备份与恢复)
            item {
                SettingsSectionHeader(title = "个性化数据备份与恢复", icon = Icons.Default.Backup)
                Spacer(modifier = Modifier.height(8.dp))
                SettingsCard {
                    SettingsActionRow(
                        title = "备份个性化数据到本地文件",
                        subtitle = "导出自定义场景、场景内电台覆盖、我的收藏与自定义音乐源为 JSON 文件",
                        actionLabel = "导出备份",
                        actionColor = DragonCyan,
                        onClick = {
                            val timeStr = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
                            createBackupLauncher.launch("DragonFM_Backup_$timeStr.json")
                        }
                    )
                    HorizontalDivider(color = DragonTheme.glassBorder, thickness = 0.5.dp)
                    SettingsActionRow(
                        title = "从本地备份文件恢复",
                        subtitle = "选择之前导出的 JSON 备份文件，一键恢复全部自定义场景、收藏及音乐源",
                        actionLabel = "选择文件恢复",
                        actionColor = DragonTheme.primary,
                        onClick = {
                            openBackupLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                        }
                    )
                }
                Spacer(modifier = Modifier.height(18.dp))
            }

            // Section 6: Version & Open Source Updates
            item {
                SettingsSectionHeader(title = "版本与开源更新", icon = Icons.Default.SystemUpdate)
                Spacer(modifier = Modifier.height(8.dp))
                SettingsCard {
                    SettingsActionRow(
                        title = "检查新版本",
                        subtitle = "当前版本: v${viewModel.currentVersionName} (Build ${viewModel.currentVersionCode}) · 自动同步 GitHub Releases",
                        actionLabel = if (updateCheckResult is UpdateCheckResult.Checking) "检查中..." else "检查更新",
                        actionColor = DragonTheme.primary,
                        onClick = {
                            viewModel.onVersionRowTapped()
                            viewModel.checkForUpdates(isManual = true)
                        }
                    )
                    HorizontalDivider(color = DragonTheme.glassBorder, thickness = 0.5.dp)
                    SettingsSwitchRow(
                        title = "开启新版本检测提醒",
                        subtitle = "开启后在启动应用时自动静默检查更新并在发现新版本时弹出更新提醒；关闭后仅支持手动检查",
                        checked = enableUpdateReminder,
                        onCheckedChange = { viewModel.setEnableUpdateReminder(it) }
                    )
                    HorizontalDivider(color = DragonTheme.glassBorder, thickness = 0.5.dp)
                    SettingsActionRow(
                        title = "GitHub 开源项目",
                        subtitle = "https://github.com/Bais233/lx-fm-android5",
                        actionLabel = "访问",
                        onClick = openGitHubRepo
                    )
                }
                Spacer(modifier = Modifier.height(18.dp))
            }

            // Section 6.5: Hidden developer options (仅调试模式解锁后可见)
            if (debugModeEnabled) {
                item {
                    SettingsSectionHeader(title = "开发者选项", icon = Icons.Default.Bolt)
                    Spacer(modifier = Modifier.height(8.dp))
                    SettingsCard {
                        SettingsSwitchRow(
                            title = "调试模式",
                            subtitle = "输出播放器详细日志（adb logcat 过滤 DragonFm）；关闭后本面板自动隐藏",
                            checked = debugModeEnabled,
                            onCheckedChange = { viewModel.setDebugModeEnabled(it) }
                        )
                        HorizontalDivider(color = DragonTheme.glassBorder, thickness = 0.5.dp)
                        SettingsInfoRow(
                            title = "当前播放状态",
                            subtitle = playerDebugState.currentStation?.name ?: "未在播放",
                            statusText = when (playerDebugState.status) {
                                PlaybackStatus.PLAYING -> "直播中"
                                PlaybackStatus.BUFFERING -> "缓冲"
                                PlaybackStatus.PAUSED -> "已暂停"
                                PlaybackStatus.ERROR -> "错误"
                                PlaybackStatus.IDLE -> "空闲"
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(18.dp))
                }
            }

            // Section 7: Application Control
            item {
                SettingsSectionHeader(title = "应用控制", icon = Icons.Default.PowerSettingsNew)
                Spacer(modifier = Modifier.height(8.dp))
                SettingsCard {
                    SettingsActionRow(
                        title = "完全退出应用程序",
                        subtitle = "停止所有音频流、销毁通知栏后台服务并释放运行内存",
                        actionLabel = "退出",
                        actionColor = LiveRed,
                        onClick = { showQuitConfirm = true }
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Secondary City Picker Dialog (省市二级联动选择器)
    if (showRegionPicker) {
        CityPickerDialog(
            currentProvince = userProvince,
            currentCity = userCity,
            defaultProvince = detectedProvince,
            defaultCity = detectedCity,
            isCustom = isCustomRegion,
            onSelect = { prov, city ->
                viewModel.setUserRegion(prov, city)
                showRegionPicker = false
                Toast.makeText(context, "已将地区设定为「$prov · $city」", Toast.LENGTH_SHORT).show()
            },
            onResetAuto = {
                viewModel.resetToAutoLocation()
                showRegionPicker = false
                Toast.makeText(context, "已恢复网络自动定位", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showRegionPicker = false }
        )
    }

    // Stream Source Mode Dialog (直播源与线路偏好)
    if (showStreamSourceDialog) {
        StreamSourceModeDialog(
            currentMode = streamSourceMode,
            onSelect = { mode ->
                viewModel.setStreamSourceMode(mode)
                showStreamSourceDialog = false
                val toastDesc = when (mode) {
                    StreamSourceMode.HYBRID -> "已启用智能极速模式（官方专线优先，自动容灾）"
                    StreamSourceMode.DEDICATED_ONLY -> "已启用纯净官方专线模式"
                    StreamSourceMode.PUBLIC_ONLY -> "已切换为全球公网广播源"
                }
                Toast.makeText(context, toastDesc, Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showStreamSourceDialog = false }
        )
    }

    // Blocked Stations Dialog
    if (showBlockedStationsDialog) {
        BlockedStationsManagerDialog(
            blockedStations = blockedStations,
            onUnblock = { viewModel.unblockStation(it) },
            onBlockByName = { viewModel.blockStationByName(it) },
            onClearAll = { viewModel.clearAllBlockedStations() },
            onDismiss = { showBlockedStationsDialog = false }
        )
    }

    // Blocked Tags Dialog
    if (showBlockedTagsDialog) {
        BlockedTagsManagerDialog(
            blockedTags = blockedTags,
            onUnblock = { viewModel.unblockTag(it) },
            onBlockTag = { viewModel.blockTag(it) },
            onDismiss = { showBlockedTagsDialog = false }
        )
    }

    // Swipe Right Action Dialog
    if (showSwipeRightDialog) {
        SwipeActionPickerDialog(
            title = "向右滑动动作设定",
            currentAction = swipeRightAction,
            onSelect = { action ->
                viewModel.setSwipeRightAction(action)
                Toast.makeText(context, "已设定右滑动作：${action.title}", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showSwipeRightDialog = false }
        )
    }

    // Swipe Left Action Dialog
    if (showSwipeLeftDialog) {
        SwipeActionPickerDialog(
            title = "向左滑动动作设定",
            currentAction = swipeLeftAction,
            onSelect = { action ->
                viewModel.setSwipeLeftAction(action)
                Toast.makeText(context, "已设定左滑动作：${action.title}", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showSwipeLeftDialog = false }
        )
    }

    // Quick Action Buttons Dialog
    if (showQuickActionsDialog) {
        QuickActionsDialog(
            currentButtons = quickActionButtons,
            onToggle = { btn -> viewModel.toggleQuickActionButton(btn) },
            onMove = { from, to ->
                val list = quickActionButtons.toMutableList()
                if (from in list.indices && to in list.indices) {
                    val item = list.removeAt(from)
                    list.add(to, item)
                    viewModel.setQuickActionButtons(list)
                }
            },
            onReset = {
                viewModel.resetQuickActionButtons()
                Toast.makeText(context, "功能按钮已恢复默认布局与排序", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showQuickActionsDialog = false }
        )
    }

    // Subtitle Font Size Dialog
    if (showSubtitleFontSizeDialog) {
        SubtitleFontSizeDialog(
            currentOption = subtitleFontSizeOption,
            onSelect = { option ->
                viewModel.setSubtitleFontSizeOption(option)
                Toast.makeText(context, "已应用歌词字号：${option.title}", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showSubtitleFontSizeDialog = false }
        )
    }

    // Add Custom Station Dialog
    if (showAddCustomDialog) {
        var nameInput by remember { mutableStateOf("") }
        var urlInput by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddCustomDialog = false },
            containerColor = DragonTheme.surface,
            title = { Text("添加自定义电台流", color = DragonTheme.textPrimary) },
            text = {
                Column {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("电台名称 (如: 私人电台)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        label = { Text("直播流 URL (m3u8/mp3/aac)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (nameInput.isNotBlank() && urlInput.isNotBlank()) {
                            viewModel.addCustomStation(nameInput, urlInput)
                            showAddCustomDialog = false
                            Toast.makeText(context, "已添加并起播「$nameInput」", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DragonCyan, contentColor = Color(0xFF070B12))
                ) {
                    Text("添加并播放", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCustomDialog = false }) {
                    Text("取消", color = DragonTheme.textSecondary)
                }
            }
        )
    }

    // Clear ASR Model Dialog
    if (showClearAsrModelConfirm) {
        AlertDialog(
            onDismissRequest = { showClearAsrModelConfirm = false },
            title = {
                Text(text = "清除离线语音模型", color = DragonTheme.textPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    text = "确定要删除已下载的端侧语音识别模型（约 29 MB）吗？清除后将释放存储空间，后续如需使用电台实时字幕需重新下载。",
                    color = DragonTheme.textSecondary,
                    fontSize = 13.5.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showClearAsrModelConfirm = false
                        val ok = viewModel.deleteAsrModel(deleteEngineAlso = true)
                        if (ok) {
                            Toast.makeText(context, "已清除全部离线语音识别模型及引擎组件", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LiveRed)
                ) {
                    Text("确认清除", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAsrModelConfirm = false }) {
                    Text("取消", color = DragonTheme.textSecondary)
                }
            },
            containerColor = DragonTheme.surface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (showAsrDownloadDialog) {
        AsrModelDownloadDialog(
            modelState = asrModelState,
            currentModelType = currentAsrModelType,
            onDismiss = { viewModel.closeAsrDownloadDialog() },
            onSelectModelType = { viewModel.setAsrModelType(it) },
            onStartDownload = { type -> viewModel.startDownloadAsrModel(type) },
            onCancelDownload = { viewModel.cancelDownloadAsrModel() }
        )
    }

    // Clear History Dialog
    if (showClearHistoryConfirm) {
        AlertDialog(
            onDismissRequest = { showClearHistoryConfirm = false },
            containerColor = DragonTheme.surface,
            title = { Text("清空收听足迹", color = DragonTheme.textPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "确定要清空全部收听足迹吗？此操作无法撤销。",
                    color = DragonTheme.textSecondary,
                    fontSize = 13.5.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearHistory()
                        showClearHistoryConfirm = false
                        Toast.makeText(context, "收听足迹已全部清空", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LiveRed)
                ) {
                    Text("确认清空", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryConfirm = false }) {
                    Text("取消", color = DragonTheme.textSecondary)
                }
            }
        )
    }


            text = title,
            color = DragonTheme.textSecondary,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.3.sp
        )
    }
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = DragonTheme.surface,
        border = BorderStroke(1.dp, DragonTheme.glassBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            content()
        }
    }
}

@Composable
private fun SettingsActionRow(
    title: String,
    subtitle: String,
    actionLabel: String,
    actionColor: Color = DragonTheme.primary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
            Text(
                text = title,
                color = DragonTheme.textPrimary,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = DragonTheme.textSecondary,
                fontSize = 12.sp,
                lineHeight = 16.5.sp
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = actionLabel,
                color = actionColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.width(2.dp))
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = actionColor,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                color = DragonTheme.textPrimary,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = DragonTheme.textSecondary,
                fontSize = 12.sp,
                lineHeight = 16.5.sp
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = if (DragonTheme.isDark) Color(0xFF070B12) else Color.White,
                checkedTrackColor = DragonTheme.primary
            )
        )
    }
}

@Composable
private fun SettingsInfoRow(
    title: String,
    subtitle: String,
    statusText: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
            Text(
                text = title,
                color = DragonTheme.textPrimary,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = DragonTheme.textSecondary,
                fontSize = 12.sp,
                lineHeight = 16.5.sp
            )
        }

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = DragonTheme.glassSurface,
            border = BorderStroke(0.5.dp, DragonTheme.glassBorder)
        ) {
            Text(
                text = statusText,
                color = DragonTheme.primary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

/**
 * Two-tier Province and Secondary City Picker Dialog
 */
@Composable
private fun CityPickerDialog(
    currentProvince: String?,
    currentCity: String?,
    defaultProvince: String? = null,
    defaultCity: String? = null,
    isCustom: Boolean,
    onSelect: (province: String, city: String) -> Unit,
    onResetAuto: () -> Unit,
    onDismiss: () -> Unit
) {
    val initialProvName = (if (isCustom && !currentProvince.isNullOrBlank()) currentProvince else defaultProvince) ?: "河南省"
    val cleanInitial = initialProvName.replace("省", "").replace("市", "").replace("自治区", "").replace("特别行政区", "").trim()
    val matchedProv = ChinaRegionData.allRegions.find {
        it.province.contains(cleanInitial)
    }?.province ?: ChinaRegionData.allRegions.first().province

    var selectedProvince by remember { mutableStateOf(matchedProv) }
    val cities = remember(selectedProvince) {
        ChinaRegionData.getCities(selectedProvince)
    }

    val targetCityClean = (if (isCustom && !currentCity.isNullOrBlank()) currentCity else defaultCity)
        ?.replace("市", "")?.replace("区", "")?.replace("县", "")?.replace("地区", "")?.trim()

    val provinceListState = rememberLazyListState()
    LaunchedEffect(matchedProv) {
        val idx = ChinaRegionData.allRegions.indexOfFirst { it.province == matchedProv }
        if (idx >= 0) {
            provinceListState.scrollToItem((idx - 2).coerceAtLeast(0))
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DragonTheme.surface,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "选择省份与二级市",
                    color = DragonTheme.textPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onResetAuto) {
                    Text("自动定位", color = DragonCyan, fontSize = 12.sp)
                }
            }
        },
        text = {
            Column(modifier = Modifier.height(360.dp)) {
                Text(
                    text = "当前选择：$selectedProvince",
                    color = DragonTheme.primary,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    // Left Column: Provinces
                    LazyColumn(
                        state = provinceListState,
                        modifier = Modifier
                            .weight(0.48f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(8.dp))
                            .background(DragonTheme.glassSurface)
                            .padding(4.dp)
                    ) {
                        items(ChinaRegionData.allRegions) { reg ->
                            val isSel = reg.province == selectedProvince
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSel) DragonTheme.primary.copy(alpha = 0.2f) else Color.Transparent,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedProvince = reg.province }
                            ) {
                                Text(
                                    text = reg.province,
                                    color = if (isSel) DragonTheme.primary else DragonTheme.textPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Right Column: Secondary Cities
                    LazyColumn(
                        modifier = Modifier
                            .weight(0.52f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(8.dp))
                            .background(DragonTheme.glassSurface)
                            .padding(4.dp)
                    ) {
                        items(cities) { city ->
                            val isCitySel = (isCustom && selectedProvince == currentProvince && city == currentCity) ||
                                    (!isCustom && selectedProvince == matchedProv && !targetCityClean.isNullOrBlank() && city.contains(targetCityClean))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isCitySel) DragonCyan.copy(alpha = 0.2f) else Color.Transparent,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSelect(selectedProvince, city)
                                    }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = city,
                                        color = if (isCitySel) DragonCyan else DragonTheme.textPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = if (isCitySel) FontWeight.Bold else FontWeight.Normal
                                    )
                                    if (isCitySel) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = DragonCyan,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭", color = DragonTheme.textSecondary)
            }
        }
    )
}

/**
 * Blocked Stations Management Dialog
 */
@Composable
private fun BlockedStationsManagerDialog(
    blockedStations: Map<String, String>,
    onUnblock: (String) -> Unit,
    onBlockByName: (String) -> Unit,
    onClearAll: () -> Unit,
    onDismiss: () -> Unit
) {
    var inputName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DragonTheme.surface,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "已屏蔽电台 (${blockedStations.size})",
                    color = DragonTheme.textPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                if (blockedStations.isNotEmpty()) {
                    TextButton(onClick = onClearAll) {
                        Text("清空全部", color = LiveRed, fontSize = 12.sp)
                    }
                }
            }
        },
        text = {
            Column(modifier = Modifier.height(320.dp)) {
                // Input Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputName,
                        onValueChange = { inputName = it },
                        placeholder = { Text("输入电台名称添加屏蔽", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Button(
                        onClick = {
                            if (inputName.isNotBlank()) {
                                onBlockByName(inputName)
                                inputName = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DragonCyan, contentColor = Color(0xFF070B12))
                    ) {
                        Text("屏蔽", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (blockedStations.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("暂无屏蔽电台", color = DragonTheme.textSecondary, fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(blockedStations.toList()) { (id, name) ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = DragonTheme.glassSurface,
                                border = BorderStroke(0.5.dp, DragonTheme.glassBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = name,
                                        color = DragonTheme.textPrimary,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    TextButton(onClick = { onUnblock(id) }) {
                                        Text("解除", color = DragonCyan, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("完成", color = DragonTheme.textPrimary)
            }
        }
    )
}

/**
 * Blocked Tags Management Dialog
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BlockedTagsManagerDialog(
    blockedTags: Set<String>,
    onUnblock: (String) -> Unit,
    onBlockTag: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var inputTag by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DragonTheme.surface,
        title = {
            Text(
                text = "屏蔽标签与流派 (${blockedTags.size})",
                color = DragonTheme.textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.height(300.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputTag,
                        onValueChange = { inputTag = it },
                        placeholder = { Text("输入关键词或标签", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Button(
                        onClick = {
                            if (inputTag.isNotBlank()) {
                                onBlockTag(inputTag)
                                inputTag = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DragonCyan, contentColor = Color(0xFF070B12))
                    ) {
                        Text("添加", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (blockedTags.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("未设置任何屏蔽标签", color = DragonTheme.textSecondary, fontSize = 13.sp)
                    }
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        blockedTags.forEach { tag ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = DragonTheme.glassSurface,
                                border = BorderStroke(0.5.dp, DragonTheme.glassBorder)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = tag, color = DragonTheme.textPrimary, fontSize = 12.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "删除",
                                        tint = LiveRed,
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clickable { onUnblock(tag) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("完成", color = DragonTheme.textPrimary)
            }
        }
    )
}

/**
 * Stream Source & Line Preference Selection Dialog
 */
@Composable
private fun StreamSourceModeDialog(
    currentMode: StreamSourceMode,
    onSelect: (StreamSourceMode) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DragonTheme.surface,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Filled.Bolt,
                    contentDescription = null,
                    tint = DragonCyan,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "选择直播源与线路偏好",
                    color = DragonTheme.textPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StreamSourceMode.values().forEach { mode ->
                    val isSelected = (mode == currentMode)
                    Surface(
                        onClick = { onSelect(mode) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) DragonCyan.copy(alpha = 0.12f) else DragonTheme.glassSurface,
                        border = BorderStroke(
                            if (isSelected) 1.5.dp else 0.5.dp,
                            if (isSelected) DragonCyan else DragonTheme.glassBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = mode.title,
                                        color = if (isSelected) DragonCyan else DragonTheme.textPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (mode == StreamSourceMode.HYBRID) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = DragonCyan.copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = "推荐",
                                                color = DragonCyan,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = mode.desc,
                                    color = DragonTheme.textSecondary,
                                    fontSize = 11.5.sp,
                                    lineHeight = 15.5.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            RadioButton(
                                selected = isSelected,
                                onClick = { onSelect(mode) },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = DragonCyan,
                                    unselectedColor = DragonTheme.textSecondary.copy(alpha = 0.5f)
                                )
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭", color = DragonCyan, fontWeight = FontWeight.Bold)
            }
        }
    )
}


@Composable
private fun QuickActionsDialog(
    currentButtons: List<QuickActionButton>,
    onToggle: (QuickActionButton) -> Unit,
    onMove: (fromIndex: Int, toIndex: Int) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    val allButtons = QuickActionButton.entries
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("下方快捷功能按钮自定义", color = DragonTheme.textPrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "勾选需要展示的功能按钮，通过上下箭头调整其在界面的排列顺序：",
                    color = DragonTheme.textSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                LazyColumn(modifier = Modifier.fillMaxWidth().height(300.dp)) {
                    itemsIndexed(currentButtons) { index, button ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = DragonTheme.glassSurface,
                            border = BorderStroke(0.5.dp, DragonTheme.glassBorder),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = true,
                                    onCheckedChange = { onToggle(button) },
                                    colors = CheckboxDefaults.colors(checkedColor = DragonTheme.primary)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(button.icon, contentDescription = null, tint = DragonTheme.primary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(button.title, color = DragonTheme.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))

                                IconButton(
                                    onClick = { if (index > 0) onMove(index, index - 1) },
                                    enabled = index > 0,
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up", modifier = Modifier.size(16.dp), tint = if (index > 0) DragonTheme.textPrimary else DragonTheme.textSecondary.copy(alpha = 0.3f))
                                }
                                IconButton(
                                    onClick = { if (index < currentButtons.size - 1) onMove(index, index + 1) },
                                    enabled = index < currentButtons.size - 1,
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down", modifier = Modifier.size(16.dp), tint = if (index < currentButtons.size - 1) DragonTheme.textPrimary else DragonTheme.textSecondary.copy(alpha = 0.3f))
                                }
                            }
                        }
                    }

                    // Disabled buttons list
                    val disabledButtons = allButtons.filter { !currentButtons.contains(it) }
                    if (disabledButtons.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("未启用的按钮（点击添加）：", color = DragonTheme.textSecondary, fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                        items(disabledButtons) { button ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = DragonTheme.glassSurface.copy(alpha = 0.5f),
                                border = BorderStroke(0.5.dp, DragonTheme.glassBorder),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp).clickable { onToggle(button) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = false,
                                        onCheckedChange = { onToggle(button) },
                                        colors = CheckboxDefaults.colors(uncheckedColor = DragonTheme.textSecondary)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(button.icon, contentDescription = null, tint = DragonTheme.textSecondary, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(button.title, color = DragonTheme.textSecondary, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("完成", color = DragonTheme.primary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onReset) {
                Text("恢复默认", color = LiveRed)
            }
        },
        containerColor = DragonTheme.surface,
        shape = RoundedCornerShape(18.dp)
    )
}

@Composable
private fun SwipeActionPickerDialog(
    title: String,
    currentAction: SwipeGestureAction,
    onSelect: (SwipeGestureAction) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(title, color = DragonTheme.textPrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                SwipeGestureAction.entries.forEach { action ->
                    val isSelected = action == currentAction
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) DragonTheme.primary.copy(alpha = 0.12f) else DragonTheme.glassSurface,
                        border = BorderStroke(
                            if (isSelected) 1.dp else 0.5.dp,
                            if (isSelected) DragonTheme.primary else DragonTheme.glassBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                onSelect(action)
                                onDismiss()
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    onSelect(action)
                                    onDismiss()
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = DragonTheme.primary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = action.title,
                                    color = if (isSelected) DragonTheme.primary else DragonTheme.textPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = action.description,
                                    color = DragonTheme.textSecondary,
                                    fontSize = 11.5.sp
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭", color = DragonTheme.textSecondary)
            }
        },
        containerColor = DragonTheme.surface,
        shape = RoundedCornerShape(18.dp)
    )
}

@Composable
private fun SubtitleFontSizeDialog(
    currentOption: SubtitleFontSize,
    onSelect: (SubtitleFontSize) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("歌词字号调节", color = DragonTheme.textPrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                SubtitleFontSize.entries.forEach { option ->
                    val isSelected = option == currentOption
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) DragonTheme.primary.copy(alpha = 0.12f) else DragonTheme.glassSurface,
                        border = BorderStroke(
                            if (isSelected) 1.dp else 0.5.dp,
                            if (isSelected) DragonTheme.primary else DragonTheme.glassBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                onSelect(option)
                                onDismiss()
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    onSelect(option)
                                    onDismiss()
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = DragonTheme.primary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = option.title,
                                    color = if (isSelected) DragonTheme.primary else DragonTheme.textPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭", color = DragonTheme.textSecondary)
            }
        },
        containerColor = DragonTheme.surface,
        shape = RoundedCornerShape(18.dp)
    )
}
