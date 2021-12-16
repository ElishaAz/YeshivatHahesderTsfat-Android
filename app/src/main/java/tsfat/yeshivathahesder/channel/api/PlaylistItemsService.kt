package tsfat.yeshivathahesder.channel.api

import tsfat.yeshivathahesder.channel.model.ItemList
import tsfat.yeshivathahesder.channel.utils.Constants
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import tsfat.yeshivathahesder.channel.model.ItemBase
import tsfat.yeshivathahesder.channel.model.VideoItem

interface PlaylistItemsService {

    @GET("playlistItems")
    suspend fun getPlaylistVideos(
        @Query("playlistId") playlistId: String,
        @Query("pageToken") pageToken: String?,
        @Query("part") part: String = "snippet,contentDetails",
        @Query("fields") fields: String = "nextPageToken, prevPageToken, items(snippet(title, thumbnails), contentDetails(videoId, videoPublishedAt))",
        @Query("maxResults") maxResults: Int = Constants.YT_API_MAX_RESULTS
    ): Response<ItemList<VideoItem>>
}