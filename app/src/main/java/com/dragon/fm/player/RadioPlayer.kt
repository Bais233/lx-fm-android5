package com.dragon.fm.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.dragon.fm.data.Station
import com.dragon.fm.net.Http
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class RadioPlayer(context: Context) {

    data class State(
        val station: Station? = null,
        val playing: Boolean = false,
        val buffering: Boolean = false,
        val error: String? = null,
    )

    private val appContext = context.applicationContext

    val exoPlayer: ExoPlayer = ExoPlayer.Builder(appContext)
        .setMediaSourceFactory(
            DefaultMediaSourceFactory(OkHttpDataSource.Factory(Http.client(appContext)))
        )
        .build()

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state

    private var retried = false
    
    private var queue: List<Station> = emptyList()
    var index: Int = 0
        private set

    init {
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                sync()
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                sync()
            }

            override fun onPlayerError(error: PlaybackException) {
                onError()
            }
        })
    }

    fun play(station: Station) {
        retried = false
        load(station, station.url)
    }

    fun playList(list: List<Station>, position: Int) {
        queue = list
        index = position.coerceIn(0, (list.size - 1).coerceAtLeast(0))
        list.getOrNull(index)?.let { play(it) }
    }

    fun next() {
        if (queue.isEmpty()) return
        index = (index + 1) % queue.size
        play(queue[index])
    }

    fun previous() {
        if (queue.isEmpty()) return
        index = (index - 1 + queue.size) % queue.size
        play(queue[index])
    }

    fun toggle() {
        val current = exoPlayer.playbackState
        when {
            exoPlayer.isPlaying -> exoPlayer.pause()
            current == Player.STATE_IDLE -> _state.value.station?.let { play(it) }
            else -> exoPlayer.play()
        }
    }

    private fun load(station: Station, url: String) {
        _state.value = State(station = station, buffering = true)
        val metadata = MediaMetadata.Builder()
            .setTitle(station.name)
            .setArtist(station.program ?: "电台")
            .build()
        exoPlayer.setMediaItem(MediaItem.Builder().setUri(url).setMediaMetadata(metadata).build())
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    private fun onError() {
        val station = _state.value.station
        val alternate = station?.alternateUrl
        if (!retried && station != null && alternate != null) {
            retried = true
            load(station, alternate)
            return
        }
        _state.value = _state.value.copy(
            buffering = false,
            playing = false,
            error = "连不上" + (if (station != null) "：" + station.name else ""),
        )
    }

    private fun sync() {
        _state.value = _state.value.copy(
            playing = exoPlayer.isPlaying,
            buffering = exoPlayer.playbackState == Player.STATE_BUFFERING,
            error = if (exoPlayer.playbackState == Player.STATE_READY) null else _state.value.error,
        )
    }
}
