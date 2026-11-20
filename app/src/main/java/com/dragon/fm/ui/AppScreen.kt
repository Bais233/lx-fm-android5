package com.dragon.fm.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dragon.fm.DragonFmApp
import com.dragon.fm.data.Province
import com.dragon.fm.data.Repository
import com.dragon.fm.data.Station
import com.dragon.fm.player.PlaybackService

private val Bg = Color(0xFF0B0F19)
private val Card = Color(0xFF141B2A)
private val Accent = Color(0xFF57D7FF)
private val Dim = Color(0xFF8A94A6)
private val Ink = Color(0xFFE6ECF5)

@Composable
fun LxFm5App() {
    val context = LocalContext.current
    var provinces by remember { mutableStateOf(listOf(Province(0, "全国"))) }
    var current by remember { mutableStateOf(Province(0, "全国")) }
    var stations by remember { mutableStateOf(emptyList<Station>()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var provincesLoaded by remember { mutableStateOf(false) }
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

    Column(modifier = Modifier.fillMaxSize().background(Bg)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 22.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("LX FM", color = Accent, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            Text("5.1 精简版", color = Dim, fontSize = 12.sp)
        }

        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(provinces) { province ->
                val selected = province.code == current.code
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (selected) Accent else Card)
                        .clickable { current = province }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text(
                        province.name,
                        color = if (selected) Color(0xFF06121C) else Ink,
                        fontSize = 13.sp
                    )
                }
            }
        }

        Box(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(top = 8.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            when {
                loading -> Text("加载中…", color = Dim, modifier = Modifier.padding(top = 40.dp))
                error != null -> Text(error ?: "", color = Dim, modifier = Modifier.padding(top = 40.dp))
                else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(stations) { station ->
                        StationRow(station, state.station?.id == station.id && state.playing) {
                            playStation(context, station)
                        }
                    }
                }
            }
        }

        PlayerBar(
            name = state.station?.name,
            program = state.station?.program,
            playing = state.playing,
            buffering = state.buffering,
            error = state.error
        ) {
            DragonFmApp.player.toggle()
        }
    }
}

@Composable
private fun StationRow(station: Station, active: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (active) Color(0xFF1B2A3E) else Card)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(station.name, color = Ink, fontSize = 15.sp, maxLines = 1)
            Text(
                station.program ?: if (station.dedicated) "官方专线" else "公网源",
                color = Dim,
                fontSize = 11.sp,
                maxLines = 1
            )
        }
        Text(
            if (station.dedicated) "官方" else "公网",
            color = if (station.dedicated) Accent else Dim,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun PlayerBar(
    name: String?,
    program: String?,
    playing: Boolean,
    buffering: Boolean,
    error: String?,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().background(Card).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(name ?: "选一个电台", color = Ink, fontSize = 14.sp, maxLines = 1)
            Text(
                when {
                    error != null -> error
                    buffering -> "正在连接…"
                    playing -> program ?: "正在播放"
                    name != null -> "已暂停"
                    else -> "点上面的频道开始"
                },
                color = if (error != null) Color(0xFFFF8A80) else Dim,
                fontSize = 11.sp,
                maxLines = 1
            )
        }
        Button(onClick = onToggle, enabled = name != null) {
            Text(if (playing) "暂停" else "播放", fontSize = 13.sp)
        }
    }
}

private fun playStation(context: Context, station: Station) {
    DragonFmApp.player.play(station)
    // 从界面里起服务：8.0+ 允许，开播后 Media3 自己转前台；5.1 没有任何限制
    runCatching { context.startService(Intent(context, PlaybackService::class.java)) }
}
