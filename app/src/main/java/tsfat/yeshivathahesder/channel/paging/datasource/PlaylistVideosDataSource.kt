package tsfat.yeshivathahesder.channel.paging.datasource

import tsfat.yeshivathahesder.channel.paging.NetworkState
import tsfat.yeshivathahesder.channel.repository.PlaylistVideosRepository
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.paging.PageKeyedDataSource
import kotlinx.coroutines.*
import timber.log.Timber
import tsfat.yeshivathahesder.channel.model.ItemBase

class PlaylistVideosDataSource(
    private val repository: PlaylistVideosRepository,
    private val coroutineScope: CoroutineScope,
    private val playlistId: String,
    private val playlistType: String
) : PageKeyedDataSource<String, ItemBase>() {

    private var supervisorJob = SupervisorJob()
    private val networkState = MutableLiveData<NetworkState>()
    private var retryQuery: (() -> Any)? =
        null // Keep reference of the last query (to be able to retry it if necessary)
    private var nextPageToken: String? = null

    override fun loadInitial(
        params: LoadInitialParams<String>,
        callback: LoadInitialCallback<String, ItemBase>
    ) {
        retryQuery = { loadInitial(params, callback) }
        executeQuery {
            callback.onResult(it, null, nextPageToken)
        }
    }

    override fun loadAfter(
        params: LoadParams<String>,
        callback: LoadCallback<String, ItemBase>
    ) {
        retryQuery = { loadAfter(params, callback) }
        executeQuery {
            callback.onResult(it, nextPageToken)
        }
    }

    override fun loadBefore(
        params: LoadParams<String>,
        callback: LoadCallback<String, ItemBase>
    ) {
        // Data is always fetched from the next page and hence loadBefore is never needed
    }

    private fun executeQuery(callback: (List<ItemBase>) -> Unit) {
        networkState.postValue(NetworkState.LOADING)
        coroutineScope.launch(getJobErrorHandler() + supervisorJob) {
            val response = repository.getPlaylistVideos(playlistId, playlistType, nextPageToken)

            if (response == null || !response.isSuccessful) {
                networkState.postValue(NetworkState.error(response?.message()))
                return@launch
            }

            val playlistItemInfo = response.body()
            nextPageToken = playlistItemInfo?.nextPageToken
            val playlistVideosList = playlistItemInfo?.items

            retryQuery = null
            networkState.postValue(NetworkState.LOADED)

            callback(playlistVideosList ?: emptyList())
        }
    }

    private fun getJobErrorHandler() = CoroutineExceptionHandler { _, e ->
        Timber.e(e, "An error happened")
        networkState.postValue(
            NetworkState.error(
                e.localizedMessage
            )
        )
    }

    override fun invalidate() {
        super.invalidate()
        supervisorJob.cancelChildren()   // Cancel possible running job to only keep last result searched by user
    }

    fun getNetworkState(): LiveData<NetworkState> = networkState

    fun refresh() = this.invalidate()

    fun retryFailedQuery() {
        val prevQuery = retryQuery
        retryQuery = null
        prevQuery?.invoke()
    }
}

const val PLAYLIST_TYPE_VIDEO = "Video"
const val PLAYLIST_TYPE_AUDIO = "Audio"