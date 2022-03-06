package tsfat.yeshivathahesder.channel.uamp.media.library

import android.content.Context
import android.net.Uri
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tsfat.yeshivathahesder.channel.uamp.R
import tsfat.yeshivathahesder.channel.uamp.media.extensions.*
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit

class FirebaseSource(private val context: Context, private val serviceScope: CoroutineScope) :
    AbstractMusicSource() {
    private var catalog: List<MediaMetadataCompat> = emptyList()

    init {
        state = STATE_INITIALIZING
    }

    override fun iterator(): Iterator<MediaMetadataCompat> = catalog.iterator()

    override suspend fun load() {
        updateCatalog()?.let { updatedCatalog ->
            catalog = updatedCatalog
            state = STATE_INITIALIZED
        } ?: run {
            catalog = emptyList()
            state = STATE_ERROR
        }
    }

    /**
     * Function to connect to a remote URI and download/process the JSON file that corresponds to
     * [MediaMetadataCompat] objects.
     */
    private suspend fun updateCatalog(): List<MediaMetadataCompat>? {
        return withContext(Dispatchers.IO) {
            val catalog = downloadCatalogSDK(context) ?: downloadCatalogREST(context)
            ?: return@withContext null

            val mediaMetadataCompats = catalog.lessons.map { song ->
                if (song.source.isBlank()) {
                    song.source = fileIdToUri(context, song.id)
                }

                MediaMetadataCompat.Builder()
                    .from(song)
                    .apply {
                        val imageUri = AlbumArtContentProvider.mapUri(Uri.parse(song.image))
                        displayIconUri = imageUri.toString() // Used by ExoPlayer and Notification
                        albumArtUri = imageUri.toString()
                    }
                    .build()
            }.toMutableList()
            // Add description keys to be used by the ExoPlayer MediaSession extension when
            // announcing metadata changes.
            mediaMetadataCompats.forEach { it.description.extras?.putAll(it.bundle) }
            mediaMetadataCompats.add(MediaMetadataCompat.Builder()
                .from(
                    CatalogItem(UAMP_INFO_ID, createdTime = UAMP_INFO_TIME_CREATED),
                    mFlag = MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
                )
                .apply {
                    displayIconUri = ""
                    albumArtUri = ""
                }
                .build())
            mediaMetadataCompats
        }
    }
}

/**
 * Extension method for [MediaMetadataCompat.Builder] to set the fields from
 * our JSON constructed object (to make the code a bit easier to see).
 */
fun MediaMetadataCompat.Builder.from(
    driveMusic: CatalogItem,
    mFlag: Int = MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
): MediaMetadataCompat.Builder {
    // The duration from the JSON is given in seconds, but the rest of the code works in
    // milliseconds. Here's where we convert to the proper units.
    val durationMs = TimeUnit.SECONDS.toMillis(/*driveMusic.duration*/ -1)

    id = driveMusic.id
    title = driveMusic.title
    artist = driveMusic.folder
    album = driveMusic.folder
//    duration = durationMs
//    genre = /*driveMusic.genre*/ ""
    mediaUri = driveMusic.source
    albumArtUri = driveMusic.image
//    trackNumber = /*driveMusic.trackNumber*/ 0
//    trackCount = /*driveMusic.totalTrackCount*/ 0
    flag = mFlag

    // To make things easier for *displaying* these, set the display properties as well.
    displayTitle = driveMusic.title
    displaySubtitle = driveMusic.folder
    displayDescription = /*driveMusic.album*/ "Yeshivat HaHesder Tsfat"
    displayIconUri = driveMusic.image

    // Add downloadStatus to force the creation of an "extras" bundle in the resulting
    // MediaMetadataCompat object. This is needed to send accurate metadata to the
    // media session during updates.
    downloadStatus = MediaDescriptionCompat.STATUS_NOT_DOWNLOADED
    putString(MediaMetadataCompat.METADATA_KEY_DATE, driveMusic.createdTime)

    // Allow it to be used in the typical builder style.
    return this
}

fun fileIdToUri(context: Context, id: String): String {
    return context.getString(
        R.string.google_drive_link_template,
        id
    )
}

private const val TAG = "FirebaseSource"