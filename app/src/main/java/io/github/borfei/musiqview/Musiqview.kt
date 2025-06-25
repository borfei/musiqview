package io.github.borfei.musiqview

import android.annotation.SuppressLint
import android.app.Application
import com.google.android.material.color.DynamicColors
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.system.exitProcess

@SuppressLint("UnsafeOptInUsageError")
class Musiqview : Application(), Thread.UncaughtExceptionHandler {
    companion object {
        /**
         * A simple conversion from [Application] class to [Musiqview]
         *
         * @param[application] The instance that will convert to, must not be null.
         * @return[Musiqview]
         */
        fun fromInstance(application: Application): Musiqview {
            return application as Musiqview
        }
    }

    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
        Thread.setDefaultUncaughtExceptionHandler(this)
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
}