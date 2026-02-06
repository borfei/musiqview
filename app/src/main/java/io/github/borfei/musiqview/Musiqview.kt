package io.github.borfei.musiqview

import android.app.Application
import com.google.android.material.color.DynamicColors
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.system.exitProcess

class Musiqview : Application(), Thread.UncaughtExceptionHandler {
    private var defaultUncaughtExceptionHandler: Thread.UncaughtExceptionHandler? = null

    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
        defaultUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    override fun uncaughtException(t: Thread, e: Throwable) {
        generateErrorLog(e)
        defaultUncaughtExceptionHandler?.uncaughtException(t, e)
        exitProcess(-1)
    }

    private fun generateErrorLog(e: Throwable) {
        val dateTime = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val directory = File(dataDir, "crash")
        val file = File(directory, "$dateTime.log")

        apply {
            directory.mkdirs()
            file.createNewFile()
        }
        file.printWriter().use { out ->
            out.println(e.stackTraceToString())
        }
    }
}