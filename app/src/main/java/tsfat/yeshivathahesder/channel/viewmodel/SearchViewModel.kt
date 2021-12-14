package tsfat.yeshivathahesder.channel.viewmodel

import tsfat.yeshivathahesder.channel.model.SearchedVideo
import tsfat.yeshivathahesder.channel.paging.NetworkState
import tsfat.yeshivathahesder.channel.paging.datasourcefactory.SearchDataSourceFactory
import tsfat.yeshivathahesder.channel.repository.SearchRepository
import tsfat.yeshivathahesder.channel.utils.Constants
import androidx.lifecycle.*
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import kotlinx.coroutines.launch

class SearchViewModel(
    private val searchRepository: SearchRepository,
    private val channelId: String,
    private val emptySearchResultText: String
) : ViewModel() {

    lateinit var searchDataSourceFactory: SearchDataSourceFactory
    var searchResultLiveData: LiveData<PagedList<SearchedVideo.Item>>? = null
    var networkStateLiveData: LiveData<NetworkState>? = null
    private var _emptyStateLiveData = MutableLiveData<Boolean>()
    val emptyStateLiveData: LiveData<Boolean>
        get() = _emptyStateLiveData

    fun searchVideos(searchQuery: String) {
        if (searchResultLiveData == null) {
            viewModelScope.launch {
                searchDataSourceFactory =
                    SearchDataSourceFactory(
                        searchRepository,
                        viewModelScope,
                        channelId,
                        searchQuery,
                        emptySearchResultText
                    )

                searchResultLiveData =
                    LivePagedListBuilder(searchDataSourceFactory, pagedListConfig())
                        .setBoundaryCallback(object :
                            PagedList.BoundaryCallback<SearchedVideo.Item>() {
                            override fun onZeroItemsLoaded() {
                                super.onZeroItemsLoaded()
                                _emptyStateLiveData.value = true
                            }

                            override fun onItemAtFrontLoaded(itemAtFront: SearchedVideo.Item) {
                                super.onItemAtFrontLoaded(itemAtFront)
                                _emptyStateLiveData.value = false
                            }

                            override fun onItemAtEndLoaded(itemAtEnd: SearchedVideo.Item) {
                                super.onItemAtEndLoaded(itemAtEnd)
                                _emptyStateLiveData.value = false
                            }
                        }).build()
                networkStateLiveData =
                    Transformations.switchMap(searchDataSourceFactory.searchDataSourceLiveData) { it.getNetworkState() }
            }
        }
    }

    fun setSearchQuery(updatedSearchQuery: String) {
        searchDataSourceFactory.setSearchQuery(updatedSearchQuery)
        searchDataSourceFactory.searchDataSourceLiveData.value?.invalidate()
    }

    /**
     * Retry possible last paged request (ie: network issue)
     */
    fun refreshFailedRequest() =
        searchDataSourceFactory.getSource()?.retryFailedQuery()

    private fun pagedListConfig() = PagedList.Config.Builder()
        .setInitialLoadSizeHint(Constants.INITIAL_PAGE_LOAD_SIZE)
        .setEnablePlaceholders(false)
        .setPageSize(Constants.PAGE_SIZE)
        .build()
}
