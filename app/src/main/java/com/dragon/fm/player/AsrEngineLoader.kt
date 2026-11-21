package com.dragon.fm.player

import android.content.Context
import android.os.Build
import java.io.File

/** 精简版桩：上游在这里 dlopen sherpa-onnx 的原生库，本移植没有原生 ASR。 */
object AsrEngineLoader {

    fun getEngineDir(context: Context): File = File(context.filesDir, "asr-engine")

    fun isEngineReady(context: Context): Boolean = false

    fun ensureLoaded(context: Context): Boolean = false

    fun getPrimaryAbi(): String = Build.SUPPORTED_ABIS.firstOrNull() ?: "armeabi-v7a"
}
