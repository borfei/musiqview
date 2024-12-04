package io.github.feivegian.music.fragments

import android.os.Bundle
import android.widget.Toast
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.github.feivegian.music.R
import io.github.feivegian.music.activities.PreferenceActivity
import java.io.File

class ExperimentsFragment : PreferenceFragmentCompat() {
    private lateinit var activity: PreferenceActivity

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preference_experiments, rootKey)
        activity = requireActivity() as PreferenceActivity
        val crashLogs = findPreference<Preference>("crash_logs")

        crashLogs?.setOnPreferenceClickListener {
            val logDir = File(requireContext().dataDir, "crash")

            if (logDir.list() == null || logDir.list()?.size!! < 1) {
                Toast.makeText(requireContext(), R.string.preference_experiments_crash_logs_empty, Toast.LENGTH_LONG).show()
                return@setOnPreferenceClickListener false
            }
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.dialog_crash_logs_title)
                .setItems(logDir.list()) { _, item ->
                    val filename = logDir.list()?.get(item) ?: String()
                    val file = File(logDir, filename)

                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle(filename)
                        .setMessage(file.readText())
                        .setNegativeButton(R.string.dialog_crash_logs_negative) { _, _ -> }
                        .show()
                }
                .setNegativeButton(R.string.dialog_crash_logs_negative) { _, _ -> }
                .setNeutralButton(R.string.dialog_crash_logs_neutral) { _, _ ->
                    val count = logDir.list()?.size
                    logDir.deleteRecursively()
                    Toast.makeText(requireContext(), getString(R.string.dialog_crash_logs_clear, count), Toast.LENGTH_LONG).show()
                }
                .show()

            true
        }
    }

    override fun onStart() {
        super.onStart()
        activity.supportActionBar?.title = getString(R.string.preference_experiments)
    }
}