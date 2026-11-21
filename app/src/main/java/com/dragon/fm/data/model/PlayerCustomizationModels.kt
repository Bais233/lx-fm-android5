package com.dragon.fm.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Equalizer
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.QueueMusic
import androidx.compose.material.icons.outlined.Star
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 播放页封面展示样式
 */
enum class PlayerCoverStyle(val id: String, val title: String, val description: String) {
    VINYL("vinyl", "经典黑胶唱片", "复古黑胶盘微刻音轨与高光反光，播放时平滑优雅旋转"),
    SQUARE("square", "四方大图封面", "高清大尺寸四方圆角封面大图，搭配流光环境氛围光晕");

    companion object {
        fun fromId(id: String): PlayerCoverStyle = entries.firstOrNull { it.id == id } ?: VINYL
    }
}

/**
 * 左右滑屏手势动作定义
 */
enum class SwipeGestureAction(val id: String, val title: String, val description: String) {
    OPEN_STUDIO("open_studio", "打开场景抽屉", "从左侧平滑滑出场景与电台定制抽屉"),
    OPEN_EXPLORE("open_explore", "打开「发现」页", "快速进入全国电台与分类检索发现页"),
    OPEN_MY("open_my", "打开「我的」页", "快速进入我的收藏、历史与录音管理页"),
    OPEN_PLAYLIST("open_playlist", "打开播放列表", "从右侧呼出当前场景下的电台播放列表"),
    PREV_SCENARIO("prev_scenario", "切换上一场景", "切入上一个电台定制场景"),
    NEXT_SCENARIO("next_scenario", "切换下一场景", "切入下一个电台定制场景"),
    NONE("none", "无动作", "关闭滑屏交互，避免误触");

    companion object {
        fun fromId(id: String, defaultAction: SwipeGestureAction): SwipeGestureAction =
            entries.firstOrNull { it.id == id } ?: defaultAction
    }
}

/**
 * 播放页下方快捷功能按钮定义与排序
 */
enum class QuickActionButton(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val defaultEnabled: Boolean = true
) {
    FAVORITE("favorite", "收藏", Icons.Outlined.Star, true),
    RECORD("record", "录音", Icons.Outlined.Mic, true),
    RECOGNITION("recognition", "识曲", Icons.Outlined.GraphicEq, true),
    SLEEP("sleep", "睡眠", Icons.Outlined.NightsStay, true),
    CAR("car", "车载", Icons.Outlined.DirectionsCar, true),
    SOUND_EFFECT("sound_effect", "音效", Icons.Outlined.Equalizer, true),
    PLAYLIST("playlist", "列表", Icons.Outlined.QueueMusic, true);

    companion object {
        fun fromId(id: String): QuickActionButton? = entries.firstOrNull { it.id == id }

        val defaultList: List<QuickActionButton> = listOf(
            FAVORITE, RECORD, RECOGNITION, SLEEP, CAR, SOUND_EFFECT, PLAYLIST
        )
    }
}

/**
 * 实时字幕字号选项
 */
enum class SubtitleFontSize(val id: String, val title: String, val activeSp: Float, val historySp: Float) {
    COMPACT("compact", "小号字体 (19sp / 紧凑排版)", 19f, 14f),
    STANDARD("standard", "标准字号 (24sp)", 24f, 17f),
    LARGE("large", "大号字体 (28sp)", 28f, 20f),
    EXTRA_LARGE("extra_large", "特大字号 (34sp / 车载适老)", 34f, 24f);

    fun next(): SubtitleFontSize = when (this) {
        COMPACT -> STANDARD
        STANDARD -> LARGE
        LARGE -> EXTRA_LARGE
        EXTRA_LARGE -> COMPACT
    }

    companion object {
        fun fromId(id: String): SubtitleFontSize = entries.firstOrNull { it.id == id } ?: STANDARD
    }
}
