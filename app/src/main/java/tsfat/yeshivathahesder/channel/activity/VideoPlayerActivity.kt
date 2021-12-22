package tsfat.yeshivathahesder.channel.activity

import tsfat.yeshivathahesder.channel.Channelify
import tsfat.yeshivathahesder.channel.R
import tsfat.yeshivathahesder.channel.fragment.VideoDetailsFragment
import tsfat.yeshivathahesder.channel.locales.LocaleHelper
import tsfat.yeshivathahesder.channel.utils.FullScreenHelper
import tsfat.yeshivathahesder.channel.utils.media.MediaSessionController
import tsfat.yeshivathahesder.channel.viewmodel.VideoPlayerViewModel
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.support.v4.media.session.MediaSessionCompat
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.navigation.findNavController
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.YouTubePlayerFullScreenListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.utils.loadOrCueVideo
import kotlinx.android.synthetic.main.activity_video_player.*
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import tsfat.yeshivathahesder.channel.di.AudioConnector


class VideoPlayerActivity : AppCompatActivity(R.layout.activity_video_player) {

    companion object {
        const val VIDEO_ID = "video_id"

        /**
         * Do not call directly. Call using PlayVideo.play instantiated using inject,
         */
        fun startActivity(context: Context?, videoId: String) {
            val intent = Intent(context, VideoPlayerActivity::class.java).apply {
                putExtra(VIDEO_ID, videoId)
            }
            context?.startActivity(intent)
        }
    }

    private val viewModel by viewModel<VideoPlayerViewModel>() // Lazy inject ViewModel

    lateinit var fullScreenHelper: FullScreenHelper
    lateinit var videoId: String
    private var videoElapsedTimeInSeconds = 0f


    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var sessionController: MediaSessionController<VideoPlayerActivity>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fullScreenHelper = FullScreenHelper(this)
        videoId = intent.getStringExtra(VIDEO_ID)!!

        // Passing the videoId as argument to the start destination
        findNavController(R.id.navHostVideoPlayer).setGraph(
            R.navigation.video_player_graph,
            bundleOf(VideoDetailsFragment.VIDEO_ID to videoId)
        )

        initYouTubePlayer()
    }

    override fun onBackPressed() {
        if (ytVideoPlayerView.isFullScreen()) ytVideoPlayerView.exitFullScreen() else super.onBackPressed()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (Channelify.isBackgroundViewEnabled) {
            ytVideoPlayerView.release()
            sessionController.finish()
        }
    }

    private fun initYouTubePlayer() {
        if (!Channelify.isBackgroundViewEnabled)
            lifecycle.addObserver(ytVideoPlayerView)
        else {
            ytVideoPlayerView.enableBackgroundPlayback(true)
        }


        ytVideoPlayerView.addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
            override fun onReady(youTubePlayer: YouTubePlayer) {
                youTubePlayer.loadOrCueVideo(lifecycle, videoId, 0f)
                addFullScreenListenerToPlayer()
                setupCustomActions(youTubePlayer)
            }

            override fun onCurrentSecond(youTubePlayer: YouTubePlayer, second: Float) {
                videoElapsedTimeInSeconds = second
            }
        })


        if (Channelify.isBackgroundViewEnabled) {
            mediaSession = MediaSessionCompat(this, "YouTube")
            sessionController = MediaSessionController(
                this,
                ytVideoPlayerView,
                mediaSession,
                VideoPlayerActivity::class.java
            )
        }
    }


    /**
     * Adds the forward and rewind action button to the Player
     */
    private fun setupCustomActions(youTubePlayer: YouTubePlayer) {
        ytVideoPlayerView.getPlayerUiController()
            .setCustomAction1(
                ContextCompat.getDrawable(this, R.drawable.ic_rewind)!!,
                View.OnClickListener {
                    videoElapsedTimeInSeconds -= 10
                    youTubePlayer.seekTo(videoElapsedTimeInSeconds)
                })
            .setCustomAction2(
                ContextCompat.getDrawable(this, R.drawable.ic_forward)!!,
                View.OnClickListener {
                    videoElapsedTimeInSeconds += 10
                    youTubePlayer.seekTo(videoElapsedTimeInSeconds)
                })
    }

    /**
     * Changes the orientation of the activity based on the
     * change of the player state (Full screen or not)
     */
    @SuppressLint("SourceLockedOrientationActivity")
    private fun addFullScreenListenerToPlayer() {
        ytVideoPlayerView.addFullScreenListener(object : YouTubePlayerFullScreenListener {
            override fun onYouTubePlayerEnterFullScreen() {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                fullScreenHelper.enterFullScreen()
            }

            override fun onYouTubePlayerExitFullScreen() {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                fullScreenHelper.exitFullScreen()
            }
        })
    }

    // Locale changes
    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }
}
