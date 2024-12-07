package io.github.feivegian.music.extensions

import android.util.Patterns

fun String.isWebUrl(): Boolean {
    return Patterns.WEB_URL.matcher(this).matches()
}