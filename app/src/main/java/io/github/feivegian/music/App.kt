package io.github.feivegian.music

import android.app.Application
import android.app.UiModeManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import com.google.android.material.color.DynamicColors
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.system.exitProcess

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

    private fun generateTraceLog(e: Throwable) {
        val dateTime = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val logDir = File(dataDir, "crash")
        val file = File(logDir, "$dateTime.log")

        apply {
            logDir.mkdirs()
            file.createNewFile()
        }
        file.printWriter().use { out ->
            out.println(e.stackTraceToString())
        }

        exitProcess(-1)
    }

    companion object {
        const val TAG = "App"

        fun Application.asApp(): App {
            return this as App
        }

        fun App.changeUiMode(mode: Int) {
            if (Build.VERSION.SDK_INT >= 31) {
                val uiModeManager = getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
                uiModeManager.setApplicationNightMode(mode)
            } else {
                AppCompatDelegate.setDefaultNightMode(mode)
            }
        }

        fun App.changeUiMode(mode: String) {
            if (Build.VERSION.SDK_INT >= 31) {
                val modeInt = when (mode.lowercase()) {
                    "light" -> {
                        UiModeManager.MODE_NIGHT_NO
                    }
                    "dark" -> {
                        UiModeManager.MODE_NIGHT_YES
                    }
                    else -> {
                        UiModeManager.MODE_NIGHT_AUTO
                    }
                }

                changeUiMode(modeInt)
            } else {
                val modeInt = when (mode.lowercase()) {
                    "light" -> {
                        AppCompatDelegate.MODE_NIGHT_NO
                    }
                    "dark" -> {
                        AppCompatDelegate.MODE_NIGHT_YES
                    }
                    else -> {
                        AppCompatDelegate.MODE_NIGHT_UNSPECIFIED
                    }
                }

                AppCompatDelegate.setDefaultNightMode(modeInt)
            }
        }
    }
}