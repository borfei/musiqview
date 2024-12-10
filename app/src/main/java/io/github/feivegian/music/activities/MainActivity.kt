package io.github.feivegian.music.activities

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import io.github.feivegian.music.R
import io.github.feivegian.music.databinding.ActivityMainBinding
import io.github.feivegian.music.extensions.adjustPaddingForSystemBarInsets
import io.github.feivegian.music.extensions.isActivityEnabled
import io.github.feivegian.music.extensions.setActivityEnabled

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private var disableOnDestroy: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Inflate activity view using ViewBinding
        binding = ActivityMainBinding.inflate(layoutInflater)
        binding.root.adjustPaddingForSystemBarInsets(top=true, bottom=true)
        setContentView(binding.root)

        // Re-set header text with the app name
        binding.welcomeHeader.text = getString(R.string.welcome_header, getString(R.string.app_name))
        // Register continue button click listener
        binding.welcomeContinue.setOnClickListener {
            disableOnDestroy = true
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