package io.github.feivegian.music.utils

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.app.UiModeManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity.ALARM_SERVICE
import androidx.appcompat.app.AppCompatDelegate
import io.github.feivegian.music.App
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.system.exitProcess

fun Application.convert(): App {
    return this as App
}

fun App.setNightMode(value: String) {
    if (Build.VERSION.SDK_INT >= 31) {
        val uiModeManager = getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        val valueMode = when (value.lowercase()) {
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

        uiModeManager.setApplicationNightMode(valueMode)
    } else {
        val valueMode = when (value.lowercase()) {
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

        AppCompatDelegate.setDefaultNightMode(valueMode)
    }
}

fun App.generateTraceLog(e: Throwable) {
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