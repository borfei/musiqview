package io.github.feivegian.music.activities

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import io.github.feivegian.music.R
import io.github.feivegian.music.databinding.ActivityPreferenceBinding
import io.github.feivegian.music.fragments.PreferenceFragment
import io.github.feivegian.music.utils.adjustMarginsForSystemBarInsets
import io.github.feivegian.music.utils.adjustPaddingForSystemBarInsets

class PreferenceActivity : AppCompatActivity(), PreferenceFragmentCompat.OnPreferenceStartFragmentCallback, SharedPreferences.OnSharedPreferenceChangeListener {
    private lateinit var binding: ActivityPreferenceBinding
    private lateinit var preferences: SharedPreferences
    private lateinit var restartSnackbar: Snackbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        preferences = PreferenceManager.getDefaultSharedPreferences(this)
        preferences.registerOnSharedPreferenceChangeListener(this)

        binding = ActivityPreferenceBinding.inflate(layoutInflater)
        binding.toolbar.adjustPaddingForSystemBarInsets(top=true)
        binding.preference.adjustMarginsForSystemBarInsets(left=true, right=true, bottom=true)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setTitle(R.string.preference_title)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        restartSnackbar = Snackbar.make(binding.root, R.string.snackbar_restart_required, Snackbar.LENGTH_INDEFINITE).also {
            it.setAction(R.string.snackbar_restart_required_action) { _ ->
                MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.dialog_restart_title)
                    .setMessage(R.string.dialog_restart_message)
                    .setPositiveButton(R.string.dialog_restart_positive) { _, _ ->
                        startActivity(Intent.makeRestartActivityTask(componentName))
                        Runtime.getRuntime().exit(0)
                    }
                    .setNegativeButton(R.string.dialog_restart_negative) { _, _ -> setShowRestartRequired(!isRestartRequiredShown()) }
                    .create()
                    .show()
            }
        }
        onBackPressedDispatcher.addCallback(object: OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })
        binding.toolbar.setNavigationOnClickListener {
            if (supportFragmentManager.backStackEntryCount > 0) {
                supportFragmentManager.popBackStack()
            } else {
                onBackPressedDispatcher.onBackPressed()
            }
        }
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.preference, PreferenceFragment())
            .commit()
    }

    override fun onDestroy() {
        super.onDestroy()
        preferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onPreferenceStartFragment(
        caller: PreferenceFragmentCompat,
        pref: Preference
    ): Boolean {
        val fragment = pref.fragment?.let { supportFragmentManager.fragmentFactory.instantiate(classLoader, it) }
        fragment?.arguments = pref.extras

        supportFragmentManager.beginTransaction()
            .replace(R.id.preference, fragment!!)
            .addToBackStack(null)
            .commit()

        return true
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

    fun setShowRestartRequired(toggle: Boolean) {
        if (toggle) {
            restartSnackbar.show()
        } else {
            restartSnackbar.dismiss()
        }
    }

    fun isRestartRequiredShown(): Boolean {
        return restartSnackbar.isShown
    }

    fun getPreferences(): SharedPreferences {
        return preferences
    }

    companion object {
        const val TAG = "SettingsActivity"
    }
}