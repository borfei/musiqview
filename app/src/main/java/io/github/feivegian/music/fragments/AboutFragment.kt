package io.github.feivegian.music.fragments

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.jakewharton.processphoenix.ProcessPhoenix
import io.github.feivegian.music.BuildConfig
import io.github.feivegian.music.R
import io.github.feivegian.music.activities.PreferenceActivity

class AboutFragment : PreferenceFragmentCompat() {
    private lateinit var activity: PreferenceActivity

    private var discoverExperimentsCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity = requireActivity() as PreferenceActivity
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preference_about, rootKey)
        val name = findPreference<Preference>("name")

        name?.summary = getString(R.string.preference_about_version, BuildConfig.VERSION_NAME)
        name?.setOnPreferenceClickListener {
            discoverExperimentsCount += 1

            if (discoverExperimentsCount >= 5) {
                val activity = requireActivity() as PreferenceActivity
                val intent = Intent(requireContext(), PreferenceActivity::class.java)

                if (!activity.areExperimentsEnabled()) {
                    Toast.makeText(requireContext(), R.string.preference_discover_experiments, Toast.LENGTH_LONG).show()
                    activity.toggleExperiments(true)
                    ProcessPhoenix.triggerRebirth(requireContext(), intent)
                }

                discoverExperimentsCount = 0
            }

            true
        }
    }

    override fun onStart() {
        super.onStart()
        activity.supportActionBar?.title = getString(R.string.preference_about)
    }
}