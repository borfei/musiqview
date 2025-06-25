package io.github.spir0th.music.activities

import android.content.DialogInterface
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
import com.google.android.material.dialog.MaterialDialogs
import com.google.android.material.snackbar.Snackbar
import com.jakewharton.processphoenix.ProcessPhoenix
import io.github.spir0th.music.R
import io.github.spir0th.music.databinding.ActivityPreferenceBinding
import io.github.spir0th.music.fragments.PreferenceFragment
import io.github.spir0th.music.utils.adjustMarginsForSystemBarInsets
import io.github.spir0th.music.utils.adjustPaddingForSystemBarInsets

class PreferenceActivity : AppCompatActivity(), PreferenceFragmentCompat.OnPreferenceStartFragmentCallback, SharedPreferences.OnSharedPreferenceChangeListener {
    private lateinit var binding: ActivityPreferenceBinding
    private lateinit var preferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        preferences = PreferenceManager.getDefaultSharedPreferences(this)
        preferences.registerOnSharedPreferenceChangeListener(this)
        binding = ActivityPreferenceBinding.inflate(layoutInflater)
        binding.toolbar.adjustPaddingForSystemBarInsets(top=true)
        binding.preference.adjustMarginsForSystemBarInsets(left=true, right=true, bottom=true)

        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

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

        Log.i(TAG, "$key: $value")
    }

    fun toggleExperiments(enable: Boolean) {
        preferences.edit().putBoolean("experiments", enable).apply()
    }

    fun areExperimentsEnabled(): Boolean {
        return preferences.getBoolean("experiments", false)
    }

    fun showRestartRequired() {
        val restart = Snackbar.make(binding.root, R.string.snackbar_restart_required, Snackbar.LENGTH_INDEFINITE)
        restart.setAction(R.string.snackbar_restart_required_action) { _ ->
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_restart_title)
                .setMessage(R.string.dialog_restart_message)
                .setPositiveButton(R.string.dialog_restart_positive) { _, _ ->
                    val intent = Intent(this, this::class.java)
                    ProcessPhoenix.triggerRebirth(this, intent)
                }
                .setNegativeButton(R.string.dialog_restart_negative) { _, _ -> }
                .create()
                .show()
        }
        restart.show()
    }

    companion object {
        const val TAG = "SettingsActivity"
    }
}