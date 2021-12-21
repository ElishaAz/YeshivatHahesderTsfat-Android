package tsfat.yeshivathahesder.channel.api

import tsfat.yeshivathahesder.channel.model.Playlists
import tsfat.yeshivathahesder.channel.utils.Constants
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface PlaylistsService {

    @GET("playlists")
    suspend fun getPlaylists(
        @Query("channelId") channelId: String,
        @Query("pageToken") pageToken: String?,
        @Query("part") part: String = "snippet,contentDetails",
        @Query("fields") fields: String = "nextPageToken, items(id, snippet(publishedAt, title, description, thumbnails), contentDetails)",
        @Query("maxResults") maxResults: Int = Constants.YT_API_MAX_RESULTS
    ): Response<Playlists<Playlists.VideoPlaylist>>
}