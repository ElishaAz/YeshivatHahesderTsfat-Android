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
import tsfat.yeshivathahesder.channel.repository.AudioRepository
import tsfat.yeshivathahesder.channel.uamp.AudioItem
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class HomeRepository(
    private val searchVideoService: SearchVideoService,
    private val channelsService: ChannelsService,
    private val playlistItemsService: PlaylistItemsService,
    private val audioRepository: AudioRepository
) {

    suspend fun getUploadsPlaylistId(channelId: String): Response<ChannelUploadsPlaylistInfo> =
        channelsService.getChannelUploadsPlaylistInfo(channelId)

    private var lastIndex = 0

    suspend fun getLatestVideos(
        playlistId: String,
        pageToken: String?
    ): Response<ItemList<ItemBase>> {
        Timber.d("Getting videos. Token: $pageToken")

        val response = playlistItemsService.getPlaylistVideos(playlistId, pageToken)

        val nextPageToken = response.body()?.nextPageToken

        val lastItem = response.body()?.items?.last()

        val audioItems: List<AudioItem> =
            audioRepository.getAudioItems(pageToken, nextPageToken, lastItem?.publishedAt ?: "")

        return joinAudioAndVideoResults(response, audioItems)
    }
}