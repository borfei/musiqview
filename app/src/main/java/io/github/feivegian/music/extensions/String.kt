package io.github.feivegian.music.extensions

import android.util.Patterns

/**
 * Checks whether the String is a valid web URL.
 *
 * @return[Boolean]
 */
fun String.isWebUrl(): Boolean {
    return Patterns.WEB_URL.matcher(this).matches()
}