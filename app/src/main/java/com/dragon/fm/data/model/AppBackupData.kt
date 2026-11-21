package com.dragon.fm.data.model

import kotlinx.serialization.Serializable

/**
 * Dragon FM 个性化配置本地备份数据模型
 * 包含：
 * 1. 自定义场景（名称、图标、搜索关键字、内置电台列表）
 * 2. 内置场景电台覆盖与增删配置
 * 3. 场景全局展示排序
 * 4. 隐藏/删除的内置场景记录
 * 5. 我的收藏电台列表
 * 6. 自定义添加的网络广播源
 * 7. 自定义电台重命名别名
 * 8. 收听历史记录
 */
@Serializable
data class AppBackupData(
    val version: Int = 1,
    val appName: String = "Dragon FM",
    val appVersion: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val exportDate: String = "",
    // 自定义场景列表
    val customScenarios: List<CustomScenarioMode> = emptyList(),
    // 内置场景内频道覆盖配置（Map: 场景名 -> 电台列表）
    val scenarioStationOverrides: Map<String, List<RadioStation>> = emptyMap(),
    // 全场景排序列表
    val scenarioOrder: List<String> = emptyList(),
    // 隐藏/删除的内置场景
    val deletedDefaultScenarios: List<String> = emptyList(),
    // 我的收藏电台列表
    val favoriteStations: List<RadioStation> = emptyList(),
    // 用户自定义添加的网络广播电台/音乐源
    val customStations: List<RadioStation> = emptyList(),
    // 自定义电台重命名别名映射
    val customNames: Map<String, String> = emptyMap(),
    // 播放历史记录
    val historyStations: List<RadioStation> = emptyList()
)

/**
 * 备份恢复结果摘要
 */
@Serializable
data class BackupRestoreSummary(
    val success: Boolean,
    val scenarioCount: Int = 0,
    val favoriteCount: Int = 0,
    val customStationCount: Int = 0,
    val historyCount: Int = 0,
    val message: String = ""
)
