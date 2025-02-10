package io.github.borfei.musiqview.ui

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.media3.common.C
import androidx.media3.session.MediaController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.github.borfei.musiqview.R
import io.github.borfei.musiqview.databinding.AlertDialogMediaInfoBinding
import io.github.borfei.musiqview.extensions.getName
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class MediaInfoAlertDialog(private val mediaController: MediaController) : DialogFragment() {
    companion object {
        const val TAG = "MediaInfoAlertDialog"
    }

    @SuppressLint("SetTextI18n")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = AlertDialogMediaInfoBinding.inflate(layoutInflater)
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_media_info_title)
            .setView(binding.root)
            .setNegativeButton(R.string.dialog_media_info_negative) { it, _ -> it.dismiss() }
            .create()

        val mediaMetadata = mediaController.mediaMetadata
        val mediaUri = mediaController.currentMediaItem?.localConfiguration?.uri

        mediaMetadata.let { meta ->
            meta.title?.let { binding.mediaInfoTitle.text = it }
            meta.displayTitle?.let { binding.mediaInfoDisplayTitle.text = it }
            meta.artist?.let { binding.mediaInfoArtist.text = it }
            meta.writer?.let { binding.mediaInfoWriter.text = it }
            meta.composer?.let { binding.mediaInfoComposer.text = it }

            meta.albumTitle?.let { binding.mediaInfoAlbum.text = it }
            meta.albumArtist?.let { binding.mediaInfoAlbumArtist.text = it }
            meta.discNumber?.let { binding.mediaInfoDiscNumber.text = it.toString() }
            meta.trackNumber?.let { binding.mediaInfoTrackNumber.text = it.toString() }
            meta.totalDiscCount?.let { binding.mediaInfoTotalDiscs.text = it.toString() }
            meta.totalTrackCount?.let { binding.mediaInfoTotalTracks.text = it.toString() }
        }

        mediaController.duration.let {
            binding.mediaInfoDuration.text = if (it != C.TIME_UNSET) {
                it.toDuration(DurationUnit.MILLISECONDS).toComponents { minutes, seconds, _ ->
                    getString(R.string.playback_seek_text_format, minutes, seconds)
                }
            } else {
                getString(R.string.media_info_entry_placeholder)
            }
        }
        mediaUri?.getName(requireContext())?.let {
            binding.mediaInfoFilename.text = it
        }

        return dialog
    }
}