package tsfat.yeshivathahesder.channel.di

import android.content.Context
import org.koin.dsl.module

val videoPlayerModule = module {
    single { PlayVideo() }
}

class PlayVideo() {
    lateinit var play: (Context?, String) -> Unit
}