package tsfat.yeshivathahesder.channel.api

import tsfat.yeshivathahesder.channel.model.ChannelUploadsPlaylistInfo
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface ChannelsService {

    @GET("channels")
    suspend fun getChannelUploadsPlaylistInfo(
        @Query("id") channelId: String,
        @Query("part") part: String = "contentDetails"
    ): Response<ChannelUploadsPlaylistInfo>
}