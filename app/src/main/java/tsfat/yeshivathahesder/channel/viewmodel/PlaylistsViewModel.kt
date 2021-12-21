package tsfat.yeshivathahesder.channel.viewmodel

import tsfat.yeshivathahesder.channel.model.Playlists
import tsfat.yeshivathahesder.channel.paging.NetworkState
import tsfat.yeshivathahesder.channel.paging.datasourcefactory.PlaylistsDataSourceFactory
import tsfat.yeshivathahesder.channel.repository.PlaylistsRepository
import tsfat.yeshivathahesder.channel.utils.Constants
import androidx.lifecycle.*
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import kotlinx.coroutines.launch
import tsfat.yeshivathahesder.channel.model.PlaylistBase

class PlaylistsViewModel(
    private val playlistsRepository: PlaylistsRepository,
    private val channelId: String
) : ViewModel() {

    lateinit var playlistsDataSourceFactory: PlaylistsDataSourceFactory
    var playlistsLiveData: LiveData<PagedList<PlaylistBase>>? = null
    var networkStateLiveData: LiveData<NetworkState>? = null
    private var _emptyStateLiveData = MutableLiveData<Boolean>()
    val emptyStateLiveData: LiveData<Boolean>
        get() = _emptyStateLiveData

    fun getPlaylists() {
        if (playlistsLiveData == null) {
            viewModelScope.launch {
                playlistsDataSourceFactory =
                    PlaylistsDataSourceFactory(
                        playlistsRepository,
                        viewModelScope,
                        channelId
                    )

                playlistsLiveData =
                    LivePagedListBuilder(playlistsDataSourceFactory, pagedListConfig())
                        .setBoundaryCallback(object :
                            PagedList.BoundaryCallback<PlaylistBase>() {
                            override fun onZeroItemsLoaded() {
                                super.onZeroItemsLoaded()
                                _emptyStateLiveData.value = true
                            }

                            override fun onItemAtFrontLoaded(itemAtFront: PlaylistBase) {
                                super.onItemAtFrontLoaded(itemAtFront)
                                _emptyStateLiveData.value = false
                            }

                            override fun onItemAtEndLoaded(itemAtEnd: PlaylistBase) {
                                super.onItemAtEndLoaded(itemAtEnd)
                                _emptyStateLiveData.value = false
                            }
                        })
                        .build()
                networkStateLiveData =
                    Transformations.switchMap(playlistsDataSourceFactory.playlistsDataSourceLiveData) { it.getNetworkState() }
            }
        }

    }

    /**
     * Retry possible last paged request (ie: network issue)
     */
    fun refreshFailedRequest() =
        playlistsDataSourceFactory.getSource()?.retryFailedQuery()

    private fun pagedListConfig() = PagedList.Config.Builder()
        .setInitialLoadSizeHint(Constants.INITIAL_PAGE_LOAD_SIZE)
        .setEnablePlaceholders(false)
        .setPageSize(Constants.PAGE_SIZE)
        .build()
}
