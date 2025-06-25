package io.github.borfei.musiqview.fragments

import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import androidx.preference.CheckBoxPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SeekBarPreference
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.github.borfei.musiqview.App
import io.github.borfei.musiqview.BuildConfig
import io.github.borfei.musiqview.Constants
import io.github.borfei.musiqview.R
import io.github.borfei.musiqview.activities.PreferenceActivity
import io.github.borfei.musiqview.extensions.changeNightMode
import java.io.File

class PreferenceFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {
    companion object {
        const val TAG = "PreferenceFragment"
    }

    private lateinit var activity: PreferenceActivity
    private lateinit var customTabsIntent: CustomTabsIntent
    private lateinit var application: App
    private lateinit var preferences: SharedPreferences

    private var uec: Int = 0

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preference, rootKey)
        activity = requireActivity() as PreferenceActivity
        customTabsIntent = CustomTabsIntent.Builder().build()
        application = App.fromInstance(activity.application)
        preferences = application.preferences
        preferences.registerOnSharedPreferenceChangeListener(this)

        val theme = findPreference<ListPreference>("looks_theme")
        val dynamicColors = findPreference<CheckBoxPreference>("looks_dynamic_colors")
        val durationInterval = findPreference<SeekBarPreference>("playback_duration_interval")
        val maxCacheSize = findPreference<SeekBarPreference>("playback_max_cache_size")
        val name = findPreference<Preference>("about_name")
        val source = findPreference<Preference>("about_source")
        val experiments = findPreference<PreferenceCategory>("experiments")
        val experimentsUnlock = findPreference<CheckBoxPreference>("experiments_unlock")
        val crashLogs = findPreference<Preference>("experiments_crash_logs")
        val deviceInfo = findPreference<Preference>("experiments_device_info")

        name?.summary = getString(R.string.preference_about_version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE)
        experiments?.isVisible = experimentsUnlock?.isChecked == true

        theme?.setOnPreferenceChangeListener { _, newValue ->
            application.changeNightMode(newValue as String)
            true
        }
        dynamicColors?.setOnPreferenceChangeListener { _, _ ->
            activity.notifyRestart()
            true
        }
        durationInterval?.setOnPreferenceChangeListener { pref, newValue ->
            val step = 100
            val preference = pref as SeekBarPreference
            preference.value = (newValue as Int / step) * step
            false
        }
        maxCacheSize?.setOnPreferenceChangeListener { pref, newValue ->
            val step = 4
            val preference = pref as SeekBarPreference
            preference.value = (newValue as Int / step) * step
            false
        }
        name?.setOnPreferenceClickListener {
            if (!BuildConfig.DEBUG) {
                // Non-debug builds are not allowed to access these features
                return@setOnPreferenceClickListener false
            }
            if (uec >= 5) {
                if (experimentsUnlock?.isChecked == false) {
                    experimentsUnlock.isChecked = true
                    activity.notifyRestart()
                }

                uec = 0 // reset to zero after unlocking experiments
            } else {
                uec += 1
            }

            true
        }
        source?.setOnPreferenceClickListener {
            val url = it.summary.toString()
            customTabsIntent.launchUrl(requireContext(), Uri.parse(url))
            true
        }
        experimentsUnlock?.setOnPreferenceChangeListener { _, _ ->
            activity.notifyRestart()
            true
        }
        crashLogs?.setOnPreferenceClickListener {
            val logs = File(requireContext().dataDir, "crash")
            val dialog = MaterialAlertDialogBuilder(activity)
                .setIcon(R.drawable.dialog_logs_48)
                .setTitle(R.string.dialog_crash_logs_title)
                .setNegativeButton(R.string.dialog_crash_logs_negative) { _, _ -> }

            if (logs.list().isNullOrEmpty()) {
                dialog.setMessage(R.string.preference_experiments_crash_logs_empty)
            } else {
                dialog
                    .setItems(logs.list()) { _, item ->
                        val filename = logs.list()?.get(item) ?: String()
                        val file = File(logs, filename)

                        MaterialAlertDialogBuilder(requireContext())
                            .setTitle(filename)
                            .setMessage(file.readText())
                            .setNegativeButton(R.string.dialog_crash_logs_negative) { _, _ -> }
                            .show()
                    }
                    .setNeutralButton(R.string.dialog_crash_logs_neutral) { _, _ ->
                        val count = logs.list()?.size
                        val ok = logs.deleteRecursively()

                        if (ok) {
                            Toast.makeText(requireContext(), getString(R.string.dialog_crash_logs_clear, count), Toast.LENGTH_LONG).show()
                        }
                    }
            }

            dialog.show()
            true
        }
        deviceInfo?.setOnPreferenceClickListener {
            val messageBuilder = StringBuilder().apply {
                appendLine(getString(R.string.dialog_device_info_message_device, Build.DEVICE))
                appendLine(getString(R.string.dialog_device_info_message_brand, Build.BRAND))
                appendLine(getString(R.string.dialog_device_info_message_model, Build.MODEL))
                appendLine(getString(R.string.dialog_device_info_message_product, Build.PRODUCT))
                appendLine(getString(R.string.dialog_device_info_message_manufacturer, Build.MANUFACTURER))
                appendLine(getString(R.string.dialog_device_info_message_supported_abi, Build.SUPPORTED_ABIS.joinToString(",")))
                appendLine()
                appendLine(getString(R.string.dialog_device_info_message_os_codename, Build.VERSION.CODENAME))
                appendLine(getString(R.string.dialog_device_info_message_os_release, Build.VERSION.RELEASE))
                appendLine(getString(R.string.dialog_device_info_message_os_sdk_version, Build.VERSION.SDK_INT))
                appendLine(getString(R.string.dialog_device_info_message_os_security_patch, Build.VERSION.SECURITY_PATCH))
            }
            MaterialAlertDialogBuilder(activity)
                .setIcon(R.drawable.dialog_device_info_48)
                .setTitle(R.string.dialog_device_info_title)
                .setMessage(messageBuilder)
                .setNegativeButton(R.string.dialog_device_info_negative) { _, _ -> }
                .show()

            true
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        preferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        var value: Any = "UNKNOWN"

        try {
            value = sharedPreferences?.getString(key, String())!!
        } catch (_: ClassCastException) {}
        try {
            value = sharedPreferences?.getStringSet(key, setOf())!!
        } catch (_: ClassCastException) {}
        try {
            value = sharedPreferences?.getBoolean(key, false)!!
        } catch (_: ClassCastException) {}
        try {
            value = sharedPreferences?.getInt(key, -1)!!
        } catch (_: ClassCastException) {}

        Log.i(TAG, "Preference changed: $key -> $value")
    }
}