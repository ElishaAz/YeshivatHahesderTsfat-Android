/*
 * Copyright 2017 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package tsfat.yeshivathahesder.channel.uamp.media.library

import android.content.Context
import android.net.Uri
import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.support.v4.media.MediaDescriptionCompat.STATUS_NOT_DOWNLOADED
import android.support.v4.media.MediaMetadataCompat
import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tsfat.yeshivathahesder.channel.uamp.R
import tsfat.yeshivathahesder.channel.uamp.media.extensions.*
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.lang.StringBuilder
import java.net.URL
import java.net.UnknownHostException
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList

/**
 * Source of [MediaMetadataCompat] objects created from a basic JSON stream.
 *
 * The definition of the JSON is specified in the docs of [DriveMusic] in this file,
 * which is the object representation of it.
 */
class DriveSource(private val context: Context) :
        AbstractMusicSource() {

    private var catalog: List<MediaMetadataCompat> = emptyList()

    init {
        state = STATE_INITIALIZING
    }

    override fun iterator(): Iterator<MediaMetadataCompat> = catalog.iterator()

    override suspend fun load() {
        updateCatalog()?.let { updatedCatalog ->
            catalog = updatedCatalog.sortedBy { it.date }
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
            val musicCat = try {
                downloadCatalogRecursive(context)
            } catch (e: IOException) {
                Log.e("DriveSource", e.stackTraceToString())
                return@withContext null
            } catch (e: UnknownHostException) {
                Log.e("DriveSource", e.stackTraceToString())
                return@withContext null
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

/**
 * Extension method for [MediaMetadataCompat.Builder] to set the fields from
 * our JSON constructed object (to make the code a bit easier to see).
 */
fun MediaMetadataCompat.Builder.from(driveMusic: DriveMusic): MediaMetadataCompat.Builder {
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
    flag = MediaItem.FLAG_PLAYABLE

    // To make things easier for *displaying* these, set the display properties as well.
    displayTitle = driveMusic.title
    displaySubtitle = driveMusic.folder
    displayDescription = /*driveMusic.album*/ "Yeshivat HaHesder Tsfat"
    displayIconUri = driveMusic.image

    // Add downloadStatus to force the creation of an "extras" bundle in the resulting
    // MediaMetadataCompat object. This is needed to send accurate metadata to the
    // media session during updates.
    downloadStatus = STATUS_NOT_DOWNLOADED
    putString(MediaMetadataCompat.METADATA_KEY_DATE, driveMusic.createdTime)

    // Allow it to be used in the typical builder style.
    return this
}