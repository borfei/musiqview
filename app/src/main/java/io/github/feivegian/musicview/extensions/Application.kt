package io.github.feivegian.musicview.extensions

import android.app.Application
import android.app.UiModeManager
import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate

/**
 * Changes the night mode of the [Application] instance
 *
 * @param[mode] The new mode, must be a value of [UiModeManager] or [AppCompatDelegate]
 */
fun Application.changeNightMode(mode: Int) {
    if (Build.VERSION.SDK_INT >= 31) {
        val uiModeManager = getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        uiModeManager.setApplicationNightMode(mode)
    } else {
        AppCompatDelegate.setDefaultNightMode(mode)
    }
}

/**
 * Changes the night mode of the [Application] instance
 *
 * This is a cast-as method, which calls the Int-variant [changeNightMode]
 * @param[mode] The new mode, accepted values are "light", "dark", "auto"
 */
fun Application.changeNightMode(mode: String) {
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

        changeNightMode(modeInt)
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