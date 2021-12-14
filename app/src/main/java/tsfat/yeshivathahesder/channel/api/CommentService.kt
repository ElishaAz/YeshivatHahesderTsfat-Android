package tsfat.yeshivathahesder.channel.api

import tsfat.yeshivathahesder.channel.model.Comment
import tsfat.yeshivathahesder.channel.utils.Constants
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface CommentService {

    /**
     * Gets the list of comments of a particular video
     */
    @GET("commentThreads")
    suspend fun getVideoComments(
        @Query("videoId") videoId: String,
        @Query("pageToken") pageToken: String?,
        @Query("order") sortOrder: String,
        @Query("part") part: String = "snippet",
        @Query("fields") fields: String = "nextPageToken, items(snippet(topLevelComment(id, snippet(authorDisplayName, authorProfileImageUrl, textOriginal, likeCount, publishedAt, updatedAt)), totalReplyCount))",
        @Query("maxResults") maxResults: Int = Constants.YT_API_MAX_RESULTS
    ): Response<Comment>
}