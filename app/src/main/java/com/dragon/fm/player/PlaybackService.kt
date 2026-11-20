package com.dragon.fm.player

import android.content.Intent
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.dragon.fm.DragonFmApp

/**
 * 前台服务 + 媒体通知全部交给 Media3 的 MediaSessionService：
 * 通知渠道、API<26 的兼容、锁屏/耳机按键它自己处理，5.1 上也能用。
 */
class PlaybackService : MediaSessionService() {

    private var session: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        session = MediaSession.Builder(this, DragonFmApp.player.exoPlayer).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (!DragonFmApp.player.exoPlayer.playWhenReady) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        session?.run {
            player.release()
            release()
        }
        session = null
        super.onDestroy()
    }
}
