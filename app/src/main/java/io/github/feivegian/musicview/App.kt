package io.github.feivegian.musicview

import android.annotation.SuppressLint
import android.app.Application
import android.content.SharedPreferences
import android.util.Log
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.preference.PreferenceManager
import com.google.android.material.color.DynamicColors
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.system.exitProcess

@SuppressLint("UnsafeOptInUsageError")
class App : Application(), SharedPreferences.OnSharedPreferenceChangeListener, Thread.UncaughtExceptionHandler {
    val preferences: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(this)
    }
    val databaseProvider: StandaloneDatabaseProvider by lazy {
        StandaloneDatabaseProvider(this)
    }

    override fun onCreate() {
        super.onCreate()
        // Initialize dynamic colors by default
        DynamicColors.applyToActivitiesIfAvailable(this)
        // Initialize custom uncaught exception handler
        Thread.setDefaultUncaughtExceptionHandler(this)
        Log.i(TAG, "Uncaught exceptions will now be handled by this instance")
        // Register custom shared preference change listener
        preferences.registerOnSharedPreferenceChangeListener(this)
        Log.i(TAG, "Registered a custom OnSharedPreferenceChangeListener")
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        Log.d(TAG, "Preference changed: $key")
    }

    override fun uncaughtException(t: Thread, e: Throwable) {
        generateTraceLog(e)
        exitProcess(-1)
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
    }

    companion object {
        const val TAG = "App"

        /**
         * A simple conversion from [Application] class to [App]
         *
         * @param[application] The instance that will convert to, must not be null.
         * @return[App]
         */
        fun fromInstance(application: Application): App {
            return application as App
        }
    }
}