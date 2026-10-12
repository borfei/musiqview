package io.github.borfei.musiqview

import android.content.Intent
import android.net.Uri
import androidx.media3.common.MediaItem

internal object PlaybackIntent {
    private val supportedSchemes = setOf("content", "file")

    fun mediaItem(intent: Intent?): MediaItem? {
        if (intent?.action != Intent.ACTION_VIEW) return null

        val uri = intent.data ?: return null
        if (uri.scheme?.lowercase() !in supportedSchemes) return null

        val mimeType = intent.type
        if (mimeType != null && !mimeType.startsWith("audio/", ignoreCase = true)) {
            return null
        }

        return MediaItem.Builder()
            .setUri(uri)
            .apply { mimeType?.let(::setMimeType) }
            .build()
    }

    fun isSupportedUri(uri: Uri?): Boolean =
        uri?.scheme?.lowercase()?.let { it in supportedSchemes } == true
}
