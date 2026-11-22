package com.dragon.fm.data.model

import kotlinx.serialization.Serializable
import java.util.UUID

/** Metadata retained for static time-aligned song lyrics. */
@Serializable
data class RecognizedSong(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val artist: String,
    val album: String = "",
    val coverArtUrl: String = "",
    val genre: String = "",
    val releaseYear: String = "",
    val shazamUrl: String = "",
    val stationName: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val matchedOffsetSeconds: Double = 0.0,
    val sampleDurationMs: Long = 0L,
    val sampleStartRealtime: Long = 0L
)
