package io.github.spir0th.music.services

import android.content.SharedPreferences
import androidx.media3.common.AudioAttributes
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.preference.PreferenceManager
import com.google.common.util.concurrent.ListenableFuture
import io.github.spir0th.music.BuildConfig

class PlaybackService : MediaSessionService(), MediaSession.Callback {
    private lateinit var preferences: SharedPreferences
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        preferences = PreferenceManager.getDefaultSharedPreferences(this)

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(AudioAttributes.DEFAULT, preferences.getBoolean("playback_audio_focus", true))
            .build()
        mediaSession = MediaSession.Builder(this, player)
            .setCallback(this)
            .build()

        mediaSession?.player?.playWhenReady = true
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

    override fun onUpdateNotification(session: MediaSession, startInForegroundRequired: Boolean) {
        // do nothing
    }

    @UnstableApi
    override fun onPlaybackResumption(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo
    ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
        return super.onPlaybackResumption(mediaSession, controller)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession
}