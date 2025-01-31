package tsfat.yeshivathahesder.channel.viewmodel

import tsfat.yeshivathahesder.channel.R
import tsfat.yeshivathahesder.channel.paging.datasourcefactory.HomeDataSourceFactory
import tsfat.yeshivathahesder.channel.paging.NetworkState
import tsfat.yeshivathahesder.channel.repository.HomeRepository
import tsfat.yeshivathahesder.channel.utils.Constants
import tsfat.yeshivathahesder.core.helper.ResultWrapper
import android.content.Context
import androidx.lifecycle.*
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import tsfat.yeshivathahesder.channel.model.ItemBase
import tsfat.yeshivathahesder.channel.model.SearchedList

class HomeViewModel(
    private val homeRepository: HomeRepository,
    private val channelId: String,
    private val context: Context
) : ViewModel() {

    private val _uploadsPlaylistIdLiveData = MutableLiveData<ResultWrapper>()
    val uploadsPlaylistIdLiveData: LiveData<ResultWrapper>
        get() = _uploadsPlaylistIdLiveData

    private val _liveVideosLiveData = MutableLiveData<List<SearchedList.Item>>()
    val liveVideosLiveData: LiveData<List<SearchedList.Item>>
        get() = _liveVideosLiveData

    lateinit var homeDataSourceFactory: HomeDataSourceFactory
    var latestVideoLiveData: LiveData<PagedList<ItemBase>>? = null
    var networkStateLiveData: LiveData<NetworkState>? = null

    fun getLatestVideos() {
        if (latestVideoLiveData == null) {
            viewModelScope.launch {
                _uploadsPlaylistIdLiveData.value = ResultWrapper.Loading

                launch(Dispatchers.IO) {
                    val response = homeRepository.getLiveVideos(channelId)
                    if (response.isSuccessful) {
                        val liveVideos = response.body()!!.items
                        _liveVideosLiveData.postValue(liveVideos)
                    }
                }

                val uploadsPlaylistIdRequest =
                    async(Dispatchers.IO) { homeRepository.getUploadsPlaylistId(channelId) }

                val response = uploadsPlaylistIdRequest.await()

                if (response.isSuccessful) {
                    val playlistId =
                        response.body()!!.items[0].contentDetails.relatedPlaylists.uploads
                    homeDataSourceFactory =
                        HomeDataSourceFactory(
                            homeRepository,
                            viewModelScope,
                            playlistId
                        )

                    latestVideoLiveData =
                        LivePagedListBuilder(homeDataSourceFactory, pagedListConfig()).build()
                    networkStateLiveData =
                        Transformations.switchMap(homeDataSourceFactory.homeDataSourceLiveData) { it.getNetworkState() }
                    _uploadsPlaylistIdLiveData.value = ResultWrapper.Success("")
                } else {
                    Timber.e("Error: ${response.raw()}")
                    _uploadsPlaylistIdLiveData.value =
                        ResultWrapper.Error(context.getString(R.string.error_fetch_videos))
                }
            }
        }

    }

    /**
     * Retry possible last paged request (ie: network issue)
     */
    fun refreshFailedRequest() =
        homeDataSourceFactory.getSource()?.retryFailedQuery()

    private fun pagedListConfig() = PagedList.Config.Builder()
        .setInitialLoadSizeHint(Constants.INITIAL_PAGE_LOAD_SIZE)
        .setEnablePlaceholders(false)
        .setPageSize(Constants.PAGE_SIZE)
        .build()
}
