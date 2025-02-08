package io.github.borfei.musiqview

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import io.github.borfei.musiqview.databinding.ActivityLauncherBinding
import io.github.borfei.musiqview.extensions.adjustPaddingForSystemBarInsets
import io.github.borfei.musiqview.extensions.isActivityEnabled
import io.github.borfei.musiqview.extensions.setActivityEnabled

class LauncherActivity : AppCompatActivity() {
    companion object {
        const val TAG = "LauncherActivity"
    }

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
            if (packageManager.isActivityEnabled(this)) {
                packageManager.setActivityEnabled(this, false)
                Log.d(TAG, "This activity can no longer be accessible.")
            }

            finish()
        }
    }
}