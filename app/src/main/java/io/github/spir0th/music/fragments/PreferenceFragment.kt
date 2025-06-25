package io.github.spir0th.music.fragments

import android.content.SharedPreferences
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import io.github.spir0th.music.R
import io.github.spir0th.music.activities.PreferenceActivity

class PreferenceFragment : PreferenceFragmentCompat() {
    private lateinit var activity: PreferenceActivity
    private lateinit var preferences: SharedPreferences

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preference, rootKey)
        activity = requireActivity() as PreferenceActivity
        preferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val experiments = findPreference<Preference>("experiments")!!
        experiments.isVisible = preferences.getBoolean("experiments", false)
    }

    override fun onStart() {
        super.onStart()
        activity.supportActionBar?.title = getString(R.string.preference_title)
    }
}