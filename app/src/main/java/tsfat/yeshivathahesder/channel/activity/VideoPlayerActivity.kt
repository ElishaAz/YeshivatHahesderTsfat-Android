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
import android.os.Build
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
import org.koin.androidx.viewmodel.ext.android.viewModel
import android.app.PictureInPictureParams
import android.content.res.Configuration
import android.util.Rational
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import tsfat.yeshivathahesder.channel.databinding.ActivityVideoPlayerBinding
import tsfat.yeshivathahesder.channel.sharedpref.AppPref


class VideoPlayerActivity : AppCompatActivity() {

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

        private val autoEnterPIP_A12: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    }

    private lateinit var binding: ActivityVideoPlayerBinding

    private val viewModel by viewModel<VideoPlayerViewModel>() // Lazy inject ViewModel

    lateinit var fullScreenHelper: FullScreenHelper
    lateinit var videoId: String
    private var videoElapsedTimeInSeconds = 0f

    private var autoEnterPIP: Boolean = true

    private lateinit var ytVideoPlayerView: YouTubePlayerView


    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var sessionController: MediaSessionController<VideoPlayerActivity>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ytVideoPlayerView = binding.ytVideoPlayerView

        fullScreenHelper = FullScreenHelper(this)
        videoId = intent.getStringExtra(VIDEO_ID)!!

        autoEnterPIP = AppPref.autoEnterPIP

        // Passing the videoId as argument to the start destination
        findNavController(R.id.navHostVideoPlayer).setGraph(
            R.navigation.video_player_graph,
            bundleOf(VideoDetailsFragment.VIDEO_ID to videoId)
        )

        initYouTubePlayer()
    }

    override fun onBackPressed() {
        if (ytVideoPlayerView.isFullScreen()) {
            ytVideoPlayerView.exitFullScreen()
        } else if (autoEnterPIP /* && !autoEnterPIP_A12 */ && shouldAutoEnterPIP) {
            enterPIP()
        } else {
            super.onBackPressed()
        }
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

//                if (autoEnterPIP_A12 && autoEnterPIP) {
//                    val aspectRatio = Rational(ytVideoPlayerView.width, ytVideoPlayerView.height)
//                    setPictureInPictureParams(
//                        PictureInPictureParams.Builder()
//                            .setAspectRatio(aspectRatio)
//                            .setSourceRectHint(ytVideoPlayerView.clipBounds)
//                            .setAutoEnterEnabled(true)
//                            .build()
//                    )
//                }
            }

            override fun onCurrentSecond(youTubePlayer: YouTubePlayer, second: Float) {
                videoElapsedTimeInSeconds = second
            }

            override fun onStateChange(
                youTubePlayer: YouTubePlayer,
                state: PlayerConstants.PlayerState
            ) {
                shouldAutoEnterPIP = state == PlayerConstants.PlayerState.PLAYING
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

    private var shouldAutoEnterPIP = false

    override fun onUserLeaveHint() {
        if (autoEnterPIP /* && !autoEnterPIP_A12 */ && shouldAutoEnterPIP) {
            enterPIP()
        }
    }

    fun enterPIP() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val aspectRatio = Rational(ytVideoPlayerView.width, ytVideoPlayerView.height)
            enterPictureInPictureMode(
                PictureInPictureParams.Builder()
                    .setAspectRatio(aspectRatio)
                    .setSourceRectHint(ytVideoPlayerView.clipBounds)
                    .build()
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            enterPictureInPictureMode()
        }
    }

    private var wasFullScreen = false
    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration?
    ) {
        if (isInPictureInPictureMode) {
            wasFullScreen = ytVideoPlayerView.isFullScreen()
            if (wasFullScreen)
                ytVideoPlayerView.exitFullScreen()
        } else {
            if (wasFullScreen)
                ytVideoPlayerView.enterFullScreen()
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
