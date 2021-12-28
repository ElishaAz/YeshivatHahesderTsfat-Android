/*
 * Copyright 2018 Google Inc. All rights reserved.
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

package tsfat.yeshivathahesder.channel.uamp

import android.net.Uri
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaBrowserCompat.MediaItem
import androidx.recyclerview.widget.DiffUtil
import tsfat.yeshivathahesder.channel.model.ItemBase
import tsfat.yeshivathahesder.channel.model.PlaylistBase
import tsfat.yeshivathahesder.channel.model.audioIdToBase
import tsfat.yeshivathahesder.channel.uamp.viewmodels.MediaItemFragmentViewModel

/**
 * Data class to encapsulate properties of a [MediaItem].
 *
 * If an item is [browsable] it means that it has a list of child media items that
 * can be retrieved by passing the mediaId to [MediaBrowserCompat.subscribe].
 *
 * Objects of this class are built from [MediaItem]s in
 * [MediaItemFragmentViewModel.subscriptionCallback].
 */
data class AudioItem(
    val mediaId: String,
    val title: String,
    val subtitle: String,
    val albumArtUri: Uri,
    val browsable: Boolean,
    override val publishedAt: String,
    var playbackRes: Int
) : ItemBase() {

    companion object {
        /**
         * Indicates [playbackRes] has changed.
         */
        const val PLAYBACK_RES_CHANGED = 1

        /**
         * [DiffUtil.ItemCallback] for a [AudioItem].
         *
         * Since all [AudioItem]s have a unique ID, it's easiest to check if two
         * items are the same by simply comparing that ID.
         *
         * To check if the contents are the same, we use the same ID, but it may be the
         * case that it's only the play state itself which has changed (from playing to
         * paused, or perhaps a different item is the active item now). In this case
         * we check both the ID and the playback resource.
         *
         * To calculate the payload, we use the simplest method possible:
         * - Since the title, subtitle, and albumArtUri are constant (with respect to mediaId),
         *   there's no reason to check if they've changed. If the mediaId is the same, none of
         *   those properties have changed.
         * - If the playback resource (playbackRes) has changed to reflect the change in playback
         *   state, that's all that needs to be updated. We return [PLAYBACK_RES_CHANGED] as
         *   the payload in this case.
         * - If something else changed, then refresh the full item for simplicity.
         */
        val diffCallback = object : DiffUtil.ItemCallback<AudioItem>() {
            override fun areItemsTheSame(
                oldItem: AudioItem,
                newItem: AudioItem
            ): Boolean =
                oldItem.mediaId == newItem.mediaId

            override fun areContentsTheSame(oldItem: AudioItem, newItem: AudioItem) =
                oldItem.mediaId == newItem.mediaId && oldItem.playbackRes == newItem.playbackRes

            override fun getChangePayload(oldItem: AudioItem, newItem: AudioItem) =
                if (oldItem.playbackRes != newItem.playbackRes) {
                    PLAYBACK_RES_CHANGED
                } else null
        }
    }

    override val baseId: String = audioIdToBase(mediaId)

    val fullTitle: String = "$title | $subtitle"

    override fun toString(): String = "AudioItem($fullTitle)"
}

data class AudioPlaylist(
    val mediaId: String,
    val title: String,
    val items: List<AudioItem>,
//    val albumArtUri: Uri,
    override val itemCount: Int,
    override val publishedAt: String
) : PlaylistBase() {
    override val baseId: String = audioIdToBase(mediaId)
    override val name: String = title

    override fun toString(): String {
        return "AudioPlaylist(title='$title')"
    }
}