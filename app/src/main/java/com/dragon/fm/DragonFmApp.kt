package com.dragon.fm

import android.app.Application
import android.content.Intent
import androidx.core.content.ContextCompat
import com.dragon.fm.data.api.RadioBrowserApi
import com.dragon.fm.data.local.FavoritesRepository
import com.dragon.fm.player.DragonMediaSessionService
import com.dragon.fm.player.DragonRadioPlayer
import com.dragon.fm.player.RecorderManager
import com.dragon.fm.player.SleepTimerManager

class DragonFmApp : Application() {

    lateinit var radioPlayer: DragonRadioPlayer
        private set

    lateinit var favoritesRepository: FavoritesRepository
        private set

    lateinit var recorderManager: RecorderManager
        private set

    lateinit var audioSpectrumManager: com.dragon.fm.player.AudioSpectrumManager
        private set

    lateinit var audioEffectManager: com.dragon.fm.player.AudioEffectManager
        private set

    lateinit var sleepTimerManager: SleepTimerManager
        private set

    lateinit var radioBrowserApi: RadioBrowserApi
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        audioEffectManager = com.dragon.fm.player.AudioEffectManager(this)
        radioPlayer = DragonRadioPlayer(this)
        val initialAudioSessionId = radioPlayer.exoPlayer.audioSessionId
        if (initialAudioSessionId > 0) {
            audioEffectManager.attachToAudioSession(initialAudioSessionId)
        }
        favoritesRepository = FavoritesRepository(this)
        recorderManager = RecorderManager(this)
        audioSpectrumManager = com.dragon.fm.player.AudioSpectrumManager()
        radioPlayer.liveAudioCaptureProcessor.addPcmListener { pcm, sr, ch ->
            audioSpectrumManager.processPcm(pcm, sr, ch)
        }
        radioBrowserApi = RadioBrowserApi()

        sleepTimerManager = SleepTimerManager {
            radioPlayer.fadeOutAndPause()
        }

        startPlaybackService()
    }

    private fun startPlaybackService() {
        try {
            val intent = Intent(this, DragonMediaSessionService::class.java)
            startService(intent)
        } catch (_: Exception) {
            // Handled gracefully
        }
    }

    companion object {
        lateinit var instance: DragonFmApp
            private set
    }
}
