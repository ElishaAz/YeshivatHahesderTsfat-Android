package tsfat.yeshivathahesder.channel.paging.datasourcefactory

import tsfat.yeshivathahesder.channel.model.Playlist
import tsfat.yeshivathahesder.channel.paging.datasource.PlaylistsDataSource
import tsfat.yeshivathahesder.channel.repository.PlaylistsRepository
import androidx.lifecycle.MutableLiveData
import androidx.paging.DataSource
import kotlinx.coroutines.CoroutineScope

class PlaylistsDataSourceFactory(
    private val playlistRepository: PlaylistsRepository,
    private val coroutineScope: CoroutineScope,
    private val channelId: String
) : DataSource.Factory<String, Playlist.Item>() {

    private var playlistsDataSource: PlaylistsDataSource? = null
    val playlistsDataSourceLiveData = MutableLiveData<PlaylistsDataSource>()

    override fun create(): DataSource<String, Playlist.Item> {
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