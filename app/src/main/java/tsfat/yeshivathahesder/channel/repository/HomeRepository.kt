package tsfat.yeshivathahesder.channel.repository

import android.util.Log
import kotlinx.coroutines.delay
import tsfat.yeshivathahesder.channel.api.ChannelsService
import tsfat.yeshivathahesder.channel.api.PlaylistItemsService
import tsfat.yeshivathahesder.channel.api.SearchVideoService
import tsfat.yeshivathahesder.channel.model.ChannelUploadsPlaylistInfo
import retrofit2.Response
import timber.log.Timber
import tsfat.yeshivathahesder.channel.di.AudioConnector
import tsfat.yeshivathahesder.channel.model.ItemBase
import tsfat.yeshivathahesder.channel.model.ItemList
import tsfat.yeshivathahesder.channel.uamp.AudioItem
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class HomeRepository(
    private val searchVideoService: SearchVideoService,
    private val channelsService: ChannelsService,
    private val playlistItemsService: PlaylistItemsService,
    private val audioConnector: AudioConnector
) {

    suspend fun getUploadsPlaylistId(channelId: String): Response<ChannelUploadsPlaylistInfo> =
        channelsService.getChannelUploadsPlaylistInfo(channelId)

    private var lastIndex = 0

//    private val lock = CountDownLatch(1)
//    private val lock = ReentrantLock()
//    private val cond = lock.newCondition()

    // TODO: add audio here
    suspend fun getLatestVideos(
        playlistId: String,
        pageToken: String?
    ): Response<ItemList<ItemBase>> {
        Timber.d("Getting videos")
        val response = playlistItemsService.getPlaylistVideos(playlistId, pageToken)

//        audioConnector.audioItems.observeForever {
//            if (it != null && it.isNotEmpty())
//                lock.countDown()
//        }

        val body = response.body() ?: ItemList(null, null, emptyList())

        var audioItemList: List<AudioItem>? = audioConnector.audioItems.value
//        if (audioItemList == null || audioItemList.isEmpty()) {
//            Timber.d("HomeRepository", "Waiting for audio items...")
//            lock.withLock {
//                cond.await()
//            }
//            Timber.d("HomeRepository", "Audio items lock released")
//            audioItemList = audioConnector.audioItems.value
//            if (audioItemList == null)
//                Timber.d("HomeRepository", "No audio items!")
//        }
//        if (audioItemList == null || audioItemList.isEmpty()) {
//            Timber.d("Waiting for audio items...")
//            lock.await()
//            Timber.d("Audio items loaded")
//        }

        while (audioItemList == null || audioItemList.isEmpty()) {
            Timber.d("Waiting for audio items...")
            delay(1000)
            audioItemList = audioConnector.audioItems.value
        }

        val allAudioItems: List<AudioItem> = audioItemList?.asReversed() ?: emptyList()

        val lastItem = body.items.last()
        var end = lastIndex

        while (end < allAudioItems.size) {
            if (allAudioItems[end].publishedAt < lastItem.publishedAt) {
                break
            }
            end++
        }
        val audioItems = allAudioItems.subList(lastIndex, end)
        lastIndex = end

        val newList = (audioItems + body.items).sortedByDescending { it.publishedAt }

        if (response.isSuccessful) {
            return Response.success(
                ItemList<ItemBase>(
                    body.nextPageToken,
                    body.prevPageToken,
                    newList
                )
            )
        } else return Response.error(response.errorBody()!!, response.raw())
    }

}