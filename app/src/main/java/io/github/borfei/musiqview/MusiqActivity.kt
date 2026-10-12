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
import androidx.core.widget.doOnTextChanged
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import io.github.borfei.musiqview.databinding.ActivityMusiqBinding
import io.github.borfei.musiqview.extensions.adjustPaddingForSystemBarInsets
import io.github.borfei.musiqview.extensions.getName
import io.github.borfei.musiqview.extensions.toBitmap
import io.github.borfei.musiqview.ui.MediaInfoAlertDialog
import io.github.borfei.musiqview.ui.MenuBottomSheetDialog
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class MusiqActivity : AppCompatActivity(), Player.Listener {
    companion object {
        const val TAG = "MusiqActivity"
    }

    private lateinit var binding: ActivityMusiqBinding

    private var seekUpdateHandler: Handler? = null
    private var seekUpdateRunnable: Runnable? = null
    private var canUpdateSeek: Boolean = false

    private var mediaController: MediaController? = null
    private var mediaControllerFuture: ListenableFuture<MediaController>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        binding = ActivityMusiqBinding.inflate(layoutInflater)
        binding.root.adjustPaddingForSystemBarInsets(top=true, bottom=true)
        setContentView(binding.root)

        seekUpdateHandler =
            Handler(Looper.myLooper() ?: Looper.getMainLooper())
        seekUpdateRunnable = Runnable {
            if (canUpdateSeek) {
                mediaController?.currentPosition?.let {
                    updatePlaybackSeekPosition(it)
                }
                seekUpdateRunnable?.let {
                    seekUpdateHandler?.post(it)
                }
            }
        }

        initializeViews()
        initializeMediaController()
    }

    override fun onStart() {
        super.onStart()
        canUpdateSeek = true
        seekUpdateRunnable?.let {
            seekUpdateHandler?.removeCallbacks(it)
            seekUpdateHandler?.post(it)
        }
    }

    override fun onStop() {
        canUpdateSeek = false
        seekUpdateHandler?.removeCallbacksAndMessages(null)
        super.onStop()
    }

    override fun onDestroy() {
        Log.i(TAG, "Releasing media controller")
        mediaControllerFuture?.cancel(false)
        mediaController?.removeListener(this)
        mediaController?.release()

        seekUpdateHandler = null
        seekUpdateRunnable = null
        mediaControllerFuture = null
        super.onDestroy()
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        super.onMediaItemTransition(mediaItem, reason)
        Log.d(TAG, "onMediaItemTransition: mediaItem = $mediaItem, reason = $reason")
        updateMediaFilename()
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
                mediaController?.availableCommands?.let { updatePlaybackCommands(it) }
                updatePlaybackSeekDuration()
            }

            Player.STATE_ENDED -> {}
            Player.STATE_IDLE -> {
                binding.playbackLoadIndicator.hide()
                binding.playbackState.isEnabled = false
            }
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
        Toast.makeText(applicationContext, error.message, Toast.LENGTH_LONG).show()
        finish()
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
            updatePlaybackSeekPosition(newPosition.positionMs)
        }
    }

    private fun updateMediaFilename() {
        val uri = mediaController?.currentMediaItem?.localConfiguration?.uri
        binding.mediaFilename.text =
            uri?.getName(this) ?: getString(R.string.filename_placeholder)
    }

    private fun updateMediaMetadata(mediaMetadata: MediaMetadata) {
        Glide.with(this).clear(binding.mediaArtwork)
        mediaMetadata.artworkData?.let {
            Glide.with(this)
                .load(it.toBitmap())
                .transition(withCrossFade())
                .into(binding.mediaArtwork)
        }
        binding.mediaTitle.text = mediaMetadata.title?.toString().orEmpty()
        binding.mediaArtists.text = mediaMetadata.artist
            ?.split("; ", ", ")
            ?.joinToString()
            .orEmpty()
    }

    private fun updatePlaybackSeekPosition(position: Long) {
        val duration = mediaController?.duration ?: C.TIME_UNSET
        binding.playbackSeekSlider.value = if (duration > 0) {
            (position.toFloat() / duration).coerceIn(0.0f, 1.0f)
        } else {
            0f
        }
        position.toDuration(DurationUnit.MILLISECONDS).toComponents { minutes, seconds, _ ->
            binding.playbackSeekTextPosition.text = getString(R.string.playback_seek_text_format).format(minutes, seconds)
        }
    }

    private fun updatePlaybackSeekDuration() {
        val duration = mediaController?.duration?.takeIf { it > 0 }
        if (duration == null) {
            binding.playbackSeekTextDuration.setText(R.string.playback_seek_text)
            return
        }
        duration.let {
            it.toDuration(DurationUnit.MILLISECONDS).toComponents { minutes, seconds, _ ->
                binding.playbackSeekTextDuration.text = getString(R.string.playback_seek_text_format).format(minutes, seconds)
            }
        }
    }

    private fun updatePlaybackCommands(commands: Player.Commands) {
        binding.playbackSeekSlider.isEnabled = Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM in commands
        binding.playbackState.isEnabled = Player.COMMAND_PLAY_PAUSE in commands
        binding.playbackRepeat.isEnabled = Player.COMMAND_SET_REPEAT_MODE in commands
    }

    private fun updatePlaybackState(isPlaying: Boolean) {
        binding.playbackState.isChecked = isPlaying
    }

    private fun initializeMediaController() {
        val mediaSessionToken = SessionToken(this, ComponentName(this, MusiqService::class.java))
        val mediaControllerFuture = MediaController.Builder(this, mediaSessionToken).buildAsync()
        this.mediaControllerFuture = mediaControllerFuture

        mediaControllerFuture.addListener({
            if (isFinishing || isDestroyed) return@addListener
            Log.i(TAG, "Media controller initialized")
            mediaController = runCatching { mediaControllerFuture.get() }
                .onFailure { Log.e(TAG, "Unable to initialize media controller", it) }
                .getOrNull()
            mediaController?.addListener(this)

            if (mediaController?.currentMediaItem != null) {
                mediaController?.mediaMetadata?.let { updateMediaMetadata(it) }
                mediaController?.currentPosition?.let { updatePlaybackSeekPosition(it) }
                mediaController?.isPlaying?.let { updatePlaybackState(it) }
                updateMediaFilename()
                updatePlaybackSeekDuration()
            } else {
                setMediaItemFromIntent(intent)
            }
        }, MoreExecutors.directExecutor())
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        setMediaItemFromIntent(intent)
    }

    private fun setMediaItemFromIntent(intent: Intent?) {
        val item = PlaybackIntent.mediaItem(intent) ?: return
        Log.d(TAG, "Intent URI: ${item.localConfiguration?.uri}")
        mediaController?.setMediaItem(item)
        mediaController?.prepare()
        mediaController?.playWhenReady = true
    }

    private fun initializeViews() {
        binding.mediaTitle.doOnTextChanged { _, _, _, count ->
            binding.mediaTitle.visibility = if (count > 0) View.VISIBLE else View.GONE
        }
        binding.mediaArtists.doOnTextChanged { _, _, _, count ->
            binding.mediaArtists.visibility = if (count > 0) View.VISIBLE else View.GONE
        }

        binding.playbackSeekSlider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                val currentDuration = mediaController?.duration
                mediaController?.seekTo(((value + 0.0) * (currentDuration ?: 0)).toLong())
            }
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

        binding.otherMenu.setOnClickListener {
            val menuBottomSheetDialog = MenuBottomSheetDialog()

            menuBottomSheetDialog.setOnViewMediaInfoListener {
                if (mediaController != null) {
                    val mediaInfoAlertDialog = MediaInfoAlertDialog.newInstance(mediaController!!)
                    mediaInfoAlertDialog.show(supportFragmentManager, MediaInfoAlertDialog.TAG)
                    it.dismissNow()
                }
            }
            menuBottomSheetDialog.setOnOpenExternalListener { dialog ->
                mediaController?.currentMediaItem?.let { mediaItem ->
                    val uri = mediaItem.localConfiguration?.uri
                    val intent = Intent(Intent.ACTION_VIEW)
                    uri?.let { intent.setDataAndTypeAndNormalize(it, "audio/*") }
                    startActivity(Intent.createChooser(intent, null))
                }

                dialog.dismissNow()
            }

            menuBottomSheetDialog.show(supportFragmentManager, MenuBottomSheetDialog.TAG)
        }
    }
}