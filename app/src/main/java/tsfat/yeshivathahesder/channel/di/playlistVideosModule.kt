package tsfat.yeshivathahesder.channel.di

import tsfat.yeshivathahesder.channel.repository.PlaylistVideosRepository
import tsfat.yeshivathahesder.channel.viewmodel.PlaylistVideosViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

/**
 * No need to provide PlaylistItemsService since it is already being provided by Koin in the
 * homeModule
 */
val playlistVideosModule = module {

    single { PlaylistVideosRepository(get()) }

    viewModel { PlaylistVideosViewModel(get()) }
}
