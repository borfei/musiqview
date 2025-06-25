package io.github.feivegian.music.activities

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import io.github.feivegian.music.App.Companion.asApp
import io.github.feivegian.music.R
import io.github.feivegian.music.databinding.ActivityMainBinding
import io.github.feivegian.music.extensions.adjustPaddingForSystemBarInsets
import io.github.feivegian.music.extensions.isActivityEnabled
import io.github.feivegian.music.extensions.setActivityEnabled

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var preferences: SharedPreferences

    private var disableOnDestroy: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Get the preference instance from App
        preferences = application.asApp().getPreferences()

        // If "welcome_hide_launcher" is true, launch PreferenceActivity instead
        if (!preferences.getBoolean("welcome_hide_launcher", true)) {
            startActivity(Intent(this, PreferenceActivity::class.java))
            finish()
        }

        // Inflate activity view using ViewBinding
        binding = ActivityMainBinding.inflate(layoutInflater)
        binding.root.adjustPaddingForSystemBarInsets(top=true, bottom=true)
        setContentView(binding.root)

        // Re-set header text & hide launcher checkbox with the app name
        binding.welcomeHeader.text = getString(R.string.welcome_header, getString(R.string.app_name))
        binding.welcomeHideLauncher.text = getString(R.string.welcome_hide_launcher, getString(R.string.app_name))
        // Register continue click listener
        binding.welcomeContinue.setOnClickListener {
            if (binding.welcomeHideLauncher.isChecked) {
                disableOnDestroy = true
            }
            preferences.edit {
                putBoolean("welcome_hide_launcher", binding.welcomeHideLauncher.isChecked)
                apply()
            }

            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        // If this activity is enabled, and disableOnDestroy is true, disable it.
        // This is to hide the application from home screen launchers
        if (packageManager.isActivityEnabled(this) && disableOnDestroy) {
            packageManager.setActivityEnabled(this, false)
        }
    }
}