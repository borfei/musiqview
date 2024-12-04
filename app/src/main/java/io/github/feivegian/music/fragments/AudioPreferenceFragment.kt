package io.github.feivegian.music.fragments

import android.content.ComponentName
import android.os.Bundle
import androidx.media3.common.AudioAttributes
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.preference.CheckBoxPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SeekBarPreference
import androidx.preference.SwitchPreference
import com.google.common.util.concurrent.MoreExecutors
import io.github.feivegian.music.R
import io.github.feivegian.music.activities.PreferenceActivity
import io.github.feivegian.music.services.PlaybackService

class AudioPreferenceFragment : PreferenceFragmentCompat() {
    private lateinit var activity: PreferenceActivity
    private var mediaController: MediaController? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preference_audio, rootKey)
        activity = requireActivity() as PreferenceActivity
        val audioFocus = findPreference<CheckBoxPreference>("playback_audio_focus")
        val durationInterval = findPreference<SeekBarPreference>("playback_duration_interval")

        audioFocus?.setOnPreferenceChangeListener { _, newValue ->
            mediaController?.setAudioAttributes(AudioAttributes.DEFAULT, newValue as Boolean)
            true
        }
        durationInterval?.setOnPreferenceChangeListener { pref, newValue ->
            // manually set step by dividing & multiplying new value
            val preference = pref as SeekBarPreference
            preference.value = (newValue as Int / 1000) * 1000
            false
        }
    }

    override fun onStart() {
        super.onStart()
        activity.supportActionBar?.title = getString(R.string.preference_audio)
        val sessionToken = SessionToken(requireContext(), ComponentName(requireContext(), PlaybackService::class.java))
        val controllerFuture = MediaController.Builder(requireContext(), sessionToken).buildAsync()

        controllerFuture.addListener({
            mediaController = controllerFuture.get()
        }, MoreExecutors.directExecutor())
    }

    override fun onStop() {
        super.onStop()
        mediaController?.release()
    }
}