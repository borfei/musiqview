package io.github.borfei.musiqview.services

import android.annotation.SuppressLint
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.TrackSelectionParameters.AudioOffloadPreferences
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.google.common.util.concurrent.ListenableFuture
import io.github.borfei.musiqview.BuildConfig

@SuppressLint("UnsafeOptInUsageError")
class PlaybackService : MediaSessionService(), MediaSession.Callback {
    companion object {
        const val TAG = "PlaybackService"
    }

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()

        val audioFocus = true
        val constantBitrateSeeking = false
        val wakeLock = false

        val loadErrorHandlingPolicy = object: DefaultLoadErrorHandlingPolicy() {
            override fun getRetryDelayMsFor(loadErrorInfo: LoadErrorHandlingPolicy.LoadErrorInfo): Long {
                Log.e(TAG, "Load Error", loadErrorInfo.exception)
                return super.getRetryDelayMsFor(loadErrorInfo)
            }
        }

        val extractorsFactory = DefaultExtractorsFactory()
            .setConstantBitrateSeekingEnabled(constantBitrateSeeking)
        val mediaSourceFactory = DefaultMediaSourceFactory(this, extractorsFactory)
            .setLoadErrorHandlingPolicy(loadErrorHandlingPolicy)

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(AudioAttributes.DEFAULT, audioFocus)
            .setWakeMode(if (wakeLock) C.WAKE_MODE_LOCAL else C.WAKE_MODE_NONE)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
        val audioOffloadPreferences = AudioOffloadPreferences.Builder()
            .setAudioOffloadMode(AudioOffloadPreferences.AUDIO_OFFLOAD_MODE_ENABLED)
            .build()

        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .setAudioOffloadPreferences(audioOffloadPreferences)
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