package io.github.borfei.musiqview.ui

import android.annotation.SuppressLint
import android.app.Dialog
import android.net.Uri
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

class MediaInfoAlertDialog : DialogFragment() {
    companion object {
        const val TAG = "MediaInfoAlertDialog"

        fun newInstance(mediaController: MediaController): MediaInfoAlertDialog {
            val bundle = Bundle()
            val mediaMetadata = mediaController.mediaMetadata
            val mediaUri = mediaController.currentMediaItem?.localConfiguration?.uri

            mediaMetadata.let { meta ->
                meta.title?.let { bundle.putString("title", it.toString()) }
                meta.displayTitle?.let { bundle.putString("displayTitle", it.toString()) }
                meta.artist?.let { bundle.putString("artist", it.toString()) }
                meta.writer?.let { bundle.putString("writer", it.toString()) }
                meta.composer?.let { bundle.putString("composer", it.toString()) }

                meta.albumTitle?.let { bundle.putString("albumTitle", it.toString()) }
                meta.albumArtist?.let {bundle.putString("albumArtist", it.toString()) }
                meta.discNumber?.let { bundle.putString("discNumber", it.toString()) }
                meta.trackNumber?.let { bundle.putString("trackNumber", it.toString()) }
                meta.totalDiscCount?.let { bundle.putString("totalDiscCount", it.toString()) }
                meta.totalTrackCount?.let { bundle.putString("totalTrackCount", it.toString()) }
            }

            bundle.putLong("duration", mediaController.duration)
            bundle.putString("uri", mediaUri.toString())

            val dialog = MediaInfoAlertDialog()
            dialog.arguments = bundle
            return dialog
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = AlertDialogMediaInfoBinding.inflate(layoutInflater)
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_media_info_title)
            .setView(binding.root)
            .setNegativeButton(R.string.dialog_media_info_negative) { it, _ -> it.dismiss() }
            .create()

        arguments?.getString("title")?.let {
            binding.mediaInfoTitle.text = it
        }
        arguments?.getString("displayTitle")?.let {
            binding.mediaInfoDisplayTitle.text = it
        }
        arguments?.getString("artist")?.let {
            binding.mediaInfoArtist.text = it
        }
        arguments?.getString("writer")?.let {
            binding.mediaInfoWriter.text = it
        }
        arguments?.getString("composer")?.let {
            binding.mediaInfoComposer.text = it
        }

        arguments?.getString("albumTitle")?.let {
            binding.mediaInfoAlbum.text = it
        }
        arguments?.getString("albumArtist")?.let {
            binding.mediaInfoAlbumArtist.text = it
        }
        arguments?.getString("discNumber")?.let {
            binding.mediaInfoDiscNumber.text = it
        }
        arguments?.getString("trackNumber")?.let {
            binding.mediaInfoTrackNumber.text = it
        }
        arguments?.getString("totalDiscCount")?.let {
            binding.mediaInfoTotalDiscs.text = it
        }
        arguments?.getString("totalTrackCount")?.let {
            binding.mediaInfoTotalTracks.text = it
        }

        arguments?.getLong("duration")?.let {
            binding.mediaInfoDuration.text = if (it != C.TIME_UNSET) {
                it.toDuration(DurationUnit.MILLISECONDS).toComponents { minutes, seconds, _ ->
                    getString(R.string.playback_seek_text_format, minutes, seconds)
                }
            } else {
                getString(R.string.media_info_entry_placeholder)
            }
        }
        arguments?.getString("uri")?.let {
            binding.mediaInfoFilename.text = Uri.parse(it).getName(requireContext())
        }

        return dialog
    }
}