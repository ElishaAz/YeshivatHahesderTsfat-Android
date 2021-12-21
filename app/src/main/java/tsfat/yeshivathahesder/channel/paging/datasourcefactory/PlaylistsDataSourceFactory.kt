package tsfat.yeshivathahesder.channel.paging.datasourcefactory

import tsfat.yeshivathahesder.channel.model.Playlists
import tsfat.yeshivathahesder.channel.paging.datasource.PlaylistsDataSource
import tsfat.yeshivathahesder.channel.repository.PlaylistsRepository
import androidx.lifecycle.MutableLiveData
import androidx.paging.DataSource
import kotlinx.coroutines.CoroutineScope
import tsfat.yeshivathahesder.channel.model.PlaylistBase

class PlaylistsDataSourceFactory(
    private val playlistRepository: PlaylistsRepository,
    private val coroutineScope: CoroutineScope,
    private val channelId: String
) : DataSource.Factory<String, PlaylistBase>() {

    private var playlistsDataSource: PlaylistsDataSource? = null
    val playlistsDataSourceLiveData = MutableLiveData<PlaylistsDataSource>()

    override fun create(): DataSource<String, PlaylistBase> {
        if (playlistsDataSource == null) {
            playlistsDataSource =
                PlaylistsDataSource(
                    playlistRepository,
                    coroutineScope,
                    channelId
                )
            playlistsDataSourceLiveData.postValue(playlistsDataSource)
        }
        return playlistsDataSource!!
    }

    fun getSource() = playlistsDataSourceLiveData.value
}