package tsfat.yeshivathahesder.channel.activity

import tsfat.yeshivathahesder.channel.R
import tsfat.yeshivathahesder.channel.locales.LocaleHelper
import tsfat.yeshivathahesder.channel.sharedpref.AppPref
import tsfat.yeshivathahesder.channel.utils.Tools
import tsfat.yeshivathahesder.channel.utils.media.MyNotificationManager
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

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
    }

    override fun onResume() {
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

    // Locale changes
    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }
}
