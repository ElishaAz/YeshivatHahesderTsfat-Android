package tsfat.yeshivathahesder.channel.utils

import android.app.Activity
import android.view.View
import android.view.View.*
import android.view.ViewGroup


/**
 * Class responsible for changing the view from full screen to non-full screen and vice versa.
 */
class FullScreenHelper(
    private val context: Activity,
    private val player: View,
    vararg views: View
) {

    private var views: Array<View> = arrayOf(*views)

    var regHeight: Int = 0

    var fullScreen: Boolean = false
        private set

    /**
     * call this method to enter full screen
     */
    fun enterFullScreen() {
        fullScreen = true
        val decorView = context.window.decorView

        hideSystemUi(decorView)

        for (view in views) {
            view.visibility = View.GONE
            view.invalidate()
        }
        val viewParams: ViewGroup.LayoutParams = player.layoutParams
        regHeight = player.height
        viewParams.height = ViewGroup.LayoutParams.MATCH_PARENT
        viewParams.width = ViewGroup.LayoutParams.MATCH_PARENT
        player.layoutParams = viewParams
    }

    /**
     * call this method to exit full screen
     */
    fun exitFullScreen() {
        fullScreen = false
        val decorView = context.window.decorView

        showSystemUi(decorView)

        for (view in views) {
            view.visibility = View.VISIBLE
            view.invalidate()
        }
        val viewParams: ViewGroup.LayoutParams = player.layoutParams
        viewParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
        viewParams.width = ViewGroup.LayoutParams.MATCH_PARENT
        player.layoutParams = viewParams
    }

    private fun hideSystemUi(mDecorView: View) {
        mDecorView.systemUiVisibility = (SYSTEM_UI_FLAG_LAYOUT_STABLE
                or SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or SYSTEM_UI_FLAG_FULLSCREEN
                or SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
    }

    private fun showSystemUi(mDecorView: View) {
        mDecorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
    }
}