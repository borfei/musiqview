package io.github.feivegian.music.fragments

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.edit
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import io.github.feivegian.music.BuildConfig
import io.github.feivegian.music.R
import io.github.feivegian.music.activities.PreferenceActivity

class AboutFragment : PreferenceFragmentCompat() {
    private lateinit var activity: PreferenceActivity
    private lateinit var customTabsIntent: CustomTabsIntent

    private var uec = 0

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preference_about, rootKey)
        activity = requireActivity() as PreferenceActivity
        customTabsIntent = CustomTabsIntent.Builder().build()
        val name = findPreference<Preference>("about_name")
        val website = findPreference<Preference>("project_website")
        val source = findPreference<Preference>("project_source")

        name?.summary = getString(R.string.preference_about_version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE)
        name?.setOnPreferenceClickListener {
            // "uec" and "ex" is named here because we want this kind of functionality to be hidden
            // once the user has unlocked this, experiments will be accessible
            uec += 1

            if (uec >= 5) {
                val ex = activity.preferences.getBoolean("experiments", false)

                if (!ex) {
                    activity.preferences.edit {
                        putBoolean("experiments", true)
                        apply()
                    }
                    if (!activity.isRestartRequiredShown()) {
                        activity.setShowRestartRequired(true)
                    }
                }

                uec = 0 // reset to zero after unlocking experiments
            }

            true
        }
        website?.setOnPreferenceClickListener {
            val url = getString(R.string.preference_project_website_summary)
            customTabsIntent.launchUrl(requireContext(), Uri.parse(url))
            true
        }
        source?.setOnPreferenceClickListener {
            val url = it.summary.toString()
            customTabsIntent.launchUrl(requireContext(), Uri.parse(url))
            true
        }
    }

    override fun onStart() {
        super.onStart()
        activity.supportActionBar?.title = getString(R.string.preference_about)
    }
}