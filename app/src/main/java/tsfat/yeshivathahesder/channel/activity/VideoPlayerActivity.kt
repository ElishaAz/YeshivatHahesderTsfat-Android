package tsfat.yeshivathahesder.channel.activity

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.support.v4.media.session.MediaSessionCompat
import android.util.Rational
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.navigation.findNavController
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.YouTubePlayerFullScreenListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.utils.loadOrCueVideo
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import com.pierfrancescosoffritti.androidyoutubeplayer.core.ui.DefaultPlayerUiController
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber
import tsfat.yeshivathahesder.channel.Channelify
import tsfat.yeshivathahesder.channel.R
import tsfat.yeshivathahesder.channel.databinding.ActivityVideoPlayerBinding
import tsfat.yeshivathahesder.channel.fragment.VideoDetailsFragment
import tsfat.yeshivathahesder.channel.locales.LocaleHelper
import tsfat.yeshivathahesder.channel.sharedpref.AppPref
import tsfat.yeshivathahesder.channel.utils.FullScreenHelper
import tsfat.yeshivathahesder.channel.utils.media.MediaSessionController
import tsfat.yeshivathahesder.channel.viewmodel.VideoPlayerViewModel


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
//            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            context?.startActivity(intent)
        }

        fun getPauseIntent(): Intent {
            val intent = Intent(ACTION_MEDIA_CONTROL)
            intent.putExtra(EXTRA_MEDIA_CONTROL_ACTION, ACTION_PAUSE)
            return intent
        }

        private val autoEnterPIP_A12: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

        const val ACTION_MEDIA_CONTROL = "tsfat.yeshivathahesder.channel.PIP_CONTROL"
        const val EXTRA_MEDIA_CONTROL_ACTION = "tsfat.yeshivathahesder.channel.EXTRA_PIP_CONTROL"

        const val ACTION_REPLAY = "action_replay"
        const val ACTION_FORWARD = "action_forward"
        const val ACTION_PLAY = "action_play"
        const val ACTION_PAUSE = "action_pause"
        const val ACTION_PLAY_PAUSE = "action_play_pause"
//        const val ACTION_REWIND = "action_rewind"
//        const val ACTION_FAST_FORWARD = "action_fast_foward"
//        const val ACTION_NEXT = "action_next"
//        const val ACTION_PREVIOUS = "action_previous"
//        const val ACTION_STOP = "action_stop"
    }

    private lateinit var binding: ActivityVideoPlayerBinding

    private val viewModel by viewModel<VideoPlayerViewModel>() // Lazy inject ViewModel

    lateinit var fullScreenHelper: FullScreenHelper
    lateinit var videoId: String
    private var videoElapsedTimeInSeconds = 0f

    private var autoEnterPIP: Boolean = true
    private var shouldAutoEnterPIP = false
    private var isPlaying = false

    private lateinit var ytVideoPlayerView: YouTubePlayerView
    private var ytPlayer: YouTubePlayer? = null


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

        val filter = IntentFilter(ACTION_MEDIA_CONTROL)
        registerReceiver(receiver, filter)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        intent ?: return

        videoId = intent.getStringExtra(VIDEO_ID)!!

        findNavController(R.id.navHostVideoPlayer).setGraph(
            R.navigation.video_player_graph,
            bundleOf(VideoDetailsFragment.VIDEO_ID to videoId)
        )
        ytPlayer?.loadOrCueVideo(lifecycle, videoId, 0f)
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

    override fun onStop() {
        super.onStop()
        if (isTaskRoot)
            finishAndRemoveTask()
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

        val listener = object : AbstractYouTubePlayerListener() {
            override fun onReady(youTubePlayer: YouTubePlayer) {
                youTubePlayer.loadOrCueVideo(lifecycle, videoId, 0f)
                addFullScreenListenerToPlayer()
                setupCustomActions(youTubePlayer)
                ytPlayer = youTubePlayer
            }

            override fun onCurrentSecond(youTubePlayer: YouTubePlayer, second: Float) {
                videoElapsedTimeInSeconds = second
            }

            override fun onStateChange(
                youTubePlayer: YouTubePlayer,
                state: PlayerConstants.PlayerState
            ) {
                shouldAutoEnterPIP = state == PlayerConstants.PlayerState.PLAYING
                isPlaying =
                    state == PlayerConstants.PlayerState.PLAYING || state == PlayerConstants.PlayerState.BUFFERING

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    setPictureInPictureParams(createPIPParams())
                }
            }
        }

//        // disable iframe ui
//        val options: IFramePlayerOptions = IFramePlayerOptions.Builder().controls(0).build()
//        ytVideoPlayerView.initialize(listener, options)
        ytVideoPlayerView.addYouTubePlayerListener(listener)

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

    override fun onUserLeaveHint() {
        if (autoEnterPIP /* && !autoEnterPIP_A12 */ && shouldAutoEnterPIP) {
            enterPIP()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun generateAction(
        icon: Int,
        title: String,
        intentAction: String
    ): RemoteAction {
        val intent = Intent(ACTION_MEDIA_CONTROL)
//        intent.setClass(this, receiver.javaClass)
        intent.putExtra(EXTRA_MEDIA_CONTROL_ACTION, intentAction)
        val pendingIntent =
            PendingIntent.getBroadcast(
                this,
                0,
                intent,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
            )
        return RemoteAction(
            Icon.createWithResource(this, icon),
            title,
            title,
            pendingIntent
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun createPIPParams(): PictureInPictureParams {
        val aspectRatio = Rational(ytVideoPlayerView.width, ytVideoPlayerView.height)

        Timber.d("Playing: $isPlaying")

//        val backTen = generateAction(
//            R.drawable.ic_rewind, "Replay 10s", ACTION_REPLAY
//        )
        val playPauseAction = if (!isPlaying) generateAction(
            R.drawable.ic_play_arrow_black_24dp, "Play", ACTION_PLAY_PAUSE
        ) else generateAction(
            R.drawable.ic_pause_black_24dp, "Pause", ACTION_PLAY_PAUSE
        )
//        val forwardTen = generateAction(
//            R.drawable.ic_forward, "Forward 10s", ACTION_FORWARD
//        )

        val actionList = if (maxNumPictureInPictureActions >= 3) listOf(
//            backTen,
            playPauseAction
//            ,
//            forwardTen
        ) else listOf(playPauseAction)
        return PictureInPictureParams.Builder()
            .setActions(actionList)
            .setAspectRatio(aspectRatio)
            .setSourceRectHint(ytVideoPlayerView.clipBounds)
            .build()
    }

    fun enterPIP() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            enterPictureInPictureMode(createPIPParams())
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

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Timber.d(intent.toString())
            handleBroadcast(intent)
            intent?.getStringExtra(EXTRA_MEDIA_CONTROL_ACTION)?.let {
                Timber.d(it)
            }
        }
    }

    private fun handleBroadcast(intent: Intent?) {
        intent ?: return
        val player = ytPlayer ?: return
        if (intent.action != ACTION_MEDIA_CONTROL) return
        val action = intent.getStringExtra(EXTRA_MEDIA_CONTROL_ACTION) ?: return
        when (action) {
            ACTION_PLAY -> player.play()
            ACTION_PAUSE -> player.pause()
            ACTION_REPLAY -> {
                videoElapsedTimeInSeconds -= resources.getInteger(R.integer.video_rewind_seconds)
                player.seekTo(videoElapsedTimeInSeconds)
            }
            ACTION_FORWARD -> {
                videoElapsedTimeInSeconds += resources.getInteger(R.integer.video_forward_seconds)
                player.seekTo(videoElapsedTimeInSeconds)
            }
            ACTION_PLAY_PAUSE -> if (isPlaying) player.pause() else player.play()
        }
    }

    /**
     * Adds the forward and rewind action button to the Player
     */
    private fun setupCustomActions(youTubePlayer: YouTubePlayer) {
        val defaultPlayerUiController = DefaultPlayerUiController(ytVideoPlayerView, youTubePlayer)
        ytVideoPlayerView.setCustomPlayerUi(defaultPlayerUiController.rootView)

        defaultPlayerUiController.showFullscreenButton(true)
        defaultPlayerUiController.showYouTubeButton(false)

        defaultPlayerUiController
            .setCustomAction1(
                ContextCompat.getDrawable(this, R.drawable.ic_rewind)!!
            ) {
                videoElapsedTimeInSeconds -= resources.getInteger(R.integer.video_rewind_seconds)
                Timber.d("Seeking to $videoElapsedTimeInSeconds")
                youTubePlayer.seekTo(videoElapsedTimeInSeconds)
            }
            .setCustomAction2(
                ContextCompat.getDrawable(this, R.drawable.ic_forward)!!
            ) {
                videoElapsedTimeInSeconds += resources.getInteger(R.integer.video_forward_seconds)
                Timber.d("Seeking to $videoElapsedTimeInSeconds")
                youTubePlayer.seekTo(videoElapsedTimeInSeconds)
            }
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
