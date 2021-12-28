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

package tsfat.yeshivathahesder.channel.uamp.viewmodels

import android.support.v4.media.MediaBrowserCompat
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import tsfat.yeshivathahesder.channel.uamp.AudioMainActivity
import tsfat.yeshivathahesder.channel.uamp.AudioItem
import tsfat.yeshivathahesder.channel.uamp.common.MusicServiceConnection
import tsfat.yeshivathahesder.channel.uamp.media.extensions.id
import tsfat.yeshivathahesder.channel.uamp.media.extensions.isPlayEnabled
import tsfat.yeshivathahesder.channel.uamp.media.extensions.isPlaying
import tsfat.yeshivathahesder.channel.uamp.media.extensions.isPrepared
import tsfat.yeshivathahesder.channel.uamp.utils.Event

/**
 * Small [ViewModel] that watches a [MusicServiceConnection] to become connected
 * and provides the root/initial media ID of the underlying [MediaBrowserCompat].
 */
class MainActivityViewModel(
    private val musicServiceConnection: MusicServiceConnection
) : ViewModel() {

    val rootMediaId: LiveData<String> =
        Transformations.map(musicServiceConnection.isConnected) { isConnected ->
            if (isConnected) {
                musicServiceConnection.rootMediaId
            } else {
                null
            }
        }

    val isConnected: LiveData<Boolean> = musicServiceConnection.isConnected

    /**
     * [navigateToMediaItem] acts as an "event", rather than state. [Observer]s
     * are notified of the change as usual with [LiveData], but only one [Observer]
     * will actually read the data. For more information, check the [Event] class.
     */
    val navigateToMediaItem: LiveData<Event<String>> get() = _navigateToMediaItem
    private val _navigateToMediaItem = MutableLiveData<Event<String>>()

    /**
     * This [LiveData] object is used to notify the MainActivity that the main
     * content fragment needs to be swapped. Information about the new fragment
     * is conveniently wrapped by the [Event] class.
     */
    val navigateToFragment: LiveData<Event<FragmentNavigationRequest>> get() = _navigateToFragment
    private val _navigateToFragment = MutableLiveData<Event<FragmentNavigationRequest>>()

    /**
     * This method takes a [AudioItem] and routes it depending on whether it's
     * browsable (i.e.: it's the parent media item of a set of other media items,
     * such as an album), or not.
     *
     * If the item is browsable, handle it by sending an event to the Activity to
     * browse to it, otherwise play it.
     */
    fun mediaItemClicked(clickedItem: AudioItem) {
        if (clickedItem.browsable) {
            browseToItem(clickedItem)
        } else {
            playMedia(clickedItem, pauseAllowed = false)
            showFragment(null, play = true)
        }
    }


    /**
     * Convenience method used to swap the fragment shown in the main activity
     *
     * @param fragment the fragment to show
     * @param backStack if true, add this transaction to the back stack
     * @param tag the name to use for this fragment in the stack
     */
    fun showFragment(
        fragment: Fragment?,
        backStack: Boolean = true,
        tag: String? = null,
        play: Boolean = false
    ) {
        _navigateToFragment.value = Event(FragmentNavigationRequest(fragment, backStack, tag, play))
    }


    /**
     * This posts a browse [Event] that will be handled by the
     * observer in [AudioMainActivity].
     */
    private fun browseToItem(audioItem: AudioItem) {
        _navigateToMediaItem.value = Event(audioItem.mediaId)
    }

    /**
     * This method takes a [AudioItem] and does one of the following:
     * - If the item is *not* the active item, then play it directly.
     * - If the item *is* the active item, check whether "pause" is a permitted command. If it is,
     *   then pause playback, otherwise send "play" to resume playback.
     */
    fun playMedia(audioItem: AudioItem, pauseAllowed: Boolean = true) {
        val nowPlaying = musicServiceConnection.nowPlaying.value
        val transportControls = musicServiceConnection.transportControls

        val isPrepared = musicServiceConnection.playbackState.value?.isPrepared ?: false
        if (isPrepared && audioItem.mediaId == nowPlaying?.id) {
            musicServiceConnection.playbackState.value?.let { playbackState ->
                when {
                    playbackState.isPlaying ->
                        if (pauseAllowed) transportControls.pause() else Unit
                    playbackState.isPlayEnabled -> transportControls.play()
                    else -> {
                        Log.w(
                            TAG, "Playable item clicked but neither play nor pause are enabled!" +
                                    " (mediaId=${audioItem.mediaId})"
                        )
                    }
                }
            }
        } else {
            transportControls.playFromMediaId(audioItem.mediaId, null)
        }
    }

    fun seekTo(duration: Long) {
        musicServiceConnection.transportControls.seekTo(duration)
    }

    fun skipBy(duration: Long) {
        musicServiceConnection.transportControls.seekTo(
            musicServiceConnection.playbackState.value?.position ?: 0 + duration
        )
    }

    fun skipToNext() {
        musicServiceConnection.transportControls.skipToNext()
    }

    fun skipToPrevious() {
        musicServiceConnection.transportControls.skipToPrevious()
    }

    fun stop() {
        musicServiceConnection.transportControls.stop()
    }

    fun playMediaId(mediaId: String, pauseAllowed: Boolean = true) {
        val nowPlaying = musicServiceConnection.nowPlaying.value
        val transportControls = musicServiceConnection.transportControls

        val isPrepared = musicServiceConnection.playbackState.value?.isPrepared ?: false
        if (isPrepared && mediaId == nowPlaying?.id) {
            musicServiceConnection.playbackState.value?.let { playbackState ->
                when {
                    playbackState.isPlaying -> if (pauseAllowed) transportControls.pause() else Unit
                    playbackState.isPlayEnabled -> transportControls.play()
                    else -> {
                        Log.w(
                            TAG, "Playable item clicked but neither play nor pause are enabled!" +
                                    " (mediaId=$mediaId)"
                        )
                    }
                }
            }
        } else {
            transportControls.playFromMediaId(mediaId, null)
        }
    }

    fun pause() {
        musicServiceConnection.transportControls.pause()
    }

    class Factory(
        private val musicServiceConnection: MusicServiceConnection
    ) : ViewModelProvider.NewInstanceFactory() {

        @Suppress("unchecked_cast")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return MainActivityViewModel(musicServiceConnection) as T
        }
    }
}

/**
 * Helper class used to pass fragment navigation requests between MainActivity
 * and its corresponding ViewModel.
 */
data class FragmentNavigationRequest(
    val fragment: Fragment?,
    val backStack: Boolean = false,
    val tag: String? = null,
    val play: Boolean = false
)

private const val TAG = "MainActivitytVM"
