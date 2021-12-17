package tsfat.yeshivathahesder.channel.di

import tsfat.yeshivathahesder.channel.R
import tsfat.yeshivathahesder.channel.api.SearchVideoService
import tsfat.yeshivathahesder.channel.repository.SearchRepository
import tsfat.yeshivathahesder.channel.viewmodel.SearchViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import retrofit2.Retrofit

val searchModule = module {

    factory { provideSearchVideoService(get()) }

    single { SearchRepository(get(), get()) }

    viewModel {
        SearchViewModel(
            get(),
            androidContext().getString(R.string.channel_id),
            androidContext().getString(R.string.error_search_empty_result_title)
        )
    }
}

private fun provideSearchVideoService(retrofit: Retrofit) =
    retrofit.create(SearchVideoService::class.java)
