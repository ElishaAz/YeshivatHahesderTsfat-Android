package aculix.channelify.app.activity

import aculix.channelify.app.Channelify
import aculix.channelify.app.R
import aculix.channelify.app.locales.LocaleHelper
import aculix.channelify.app.sharedpref.AppPref
import aculix.channelify.app.utils.Tools
import aculix.channelify.app.utils.getAdaptiveBannerAdSize
import aculix.core.extensions.makeGone
import android.content.Context
import android.content.DialogInterface
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.github.javiersantos.appupdater.AppUpdater
import com.github.javiersantos.appupdater.enums.Display
import com.github.javiersantos.appupdater.enums.UpdateFrom
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
