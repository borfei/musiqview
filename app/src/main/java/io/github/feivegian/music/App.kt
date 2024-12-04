package io.github.feivegian.music

import android.app.Application
import android.content.SharedPreferences
import android.util.Log
import androidx.preference.PreferenceManager
import com.google.android.material.color.DynamicColors
import io.github.feivegian.music.utils.generateTraceLog
import io.github.feivegian.music.utils.setNightMode
import io.github.feivegian.music.R

class App : Application(), Thread.UncaughtExceptionHandler {
    private lateinit var preferences: SharedPreferences
    override fun onCreate() {
        super.onCreate()
        preferences = PreferenceManager.getDefaultSharedPreferences(this)
        preferences.getString("theme", "auto")?.let { setNightMode(it) }
        preferences.getBoolean("theme_dynamic_colors", false).let { if (it) { DynamicColors.applyToActivitiesIfAvailable(this) } }
        Log.i(TAG, "Initialized ${getString(R.string.app_name)} version ${BuildConfig.VERSION_NAME} (code ${BuildConfig.VERSION_CODE})")
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    override fun uncaughtException(t: Thread, e: Throwable) {
        generateTraceLog(e)
    }

    companion object {
        const val TAG = "App"
    }
}