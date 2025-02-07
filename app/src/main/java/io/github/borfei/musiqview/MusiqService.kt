package io.github.borfei.musiqview

import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.TrackSelectionParameters.AudioOffloadPreferences
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.google.common.util.concurrent.ListenableFuture

@OptIn(UnstableApi::class)
class MusiqService : MediaSessionService(), MediaSession.Callback {
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(AudioAttributes.DEFAULT, true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()
        val preferences = AudioOffloadPreferences.Builder()
            .setAudioOffloadMode(AudioOffloadPreferences.AUDIO_OFFLOAD_MODE_ENABLED)
            .build()

        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .setAudioOffloadPreferences(preferences)
            .build()
        mediaSession = MediaSession.Builder(this, player)
            .setCallback(this)
            .build()
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()

            mediaSession = null
        }

        super.onDestroy()
    }

    override fun onConnect(
        session: MediaSession,
        controller: MediaSession.ControllerInfo
    ): MediaSession.ConnectionResult {
        if (controller.packageName != BuildConfig.APPLICATION_ID) {
            return MediaSession.ConnectionResult.reject()
        }

        return super.onConnect(session, controller)
    }

    override fun onDisconnected(session: MediaSession, controller: MediaSession.ControllerInfo) {
        if (session.connectedControllers.isEmpty()) {
            stopSelf()
        }

        super.onDisconnected(session, controller)
    }

    override fun onUpdateNotification(session: MediaSession, startInForegroundRequired: Boolean) {}

    @UnstableApi
    override fun onPlaybackResumption(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo
    ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
        return super.onPlaybackResumption(mediaSession, controller)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession
}