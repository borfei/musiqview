package io.github.borfei.musiqview

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import io.github.borfei.musiqview.databinding.ActivityLauncherBinding
import io.github.borfei.musiqview.extensions.adjustPaddingForSystemBarInsets

class LauncherActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLauncherBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        binding = ActivityLauncherBinding.inflate(layoutInflater)
        binding.root.adjustPaddingForSystemBarInsets(top=true, bottom=true)
        setContentView(binding.root)

        val appName = getString(R.string.app_name)
        binding.launcherGreeting.text = getString(R.string.launcher_greeting, appName)
        binding.launcherInstructions.text = getString(R.string.launcher_instructions, appName, appName)

        binding.launcherSetDefault.setOnClickListener {
            startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
        }
        binding.launcherFinish.setOnClickListener {
            finish()
        }
    }
}