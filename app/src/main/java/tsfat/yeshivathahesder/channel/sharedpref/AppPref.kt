package tsfat.yeshivathahesder.channel.sharedpref

import android.content.Context
import android.content.res.Configuration
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import com.chibatching.kotpref.KotprefModel
import timber.log.Timber
import tsfat.yeshivathahesder.channel.R

object AppPref : KotprefModel() {
    override val kotprefName: String = context.getString(R.string.preference_file_name)

    var theme by stringPref(context.getString(R.string.theme_value_system), R.string.preference_theme_key)
    var localeOverride by stringPref("system", R.string.preference_locale_key)
    var lastDismissedUpdate by intPref(0)
    var autoEnterPIP by booleanPref(true, R.string.preference_auto_pip_key)

    val modeNight: Int
        get() =
            when (theme) {
                context.getString(R.string.theme_value_system) -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                context.getString(R.string.theme_value_light) -> AppCompatDelegate.MODE_NIGHT_NO
                context.getString(R.string.theme_value_dark) -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_UNSPECIFIED
            }
}