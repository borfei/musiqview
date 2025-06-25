package io.github.feivegian.music

import android.app.Application
import android.content.SharedPreferences
import android.util.Log
import androidx.preference.PreferenceManager
import com.google.android.material.color.DynamicColors
import io.github.feivegian.music.utils.generateTraceLog
import io.github.feivegian.music.utils.setNightMode

class App : Application(), Thread.UncaughtExceptionHandler {
    private lateinit var preferences: SharedPreferences

    private var theme: String = "auto"
    private var dynamicColors: Boolean = false

    override fun onCreate() {
        super.onCreate()
        // Init preferences & toggle required values
        preferences = PreferenceManager.getDefaultSharedPreferences(this)
        theme = preferences.getString("looks_theme", "auto").toString()
        dynamicColors = preferences.getBoolean("looks_dynamic_colors", false)

        setNightMode(theme)
        dynamicColors.let {
            if (it && DynamicColors.isDynamicColorAvailable()) {
                DynamicColors.applyToActivitiesIfAvailable(this)
            }
        }

        // Initialize custom uncaught exception handler & greet the logcat
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