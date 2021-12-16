package tsfat.yeshivathahesder.channel.di

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import tsfat.yeshivathahesder.channel.R
import tsfat.yeshivathahesder.channel.api.ChannelsService
import tsfat.yeshivathahesder.channel.api.PlaylistItemsService
import tsfat.yeshivathahesder.channel.repository.HomeRepository
import tsfat.yeshivathahesder.channel.viewmodel.HomeViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import retrofit2.Retrofit
import tsfat.yeshivathahesder.channel.uamp.AudioItem
import java.util.concurrent.Callable

class AudioConnector() {
    var audioItems: MutableLiveData<List<AudioItem>> = MutableLiveData()
    lateinit var playItem: (AudioItem) -> Unit
}

val homeModule = module {

    factory { provideChannelsService(get()) }
    factory { providePlaylistItemsService(get()) }

    single { HomeRepository(get(), get(), get(), get()) }
    single { AudioConnector() }

    viewModel {
        HomeViewModel(
            get(),
            androidContext().getString(R.string.channel_id),
            androidContext()
        )
    }
}

private fun provideChannelsService(retrofit: Retrofit) =
    retrofit.create(ChannelsService::class.java)

private fun providePlaylistItemsService(retrofit: Retrofit) =
    retrofit.create(PlaylistItemsService::class.java)