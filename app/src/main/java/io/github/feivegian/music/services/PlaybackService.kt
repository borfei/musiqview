package io.github.feivegian.music.services

import android.content.SharedPreferences
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
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.google.common.util.concurrent.ListenableFuture
import io.github.feivegian.music.App.Companion.asApp
import io.github.feivegian.music.BuildConfig

@UnstableApi
class PlaybackService : MediaSessionService(), MediaSession.Callback {
    private lateinit var preferences: SharedPreferences
    private var audioFocus: Boolean = true
    private var constantBitrateSeeking: Boolean = false
    private var wakeLock: Boolean = false

    private var cache: SimpleCache? = null
    private var maxCacheSize: Long = 512 // in megabytes

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        preferences = application.asApp().getPreferences()
        audioFocus = preferences.getBoolean("playback_audio_focus", audioFocus)
        constantBitrateSeeking = preferences.getBoolean("playback_constant_bitrate_seeking", constantBitrateSeeking)
        maxCacheSize = preferences.getInt("playback_max_cache_size", maxCacheSize.toInt()).toLong()
        wakeLock = preferences.getBoolean("other_wake_lock", wakeLock)

        cache = SimpleCache(cacheDir,
            LeastRecentlyUsedCacheEvictor((maxCacheSize * 1024) * 1024), // convert to byte size
            application.asApp().getDatabaseProvider())
        val cacheDataSourceFactory = CacheDataSource.Factory()
            .setCache(cache!!)
            .setUpstreamDataSourceFactory(DefaultDataSource.Factory(this))

        val extractorsFactory = DefaultExtractorsFactory()
            .setConstantBitrateSeekingEnabled(constantBitrateSeeking)
        val mediaSourceFactory = DefaultMediaSourceFactory(this, extractorsFactory)
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

        mediaSession?.player?.playWhenReady = true
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