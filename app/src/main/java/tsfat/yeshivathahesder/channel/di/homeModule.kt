package tsfat.yeshivathahesder.channel.di

import tsfat.yeshivathahesder.channel.R
import tsfat.yeshivathahesder.channel.api.ChannelsService
import tsfat.yeshivathahesder.channel.api.PlaylistItemsService
import tsfat.yeshivathahesder.channel.repository.HomeRepository
import tsfat.yeshivathahesder.channel.viewmodel.HomeViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import retrofit2.Retrofit
import tsfat.yeshivathahesder.channel.api.LiveVideosService

val homeModule = module {

    factory { provideChannelsService(get()) }
    factory { providePlaylistItemsService(get()) }
    factory { provideLiveVideosService(get()) }

    single { HomeRepository(get(), get(), get(), get(),get()) }

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

private fun provideLiveVideosService(retrofit: Retrofit) =
    retrofit.create(LiveVideosService::class.java)