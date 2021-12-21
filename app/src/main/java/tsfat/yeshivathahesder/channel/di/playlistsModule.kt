package tsfat.yeshivathahesder.channel.di

import tsfat.yeshivathahesder.channel.R
import tsfat.yeshivathahesder.channel.api.PlaylistsService
import tsfat.yeshivathahesder.channel.repository.PlaylistsRepository
import tsfat.yeshivathahesder.channel.viewmodel.PlaylistsViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import retrofit2.Retrofit

val playlistsModule = module {

    factory { providePlaylistsService(get()) }

    single { PlaylistsRepository(get(),get()) }

    viewModel { PlaylistsViewModel(get(), androidContext().getString(R.string.channel_id)) }
}

private fun providePlaylistsService(retrofit: Retrofit) =
    retrofit.create(PlaylistsService::class.java)
