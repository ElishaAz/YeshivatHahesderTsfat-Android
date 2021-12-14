package tsfat.yeshivathahesder.channel.activity

import tsfat.yeshivathahesder.channel.Channelify
import tsfat.yeshivathahesder.channel.R
import tsfat.yeshivathahesder.channel.locales.LocaleHelper
import tsfat.yeshivathahesder.channel.sharedpref.AppPref
import tsfat.yeshivathahesder.channel.utils.Tools
import tsfat.yeshivathahesder.channel.utils.getAdaptiveBannerAdSize
import tsfat.yeshivathahesder.channel.utils.media.MyNotificationManager
import tsfat.yeshivathahesder.core.extensions.makeGone
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.gms.ads.*
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private lateinit var adView: AdView
    private var initialLayoutComplete = false
    private lateinit var initialLocale: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initialLocale = AppPref.localeOverride

        Tools.showUpdateDialog(this, true)
        MyNotificationManager
            .createNotificationChannel(this)

        val navController = findNavController(R.id.navHostFragment)
        bottomNavView.setupWithNavController(navController)

        if (Channelify.isAdEnabled) setupAd() else adViewContainerMain.makeGone()

    }

    override fun onPause() {
        if (Channelify.isAdEnabled) adView.pause()
        super.onPause()
    }

    override fun onResume() {
        if (Channelify.isAdEnabled) adView.resume()
        super.onResume()

        checkLocaleChange()
    }

    fun checkLocaleChange() {
        // Locale changes
        if (initialLocale != AppPref.localeOverride) {
            recreate();
            initialLocale = AppPref.localeOverride
            Log.d("MainActivity", "Locale changed!")
        }
    }

    override fun onDestroy() {
        if (Channelify.isAdEnabled) adView.destroy()
        super.onDestroy()
    }

    // Locale changes
    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }

    private fun setupAd() {
        adView = AdView(this)
        adViewContainerMain.addView(adView)
        adViewContainerMain.viewTreeObserver.addOnGlobalLayoutListener {
            if (!initialLayoutComplete) {
                initialLayoutComplete = true

                adView.adUnitId = getString(R.string.main_banner_ad_id)
                adView.adSize = getAdaptiveBannerAdSize(adViewContainerMain)
                adView.loadAd(AdRequest.Builder().build())
            }
        }
    }
}
