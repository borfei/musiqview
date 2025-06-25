package io.github.borfei.musiqview.services

import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.TrackSelectionParameters.AudioOffloadPreferences
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.google.common.util.concurrent.ListenableFuture
import io.github.borfei.musiqview.App
import io.github.borfei.musiqview.BuildConfig
import io.github.borfei.musiqview.Constants
import java.io.File

@UnstableApi
class PlaybackService : MediaSessionService(), MediaSession.Callback {
    private var cache: SimpleCache? = null
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val app = App.fromInstance(application)
        val preferences = app.preferences

        val audioFocus =
            preferences.getBoolean(Constants.PREFERENCE_PLAYBACK_AUDIO_FOCUS, true)
        val constantBitrateSeeking =
            preferences.getBoolean(Constants.PREFERENCE_PLAYBACK_CONSTANT_BITRATE_SEEKING, false)
        val maxCacheSize =
            preferences.getInt(Constants.PREFERENCE_PLAYBACK_MAX_CACHE_SIZE, 32).toLong()
        val wakeLock =
            preferences.getBoolean(Constants.PREFERENCE_OTHER_WAKE_LOCK, false)

        cache = SimpleCache(
            File(cacheDir, "media"),
            LeastRecentlyUsedCacheEvictor((maxCacheSize * 1024) * 1024), // convert to byte size
            app.databaseProvider)
        val cacheDataSourceFactory = CacheDataSource.Factory()
            .setCache(cache!!)
            .setUpstreamDataSourceFactory(DefaultDataSource.Factory(this))
        val loadErrorHandlingPolicy = object: DefaultLoadErrorHandlingPolicy() {
            override fun getRetryDelayMsFor(loadErrorInfo: LoadErrorHandlingPolicy.LoadErrorInfo): Long {
                Log.e(Constants.TAG_SERVICE_PLAYBACK, "Load Error", loadErrorInfo.exception)
                return super.getRetryDelayMsFor(loadErrorInfo)
            }
        }
        val extractorsFactory = DefaultExtractorsFactory()
            .setConstantBitrateSeekingEnabled(constantBitrateSeeking)
        val mediaSourceFactory = DefaultMediaSourceFactory(this, extractorsFactory)
            .setLoadErrorHandlingPolicy(loadErrorHandlingPolicy)
            .setDataSourceFactory(cacheDataSourceFactory)
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
            cache?.release()
            release()

            cache = null
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