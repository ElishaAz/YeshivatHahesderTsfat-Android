package tsfat.yeshivathahesder.channel.di

import tsfat.yeshivathahesder.channel.api.CommentRepliesService
import tsfat.yeshivathahesder.channel.repository.CommentRepliesRepository
import tsfat.yeshivathahesder.channel.viewmodel.CommentRepliesViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import retrofit2.Retrofit

val commentRepliesModule = module {

    factory { provideCommentRepliesService(get()) }

    single { CommentRepliesRepository(get()) }

    viewModel { CommentRepliesViewModel(get()) }
}

private fun provideCommentRepliesService(retrofit: Retrofit) =
    retrofit.create(CommentRepliesService::class.java)
