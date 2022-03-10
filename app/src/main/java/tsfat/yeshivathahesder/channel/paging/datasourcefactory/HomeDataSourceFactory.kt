package tsfat.yeshivathahesder.channel.paging.datasourcefactory

import tsfat.yeshivathahesder.channel.paging.datasource.HomeDataSource
import tsfat.yeshivathahesder.channel.repository.HomeRepository
import androidx.lifecycle.MutableLiveData
import androidx.paging.DataSource
import kotlinx.coroutines.CoroutineScope
import tsfat.yeshivathahesder.channel.model.ItemBase

class HomeDataSourceFactory(
    private val homeRepository: HomeRepository,
    private val coroutineScope: CoroutineScope,
    private val playlistId: String
) : DataSource.Factory<String, ItemBase>() {

    private var homeDataSource: HomeDataSource? = null
    val homeDataSourceLiveData = MutableLiveData<HomeDataSource>()

    override fun create(): DataSource<String, ItemBase> {
        if (homeDataSource == null) {
            homeDataSource =
                HomeDataSource(
                    homeRepository,
                    coroutineScope,
                    playlistId
                )
            homeDataSourceLiveData.postValue(homeDataSource!!)
        }
        return homeDataSource!!
    }

    fun getSource() = homeDataSourceLiveData.value
}