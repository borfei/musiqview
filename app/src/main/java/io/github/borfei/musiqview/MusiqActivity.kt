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
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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
        const val TAG = "MusiqActivity"
    }

    private lateinit var binding: ActivityMusiqBinding

    private var seekUpdateHandler: Handler? = null
    private var seekUpdateRunnable: Runnable? = null
    private val seekUpdateInterval: Long = 1000

    private var mediaController: MediaController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        binding = ActivityMusiqBinding.inflate(layoutInflater)
        binding.root.adjustPaddingForSystemBarInsets(top=true, bottom=true)
        setContentView(binding.root)

        WindowCompat.getInsetsController(window, window.decorView).apply {
            setImmersiveMode(false)
        }

        seekUpdateHandler =
            Handler(Looper.myLooper() ?: Looper.getMainLooper())
        seekUpdateRunnable = Runnable {
            mediaController?.currentPosition?.let {
                updatePlaybackSeek(it)
            }
            seekUpdateRunnable?.let {
                seekUpdateHandler?.postDelayed(it, seekUpdateInterval)
            }
        }

        initializeViews()
        initializeMediaController()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "Releasing media controller")
        mediaController?.removeListener(this)
        mediaController?.release()
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        super.onMediaItemTransition(mediaItem, reason)
        Log.d(TAG, "onMediaItemTransition: mediaItem = $mediaItem, reason = $reason")

        mediaItem?.localConfiguration?.uri?.let {
            binding.mediaFilename.text = it.getName(this)
        }
    }

    override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
        super.onMediaMetadataChanged(mediaMetadata)
        Log.v(TAG, "onMediaMetadataChanged: mediaMetadata = $mediaMetadata")
        updateMediaMetadata(mediaMetadata)
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        super.onPlaybackStateChanged(playbackState)
        Log.d(TAG, "onPlaybackStateChanged: playbackState = $playbackState")

        when (playbackState) {
            Player.STATE_BUFFERING -> {
                binding.playbackLoadIndicator.show()
                binding.playbackState.isEnabled = false
            }
            Player.STATE_READY -> {
                binding.playbackLoadIndicator.hide()
                binding.playbackState.isEnabled = true
                updatePlaybackDuration()
            }

            Player.STATE_ENDED -> finish()
            Player.STATE_IDLE -> {}
        }
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        super.onIsPlayingChanged(isPlaying)
        Log.v(TAG, "onIsPlayingChanged: isPlaying = $isPlaying")
        updatePlaybackState(isPlaying)
    }

    override fun onRepeatModeChanged(repeatMode: Int) {
        super.onRepeatModeChanged(repeatMode)
        Log.v(TAG, "onRepeatModeChanged: repeatMode = $repeatMode")
        binding.playbackRepeat.isChecked = repeatMode == Player.REPEAT_MODE_ALL
    }

    override fun onPlayerError(error: PlaybackException) {
        super.onPlayerError(error)
        Log.e(TAG, "onPlayerError: ${error.message}")

        // Display playback error message in an alert dialog
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
        Log.v(TAG, "onPositionDiscontinuity: " +
                "oldPosition = $oldPosition, " +
                "newPosition = $newPosition, " +
                "reason = $reason")

        // If this event was called due to user seeking, update the UI seek state
        if (reason == Player.DISCONTINUITY_REASON_SEEK) {
            updatePlaybackSeek(newPosition.positionMs)
        }
    }

    private fun updateMediaMetadata(mediaMetadata: MediaMetadata) {
        mediaMetadata.artworkData?.let {
            Glide.with(this)
                .load(it.toBitmap())
                .transition(withCrossFade())
                .into(binding.mediaArtwork)
        }
        mediaMetadata.title?.let {
            binding.mediaTitle.text = it.toString()
        }
        mediaMetadata.artist?.let {
            val artists = it.split("; ", ", ")
            binding.mediaArtists.text = artists.joinToString()
        }
    }

    private fun updatePlaybackSeek(position: Long) {
        binding.playbackSeekSlider.value = (position + 0.0f) / (mediaController?.duration ?: 0)

        position.toDuration(DurationUnit.MILLISECONDS).toComponents { minutes, seconds, _ ->
            binding.playbackSeekTextPosition.text = getString(R.string.playback_seek_text_format).format(minutes, seconds)
        }
    }

    private fun updatePlaybackDuration() {
        mediaController?.duration?.let {
            it.toDuration(DurationUnit.MILLISECONDS).toComponents { minutes, seconds, _ ->
                binding.playbackSeekTextDuration.text = getString(R.string.playback_seek_text_format).format(minutes, seconds)
            }
        }
    }

    private fun updatePlaybackState(isPlaying: Boolean) {
        binding.playbackState.isChecked = isPlaying

        if (isPlaying) {
            seekUpdateRunnable?.let {
                seekUpdateHandler?.post(it)
            }
        } else {
            seekUpdateHandler?.removeCallbacksAndMessages(null)
        }
    }

    private fun initializeMediaController() {
        val mediaSessionToken = SessionToken(this, ComponentName(this, MusiqService::class.java))
        val mediaControllerFuture = MediaController.Builder(this, mediaSessionToken).buildAsync()

        mediaControllerFuture.addListener({
            Log.i(TAG, "Media controller initialized")
            mediaController = mediaControllerFuture.get()
            mediaController?.addListener(this)

            // If an intent URI has received, load it as a media item
            intent?.let {
                if (intent.action == Intent.ACTION_VIEW) {
                    intent.data?.let {
                        Log.d(TAG, "Intent URI: $it")
                        mediaController?.setMediaItem(MediaItem.fromUri(it))
                        mediaController?.prepare()
                    }
                }
            }

            // Make sure to begin the playback when ready and prepared
            mediaController?.playWhenReady = true
        }, MoreExecutors.directExecutor())
    }

    private fun initializeViews() {
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
            mediaController?.repeatMode = if (isChecked) {
                Player.REPEAT_MODE_ALL
            } else {
                Player.REPEAT_MODE_OFF
            }
        }

        binding.playbackOptions.setOnClickListener {
            // TODO: Implement options menu here
            Toast.makeText(this, R.string.under_construction, Toast.LENGTH_LONG).show()
        }

        binding.playbackSeekSlider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                val currentDuration = mediaController?.duration
                mediaController?.seekTo(((value + 0.0) * (currentDuration ?: 0)).toLong())
            }
        }
    }
}