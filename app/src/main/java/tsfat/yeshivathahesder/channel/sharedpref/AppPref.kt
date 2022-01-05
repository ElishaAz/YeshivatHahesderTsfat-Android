package tsfat.yeshivathahesder.channel.sharedpref

import com.chibatching.kotpref.KotprefModel

object AppPref : KotprefModel() {
    var isLightThemeEnabled by booleanPref(true)
    var localeOverride by stringPref("system")
    var lastDismissedUpdate by intPref(0)
    var autoEnterPIP by booleanPref(true)
}