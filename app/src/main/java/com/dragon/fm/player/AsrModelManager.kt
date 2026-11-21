package com.dragon.fm.player

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * 精简版桩：上游这里负责 sherpa-onnx 语音模型的下载与校验。
 * 本移植不带原生 ASR，只保留 UI 需要的类型与状态，所有操作都返回「不可用」。
 * ponytail: 要恢复实时字幕时，把这个文件换回上游实现即可。
 */
enum class AsrModelType(
    val id: String,
    val title: String,
    val subtitle: String,
    val tag: String,
    val estimatedSizeMB: Int,
    val isDefault: Boolean,
    val baseUrls: List<String>,
    val encoderFileName: String,
    val decoderFileName: String,
    val joinerFileName: String,
    val tokensFileName: String,
    val files: List<AsrModelManager.ModelFileInfo>,
) {
    COMPACT_ZH(
        id = "zipformer_zh_14m",
        title = "极速精简中文版",
        subtitle = "超轻量，普通话新闻、访谈",
        tag = "默认推荐 · 仅约 29 MB",
        estimatedSizeMB = 29,
        isDefault = true,
        baseUrls = emptyList(),
        encoderFileName = "encoder.onnx",
        decoderFileName = "decoder.onnx",
        joinerFileName = "joiner.onnx",
        tokensFileName = "tokens.txt",
        files = emptyList(),
    ),
    BILINGUAL_ZH_EN(
        id = "zipformer_multi_zh_en",
        title = "中英混合双语版",
        subtitle = "中英混杂深度识别",
        tag = "中英双语 · 约 190 MB",
        estimatedSizeMB = 190,
        isDefault = false,
        baseUrls = emptyList(),
        encoderFileName = "encoder.onnx",
        decoderFileName = "decoder.onnx",
        joinerFileName = "joiner.onnx",
        tokensFileName = "tokens.txt",
        files = emptyList(),
    );

    companion object {
        fun fromId(id: String): AsrModelType = entries.firstOrNull { it.id == id } ?: COMPACT_ZH
    }
}

sealed class AsrModelState {
    object NotDownloaded : AsrModelState()
    data class Downloading(
        val progress: Float,
        val currentFileName: String,
        val downloadedBytes: Long,
        val totalBytes: Long,
        val modelType: AsrModelType,
    ) : AsrModelState()

    data class Ready(val modelType: AsrModelType, val totalSizeBytes: Long) : AsrModelState()
    data class Error(val message: String) : AsrModelState()
}

data class AsrModelPaths(
    val tokensPath: String,
    val encoderPath: String,
    val decoderPath: String,
    val joinerPath: String,
)

class AsrModelManager private constructor(private val context: Context) {

    data class ModelFileInfo(val name: String, val expectedSize: Long)

    private val _currentModelType = MutableStateFlow(AsrModelType.COMPACT_ZH)
    val currentModelType: StateFlow<AsrModelType> = _currentModelType.asStateFlow()

    private val _modelState = MutableStateFlow<AsrModelState>(AsrModelState.NotDownloaded)
    val modelState: StateFlow<AsrModelState> = _modelState.asStateFlow()

    fun setSelectedModelType(type: AsrModelType) {
        _currentModelType.value = type
    }

    fun getModelDir(type: AsrModelType = _currentModelType.value): File =
        File(context.filesDir, "asr/" + type.id)

    fun checkStatus(): Boolean = false

    fun isModelReady(): Boolean = false

    fun getModelPaths(type: AsrModelType = _currentModelType.value): AsrModelPaths? = null

    fun cancelDownload() {
        _modelState.value = AsrModelState.NotDownloaded
    }

    fun deleteModel(deleteEngineAlso: Boolean = true): Boolean = false

    fun startDownload(
        targetType: AsrModelType = _currentModelType.value,
        onFinished: ((Boolean) -> Unit)? = null,
    ) {
        _modelState.value = AsrModelState.Error("这个精简版不带语音识别模型")
        onFinished?.invoke(false)
    }

    companion object {
        @Volatile
        private var instance: AsrModelManager? = null

        fun getInstance(context: Context): AsrModelManager =
            instance ?: synchronized(this) {
                instance ?: AsrModelManager(context.applicationContext).also { instance = it }
            }
    }
}
