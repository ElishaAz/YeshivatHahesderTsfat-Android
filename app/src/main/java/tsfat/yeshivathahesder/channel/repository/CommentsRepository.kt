package tsfat.yeshivathahesder.channel.repository

import tsfat.yeshivathahesder.channel.api.CommentService
import tsfat.yeshivathahesder.channel.model.Comment
import retrofit2.Response

class CommentsRepository(private val commentService: CommentService) {

    suspend fun getVideoComments(videoId: String, pageToken: String?, sortOrder: String): Response<Comment> =
        commentService.getVideoComments(videoId, pageToken, sortOrder)
}