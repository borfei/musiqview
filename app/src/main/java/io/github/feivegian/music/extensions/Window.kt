package io.github.feivegian.music.extensions

import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Sets the window's immersive mode to be enabled.
 *
 * @param[toggle] State of the mode
 */
fun WindowInsetsControllerCompat.setImmersiveMode(toggle: Boolean) {
    if (toggle) {
        systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        hide(WindowInsetsCompat.Type.systemBars())
    } else {
        show(WindowInsetsCompat.Type.systemBars())
    }
}