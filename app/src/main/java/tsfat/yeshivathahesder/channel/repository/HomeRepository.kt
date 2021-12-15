package tsfat.yeshivathahesder.channel.repository

import tsfat.yeshivathahesder.channel.api.ChannelsService
import tsfat.yeshivathahesder.channel.api.PlaylistItemsService
import tsfat.yeshivathahesder.channel.api.SearchVideoService
import tsfat.yeshivathahesder.channel.model.ChannelUploadsPlaylistInfo
import retrofit2.Response

class HomeRepository(
    private val searchVideoService: SearchVideoService,
    private val channelsService: ChannelsService,
    private val playlistItemsService: PlaylistItemsService
) {

    suspend fun getUploadsPlaylistId(channelId: String): Response<ChannelUploadsPlaylistInfo> =
        channelsService.getChannelUploadsPlaylistInfo(channelId)

    // TODO: add audio here
    suspend fun getLatestVideos(playlistId: String, pageToken: String?) =
        playlistItemsService.getPlaylistVideos(playlistId, pageToken)

}