package tsfat.yeshivathahesder.channel.utils

import tsfat.yeshivathahesder.channel.locales.LocaleHelper
import android.annotation.SuppressLint
import com.github.marlonlom.utilities.timeago.TimeAgo
import com.github.marlonlom.utilities.timeago.TimeAgoMessages
import java.text.SimpleDateFormat
import java.util.*

object DateTimeUtils {

    /**
     * Returns time in ago format
     * Eg. 14 hours ago
     * Eg. 2 days ago
     */
    @SuppressLint("SimpleDateFormat")
    fun getTimeAgo(timeInIso8601: String): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
        sdf.timeZone = TimeZone.getTimeZone("GMT")
        val timeInMillis = sdf.parse(timeInIso8601).time
        return TimeAgo.using(
            timeInMillis,
            TimeAgoMessages.Builder().withLocale(LocaleHelper.getLocale()).build()
        )
    }

    /**
     * Returns data in format MMM dd, yyyy
     * Eg. Dec 02, 2019
     */
    @SuppressLint("SimpleDateFormat")
    fun getPublishedDate(timeInIso8601: String): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
        sdf.timeZone = TimeZone.getTimeZone("GMT")
        val date = sdf.parse(timeInIso8601)

        val publishedDateSdf = SimpleDateFormat("MMM dd, yyyy ", LocaleHelper.getLocale())
        return publishedDateSdf.format(date)
    }
}