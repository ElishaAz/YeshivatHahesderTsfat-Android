package tsfat.yeshivathahesder.channel.paging.datasource

import tsfat.yeshivathahesder.channel.model.Comment
import tsfat.yeshivathahesder.channel.paging.NetworkState
import tsfat.yeshivathahesder.channel.repository.CommentsRepository
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.paging.PageKeyedDataSource
import kotlinx.coroutines.*
import timber.log.Timber

class CommentsDataSource(
    private val commentsRepository: CommentsRepository,
    private val coroutineScope: CoroutineScope,
    private val videoId: String,
    var sortOrder: String
) : PageKeyedDataSource<String, Comment.Item>() {

    private var supervisorJob = SupervisorJob()
    private val networkState = MutableLiveData<NetworkState>()
    private var retryQuery: (() -> Any)? =
        null // Keep reference of the last query (to be able to retry it if necessary)
    private var nextPageToken: String? = null

    override fun loadInitial(
        params: LoadInitialParams<String>,
        callback: LoadInitialCallback<String, Comment.Item>
    ) {
        retryQuery = { loadInitial(params, callback) }
        executeQuery {
            callback.onResult(it, null, nextPageToken)
        }
    }

    override fun loadAfter(
        params: LoadParams<String>,
        callback: LoadCallback<String, Comment.Item>
    ) {
        retryQuery = { loadAfter(params, callback) }
        executeQuery {
            callback.onResult(it, nextPageToken)
        }
    }

    override fun loadBefore(
        params: LoadParams<String>,
        callback: LoadCallback<String, Comment.Item>
    ) {
        // Data is always fetched from the next page and hence loadBefore is never needed
    }

    private fun executeQuery(callback: (List<Comment.Item>) -> Unit) {
        networkState.postValue(NetworkState.LOADING)
        coroutineScope.launch(getJobErrorHandler() + supervisorJob) {
            val response = commentsRepository.getVideoComments(videoId, nextPageToken, sortOrder)

            if (!response.isSuccessful) {
                networkState.postValue(NetworkState.error(response.message()))
                return@launch
            }

            val comment = response.body()
            nextPageToken = comment?.nextPageToken
            val commentsList = comment?.items
            retryQuery = null
            networkState.postValue(NetworkState.LOADED)

            callback(commentsList ?: emptyList())
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