package io.github.spir0th.music.fragments

import android.os.Bundle
import androidx.preference.CheckBoxPreference
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import io.github.spir0th.music.App
import io.github.spir0th.music.R
import io.github.spir0th.music.activities.PreferenceActivity
import io.github.spir0th.music.utils.convert
import io.github.spir0th.music.utils.setNightMode

class AppearancePreferenceFragment : PreferenceFragmentCompat() {
    private lateinit var application: App
    private lateinit var activity: PreferenceActivity

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preference_appearance, rootKey)
        activity = requireActivity() as PreferenceActivity
        application = activity.application.convert()
        val theme = findPreference<ListPreference>("theme")
        val dynamicColors = findPreference<CheckBoxPreference>("theme_dynamic_colors")

        theme?.setOnPreferenceChangeListener { _, newValue ->
            application.setNightMode(newValue as String)
            true
        }
        dynamicColors?.setOnPreferenceChangeListener { _, _ ->
            activity.showRestartRequired()
            true
        }
    }

    override fun onStart() {
        super.onStart()
        activity.supportActionBar?.title = getString(R.string.preference_appearance)
    }
}