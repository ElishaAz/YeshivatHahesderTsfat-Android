package tsfat.yeshivathahesder.channel.viewmodel

import tsfat.yeshivathahesder.channel.paging.NetworkState
import tsfat.yeshivathahesder.channel.paging.datasourcefactory.PlaylistVideosDataSourceFactory
import tsfat.yeshivathahesder.channel.repository.PlaylistVideosRepository
import tsfat.yeshivathahesder.channel.utils.Constants
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import kotlinx.coroutines.launch
import tsfat.yeshivathahesder.channel.model.ItemBase

class PlaylistVideosViewModel(
    private val playlistVideosRepository: PlaylistVideosRepository
) : ViewModel() {

    lateinit var playlistVideosDataSourceFactory: PlaylistVideosDataSourceFactory
    var playlistVideosLiveData: LiveData<PagedList<ItemBase>>? = null
    var networkStateLiveData: LiveData<NetworkState>? = null

    fun getPlaylistVideos(playlistId: String, playlistType: String) {
        if (playlistVideosLiveData == null) {
            viewModelScope.launch {
                playlistVideosDataSourceFactory =
                    PlaylistVideosDataSourceFactory(
                        playlistVideosRepository,
                        viewModelScope,
                        playlistId,
                        playlistType
                    )

                playlistVideosLiveData =
                    LivePagedListBuilder(playlistVideosDataSourceFactory, pagedListConfig()).build()
                networkStateLiveData =
                    Transformations.switchMap(playlistVideosDataSourceFactory.playlistVideosDataSourceLiveData) { it.getNetworkState() }
            }
        }

    }

    /**
     * Retry possible last paged request (ie: network issue)
     */
    fun refreshFailedRequest() =
        playlistVideosDataSourceFactory.getSource()?.retryFailedQuery()

    private fun pagedListConfig() = PagedList.Config.Builder()
        .setInitialLoadSizeHint(Constants.INITIAL_PAGE_LOAD_SIZE)
        .setEnablePlaceholders(false)
        .setPageSize(Constants.PAGE_SIZE)
        .build()
}
