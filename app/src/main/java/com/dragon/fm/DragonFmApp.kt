package com.dragon.fm

import android.app.Application
import com.dragon.fm.player.RadioPlayer

class DragonFmApp : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: DragonFmApp
            private set

        val player: RadioPlayer by lazy { RadioPlayer(instance) }
    }
}
