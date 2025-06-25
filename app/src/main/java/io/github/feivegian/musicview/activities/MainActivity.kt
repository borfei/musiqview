package io.github.feivegian.musicview.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import io.github.feivegian.musicview.App
import io.github.feivegian.musicview.R
import io.github.feivegian.musicview.databinding.ActivityMainBinding
import io.github.feivegian.musicview.extensions.adjustPaddingForSystemBarInsets
import io.github.feivegian.musicview.extensions.isActivityEnabled
import io.github.feivegian.musicview.extensions.setActivityEnabled

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private var hideOnDestroy: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val preferences = App.fromInstance(application).preferences

        // If PREFERENCE_WELCOME_HIDE_LAUNCHER is true, launch PreferenceActivity instead
        preferences.getBoolean(PREFERENCE_WELCOME_HIDE_LAUNCHER, true).let {
            if (!it) {
                Log.i(TAG, "Launcher not hidden, starting PreferenceActivity")
                startActivity(Intent(this, PreferenceActivity::class.java))
            }
        }

        // Inflate activity view using ViewBinding
        binding = ActivityMainBinding.inflate(layoutInflater)
        binding.root.adjustPaddingForSystemBarInsets(top=true, bottom=true)
        setContentView(binding.root)

        // Parse header text & hide launcher checkbox with the app name
        binding.welcomeHeader.text = getString(R.string.welcome_header, getString(R.string.app_name))
        binding.welcomeHideLauncher.text = getString(R.string.welcome_hide_launcher, getString(R.string.app_name))

        // Register continue click listener
        binding.welcomeContinue.setOnClickListener {
            if (!binding.welcomeHideLauncher.isChecked) {
                hideOnDestroy = false
            }
            preferences.edit {
                putBoolean(PREFERENCE_WELCOME_HIDE_LAUNCHER, binding.welcomeHideLauncher.isChecked)
                apply()
            }

            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        // If this activity is enabled, and hideOnDestroy is true, disable it.
        // This is to hide the application from home screen launchers
        if (packageManager.isActivityEnabled(this) && hideOnDestroy) {
            packageManager.setActivityEnabled(this, false)
        }
    }

    companion object {
        const val TAG = "MainActivity"
        const val PREFERENCE_WELCOME_HIDE_LAUNCHER = "welcome_hide_launcher"
    }
}