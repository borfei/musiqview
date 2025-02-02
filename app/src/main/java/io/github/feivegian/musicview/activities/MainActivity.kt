package io.github.feivegian.musicview.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import io.github.feivegian.musicview.Constants
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

        // Inflate activity view using ViewBinding
        binding = ActivityMainBinding.inflate(layoutInflater)
        binding.root.adjustPaddingForSystemBarInsets(top=true, bottom=true)
        setContentView(binding.root)

        // Parse TextViews with actual strings
        val appName = getString(R.string.app_name)
        binding.welcomeHeader.text = getString(R.string.welcome_header, appName)
        binding.welcomePermission.text = getString(R.string.welcome_permission, appName)
        binding.welcomeInstruction.text = getString(R.string.welcome_instruction, appName)
        binding.welcomeConfiguration.text = getString(R.string.welcome_configuration, appName)

        // Register button click listeners
        binding.welcomeContinue.setOnClickListener {
            hideOnDestroy = true
            finish()
        }
        binding.welcomeCustomize.setOnClickListener {
            hideOnDestroy = true
            startActivity(Intent(this, PreferenceActivity::class.java))
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // When the activity is destroyed and hideOnDestroy is true, disable it afterwards
        if (packageManager.isActivityEnabled(this, this::class.java) && hideOnDestroy) {
            packageManager.setActivityEnabled(this, this::class.java, false)
            Log.d(Constants.TAG_ACTIVITY_MAIN, "Disabled activity; use app settings to re-enable it back")
        }
    }
}