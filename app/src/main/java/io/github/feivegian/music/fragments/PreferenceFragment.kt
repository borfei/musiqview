package io.github.feivegian.music.fragments

import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import androidx.preference.CheckBoxPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.SeekBarPreference
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.github.feivegian.music.App
import io.github.feivegian.music.BuildConfig
import io.github.feivegian.music.R
import io.github.feivegian.music.activities.PreferenceActivity
import io.github.feivegian.music.utils.convert
import io.github.feivegian.music.utils.setNightMode
import java.io.File

class PreferenceFragment : PreferenceFragmentCompat() {
    private lateinit var activity: PreferenceActivity
    private lateinit var customTabsIntent: CustomTabsIntent
    private lateinit var application: App
    private lateinit var preferences: SharedPreferences

    private var uec: Int = 0

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preference, rootKey)
        activity = requireActivity() as PreferenceActivity
        customTabsIntent = CustomTabsIntent.Builder().build()
        application = activity.application.convert()
        preferences = PreferenceManager.getDefaultSharedPreferences(requireContext())

        val theme = findPreference<ListPreference>("looks_theme")
        val dynamicColors = findPreference<CheckBoxPreference>("looks_dynamic_colors")
        val durationInterval = findPreference<SeekBarPreference>("playback_duration_interval")
        val name = findPreference<Preference>("about_name")
        val website = findPreference<Preference>("project_website")
        val source = findPreference<Preference>("project_source")
        val experiments = findPreference<PreferenceCategory>("about_experiments")
        val experimentsUnlock = findPreference<CheckBoxPreference>("experiments_unlock")
        val crashLogs = findPreference<Preference>("experiments_crash_logs")

        name?.summary = getString(R.string.preference_about_version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE)
        experiments?.isVisible = experimentsUnlock?.isChecked == true

        theme?.setOnPreferenceChangeListener { _, newValue ->
            application.setNightMode(newValue as String)
            true
        }
        dynamicColors?.setOnPreferenceChangeListener { _, _ ->
            activity.setShowRestartRequired(true)
            true
        }
        durationInterval?.setOnPreferenceChangeListener { pref, newValue ->
            // manually set step by dividing & multiplying new value
            val preference = pref as SeekBarPreference
            preference.value = (newValue as Int / 100) * 100
            false
        }
        name?.setOnPreferenceClickListener {
            // "uec" and "ex" is named here because we want this kind of functionality to be hidden
            // once the user has unlocked this, experiments will be accessible
            if (uec >= 5) {
                if (experimentsUnlock?.isChecked == false) {
                    experimentsUnlock.isChecked = true
                    activity.recreate() // restart activity to show experiments
                }

                uec = 0 // reset to zero after unlocking experiments
            } else {
                uec += 1
            }

            true
        }
        website?.setOnPreferenceClickListener {
            val url = getString(R.string.preference_about_project_website_summary)
            customTabsIntent.launchUrl(requireContext(), Uri.parse(url))
            true
        }
        source?.setOnPreferenceClickListener {
            val url = it.summary.toString()
            customTabsIntent.launchUrl(requireContext(), Uri.parse(url))
            true
        }
        experimentsUnlock?.setOnPreferenceChangeListener { _, _ ->
            activity.recreate() // restart activity to hide experiments
            true
        }
        // TODO: Use an efficient way of building AlertDialogs
        crashLogs?.setOnPreferenceClickListener {
            val logs = File(requireContext().dataDir, "crash")

            if (logs.list() == null || logs.list()?.size!! < 1) {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.dialog_crash_logs_title)
                    .setMessage(R.string.preference_about_experiments_crash_logs_empty)
                    .setNegativeButton(R.string.dialog_crash_logs_negative) { _, _ -> }
                    .show()

                return@setOnPreferenceClickListener false
            }
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.dialog_crash_logs_title)
                .setItems(logs.list()) { _, item ->
                    val filename = logs.list()?.get(item) ?: String()
                    val file = File(logs, filename)

                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle(filename)
                        .setMessage(file.readText())
                        .setNegativeButton(R.string.dialog_crash_logs_negative) { _, _ -> }
                        .show()
                }
                .setNegativeButton(R.string.dialog_crash_logs_negative) { _, _ -> }
                .setNeutralButton(R.string.dialog_crash_logs_neutral) { _, _ ->
                    val count = logs.list()?.size
                    logs.deleteRecursively()
                    Toast.makeText(requireContext(), getString(R.string.dialog_crash_logs_clear, count), Toast.LENGTH_LONG).show()
                }
                .show()

            true
        }
    }
}