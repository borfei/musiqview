package io.github.borfei.musiqview.activities

import android.animation.LayoutTransition
import android.content.ComponentName
import android.content.Intent
import android.content.SharedPreferences
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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.descendants
import androidx.core.widget.doOnTextChanged
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Player.DISCONTINUITY_REASON_SEEK
import androidx.media3.common.Player.STATE_READY
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.bumptech.glide.Glide
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
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class PlaybackActivity : AppCompatActivity(), Player.Listener {
    companion object {
        const val TAG = "PlaybackActivity"

        /**
         * Decode the ByteArray-based data into BitmapFactory
         * which can then by used to create a proper Bitmap as the return value.
         *
         * If you want to create a empty bitmap, use 1 byte instead.
         *
         * @param[data] The data you want to convert.
         * @return[Bitmap]
         */
        fun byteArrayToBitmap(data: ByteArray): Bitmap {

            val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
            return bitmap ?: Bitmap.createBitmap(800, 600, Bitmap.Config.ARGB_8888)
        }

        /**
         * Converts duration into timestamp (mm:ss)
         *
         * @param[duration] The duration you want to convert.
         * @return[String]
         */
        fun durationToTimestamp(duration: Duration): String {
            return duration.toComponents { minutes, seconds, _ ->
                "%02d:%02d".format(minutes, seconds)
            }
        }
    }

    private lateinit var binding: ActivityPlaybackBinding

    private var isMetadataDisplayed: Boolean = true
    private var isLayoutAnimated: Boolean = true
    private var isImmersive: Boolean = false
    private var isWakeLock: Boolean = false

    private val app: App by lazy {
        App.fromInstance(application)
    }
    private val preferences: SharedPreferences by lazy {
        app.preferences
    }

    private val durationUpdateHandler: Handler by lazy {
        Handler(Looper.myLooper() ?: Looper.getMainLooper())
    }
    private val durationUpdateRunnable: Runnable by lazy {
        Runnable {
            mediaController?.currentPosition?.let {
                updateSeek(it)
            }
            durationUpdateRunnable.let {
                durationUpdateHandler.postDelayed(it, durationUpdateInterval.toLong())
            }
        }
    }
    private var durationUpdateInterval: Int = 1000

    private var mediaController: MediaController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        // Enable edge-to-edge for easy integration with system bars
        enableEdgeToEdge()

        super.onCreate(savedInstanceState)
        // Inflate activity layout via ViewBinding and adjust padding for system bar insets
        binding = ActivityPlaybackBinding.inflate(layoutInflater)
        binding.root.adjustPaddingForSystemBarInsets(top=true, bottom=true)
        // Finally for inflater, set the activity's view to it's proper content
        setContentView(binding.root)

        // Load preferences and the activity's preference variables
        preferences.let {
            durationUpdateInterval = it.getInt(Constants.PREFERENCE_PLAYBACK_DURATION_INTERVAL, durationUpdateInterval)

            isMetadataDisplayed = it.getBoolean(Constants.PREFERENCE_INTERFACE_DISPLAY_METADATA, isMetadataDisplayed)
            isLayoutAnimated = it.getBoolean(Constants.PREFERENCE_OTHER_ANIMATE_LAYOUT_CHANGES, isLayoutAnimated)
            isWakeLock = it.getBoolean(Constants.PREFERENCE_OTHER_WAKE_LOCK, isWakeLock)
            isImmersive = it.getBoolean(Constants.PREFERENCE_OTHER_IMMERSIVE_MODE, isImmersive)
        }
        // Make sure the root layout's transition manager is initialized
        // if isLayoutAnimated is set to true
        //
        // The descendants of the root layout is also affected by isLayoutAnimated
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
        // Register certain view listeners
        binding.mediaTitle.doOnTextChanged { _, _, _, count ->
            binding.mediaTitle.visibility = if (count > 0) View.VISIBLE else View.GONE
        }
        binding.mediaArtists.doOnTextChanged { _, _, _, count ->
            binding.mediaArtists.visibility = if (count > 0) View.VISIBLE else View.GONE
        }
        binding.playbackState.addOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                mediaController?.play()
            } else {
                mediaController?.pause()
            }
        }
        binding.playbackOpenExternal.setOnClickListener {
            val currentMediaItem = mediaController?.currentMediaItem
            val openExternalIntent = Intent(Intent.ACTION_VIEW)
            openExternalIntent.setDataAndType(currentMediaItem?.localConfiguration?.uri, "audio/*")
            startActivity(Intent.createChooser(openExternalIntent, null))
        }
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

        // Toggle system immersive mode based on isImmersive's value
        WindowCompat.getInsetsController(window, window.decorView).setImmersiveMode(isImmersive)
        // Hotfix for marquee text animation (by default it will not animate itself unless selected)
        binding.mediaFilename.isSelected = true

        // Initialize media session token & media controller
        val sessionToken = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        val controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()

        controllerFuture.addListener({
            mediaController = controllerFuture.get()
            mediaController?.addListener(this)

            // If an intent URI has received, load it as a media item
            intent?.let {
                if (intent.action == Intent.ACTION_VIEW) {
                    intent.data?.let {
                        Log.d(TAG, "Received Intent URI: $it")
                        mediaController?.setMediaItem(MediaItem.fromUri(it))
                        mediaController?.prepare()
                    }
                }
            }

            // Make sure to begin the playback when ready and prepared
            mediaController?.playWhenReady = true
        },
            MoreExecutors.directExecutor()
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        // Cancel all pending callbacks of duration updater
        durationUpdateHandler.removeCallbacksAndMessages(null)
        // Disconnect from media session
        mediaController?.removeListener(this)
        mediaController?.release()
    }

    override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
        super.onMediaMetadataChanged(mediaMetadata)
        updateMetadata(mediaMetadata)
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        super.onPlaybackStateChanged(playbackState)

        // If playback state is ready, we'll:
        // - Update the seek duration text to media duration
        // - Display the audio file's proper filename
        if (playbackState == STATE_READY) {
            mediaController?.currentMediaItem?.let {
                binding.mediaFilename.text = it.localConfiguration?.uri?.getName(this)
            }

            mediaController?.duration?.let { updateDuration(it) }
        }
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        super.onIsPlayingChanged(isPlaying)
        updateState(isPlaying)
    }

    override fun onPlayerError(error: PlaybackException) {
        super.onPlayerError(error)

        // When error occurs, display it's message in a MaterialAlertDialog
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

        // If the reason is seeking, update the seek position
        if (reason == DISCONTINUITY_REASON_SEEK) {
            updateSeek(newPosition.positionMs)
        }
    }

    private fun updateMetadata(metadata: MediaMetadata) {
        // Display available media metadata
        if (!isMetadataDisplayed) {
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

        // Display available media artwork
        //
        // If isLayoutAnimated is true, we'll use Glide to have a cross-fade animation
        // otherwise, we have to use ImageView's built-in setImageBitmap method instead
        val mediaArtworkData = metadata.artworkData ?: byteArrayOf(1)
        val mediaArtworkBitmap = byteArrayToBitmap(mediaArtworkData)

        if (isLayoutAnimated) {
            Glide.with(this)
                .load(mediaArtworkBitmap)
                .into(binding.mediaArtwork)
        } else {
            binding.mediaArtwork.setImageBitmap(mediaArtworkBitmap)
        }
    }

    private fun updateDuration(duration: Long) {
        // Update playback seek duration text from timestamp-based media position
        binding.playbackSeekTextDuration.text = durationToTimestamp(duration.toDuration(DurationUnit.MILLISECONDS))
    }

    private fun updateSeek(position: Long) {
        // Update playback seek slider from media position
        binding.playbackSeekSlider.value = (position + 0.0f) / (mediaController?.duration ?: 0)
        // Update playback seek position text from timestamp-based media position
        binding.playbackSeekTextPosition.text = durationToTimestamp(position.toDuration(DurationUnit.MILLISECONDS))
    }

    private fun updateState(isPlaying: Boolean) {
        // Set playback state checked to isPlaying
        binding.playbackState.isChecked = isPlaying

        // Start the duration updater if isPlaying is true
        // otherwise if false, we have to stop it's pending callbacks & messages
        //
        // Same goes for the wake lock acquisition, if true, keep the screen on
        // otherwise, remove the appropriate flag from the application window flags
        durationUpdateHandler.let {
            if (isPlaying) {
                durationUpdateHandler.post(durationUpdateRunnable)
            } else {
                it.removeCallbacksAndMessages(null)
            }
        }
        if (isWakeLock) {
            if (isPlaying) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }
}