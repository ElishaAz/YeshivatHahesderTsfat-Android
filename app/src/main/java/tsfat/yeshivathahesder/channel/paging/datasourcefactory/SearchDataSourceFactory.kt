package tsfat.yeshivathahesder.channel.paging.datasourcefactory

import tsfat.yeshivathahesder.channel.model.SearchedList
import tsfat.yeshivathahesder.channel.paging.datasource.SearchDataSource
import tsfat.yeshivathahesder.channel.repository.SearchRepository
import androidx.lifecycle.MutableLiveData
import androidx.paging.DataSource
import kotlinx.coroutines.CoroutineScope

class SearchDataSourceFactory(
    private val searchRepository: SearchRepository,
    private val coroutineScope: CoroutineScope,
    private val channelId: String,
    private var searchQuery: String,
    private val emptySearchResultText: String
) : DataSource.Factory<String, SearchedList.SearchItem>() {

    private var searchDataSource: SearchDataSource? = null
    val searchDataSourceLiveData = MutableLiveData<SearchDataSource>()

    override fun create(): DataSource<String, SearchedList.SearchItem> {
        // Also called every time when invalidate() is executed
        searchDataSource =
            SearchDataSource(
                searchRepository,
                coroutineScope,
                channelId,
                searchQuery,
                emptySearchResultText
            )
        searchDataSourceLiveData.postValue(searchDataSource!!)

        return searchDataSource!!
    }

    fun setSearchQuery(updatedSearchQuery: String) {
        searchQuery = updatedSearchQuery
    }

    fun getSource() = searchDataSourceLiveData.value
}