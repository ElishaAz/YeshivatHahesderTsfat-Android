package tsfat.yeshivathahesder.channel.repository

import tsfat.yeshivathahesder.channel.api.CommentRepliesService
import tsfat.yeshivathahesder.channel.model.CommentReply
import retrofit2.Response

class CommentRepliesRepository(private val commentRepliesService: CommentRepliesService) {

    suspend fun getCommentReplies(commentId: String, pageToken: String?): Response<CommentReply> =
        commentRepliesService.getCommentReplies(commentId, pageToken)
}