package io.github.borfei.musiqview.activities

import android.animation.LayoutTransition
import android.content.ComponentName
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.enableEdgeToEdge
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.descendants
import androidx.core.widget.doOnTextChanged
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Player.DISCONTINUITY_REASON_SEEK
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.slider.Slider
import com.google.common.util.concurrent.MoreExecutors
import io.github.borfei.musiqview.App
import io.github.borfei.musiqview.Constants
import io.github.borfei.musiqview.R
import io.github.borfei.musiqview.databinding.ActivityPlaybackBinding
import io.github.borfei.musiqview.extensions.adjustPaddingForSystemBarInsets
import io.github.borfei.musiqview.extensions.getName
import io.github.borfei.musiqview.extensions.setImmersiveMode
import io.github.borfei.musiqview.services.PlaybackService
import java.util.Locale

class PlaybackActivity : AppCompatActivity(), Player.Listener {
    private lateinit var binding: ActivityPlaybackBinding

    private var isMetadataDisplayed: Boolean = true
    private var isLayoutAnimated: Boolean = true
    private var isWakeLock: Boolean = false

    private var durationUpdateHandler: Handler? = null
    private var durationUpdateRunnable: Runnable? = null
    private var durationUpdateInterval: Int = 1000

    private var mediaController: MediaController? = null
    private var mediaItem: MediaItem = MediaItem.EMPTY

    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // Get application instance & initialize preferences
        val application = App.fromInstance(application)
        val preferences = application.preferences

        preferences.getBoolean(Constants.PREFERENCE_INTERFACE_DISPLAY_METADATA, isMetadataDisplayed).let {
            isMetadataDisplayed = it
        }
        preferences.getInt(Constants.PREFERENCE_PLAYBACK_DURATION_INTERVAL, durationUpdateInterval).let {
            durationUpdateInterval = it
        }
        preferences.getBoolean(Constants.PREFERENCE_OTHER_ANIMATE_LAYOUT_CHANGES, isLayoutAnimated).let {
            isLayoutAnimated = it
        }
        preferences.getBoolean(Constants.PREFERENCE_OTHER_WAKE_LOCK, isWakeLock).let {
            isWakeLock = it
        }

        // Inflate activity view using ViewBinding
        binding = ActivityPlaybackBinding.inflate(layoutInflater)
        binding.root.adjustPaddingForSystemBarInsets(top=true, bottom=true)
        setContentView(binding.root)

        // Connect activity to media session
        val sessionToken = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        val controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()

        // Initialize duration updater
        durationUpdateHandler = Handler(Looper.myLooper() ?: Looper.getMainLooper())
        durationUpdateRunnable = Runnable {
            updateSeek()

            durationUpdateRunnable?.let {
                durationUpdateHandler?.postDelayed(it, durationUpdateInterval.toLong())
            }
        }

        // Manually select the media filename text view to begin marquee animation
        binding.mediaFilename.isSelected = true

        // Make all ViewGroups animate when their child orders are changed
        // If PREFERENCE_OTHER_ANIMATE_LAYOUT_CHANGES is false, then they won't be animated
        binding.root.layoutTransition = if (isLayoutAnimated) {
            LayoutTransition()
        } else {
            null
        }
        binding.root.descendants.forEach { view ->
            if (view is ViewGroup) {
                view.layoutTransition = if (isLayoutAnimated) {
                    LayoutTransition()
                } else {
                    null
                }
            }
        }
        // Set immersive mode to enabled if the following conditions are met:
        //  If "enabled" -> hide system bars
        //  If "landscape" -> only hide system bars if orientation is landscape
        //  Otherwise, don't hide the system bars
        WindowCompat.getInsetsController(window, window.decorView).setImmersiveMode(when (preferences.getString(
            Constants.PREFERENCE_OTHER_IMMERSIVE_MODE, "landscape")) {
            "enabled" -> {
                true
            }
            "landscape" -> {
                resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
            }
            else -> {
                false
            }
        })
        // When text changes for media filename/title/artists, toggle visibility based on text count
        binding.mediaFilename.doOnTextChanged { _, _, _, count ->
            binding.mediaFilename.visibility = if (count > 0) View.VISIBLE else View.GONE
        }
        binding.mediaTitle.doOnTextChanged { _, _, _, count ->
            binding.mediaTitle.visibility = if (count > 0) View.VISIBLE else View.GONE
        }
        binding.mediaArtists.doOnTextChanged { _, _, _, count ->
            binding.mediaArtists.visibility = if (count > 0) View.VISIBLE else View.GONE
        }
        // Register playback control listeners
        binding.playbackState.addOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                mediaController?.play()
            } else {
                mediaController?.pause()
            }
        }
        binding.playbackOpenExternal.setOnClickListener {
            val openExternalIntent = Intent(Intent.ACTION_VIEW)
            openExternalIntent.setDataAndType(mediaItem.localConfiguration?.uri, "audio/*")
            startActivity(Intent.createChooser(openExternalIntent, null))
        }
        /*
        binding.playbackSeekSlider?.setLabelFormatter { value ->
            val duration = mediaController?.duration ?: 0
            val valueLong = ((value + 0.0) * duration).toLong()
            parseSeekPosition(valueLong)
        }
        */
        binding.playbackSeekSlider.addOnSliderTouchListener(object: Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {
                mediaController?.pause()
            }

            override fun onStopTrackingTouch(slider: Slider) {
                mediaController?.play()
            }
        })
        binding.playbackSeekSlider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                val currentDuration = mediaController?.duration
                mediaController?.seekTo(((value + 0.0) * (currentDuration ?: 0)).toLong())
            }
        }
        controllerFuture.addListener({
            mediaController = controllerFuture.get()
            mediaController?.addListener(this)

            // Store media URI from intent as a local variable to keep track of information
            intent?.let {
                // Determine it's action before getting the intent data
                //
                // With Intent.ACTION_VIEW, it's clear that the intent came from
                // the one defined in AndroidManifest.xml
                if (intent.action == Intent.ACTION_VIEW) {
                    intent.data?.let {
                        Log.d(Constants.TAG_ACTIVITY_PLAYBACK, "Intent URI: $it")
                        mediaItem = MediaItem.fromUri(it)
                        updateInfo(mediaItem.mediaMetadata)
                    }
                }
            }
            // If there's no media item set, load the stored media item & prepare playback
            // Otherwise, update the entire UI with the currently loaded media item
            if (mediaController?.currentMediaItem == null) {
                mediaController?.setMediaItem(mediaItem)
                mediaController?.prepare()
                mediaController?.playWhenReady = true
                Log.d(Constants.TAG_ACTIVITY_PLAYBACK, "Preparing media URI")
            } else {
                update()
                Log.d(Constants.TAG_ACTIVITY_PLAYBACK, "Update called; playback already loaded")
            }
        },
            MoreExecutors.directExecutor()
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        // Release duration updater
        durationUpdateRunnable = null
        durationUpdateHandler = null
        // Disconnect from media session
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
            .setCancelable(false)
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
        // Set media filename/title/artists to the available metadata
        //
        // If metadata is unavailable, or is opted to hide by user
        // we'll be displaying the media filename instead, leaving the other two to be hidden
        if (!isMetadataDisplayed) {
            binding.mediaFilename.text = mediaItem.localConfiguration?.uri?.getName(this)
            binding.mediaTitle.text = String()
            binding.mediaArtists.text = String()
        } else {
            metadata.title?.let {
                binding.mediaTitle.text = it.toString()
            }
            metadata.artist?.let {
                val artists = it.split("; ", ", ")
                binding.mediaArtists.text = artists.joinToString()
            }
        }

        // Load artwork from metadata, if available
        val artworkData = parseArtwork(metadata.artworkData ?: byteArrayOf(1))

        if (isLayoutAnimated) {
            Glide.with(this)
                .load(artworkData)
                .transition(withCrossFade())
                .into(binding.mediaArtwork)
        } else {
            binding.mediaArtwork.setImageBitmap(artworkData)
        }
    }

    private fun updateSeek(position: Long = mediaController?.currentPosition ?: 0,
                           duration: Long = mediaController?.duration ?: 0) {
        // Create float-based position value
        var positionFloat = (position + 0.0f) / (mediaController?.duration ?: 0)

        // float-based position value cannot be greater than 1.0f or lesser than 0.0f
        if (positionFloat > 1.0f) {
            positionFloat = 1.0f
        } else if (positionFloat < 0.0f) {
            positionFloat = 0.0f
        }

        // Update the playback slider value using it's float-based position
        // Also update it's text start/end value by using the Long values
        binding.playbackSeekSlider.value = positionFloat
        binding.playbackSeekTextPosition.text = parseDuration(position)
        binding.playbackSeekTextDuration.text = parseDuration(duration)
    }

    private fun updateState(isPlaying: Boolean = mediaController?.isPlaying ?: false) {
        binding.playbackState.isChecked = isPlaying

        durationUpdateHandler?.let {
            if (isPlaying) {
                durationUpdateRunnable?.let { runnable -> it.post(runnable) }
                Log.d(Constants.TAG_ACTIVITY_PLAYBACK, "Post duration update until paused/stopped")
            } else {
                it.removeCallbacksAndMessages(null)
                Log.d(Constants.TAG_ACTIVITY_PLAYBACK, "Stopped pending callbacks & messages of duration update")
            }
        }
        if (isWakeLock) {
            if (isPlaying) {
                Log.d(Constants.TAG_ACTIVITY_PLAYBACK, "Append FLAG_KEEP_SCREEN_ON to window flags")
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                Log.d(Constants.TAG_ACTIVITY_PLAYBACK, "Remove FLAG_KEEP_SCREEN_ON from window flags")
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    private fun update() {
        updateInfo()
        updateSeek()
        updateState()
    }

    private fun parseArtwork(data: ByteArray): Bitmap {
        val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
        return bitmap ?: Bitmap.createBitmap(800, 600, Bitmap.Config.ARGB_8888)
    }

    private fun parseDuration(value: Long): String {
        val locale = Locale.getDefault()
        val valueMicroseconds = value / 1000
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