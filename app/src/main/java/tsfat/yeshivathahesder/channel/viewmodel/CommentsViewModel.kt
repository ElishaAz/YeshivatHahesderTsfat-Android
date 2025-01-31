package tsfat.yeshivathahesder.channel.viewmodel

import tsfat.yeshivathahesder.channel.model.Comment
import tsfat.yeshivathahesder.channel.paging.NetworkState
import tsfat.yeshivathahesder.channel.paging.datasourcefactory.CommentsDataSourceFactory
import tsfat.yeshivathahesder.channel.repository.CommentsRepository
import tsfat.yeshivathahesder.channel.utils.Constants
import android.content.Context
import androidx.lifecycle.*
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import kotlinx.coroutines.launch

class CommentsViewModel(
    private val commentsRepository: CommentsRepository,
    private val context: Context
) : ViewModel() {

    lateinit var commentsDataSourceFactory: CommentsDataSourceFactory
    var commentsLiveData: LiveData<PagedList<Comment.Item>>? = null
    var networkStateLiveData: LiveData<NetworkState>? = null
    private var _emptyStateLiveData = MutableLiveData<Boolean>()
    val emptyStateLiveData: LiveData<Boolean>
        get() = _emptyStateLiveData


    fun getVideoComments(videoId: String, sortOrder: String) {
        if (commentsLiveData == null) {
            viewModelScope.launch {
                    commentsDataSourceFactory =
                        CommentsDataSourceFactory(
                            commentsRepository,
                            viewModelScope,
                            videoId,
                            sortOrder
                        )

                    commentsLiveData = LivePagedListBuilder(commentsDataSourceFactory, pagedListConfig())
                        .setBoundaryCallback(object :
                            PagedList.BoundaryCallback<Comment.Item>() {
                            override fun onZeroItemsLoaded() {
                                super.onZeroItemsLoaded()
                                _emptyStateLiveData.value = true
                            }

                            override fun onItemAtFrontLoaded(itemAtFront: Comment.Item) {
                                super.onItemAtFrontLoaded(itemAtFront)
                                _emptyStateLiveData.value = false
                            }

                            override fun onItemAtEndLoaded(itemAtEnd: Comment.Item) {
                                super.onItemAtEndLoaded(itemAtEnd)
                                _emptyStateLiveData.value = false
                            }
                        })
                        .build()
                    networkStateLiveData = Transformations.switchMap(commentsDataSourceFactory.commentsDataSourceLiveData) { it.getNetworkState() }
            }
        }

    }

    fun sortComments(updatedSortOrder: String) {
        commentsDataSourceFactory.setSortOrder(updatedSortOrder)
        commentsDataSourceFactory.commentsDataSourceLiveData.value?.invalidate()
        }

    /**
     * Retry possible last paged request (ie: network issue)
     */
    fun refreshFailedRequest() =
        commentsDataSourceFactory.getSource()?.retryFailedQuery()

    private fun pagedListConfig() = PagedList.Config.Builder()
        .setInitialLoadSizeHint(Constants.INITIAL_PAGE_LOAD_SIZE)
        .setEnablePlaceholders(false)
        .setPageSize(Constants.PAGE_SIZE)
        .build()
}
