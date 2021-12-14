package tsfat.yeshivathahesder.channel.di

import tsfat.yeshivathahesder.channel.api.VideosService
import tsfat.yeshivathahesder.channel.db.ChannelifyDatabase
import tsfat.yeshivathahesder.channel.repository.VideoDetailsRepository
import tsfat.yeshivathahesder.channel.viewmodel.VideoDetailsViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import retrofit2.Retrofit

val videoDetailsModule = module {
    factory { provideVideosService(get()) }

    single { provideFavoriteVideoDao(get()) }
    single { VideoDetailsRepository(get(), get()) }

    viewModel { VideoDetailsViewModel(get(), androidContext()) }
}

private fun provideVideosService(retrofit: Retrofit) =
    retrofit.create(VideosService::class.java)

private fun provideFavoriteVideoDao(channelifyDatabase: ChannelifyDatabase) =
    channelifyDatabase.favoriteVideoDao()


