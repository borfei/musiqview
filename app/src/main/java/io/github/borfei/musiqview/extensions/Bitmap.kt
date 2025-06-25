package io.github.borfei.musiqview.extensions

import android.graphics.Bitmap
import android.graphics.BitmapFactory

/**
 * Decode the data into BitmapFactory which can then
 * be used to create a proper Bitmap as the return value.
 *
 * If you want to create a empty bitmap, use 1 byte instead.
 *
 * @return[Bitmap]
 */
fun ByteArray.toBitmap(): Bitmap {
    val bitmap = BitmapFactory.decodeByteArray(this, 0, size)
    return bitmap ?: Bitmap.createBitmap(800, 600, Bitmap.Config.ARGB_8888)
}
