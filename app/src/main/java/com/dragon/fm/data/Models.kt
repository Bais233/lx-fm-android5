package com.dragon.fm.data

data class Station(
    val id: String,
    val name: String,
    val url: String,
    val alternateUrl: String? = null,
    val region: String = "",
    val program: String? = null,
    val bitrate: Int = 0,
    val dedicated: Boolean = false,
)

data class Province(
    val code: Int,
    val name: String,
)
