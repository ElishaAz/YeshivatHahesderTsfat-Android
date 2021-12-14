package tsfat.yeshivathahesder.channel.paging.datasourcefactory

import tsfat.yeshivathahesder.channel.model.Comment
import tsfat.yeshivathahesder.channel.paging.datasource.CommentsDataSource
import tsfat.yeshivathahesder.channel.repository.CommentsRepository
import androidx.lifecycle.MutableLiveData
import androidx.paging.DataSource
import kotlinx.coroutines.CoroutineScope

class CommentsDataSourceFactory(
    private val commentsRepository: CommentsRepository,
    private val coroutineScope: CoroutineScope,
    private val videoId: String,
    private var sortOrder: String
) : DataSource.Factory<String, Comment.Item>() {

    private var commentsDataSource: CommentsDataSource? = null
    val commentsDataSourceLiveData = MutableLiveData<CommentsDataSource>()

    override fun create(): DataSource<String, Comment.Item> {
    // Also called every time when invalidate() is executed
            commentsDataSource =
                CommentsDataSource(
                    commentsRepository,
                    coroutineScope,
                    videoId,
                    sortOrder
                )
            commentsDataSourceLiveData.postValue(commentsDataSource)

        return commentsDataSource!!
    }

    fun setSortOrder(updatedSortOrder: String) {
        sortOrder = updatedSortOrder
    }

    fun getSource() = commentsDataSourceLiveData.value
}