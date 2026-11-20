package com.dragon.fm.data

import android.content.Context
import com.dragon.fm.net.Http
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object Repository {

    private var dedicated: DedicatedApi? = null
    private var browser: RadioBrowserApi? = null

    fun init(context: Context) {
        if (dedicated != null && browser != null) return
        val client = Http.client(context)
        dedicated = DedicatedApi(client)
        browser = RadioBrowserApi(client)
    }

    suspend fun provinces(): List<Province> = withContext(Dispatchers.IO) {
        dedicated?.provinces() ?: emptyList()
    }

    suspend fun stations(province: Province): List<Station> = withContext(Dispatchers.IO) {
        val official = dedicated?.broadcasts(province.code) ?: emptyList()
        if (official.isNotEmpty()) return@withContext official
        browser?.searchChinese(if (province.code == 0) null else province.name) ?: emptyList()
    }
}
