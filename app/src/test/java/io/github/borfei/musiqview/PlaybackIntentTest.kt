package io.github.borfei.musiqview

import android.content.Intent
import android.net.Uri
import io.github.borfei.musiqview.extensions.getName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PlaybackIntentTest {
    @Test
    fun createsAudioItemFromContentUri() {
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(Uri.parse("content://media/external/audio/1"), "audio/mpeg")

        val item = PlaybackIntent.mediaItem(intent)

        assertEquals(Uri.parse("content://media/external/audio/1"), item?.localConfiguration?.uri)
        assertEquals("audio/mpeg", item?.localConfiguration?.mimeType)
    }

    @Test
    fun createsItemFromFileUriWithoutMimeType() {
        val intent = Intent(Intent.ACTION_VIEW)
            .setData(Uri.parse("file:///music/song.mp3"))

        val item = PlaybackIntent.mediaItem(intent)

        assertEquals(Uri.parse("file:///music/song.mp3"), item?.localConfiguration?.uri)
        assertNull(item?.localConfiguration?.mimeType)
    }

    @Test
    fun rejectsUnsupportedIntent() {
        assertNull(PlaybackIntent.mediaItem(Intent(Intent.ACTION_SEND).setData(Uri.parse("file:///song.mp3"))))
        assertNull(
            PlaybackIntent.mediaItem(
                Intent(Intent.ACTION_VIEW)
                    .setDataAndType(Uri.parse("content://media/1"), "text/plain")
            )
        )
        assertNull(PlaybackIntent.mediaItem(Intent(Intent.ACTION_VIEW)))
    }

    @Test
    fun recognizesOnlySupportedUriSchemes() {
        assertTrue(PlaybackIntent.isSupportedUri(Uri.parse("content://media/1")))
        assertTrue(PlaybackIntent.isSupportedUri(Uri.parse("file:///song.mp3")))
        assertTrue(!PlaybackIntent.isSupportedUri(Uri.parse("https://example.test/song.mp3")))
        assertTrue(!PlaybackIntent.isSupportedUri(null))
    }

    @Test
    fun fallsBackToPathNameWhenUriHasNoDisplayName() {
        val context = RuntimeEnvironment.getApplication()

        assertEquals(
            "song.mp3",
            Uri.parse("file:///music/song.mp3").getName(context)
        )
    }
}
