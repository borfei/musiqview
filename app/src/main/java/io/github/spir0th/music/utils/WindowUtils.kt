package io.github.spir0th.music.utils

import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

fun WindowInsetsControllerCompat.setImmersiveMode(toggle: Boolean) {
    if (toggle) {
        systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        hide(WindowInsetsCompat.Type.systemBars())
    } else {
        show(WindowInsetsCompat.Type.systemBars())
    }
}