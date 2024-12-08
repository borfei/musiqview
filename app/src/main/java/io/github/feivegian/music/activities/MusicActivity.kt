package io.github.feivegian.music.activities

import android.animation.LayoutTransition
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.slider.Slider
import com.google.common.util.concurrent.MoreExecutors
import io.github.feivegian.music.App.Companion.asApp
import io.github.feivegian.music.R
import io.github.feivegian.music.databinding.ActivityMusicBinding
import io.github.feivegian.music.extensions.adjustPaddingForSystemBarInsets
import io.github.feivegian.music.extensions.setImmersiveMode
import io.github.feivegian.music.services.PlaybackService
import java.util.Locale

@SuppressLint("UnsafeOptInUsageError")
class MusicActivity : AppCompatActivity(), Player.Listener {
    enum class ImmersiveMode {
        DISABLED, ENABLED, LANDSCAPE_ONLY
    }

    private lateinit var binding: ActivityMusicBinding
    private lateinit var preferences: SharedPreferences

    private val loopHandler: Handler? = Looper.myLooper()?.let { Handler(it) }
    private var loopRunnable: Runnable? = null
    private var loopInterval: Int = 0
    private var mediaController: MediaController? = null
    private var mediaItem: MediaItem = MediaItem.EMPTY

    private var headerFormat: String = "%title%"
    private var subheaderFormat: String = "%album_artist%"

    private var animateLayoutChanges: Boolean = true
    private var immersiveMode: ImmersiveMode = ImmersiveMode.LANDSCAPE_ONLY
    private var wakeLock: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize preferences
        preferences = application.asApp().getPreferences()
        loopInterval = preferences.getInt("playback_duration_interval", loopInterval)
        headerFormat = preferences.getString("interface_header_format", headerFormat).toString()
        subheaderFormat = preferences.getString("interface_subheader_format", subheaderFormat).toString()
        animateLayoutChanges = preferences.getBoolean("other_animate_layout_changes", animateLayoutChanges)
        immersiveMode = when (preferences.getString("other_immersive_mode", "landscape")) {
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

        // Connect activity to media session
        val sessionToken = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        val controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()

        // Initialize loop runnable
        loopRunnable = Runnable {
            updateSeek() // Update seek position every loop
            loopRunnable?.let { loopHandler?.postDelayed(it, loopInterval.toLong()) }
        }

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
            } else {
                mediaController?.pause()
            }
        }
        binding.playbackSeek.setLabelFormatter { value ->
            if (mediaController?.currentMediaItem == null) {
                return@setLabelFormatter convertMsToDuration(0)
            }

            val duration = mediaController?.duration ?: 0
            convertMsToDuration(((value + 0.0) * duration).toLong())
        }
        binding.playbackSeek.addOnSliderTouchListener(object: Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {
                binding.playbackState.visibility = View.INVISIBLE
                binding.playbackSeekPosition.visibility = View.INVISIBLE
                mediaController?.pause()
            }

            override fun onStopTrackingTouch(slider: Slider) {
                binding.playbackState.visibility = View.VISIBLE
                binding.playbackSeekPosition.visibility = View.VISIBLE
                val duration = mediaController?.duration ?: 0
                mediaController?.seekTo(((slider.value + 0.0) * duration).toLong())
                mediaController?.play()
            }
        })
        controllerFuture.addListener({
            mediaController = controllerFuture.get()
            mediaController?.addListener(this)
            update()

            // Accept media URI from intent if mediaItem is set to an empty MediaItem
            if (mediaItem == MediaItem.EMPTY) {
                intent?.let {
                    // Determine it's action before getting the intent data
                    //
                    // With Intent.ACTION_VIEW, it's clear that the intent came from
                    // the one defined in AndroidManifest.xml
                    when (intent.action) {
                        Intent.ACTION_VIEW -> {
                            intent.data?.let {
                                mediaItem = MediaItem.fromUri(it)
                            }

                            intent.data = null
                        }
                    }

                    // prepare for playback
                    mediaController?.prepare()
                }
            }
        },
            MoreExecutors.directExecutor()
        )
    }

    override fun onStart() {
        super.onStart()
        loopRunnable?.let { loopHandler?.post(it) }
    }

    override fun onStop() {
        super.onStop()
        loopRunnable?.let { loopHandler?.removeCallbacks(it) }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaController?.removeListener(this)
        mediaController?.release()
    }

    override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
        super.onMediaMetadataChanged(mediaMetadata)
        updateInfo(mediaMetadata)
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        super.onIsPlayingChanged(isPlaying)
        updateState(isPlaying)
    }

    override fun onPlayerError(error: PlaybackException) {
        super.onPlayerError(error)

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_playback_error_title)
            .setMessage(error.message)
            .setNegativeButton(R.string.dialog_playback_error_negative) { _, _ ->
                finish()
            }
            .show()
    }

    private fun updateInfo(metadata: MediaMetadata = mediaController?.mediaMetadata ?: MediaMetadata.EMPTY) {
        // parse header/sub-header formatters
        var parsedHeader = headerFormat // we use the format to use String.replace later
        var parsedSubheader = subheaderFormat // same goes to this variable too
        val formats = hashMapOf(
            "%album_artist%" to metadata.albumArtist,
            "%album_title%" to metadata.albumTitle,
            "%artist%" to metadata.artist,
            "%composer%" to metadata.composer,
            "%description%" to metadata.description,
            "%display_title%" to metadata.displayTitle,
            "%genre%" to metadata.genre,
            "%subtitle%" to metadata.subtitle,
            "%trackNumber%" to metadata.trackNumber.toString(), // cast from int to string
            "%writer%" to metadata.writer,
            "%title%" to metadata.title,
        )
        for ((format, value) in formats) {
            if (value.isNullOrBlank()) {
                // when value is null or blank, replace it with a placeholder instead
                parsedHeader = parsedHeader.replace(format, "<unknown>")
                parsedSubheader = parsedSubheader.replace(format, "<unknown>")
                continue
            }

            parsedHeader = parsedHeader.replace(format, value.toString())
            parsedSubheader = parsedSubheader.replace(format, value.toString())
        }

        binding.infoHeader.text = parsedHeader
        binding.infoSubheader.text = parsedSubheader

        // Information texts may be hidden when their text length is less than zero
        binding.infoHeader.visibility = when (parsedHeader.isNotEmpty()) {
            true -> {
                View.VISIBLE
            }
            false -> {
                View.GONE
            }
        }
        binding.infoSubheader.visibility = when (parsedSubheader.isNotEmpty()) {
            true -> {
                View.VISIBLE
            }
            false -> {
                View.GONE
            }
        }

        // Load cover art from metadata
        val artworkData = metadata.artworkData ?: byteArrayOf(1)
        var artworkBitmap = BitmapFactory.decodeByteArray(artworkData, 0, artworkData.size)

        if (artworkBitmap == null) {
            artworkBitmap = Bitmap.createBitmap(800, 600, Bitmap.Config.ARGB_8888)
        }
        Glide.with(this)
            .load(artworkBitmap)
            .transition(withCrossFade())
            .into(binding.coverArt)
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
        binding.playbackSeekPosition.text = convertMsToDuration(position)
    }

    private fun updateState(isPlaying: Boolean = mediaController?.isPlaying ?: false) {
        binding.playbackState.isChecked = isPlaying

        if (wakeLock) {
            when (isPlaying) {
                true -> { window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
                false -> { window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
            }
        }
    }

    private fun update() {
        updateInfo()
        updateSeek()
        updateState()
    }

    private fun convertMsToDuration(milliseconds: Long): String {
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
}