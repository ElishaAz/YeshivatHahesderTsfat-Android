package tsfat.yeshivathahesder.channel.di

import tsfat.yeshivathahesder.channel.R
import tsfat.yeshivathahesder.channel.api.ChannelInfoService
import tsfat.yeshivathahesder.channel.repository.AboutRepository
import tsfat.yeshivathahesder.channel.viewmodel.AboutViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import retrofit2.Retrofit

val aboutModule = module {

    factory { provideChannelInfoService(get()) }

    single { AboutRepository(get()) }

    viewModel { AboutViewModel(get(), androidContext().getString(R.string.channel_id), androidContext()) }
}

private fun provideChannelInfoService(retrofit: Retrofit) =
    retrofit.create(ChannelInfoService::class.java)
