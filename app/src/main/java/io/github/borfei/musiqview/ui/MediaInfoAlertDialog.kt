package io.github.borfei.musiqview.ui

import android.annotation.SuppressLint
import android.app.Dialog
import android.net.Uri
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.fragment.app.DialogFragment
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.github.borfei.musiqview.R
import io.github.borfei.musiqview.databinding.AlertDialogMediaInfoBinding
import io.github.borfei.musiqview.extensions.getName

class MediaInfoAlertDialog(
    private val mediaMetadata: MediaMetadata,
    private val mediaUri: Uri
) : DialogFragment() {
    companion object {
        const val TAG = "MediaInfoAlertDialog"
    }

    @SuppressLint("SetTextI18n")
    @OptIn(UnstableApi::class)
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = AlertDialogMediaInfoBinding.inflate(layoutInflater)
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_media_info_title)
            .setView(binding.root)
            .setNegativeButton(R.string.dialog_media_info_negative) { it, _ -> it.dismiss() }
            .create()

        mediaMetadata.title?.let {
            if (it.isNotEmpty()) {
                binding.mediaInfoTitle.text = it
            }
        }
        mediaMetadata.displayTitle?.let {
            if (it.isNotEmpty()) {
                binding.mediaInfoDisplayTitle.text = it
            }
        }
        mediaMetadata.artist?.let {
            if (it.isNotEmpty()) {
                binding.mediaInfoArtist.text = it
            }
        }
        mediaMetadata.writer?.let {
            if (it.isNotEmpty()) {
                binding.mediaInfoWriter.text = it
            }
        }
        mediaMetadata.composer?.let {
            if (it.isNotEmpty()) {
                binding.mediaInfoComposer.text = it
            }
        }

        mediaMetadata.albumTitle?.let {
            if (it.isNotEmpty()) {
                binding.mediaInfoAlbum.text = it
            }
        }
        mediaMetadata.albumArtist?.let {
            if (it.isNotEmpty()) {
                binding.mediaInfoAlbumArtist.text = it
            }
        }
        mediaMetadata.discNumber?.let {
            if (it > 0) {
                binding.mediaInfoDiscNumber.text = it.toString()
            }
        }
        mediaMetadata.trackNumber?.let {
            if (it > 0) {
                binding.mediaInfoTrackNumber.text = it.toString()
            }
        }
        mediaMetadata.totalDiscCount?.let {
            if (it > 0) {
                binding.mediaInfoTotalDiscs.text = it.toString()
            }
        }
        mediaMetadata.totalTrackCount?.let {
            if (it > 0) {
                binding.mediaInfoTotalTracks.text = it.toString()
            }
        }

        mediaMetadata.durationMs?.let {
            if (it > 0) {
                binding.mediaInfoDuration.text = it.toString()
            }
        }
        binding.mediaInfoFilename.text = mediaUri.getName(requireContext())
        return dialog
    }
}