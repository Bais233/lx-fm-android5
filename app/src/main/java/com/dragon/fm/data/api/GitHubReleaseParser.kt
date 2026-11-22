package com.dragon.fm.data.api

import com.dragon.fm.data.model.AppUpdateInfo
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal object GitHubReleaseParser {
    private val json = Json { ignoreUnknownKeys = true }
    private val versionCodePattern = Regex("(?m)^[ \t]*versionCode[ \t]*:[ \t]*([0-9]+)[ \t]*\r?$")

    fun parse(body: String): AppUpdateInfo {
        val release = json.parseToJsonElement(body).jsonObject
        val tagName = release.string("tag_name").orEmpty().trim()
        val releaseName = release.string("name").orEmpty().trim()
        val versionName = tagName.removePrefix("v").ifBlank { releaseName }
        if (versionName.isBlank()) {
            throw IllegalArgumentException("最新 Release 缺少 tag_name 和 name，无法确定版本")
        }

        val changelog = release.string("body").orEmpty()
        val codeMatch = versionCodePattern.find(changelog)
            ?: throw IllegalArgumentException("最新 Release 正文缺少 versionCode: N 元数据，无法安全判断更新")
        val versionCode = codeMatch.groupValues[1].toIntOrNull()
            ?: throw IllegalArgumentException("最新 Release 的 versionCode 无效")

        val apkAssets = (release["assets"] as? JsonArray).orEmpty().mapNotNull { element ->
            val asset = element as? JsonObject ?: return@mapNotNull null
            val name = asset.string("name") ?: return@mapNotNull null
            val url = asset.string("browser_download_url") ?: return@mapNotNull null
            if (name.endsWith(".apk", ignoreCase = true) && url.isNotBlank()) name to url else null
        }
        if (apkAssets.isEmpty()) {
            throw IllegalArgumentException("最新 Release 未提供可下载的 APK 文件")
        }

        val universal = apkAssets.firstOrNull { it.first.contains("universal", ignoreCase = true) }
            ?: apkAssets.first()
        val abiUrls = apkAssets.mapNotNull { (name, url) ->
            assetAbi(name)?.let { abi -> abi to url }
        }.toMap()

        return AppUpdateInfo(
            versionCode = versionCode,
            versionName = versionName,
            changelog = changelog,
            downloadUrl = universal.second,
            abiUrls = abiUrls,
            releaseDate = release.string("published_at").orEmpty()
        )
    }

    private fun JsonObject.string(key: String): String? =
        this[key]?.jsonPrimitive?.contentOrNull

    private fun assetAbi(assetName: String): String? =
        listOf("arm64-v8a", "armeabi-v7a", "x86_64", "x86")
            .firstOrNull { assetName.contains(it, ignoreCase = true) }
}
