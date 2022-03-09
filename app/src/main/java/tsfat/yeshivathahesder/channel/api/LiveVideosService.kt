package tsfat.yeshivathahesder.channel.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.QueryMap
import tsfat.yeshivathahesder.channel.model.SearchedList

interface LiveVideosService {

//    ?part=snippet&channelId=UCXswCcAMb5bvEUIDEzXFGYg&type=video&eventType=live&key=[API_KEY]

    @GET("search")
    suspend fun getLiveVideos(
        @Query("channelId") channelId: String,
        @QueryMap defaultQueryMap: HashMap<String, String>,
        @Query("type") type: String = "video",
        @Query("eventType") eventType: String = "live"
    ): Response<SearchedList<SearchedList.Item>>
}