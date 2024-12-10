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
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.descendants
import androidx.core.widget.doOnTextChanged
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Player.DISCONTINUITY_REASON_SEEK
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

    private var displayMetadata: Boolean = true

    private val loopHandler: Handler? = Looper.myLooper()?.let { Handler(it) }
    private var loopRunnable: Runnable? = null
    private var loopInterval: Int = 0
    private var mediaController: MediaController? = null
    private var mediaItem: MediaItem = MediaItem.EMPTY

    private var animateLayoutChanges: Boolean = true
    private var wakeLock: Boolean = false
    private var immersiveMode: ImmersiveMode = ImmersiveMode.LANDSCAPE_ONLY

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Initialize preferences
        preferences = application.asApp().getPreferences()
        displayMetadata = preferences.getBoolean("interface_display_metadata", displayMetadata)
        loopInterval = preferences.getInt("playback_duration_interval", loopInterval)
        animateLayoutChanges = preferences.getBoolean("other_animate_layout_changes", animateLayoutChanges)
        wakeLock = preferences.getBoolean("other_wake_lock", wakeLock)
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
        // Animate every layout change by depending on the preference check
        binding.root.layoutTransition = if (animateLayoutChanges) {
            LayoutTransition()
        } else {
            null
        }
        binding.root.descendants.forEach { view ->
            if (view is ViewGroup) {
                view.layoutTransition = if (animateLayoutChanges) {
                    LayoutTransition()
                } else {
                    null
                }
            }
        }
        // When text changes for title and sub-title, toggle visibility based on text count
        binding.title.doOnTextChanged { _, _, _, count ->
            binding.title.visibility = if (count > 0) View.VISIBLE else View.GONE
        }
        binding.subtitle.doOnTextChanged { _, _, _, count ->
            binding.subtitle.visibility = if (count > 0) View.VISIBLE else View.GONE
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
            val duration = mediaController?.duration ?: 0
            val valueLong = ((value + 0.0) * duration).toLong()
            parseSeekPosition(valueLong)
        }
        binding.playbackSeek.addOnSliderTouchListener(object: Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {
                binding.playbackSeekText.visibility = View.INVISIBLE
            }

            override fun onStopTrackingTouch(slider: Slider) {
                binding.playbackSeekText.visibility = View.VISIBLE

                // This is the only way to get the playback resume
                if (mediaController?.isPlaying == false) {
                    mediaController?.play()
                }
            }
        })
        binding.playbackSeek.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                mediaController?.seekTo(((value + 0.0) * (mediaController?.duration ?: 0)).toLong())
            }
        }
        controllerFuture.addListener({
            mediaController = controllerFuture.get()
            mediaController?.addListener(this)
            update()

            // Store media URI from intent as a local variable to keep track of information
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
            }
            // If there's no media item set, load the previously-stored media item & prepare playback
            if (mediaController?.currentMediaItem == null) {
                mediaController?.setMediaItem(mediaItem)
                mediaController?.prepare()
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
            .setIcon(R.drawable.dialog_error_48)
            .setTitle(R.string.dialog_playback_error_title)
            .setMessage(error.message)
            .setNegativeButton(R.string.dialog_playback_error_negative) { _, _ ->
                finish()
            }
            .show()
    }

    override fun onPositionDiscontinuity(
        oldPosition: Player.PositionInfo,
        newPosition: Player.PositionInfo,
        reason: Int
    ) {
        super.onPositionDiscontinuity(oldPosition, newPosition, reason)

        if (reason == DISCONTINUITY_REASON_SEEK) {
            updateSeek(newPosition.positionMs)
        }
    }

    private fun updateInfo(metadata: MediaMetadata = mediaController?.mediaMetadata ?: MediaMetadata.EMPTY) {
        // Set title/subtitle to the available metadata, if available
        // When metadata is unavailable (or it's preference is false), use file name instead
        val info = parseInfo(metadata)
        binding.title.text = info.first
        binding.subtitle.text = info.second
        // Load artwork from metadata, if available
        Glide.with(this)
            .load(parseArtwork(metadata.artworkData ?: byteArrayOf(1)))
            .transition(withCrossFade())
            .into(binding.artwork)
    }

    private fun updateSeek(value: Long = mediaController?.currentPosition ?: 0) {
        // convert position into float (pain)
        var seekValue = (value + 0.0f) / (mediaController?.duration ?: 0)
        // seekValue cannot be greater than 1.0f or lesser than 0.0f
        if (seekValue > 1.0f) {
            seekValue = 1.0f
        } else if (seekValue < 0.0f) {
            seekValue = 0.0f
        }

        // update slider value based on float
        // and also slider text but based on long
        binding.playbackSeek.value = seekValue
        binding.playbackSeekText.text = parseSeekPosition(value)
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

    private fun parseInfo(metadata: MediaMetadata): Pair<String, String> {
        var title = mediaItem.localConfiguration?.uri?.lastPathSegment ?: String()
        var subtitle = String() // empty fallback

        if (displayMetadata) {
            metadata.title?.let {
                title = it.toString()
            }
            metadata.artist?.let {
                val artists = it.split("; ", ", ")
                subtitle = artists.joinToString()
            }
        }

        return Pair(title, subtitle)
    }

    private fun parseArtwork(data: ByteArray): Bitmap {
        val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
        return bitmap ?: Bitmap.createBitmap(800, 600, Bitmap.Config.ARGB_8888)
    }

    private fun parseSeekPosition(position: Long): String {
        val locale = Locale.getDefault()
        val valueMicroseconds = position / 1000
        val valueMinutes = valueMicroseconds / 60
        val valueSeconds = valueMicroseconds % 60

        return if (valueMicroseconds >= 360) {
            val valueHours = valueMicroseconds / 360
            getString(R.string.playback_seek_format_long, valueHours, valueMinutes, String.format(locale, "%1$02d", valueSeconds))
        } else {
            getString(R.string.playback_seek_format_short, valueMinutes, String.format(locale, "%1$02d", valueSeconds))
        }
    }
}