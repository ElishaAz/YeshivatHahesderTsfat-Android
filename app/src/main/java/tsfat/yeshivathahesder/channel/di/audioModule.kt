package tsfat.yeshivathahesder.channel.di

import androidx.lifecycle.MutableLiveData
import org.koin.dsl.module
import tsfat.yeshivathahesder.channel.repository.AudioRepository
import tsfat.yeshivathahesder.channel.uamp.AudioItem
import tsfat.yeshivathahesder.channel.uamp.AudioPlaylist

class AudioConnector() {
    var hashItems: MutableLiveData<Boolean> = MutableLiveData()
    var audioItems: MutableLiveData<List<AudioItem>> = MutableLiveData()
    var playlists: MutableLiveData<List<AudioPlaylist>> = MutableLiveData()
    lateinit var playItem: (String) -> Unit
}

val audioModel = module {
    single { AudioConnector() }
    single { AudioRepository(get()) }
}