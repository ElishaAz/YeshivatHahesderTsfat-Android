package tsfat.yeshivathahesder.channel

import tsfat.yeshivathahesder.channel.di.*
import tsfat.yeshivathahesder.channel.sharedpref.AppPref
import android.app.Application
import android.content.res.Configuration
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.appcompat.app.AppCompatDelegate
import com.chibatching.kotpref.Kotpref
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import timber.log.Timber
import java.util.*

class Channelify : Application() {

    companion object {
        var isBackgroundViewEnabled = false
        var isUpdateNotifyEnabled = false
    }

    override fun onCreate() {
        super.onCreate()
        isBackgroundViewEnabled = resources.getBoolean(R.bool.enable_background_view)
        isUpdateNotifyEnabled = resources.getBoolean(R.bool.enable_update_notify)

        initializeKotpref()
        setThemeMode()
        initializeTimber()
        initializeKoin()
    }

    private fun setThemeMode() {
        AppCompatDelegate.setDefaultNightMode(AppPref.modeNight)
    }

    private fun initializeTimber() {
//        if (BuildConfig.DEBUG) {
        Timber.plant(Timber.DebugTree())
//        }
    }

    private fun initializeKotpref() {
        Kotpref.init(this)
    }

    private fun initializeKoin() {
        startKoin {
            androidLogger(if (BuildConfig.DEBUG) Level.ERROR else Level.NONE)
            androidContext(this@Channelify)
            modules(
                listOf(
                    appModule,
                    audioModel,
                    homeModule,
                    videoPlayerModule,
                    commentsModule,
                    videoDetailsModule,
                    commentRepliesModule,
                    playlistsModule,
                    playlistVideosModule,
                    favoritesModule,
                    searchModule,
                    aboutModule
                )
            )
        }
    }
}