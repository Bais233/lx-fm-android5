package com.dragon.fm.ui

import android.content.Context
import android.content.Intent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.dragon.fm.DragonFmApp
import com.dragon.fm.data.Province
import com.dragon.fm.data.Repository
import com.dragon.fm.data.Station
import com.dragon.fm.player.PlaybackService

private val Bg = Color(0xFFEEF3F9)
private val Card = Color(0xFFFFFFFF)
private val Accent = Color(0xFF2B6EF6)
private val Ink = Color(0xFF101828)
private val Dim = Color(0xFF8A94A6)
private val ChipBg = Color(0xFFE3EBF6)

@Composable
fun LxFm5App() {
    val context = LocalContext.current
    var provinces by remember { mutableStateOf(listOf(Province(0, "全国"))) }
    var current by remember { mutableStateOf(Province(0, "全国")) }
    var stations by remember { mutableStateOf(emptyList<Station>()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var provincesLoaded by remember { mutableStateOf(false) }
    var showPicker by remember { mutableStateOf(false) }
    val state by DragonFmApp.player.state.collectAsState()

    LaunchedEffect(current) {
        loading = true
        error = null
        Repository.init(context)
        if (!provincesLoaded) {
            val remote = Repository.provinces()
            if (remote.isNotEmpty()) provinces = listOf(Province(0, "全国")) + remote
            provincesLoaded = true
        }
        val list = Repository.stations(current)
        stations = list
        if (list.isEmpty()) error = "这个省没取到电台，换一个试试"
        loading = false
    }

    Box(modifier = Modifier.fillMaxSize().background(Bg)) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp)) {

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Card)
                        .clickable { showPicker = true }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("☰", color = Ink, fontSize = 15.sp)
                    Spacer(Modifier.width(8.dp))
                    Text("精选推荐", color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Card)
                        .clickable { showPicker = true }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(current.name, color = Accent, fontSize = 13.sp)
                }
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                when {
                    loading -> Text("正在加载…", color = Dim, fontSize = 14.sp)
                    error != null -> Text(error ?: "", color = Dim, fontSize = 14.sp)
                    else -> NowPlaying(
                        station = state.station,
                        playing = state.playing,
                        buffering = state.buffering,
                        error = state.error,
                    )
                }
            }

            Transport(
                playing = state.playing,
                hasStation = state.station != null,
                index = DragonFmApp.player.index,
                total = stations.size,
                onPrev = { DragonFmApp.player.previous() },
                onToggle = { DragonFmApp.player.toggle() },
                onNext = { DragonFmApp.player.next() },
                onList = { showPicker = true },
            )
        }
    }

    if (showPicker) {
        PickerDialog(
            provinces = provinces,
            stations = stations,
            current = current,
            onPickProvince = { current = it },
            onPickStation = { position ->
                startPlayback(context, stations, position)
                showPicker = false
            },
            onDismiss = { showPicker = false },
        )
    }
}

@Composable
private fun NowPlaying(station: Station?, playing: Boolean, buffering: Boolean, error: String?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Vinyl(station, playing, 240.dp)
        Spacer(Modifier.height(30.dp))

        val line = when {
            error != null -> error
            station?.program != null -> "正在直播： " + station.program
            buffering -> "正在连接…"
            else -> null
        }
        if (line != null) {
            Text(line, color = if (error != null) Color(0xFFE2554B) else Accent, fontSize = 14.sp)
            Spacer(Modifier.height(8.dp))
        }

        Text(
            text = station?.name ?: "选一个电台",
            color = Ink,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Pill(if (playing) "● LIVE 直播" else "○ 待播放")
            Pill(if (station?.dedicated == true) "高品质专线" else "公网源")
        }
        Spacer(Modifier.height(26.dp))
        WaveRule()
    }
}

@Composable
private fun Pill(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Card)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(text, color = Ink, fontSize = 12.sp)
    }
}

@Composable
private fun Vinyl(station: Station?, playing: Boolean, size: Dp) {
    val transition = rememberInfiniteTransition(label = "vinyl")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(32000, easing = LinearEasing), RepeatMode.Restart),
        label = "angle",
    )

    Box(
        modifier = Modifier
            .size(size)
            .graphicsLayer { rotationZ = if (playing) angle else 0f },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val radius = kotlin.math.min(this.size.width, this.size.height) / 2f
            drawCircle(Color(0xFF1B1B1F), radius = radius)
            var groove = radius * 0.96f
            while (groove > radius * 0.60f) {
                drawCircle(Color(0xFF2B2B31), radius = groove, style = Stroke(width = 1.5f))
                groove -= radius * 0.035f
            }
        }

        Box(
            modifier = Modifier.size(size * 0.44f).clip(CircleShape).background(Color.White),
            contentAlignment = Alignment.Center,
        ) {
            val icon = station?.icon
            if (!icon.isNullOrBlank()) {
                AsyncImage(
                    model = icon,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Text("FM", color = Accent, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }

        Box(Modifier.size(size * 0.035f).clip(CircleShape).background(Color(0xFF1B1B1F)))
    }
}

@Composable
private fun WaveRule() {
    Canvas(Modifier.fillMaxWidth().height(16.dp)) {
        val step = 12.dp.toPx()
        var x = 0f
        while (x < size.width) {
            drawLine(
                color = Accent,
                start = Offset(x, size.height / 2f),
                end = Offset(x + step * 0.45f, size.height / 2f),
                strokeWidth = 2.5.dp.toPx(),
            )
            x += step
        }
    }
}

@Composable
private fun Transport(
    playing: Boolean,
    hasStation: Boolean,
    index: Int,
    total: Int,
    onPrev: () -> Unit,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onList: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 22.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 6.dp, end = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (total > 0) "列表 · " + (index + 1) + "/" + total else "列表",
                color = Dim,
                fontSize = 12.sp,
                modifier = Modifier.clickable { onList() }.padding(6.dp),
            )
            Spacer(Modifier.weight(1f))
            Text("LX FM 5.1", color = Dim, fontSize = 11.sp)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier.size(52.dp).clip(CircleShape).clickable { onPrev() },
                contentAlignment = Alignment.Center,
            ) {
                Triangle(color = if (hasStation) Ink else Dim, right = false)
            }
            Spacer(Modifier.width(26.dp))
            Box(
                modifier = Modifier
                    .size(74.dp)
                    .clip(CircleShape)
                    .background(if (hasStation) Accent else ChipBg)
                    .clickable { if (hasStation) onToggle() },
                contentAlignment = Alignment.Center,
            ) {
                PlayPause(playing = playing, color = Color.White)
            }
            Spacer(Modifier.width(26.dp))
            Box(
                modifier = Modifier.size(52.dp).clip(CircleShape).clickable { onNext() },
                contentAlignment = Alignment.Center,
            ) {
                Triangle(color = if (hasStation) Ink else Dim, right = true)
            }
        }
    }
}

@Composable
private fun Triangle(color: Color, right: Boolean) {
    Canvas(Modifier.size(20.dp)) {
        val w = size.width
        val h = size.height
        val path = Path()
        if (right) {
            path.moveTo(0f, 0f)
            path.lineTo(w, h / 2f)
            path.lineTo(0f, h)
        } else {
            path.moveTo(w, 0f)
            path.lineTo(0f, h / 2f)
            path.lineTo(w, h)
        }
        path.close()
        drawPath(path, color)
    }
}

@Composable
private fun PlayPause(playing: Boolean, color: Color) {
    Canvas(Modifier.size(26.dp)) {
        val w = size.width
        val h = size.height
        if (playing) {
            val bar = w * 0.20f
            drawRect(color, topLeft = Offset(w * 0.22f, 0f), size = Size(bar, h))
            drawRect(color, topLeft = Offset(w * 0.58f, 0f), size = Size(bar, h))
        } else {
            val path = Path()
            path.moveTo(w * 0.24f, 0f)
            path.lineTo(w * 0.88f, h / 2f)
            path.lineTo(w * 0.24f, h)
            path.close()
            drawPath(path, color)
        }
    }
}

@Composable
private fun PickerDialog(
    provinces: List<Province>,
    stations: List<Station>,
    current: Province,
    onPickProvince: (Province) -> Unit,
    onPickStation: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Bg)
                .padding(14.dp)
        ) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(provinces) { province ->
                    val selected = province.code == current.code
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (selected) Accent else Card)
                            .clickable { onPickProvince(province) }
                            .padding(horizontal = 13.dp, vertical = 7.dp)
                    ) {
                        Text(
                            province.name,
                            color = if (selected) Color.White else Ink,
                            fontSize = 12.sp,
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                itemsIndexed(stations) { position, station ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Card)
                            .clickable { onPickStation(position) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(station.name, color = Ink, fontSize = 14.sp, maxLines = 1)
                            val sub = station.program ?: if (station.dedicated) "官方专线" else "公网源"
                            Text(sub, color = Dim, fontSize = 11.sp, maxLines = 1)
                        }
                        Text(
                            if (station.dedicated) "官方" else "公网",
                            color = if (station.dedicated) Accent else Dim,
                            fontSize = 11.sp,
                        )
                    }
                }
            }
        }
    }
}

private fun startPlayback(context: Context, stations: List<Station>, position: Int) {
    DragonFmApp.player.playList(stations, position)
    // 从界面里起服务：8.0+ 允许，开播后 Media3 自己转前台；5.1 没有限制
    runCatching { context.startService(Intent(context, PlaybackService::class.java)) }
}
