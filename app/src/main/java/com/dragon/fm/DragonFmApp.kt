package com.dragon.fm

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.dragon.fm.net.Http
import com.dragon.fm.player.RadioPlayer

class DragonFmApp : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    // 台标图片也走同一个补了 ISRG Root X1 的 OkHttp，否则 5.1 上 https 图全挂
    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .okHttpClient { Http.client(this) }
        .build()

    companion object {
        lateinit var instance: DragonFmApp
            private set

        val player: RadioPlayer by lazy { RadioPlayer(instance) }
    }
}
