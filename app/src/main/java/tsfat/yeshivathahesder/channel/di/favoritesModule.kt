package tsfat.yeshivathahesder.channel.di

import tsfat.yeshivathahesder.channel.repository.FavoritesRepository
import tsfat.yeshivathahesder.channel.viewmodel.FavoritesViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val favoritesModule = module {

    // FavoriteVideoDao is already injected in videoDetailsModule

    single { FavoritesRepository(get()) }

    viewModel { FavoritesViewModel(get()) }
}
