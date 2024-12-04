package io.github.feivegian.music.activities

import android.animation.LayoutTransition
import android.content.ComponentName
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.preference.PreferenceManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.google.android.material.slider.Slider
import com.google.android.material.snackbar.Snackbar
import com.google.common.util.concurrent.MoreExecutors
import io.github.feivegian.music.R
import io.github.feivegian.music.databinding.ActivityMusicBinding
import io.github.feivegian.music.services.PlaybackService
import io.github.feivegian.music.utils.adjustPaddingForSystemBarInsets
import io.github.feivegian.music.utils.setImmersiveMode
import java.util.Locale

class MusicActivity : AppCompatActivity(), Player.Listener {
    enum class ImmersiveMode {
        DISABLED, ENABLED, LANDSCAPE_ONLY
    }

    private lateinit var binding: ActivityMusicBinding
    private lateinit var preferences: SharedPreferences

    private val loopHandler: Handler? = Looper.myLooper()?.let { Handler(it) }
    private var loopRunnable: Runnable? = null
    private var loopInterval: Int = 0
    private var loopHandling: Boolean = false
    private var mediaController: MediaController? = null

    private var titleFormat: String = "%title%"
    private var subtitleFormat: String = "%album_artist%"

    private var immersiveMode: ImmersiveMode = ImmersiveMode.LANDSCAPE_ONLY
    private var animateLayoutChanges: Boolean = true

    private var wakeLock: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Initialize preferences
        preferences = PreferenceManager.getDefaultSharedPreferences(this)
        loopInterval = preferences.getInt("playback_duration_interval", loopInterval)
        titleFormat = preferences.getString("info_title_format", titleFormat).toString()
        subtitleFormat = preferences.getString("info_subtitle_format", subtitleFormat).toString()
        animateLayoutChanges = preferences.getBoolean("layout_animate_changes", animateLayoutChanges)
        immersiveMode = when (preferences.getString("layout_immersive_mode", "landscape")) {
            "enabled" -> {
                ImmersiveMode.ENABLED
            }
            "disabled" -> {
                ImmersiveMode.DISABLED
            }
            "landscape" -> {
                ImmersiveMode.LANDSCAPE_ONLY
            }
            else -> {
                immersiveMode
            }
        }
        wakeLock = preferences.getBoolean("other_wake_lock", false)

        // Inflate activity view using ViewBinding
        binding = ActivityMusicBinding.inflate(layoutInflater)
        binding.root.adjustPaddingForSystemBarInsets(top=true, bottom=true)
        setContentView(binding.root)

        // Register onBackPressedDispatcher for custom activity exit processing
        onBackPressedDispatcher.addCallback(this, object: OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })
        // Toggle immersive mode by depending on the preference check
        // If set to LANDSCAPE_ONLY, immersive mode will be enabled if current orientation is landscape
        WindowCompat.getInsetsController(window, window.decorView).setImmersiveMode(when (immersiveMode) {
            ImmersiveMode.ENABLED -> {
                true
            }
            ImmersiveMode.LANDSCAPE_ONLY -> {
                resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
            }
            else -> {
                false
            }
        })
        // Animate layout changes by depending on the preference check
        if (animateLayoutChanges) {
            binding.playbackControls.layoutTransition = LayoutTransition()
        } else {
            binding.playbackControls.layoutTransition = null
        }
        // Register playback controls listeners
        binding.playbackState.addOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                mediaController?.play()
                startLoopHandler()
            } else {
                mediaController?.pause()
                stopLoopHandler()
            }
        }
        binding.playbackSeek.setLabelFormatter { value ->
            val duration = mediaController?.duration ?: 0
            parsePlaybackDurationToString(((value + 0.0) * duration).toLong())
        }
        binding.playbackSeek.addOnSliderTouchListener(object: Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {
                binding.playbackSeekPosition.visibility = View.GONE
                mediaController?.pause()
            }

            override fun onStopTrackingTouch(slider: Slider) {
                binding.playbackSeekPosition.visibility = View.VISIBLE
                val duration = mediaController?.duration ?: 0
                mediaController?.seekTo(((slider.value + 0.0) * duration).toLong())
                mediaController?.play()
            }
        })
        // Connect activity to media session
        val sessionToken = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        val controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()

        controllerFuture.addListener({
            mediaController = controllerFuture.get()
            mediaController?.addListener(this)
            update()
            handleIncomingIntents()
        },
            MoreExecutors.directExecutor()
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaController?.removeListener(this)
        mediaController?.release()

        // If wake lock is enabled & is acquired, release it
        if (wakeLock) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
        super.onMediaMetadataChanged(mediaMetadata)
        updateInfo(mediaMetadata)
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        super.onIsPlayingChanged(isPlaying)
        binding.playbackState.isChecked = isPlaying

        if (wakeLock) {
            when (isPlaying) {
                true -> { window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
                false -> { window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
            }
        }
    }

    override fun onPlayerError(error: PlaybackException) {
        super.onPlayerError(error)
        Snackbar.make(binding.root, R.string.playback_file_open_error, Snackbar.LENGTH_LONG).show()
    }

    override fun onPositionDiscontinuity(
        oldPosition: Player.PositionInfo,
        newPosition: Player.PositionInfo,
        reason: Int
    ) {
        super.onPositionDiscontinuity(oldPosition, newPosition, reason)
        updateSeek(newPosition.positionMs)
    }

    private fun parsePlaybackDurationToString(milliseconds: Long): String {
        val locale = Locale.getDefault()
        val microseconds = milliseconds / 1000
        val minutes = microseconds / 60
        val seconds = microseconds % 60

        if (microseconds >= 360) {
            val hours = microseconds / 360
            return getString(R.string.playback_seek_format_long, hours, minutes, String.format(locale, "%1$02d", seconds))
        }

        return getString(R.string.playback_seek_format_short, minutes, String.format(locale, "%1$02d", seconds))
    }

    private fun updateInfo(metadata: MediaMetadata = mediaController?.mediaMetadata ?: MediaMetadata.EMPTY) {
        // parse title/sub-title formatters
        // TODO: add more formats here
        var title = titleFormat
        var subtitle = subtitleFormat
        title = title.replace("%title%", metadata.title.toString(), true)
        title = title.replace("%artist%", metadata.artist.toString(), true)
        title = title.replace("%album_artist%", metadata.albumArtist.toString(), true)
        title = title.replace("%album%", metadata.albumTitle.toString(), true)
        subtitle = subtitle.replace("%title%", metadata.title.toString(), true)
        subtitle = subtitle.replace("%artist%", metadata.artist.toString(), true)
        subtitle = subtitle.replace("%album_artist%", metadata.albumArtist.toString(), true)
        subtitle = subtitle.replace("%album%", metadata.albumTitle.toString(), true)
        // load cover art from metadata
        val artworkData = metadata.artworkData ?: byteArrayOf(1)
        var artworkBitmap = BitmapFactory.decodeByteArray(artworkData, 0, artworkData.size)
        if (artworkBitmap == null) {
            artworkBitmap = Bitmap.createBitmap(800, 600, Bitmap.Config.ARGB_8888)
        }

        // Set the loaded cover art to it's respective view
        Glide.with(this)
            .load(artworkBitmap)
            .transition(withCrossFade())
            .into(binding.coverArt)

        binding.infoTitle.text = title
        binding.infoSubtitle.text = subtitle

        // Information texts may be hidden when their text length is less than zero
        binding.infoTitle.visibility = when (title.isNotEmpty()) {
            true -> {
                View.VISIBLE
            }
            false -> {
                View.GONE
            }
        }
        binding.infoSubtitle.visibility = when (subtitle.isNotEmpty()) {
            true -> {
                View.VISIBLE
            }
            false -> {
                View.GONE
            }
        }
    }

    private fun updateSeek(position: Long = mediaController?.currentPosition ?: 0) {
        val duration = mediaController?.duration ?: 0
        var positionFloat = (position + 0.0f) / duration // convert position into float (pain)

        if (positionFloat > 1.0f) {
            positionFloat = 1.0f
        } else if (positionFloat < 0.0f) {
            positionFloat = 0.0f
        }

        binding.playbackSeek.value = positionFloat
        binding.playbackSeekPosition.text = parsePlaybackDurationToString(position)
    }

    private fun update() {
        if (mediaController?.currentMediaItem == null) {
            return
        }

        updateInfo()
        updateSeek()

        binding.playbackState.isChecked = mediaController?.isPlaying == true
    }

    private fun handleIncomingIntents() {
        when (intent?.action) {
            Intent.ACTION_VIEW -> {
                intent?.data?.let {
                    val item = MediaItem.fromUri(it)
                    intent?.data = null
                    mediaController?.setMediaItem(item)
                    mediaController?.prepare()
                }
            }
        }
    }

    private fun startLoopHandler() {
        if (loopHandling) {
            return
        }
        loopRunnable = Runnable {
            updateSeek()
            loopRunnable?.let { loopHandler?.postDelayed(it, loopInterval.toLong()) }
        }

        loopRunnable?.let { loopHandler?.post(it) }
        loopHandling = true
    }

    private fun stopLoopHandler() {
        if (!loopHandling) {
            return
        }

        loopRunnable?.let { loopHandler?.removeCallbacks(it) }
        loopHandling = false
    }

    companion object {
        const val TAG = "MusicActivity"
    }
}