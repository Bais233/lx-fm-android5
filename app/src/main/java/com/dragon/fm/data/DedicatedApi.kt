package com.dragon.fm.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import okhttp3.OkHttpClient
import okhttp3.Request
import java.security.MessageDigest
import java.util.Locale

/**
 * 官方专线源（ytmsout.radio.cn），签名规则沿用 LX-FM 原版：
 * uppercase(MD5(sorted_params + "&timestamp=" + ts + "&key=" + KEY))
 */
class DedicatedApi(private val client: OkHttpClient) {

    companion object {
        private const val BASE = "https://ytmsout.radio.cn/web"
        private const val SIGN_KEY = "f0fc4c668392f9f9a447e48584c214ee"
        private const val UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private fun sign(params: Map<String, Any>): Pair<String, String> {
        val ts = System.currentTimeMillis().toString()
        val sorted = params.toSortedMap().entries.joinToString("&") { it.key + "=" + it.value }
        val raw = if (sorted.isBlank()) {
            "timestamp=" + ts + "&key=" + SIGN_KEY
        } else {
            sorted + "&timestamp=" + ts + "&key=" + SIGN_KEY
        }
        val digest = MessageDigest.getInstance("MD5").digest(raw.toByteArray(Charsets.UTF_8))
        val sb = StringBuilder(digest.size * 2)
        for (b in digest) sb.append(String.format(Locale.US, "%02X", b))
        return ts to sb.toString()
    }

    private fun get(path: String, params: Map<String, Any>, query: String): JsonObject? {
        val pair = sign(params)
        val request = Request.Builder()
            .url(BASE + path + query)
            .header("User-Agent", UA)
            .header("sign", pair.second)
            .header("timestamp", pair.first)
            .header("equipmentId", "0000")
            .header("platformCode", "WEB")
            .build()
        return try {
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val body = resp.body?.string().orEmpty()
                runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull()
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun JsonObject.str(key: String): String? =
        (this[key] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }

    private fun JsonObject.int(key: String): Int =
        (this[key] as? JsonPrimitive)?.intOrNull ?: 0

    suspend fun provinces(): List<Province> = withContext(Dispatchers.IO) {
        val root = get("/appProvince/list/all", emptyMap(), "") ?: return@withContext emptyList()
        val arr = root["data"] as? JsonArray ?: return@withContext emptyList()
        arr.mapNotNull { element ->
            val o = element as? JsonObject ?: return@mapNotNull null
            val code = o.int("provinceCode")
            val name = o.str("provinceName") ?: return@mapNotNull null
            if (code == 0) null else Province(code, name)
        }
    }

    suspend fun broadcasts(provinceCode: Int): List<Station> = withContext(Dispatchers.IO) {
        val params = mapOf<String, Any>("categoryId" to 0, "provinceCode" to provinceCode)
        val query = "?categoryId=0&provinceCode=" + provinceCode
        val root = get("/appBroadcast/list", params, query) ?: return@withContext emptyList()
        val arr = root["data"] as? JsonArray ?: return@withContext emptyList()
        arr.mapNotNull { element ->
            val o = element as? JsonObject ?: return@mapNotNull null
            val contentId = o.str("contentId") ?: return@mapNotNull null
            val title = o.str("title") ?: return@mapNotNull null
            val url = o.str("mp3PlayUrlHigh") ?: o.str("mp3PlayUrlLow") ?: o.str("playUrlLow")
                ?: return@mapNotNull null
            val alt = o.str("playUrlLow")?.takeIf { it != url }
            val program = o.str("subtitle")
                ?.replace("正在直播：", "")
                ?.replace("正在直播:", "")
                ?.trim()
                ?.takeIf { it.isNotEmpty() && it != "暂无节目单" }
            Station(
                id = "d_" + contentId,
                name = title.trim(),
                url = url,
                alternateUrl = alt,
                program = program,
                dedicated = true,
            )
        }
    }
}
