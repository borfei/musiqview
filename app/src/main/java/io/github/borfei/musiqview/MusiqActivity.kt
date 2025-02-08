package io.github.borfei.musiqview

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.widget.doOnTextChanged
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Player.DISCONTINUITY_REASON_SEEK
import androidx.media3.common.Player.REPEAT_MODE_ALL
import androidx.media3.common.Player.REPEAT_MODE_OFF
import androidx.media3.common.Player.REPEAT_MODE_ONE
import androidx.media3.common.Player.STATE_READY
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.slider.Slider
import com.google.common.util.concurrent.MoreExecutors
import io.github.borfei.musiqview.databinding.ActivityMusiqBinding
import io.github.borfei.musiqview.extensions.adjustPaddingForSystemBarInsets
import io.github.borfei.musiqview.extensions.getName
import io.github.borfei.musiqview.extensions.setImmersiveMode
import io.github.borfei.musiqview.extensions.toBitmap
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class MusiqActivity : AppCompatActivity(), Player.Listener {
    companion object {
        const val TAG = "PlaybackActivity"
    }

    private lateinit var binding: ActivityMusiqBinding

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
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        WindowCompat.getInsetsController(window, window.decorView).setImmersiveMode(false)
        binding = ActivityMusiqBinding.inflate(layoutInflater)
        binding.root.adjustPaddingForSystemBarInsets(top=true, bottom=true)
        setContentView(binding.root)

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
        binding.playbackRepeat.addOnCheckedChangeListener { _, isChecked ->
            mediaController?.repeatMode = if (isChecked) REPEAT_MODE_ALL else REPEAT_MODE_OFF
        }

        binding.playbackOptions.setOnClickListener {
            // TODO: Implement options menu here
            Toast.makeText(this, R.string.under_construction, Toast.LENGTH_LONG).show()
        }

        binding.playbackSeekSlider.apply {
            addOnSliderTouchListener(object: Slider.OnSliderTouchListener {
                override fun onStartTrackingTouch(slider: Slider) {
                    mediaController?.pause()
                }

                override fun onStopTrackingTouch(slider: Slider) {
                    mediaController?.play()
                }
            })

            addOnChangeListener { _, value, fromUser ->
                if (fromUser) {
                    val currentDuration = mediaController?.duration
                    mediaController?.seekTo(((value + 0.0) * (currentDuration ?: 0)).toLong())
                }
            }
        }

        val mediaSessionToken = SessionToken(this, ComponentName(this, MusiqService::class.java))
        val mediaControllerFuture = MediaController.Builder(this, mediaSessionToken).buildAsync()

        mediaControllerFuture.addListener({
            mediaController = mediaControllerFuture.get()
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
        }, MoreExecutors.directExecutor())
    }

    override fun onDestroy() {
        super.onDestroy()
        durationUpdateHandler.removeCallbacksAndMessages(null)
        mediaController?.removeListener(this)
        mediaController?.release()
    }

    override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
        super.onMediaMetadataChanged(mediaMetadata)
        updateMetadata(mediaMetadata)
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        super.onPlaybackStateChanged(playbackState)

        if (playbackState == STATE_READY) {
            mediaController?.currentMediaItem?.let {
                binding.mediaFilename.text = it.localConfiguration?.uri?.getName(this)
                binding.mediaFilename.isSelected = true
            }
            mediaController?.duration?.let {
                updateDuration(it)
            }
        }
    }

    override fun onIsLoadingChanged(isLoading: Boolean) {
        super.onIsLoadingChanged(isLoading)

        if (isLoading) {
            binding.playbackLoadIndicator.visibility = View.VISIBLE
        } else {
            binding.playbackLoadIndicator.visibility = View.GONE
        }
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        super.onIsPlayingChanged(isPlaying)
        updateState(isPlaying)
    }

    override fun onRepeatModeChanged(repeatMode: Int) {
        super.onRepeatModeChanged(repeatMode)
        val isRepeating = repeatMode == REPEAT_MODE_ALL || repeatMode == REPEAT_MODE_ONE
        binding.playbackRepeat.isChecked = isRepeating
    }

    override fun onPlayerError(error: PlaybackException) {
        super.onPlayerError(error)

        // When error occurs, display it's message in a MaterialAlertDialog
        MaterialAlertDialogBuilder(this)
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
        metadata.artworkData?.let {
            Glide.with(this)
                .load(it.toBitmap())
                .transition(withCrossFade())
                .into(binding.mediaArtwork)
        }
        metadata.title?.let {
            binding.mediaTitle.text = it.toString()
        }
        metadata.artist?.let {
            val artists = it.split("; ", ", ")
            binding.mediaArtists.text = artists.joinToString()
        }
    }

    private fun updateDuration(duration: Long) {
        // Update playback seek duration text to duration-to-timestamp value
        duration.toDuration(DurationUnit.MILLISECONDS).toComponents { minutes, seconds, _ ->
            binding.playbackSeekTextDuration.text =
                getString(R.string.playback_seek_text_format).format(minutes, seconds)
        }
    }

    private fun updateSeek(position: Long) {
        // Update playback seek slider from specified position
        binding.playbackSeekSlider.value = (position + 0.0f) / (mediaController?.duration ?: 0)
        // Update playback seek position text to position-to-timestamp value
        position.toDuration(DurationUnit.MILLISECONDS).toComponents { minutes, seconds, _ ->
            binding.playbackSeekTextPosition.text =
                getString(R.string.playback_seek_text_format).format(minutes, seconds)
        }
    }

    private fun updateState(isPlaying: Boolean) {
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
    }
}