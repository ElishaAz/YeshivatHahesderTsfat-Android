package tsfat.yeshivathahesder.channel.viewmodel

import tsfat.yeshivathahesder.channel.model.CommentReply
import tsfat.yeshivathahesder.channel.paging.NetworkState
import tsfat.yeshivathahesder.channel.paging.datasourcefactory.CommentRepliesDataSourceFactory
import tsfat.yeshivathahesder.channel.repository.CommentRepliesRepository
import tsfat.yeshivathahesder.channel.utils.Constants
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import kotlinx.coroutines.launch

class CommentRepliesViewModel(private val commentRepliesRepository: CommentRepliesRepository) :
    ViewModel() {

    lateinit var commentRepliesDataSourceFactory: CommentRepliesDataSourceFactory
    var commentRepliesLiveData: LiveData<PagedList<CommentReply.Item>>? = null
    var networkStateLiveData: LiveData<NetworkState>? = null

    fun getCommentReplies(commentId: String) {
        if (commentRepliesLiveData == null) {
            viewModelScope.launch {
                commentRepliesDataSourceFactory =
                    CommentRepliesDataSourceFactory(
                        commentRepliesRepository,
                        viewModelScope,
                        commentId
                    )

                commentRepliesLiveData =
                    LivePagedListBuilder(commentRepliesDataSourceFactory, pagedListConfig()).build()
                networkStateLiveData =
                    Transformations.switchMap(commentRepliesDataSourceFactory.commentRepliesDataSourceLiveData) { it.getNetworkState() }
            }
        }

    }

    /**
     * Retry possible last paged request (ie: network issue)
     */
    fun refreshFailedRequest() =
        commentRepliesDataSourceFactory.getSource()?.retryFailedQuery()

    private fun pagedListConfig() = PagedList.Config.Builder()
        .setInitialLoadSizeHint(Constants.INITIAL_PAGE_LOAD_SIZE)
        .setEnablePlaceholders(false)
        .setPageSize(Constants.PAGE_SIZE)
        .build()
}