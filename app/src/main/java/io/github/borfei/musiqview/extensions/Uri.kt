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
    val returnCursor = context.contentResolver.query(this, null, null, null, null)
    val nameIndex = returnCursor?.getColumnIndex(OpenableColumns.DISPLAY_NAME)
    returnCursor?.moveToFirst()
    val fileName = nameIndex?.let { returnCursor.getString(it) }
    returnCursor?.close()
    return fileName
}
