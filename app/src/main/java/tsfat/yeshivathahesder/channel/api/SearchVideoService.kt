package tsfat.yeshivathahesder.channel.api

import tsfat.yeshivathahesder.channel.model.SearchedList
import tsfat.yeshivathahesder.channel.utils.Constants
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.QueryMap

interface SearchVideoService {

    @GET("search")
    suspend fun searchVideos(
        @Query("q") searchQuery: String,
        @Query("channelId") channelId: String,
        @Query("pageToken") pageToken: String?,
        @QueryMap defaultQueryMap: HashMap<String, String>,
        @Query("maxResults") maxResults: Int = Constants.YT_API_MAX_RESULTS
    ): Response<SearchedList<SearchedList.Item>>
}