package com.biito.player.media.service

import android.content.Intent
import androidx.media3.common.Player
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class BiitoMediaService : MediaSessionService() {

    @Inject
    lateinit var player: Player

    @Inject
    lateinit var mediaSession: MediaSession

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession {
        return mediaSession
    }

    override fun onDestroy() {
        mediaSession.run {
            release()
            if (player.playbackState != Player.STATE_IDLE) {
                player.release()
            }
        }
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        if ((!player.playWhenReady) || (player.mediaItemCount == 0)) {
            stopSelf()
        }
    }
}