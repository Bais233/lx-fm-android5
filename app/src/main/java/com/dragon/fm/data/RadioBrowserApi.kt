package com.dragon.fm.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder

/** 公网兜底源：radio-browser.info */
class RadioBrowserApi(private val client: OkHttpClient) {

    private val servers = listOf(
        "https://de1.api.radio-browser.info",
        "https://nl1.api.radio-browser.info",
        "https://at1.api.radio-browser.info",
    )

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private fun JsonObject.str(key: String): String? =
        (this[key] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }

    private fun JsonObject.toStation(): Station? {
        val uuid = str("stationuuid") ?: return null
        val name = str("name")?.trim() ?: return null
        val url = str("url_resolved") ?: str("url") ?: return null
        return Station(
            id = "b_" + uuid,
            name = name,
            url = url,
            region = str("state") ?: str("country") ?: "",
            bitrate = (this["bitrate"] as? JsonPrimitive)?.intOrNull ?: 0,
            dedicated = false,
        )
    }

    private fun fetch(server: String, keyword: String?, limit: Int): List<Station> {
        val name = if (keyword.isNullOrBlank()) "" else "&name=" + URLEncoder.encode(keyword, "UTF-8")
        val url = server + "/json/stations/search?countrycode=CN&hidebroken=true&order=votes&reverse=true&limit=" + limit + name
        return try {
            val request = Request.Builder().url(url).header("User-Agent", "LXFM5/1.0").build()
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) {
                    emptyList()
                } else {
                    val arr = json.parseToJsonElement(resp.body?.string().orEmpty()) as? JsonArray
                    arr?.mapNotNull { (it as? JsonObject)?.toStation() } ?: emptyList()
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun searchChinese(keyword: String?, limit: Int = 100): List<Station> = withContext(Dispatchers.IO) {
        for (server in servers) {
            val list = fetch(server, keyword, limit)
            if (list.isNotEmpty()) return@withContext list
        }
        emptyList()
    }
}
