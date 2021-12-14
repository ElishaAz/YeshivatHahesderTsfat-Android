package tsfat.yeshivathahesder.channel.paging.datasourcefactory

import tsfat.yeshivathahesder.channel.model.PlaylistItemInfo
import tsfat.yeshivathahesder.channel.paging.datasource.PlaylistVideosDataSource
import tsfat.yeshivathahesder.channel.repository.PlaylistVideosRepository
import androidx.lifecycle.MutableLiveData
import androidx.paging.DataSource
import kotlinx.coroutines.CoroutineScope

class PlaylistVideosDataSourceFactory(
    private val playlistVideosRepository: PlaylistVideosRepository,
    private val coroutineScope: CoroutineScope,
    private val playlistId: String
) : DataSource.Factory<String, PlaylistItemInfo.ItemBase>() {

    private var playlistVideosDataSource: PlaylistVideosDataSource? = null
    val playlistVideosDataSourceLiveData = MutableLiveData<PlaylistVideosDataSource>()

    override fun create(): DataSource<String, PlaylistItemInfo.ItemBase> {
        if (playlistVideosDataSource == null) {
            playlistVideosDataSource =
                PlaylistVideosDataSource(
                    playlistVideosRepository,
                    coroutineScope,
                    playlistId
                )
            playlistVideosDataSourceLiveData.postValue(playlistVideosDataSource)
        }
        return playlistVideosDataSource!!
    }

    fun getSource() = playlistVideosDataSourceLiveData.value
}