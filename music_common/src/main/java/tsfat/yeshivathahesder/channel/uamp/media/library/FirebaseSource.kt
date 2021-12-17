package tsfat.yeshivathahesder.channel.uamp.media.library

import android.content.Context
import android.net.Uri
import android.support.v4.media.MediaMetadataCompat
import android.util.Log
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import tsfat.yeshivathahesder.channel.uamp.media.extensions.albumArtUri
import tsfat.yeshivathahesder.channel.uamp.media.extensions.displayIconUri
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.URL
import java.util.*

class FirebaseSource(private val context: Context) : AbstractMusicSource() {
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
            var musicCat = try {
                downloadCatalog(context)
            } catch (ioException: IOException) {
                return@withContext null
            }
            val now = Date().time
            val timestamp = musicCat.timestamp?.time ?: 0
            if (musicCat.music.isNullOrEmpty() || now - timestamp > waitForDownloadAfter) {
                Log.d(TAG, "Catalog too old. Reloading from Drive.")
                uploadCatalog(context)?.let { musicCat = it }
            } else if (now - timestamp > uploadAfter) {
                Log.d(TAG, "Reloading catalog from Drive non-blocking.")
                launch(Dispatchers.IO) {
                    uploadCatalog(context)
                }
            }

            val mediaMetadataCompats = musicCat.music.map { song ->
                if (song.source.isBlank()) {
                    song.source = fileIdToUri(context, song.id)
                }

                val imageUri = AlbumArtContentProvider.mapUri(Uri.parse(song.image))

                MediaMetadataCompat.Builder()
                        .from(song)
                        .apply {
                            displayIconUri = imageUri.toString() // Used by ExoPlayer and Notification
                            albumArtUri = imageUri.toString()
                        }
                        .build()
            }.toList()
            // Add description keys to be used by the ExoPlayer MediaSession extension when
            // announcing metadata changes.
            mediaMetadataCompats.forEach { it.description.extras?.putAll(it.bundle) }
            mediaMetadataCompats
        }
    }


}

private const val TAG = "FirebaseSource"
private const val uploadAfter = 60 * 60 * 1000
private const val waitForDownloadAfter = 2 * 24 * 60 * 60 * 10000