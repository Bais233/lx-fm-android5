package com.dragon.fm.data.model

import kotlinx.serialization.Serializable
import java.io.File

@Serializable
data class AppUpdateInfo(
    val versionCode: Int = 1,
    val versionName: String = "1.0.0",
    val changelog: String = "",
    val downloadUrl: String = "",
    val abiUrls: Map<String, String> = emptyMap(),
    val forceUpdate: Boolean = false,
    val releaseDate: String = ""
) {
    /**
     * 智能匹配当前设备最佳 ABI 架构包
     * 优先匹配 Build.SUPPORTED_ABIS 首选架构（如 arm64-v8a、armeabi-v7a、x86_64）
     * 若未配置对应分包，则平滑降级至 universal 通用全架构包 downloadUrl
     */
    fun resolveBestDownloadUrl(): String {
        if (abiUrls.isNotEmpty()) {
            val supportedAbis = android.os.Build.SUPPORTED_ABIS
            for (abi in supportedAbis) {
                val matched = abiUrls[abi]
                if (!matched.isNullOrBlank()) {
                    return matched
                }
            }
        }
        return downloadUrl
    }

    /**
     * 获取当前设备匹配到的 ABI 架构名称（用于 UI 展示与文件归档）
     */
    fun getResolvedAbiName(): String {
        if (abiUrls.isNotEmpty()) {
            val supportedAbis = android.os.Build.SUPPORTED_ABIS
            for (abi in supportedAbis) {
                if (!abiUrls[abi].isNullOrBlank()) {
                    return abi
                }
            }
        }
        return android.os.Build.SUPPORTED_ABIS.firstOrNull() ?: "universal"
    }
}

sealed class UpdateCheckResult {
    object Idle : UpdateCheckResult()
    object Checking : UpdateCheckResult()
    data class HasUpdate(val info: AppUpdateInfo, val isManual: Boolean = false) : UpdateCheckResult()
    data class UpToDate(val currentVersion: String, val isManual: Boolean = false) : UpdateCheckResult()
    data class Error(val message: String, val isManual: Boolean = false) : UpdateCheckResult()
}

sealed class DownloadState {
    object Idle : DownloadState()
    data class Downloading(
        val progress: Float,
        val currentBytes: Long,
        val totalBytes: Long
    ) : DownloadState()
    data class Completed(val apkFile: File) : DownloadState()
    data class Error(val message: String) : DownloadState()
}
