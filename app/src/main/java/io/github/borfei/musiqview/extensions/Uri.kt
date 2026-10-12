package io.github.borfei.musiqview.extensions

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns

/**
 * Returns the user-friendly name for [Uri]
 *
 *
 * @param[context] The context to be parsed the [Uri] with. Must not be null
 * @return[String]
 */
fun Uri.getName(context: Context): String? {
    val queriedName = runCatching {
        context.contentResolver.query(this, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && cursor.moveToFirst()) cursor.getString(nameIndex) else null
            }
    }.getOrNull()

    return queriedName?.takeIf { it.isNotBlank() } ?: lastPathSegment
}
