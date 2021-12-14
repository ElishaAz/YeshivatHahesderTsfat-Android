package tsfat.yeshivathahesder.channel.utils.media

import tsfat.yeshivathahesder.channel.R
import android.annotation.SuppressLint
import android.content.Context
import android.media.MediaMetadata
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.NotificationCompat
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import android.content.Intent

import android.support.v4.media.session.MediaControllerCompat
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.media.session.MediaButtonReceiver

class MediaSessionController<T>(
    val context: Context,
    val ytVideoPlayerView: YouTubePlayerView,
    val mediaSession: MediaSessionCompat,
    val clazz: Class<T>
) {

    companion object {
        const val ACTION_MEDIA_CONTROL = "tsfat.yeshivathahesder.channel.MEDIA_CONTROL"
        const val EXTRA_MEDIA_CONTROL_ACTION = "tsfat.yeshivathahesder.channel.EXTRA_MEDIA_CONTROL"

        const val ACTION_PLAY = "action_play"
        const val ACTION_PAUSE = "action_pause"
        const val ACTION_REWIND = "action_rewind"
        const val ACTION_FAST_FORWARD = "action_fast_foward"
        const val ACTION_NEXT = "action_next"
        const val ACTION_PREVIOUS = "action_previous"
        const val ACTION_STOP = "action_stop"
    }


    private val mediaNotificationId: Int = 293587293 //Random.Default.nextInt()

    var youTubePlayer: YouTubePlayer? = null
    var second: Float = 0f
    var duration: Float = 0f
    var playbackRate: Float = 1F
    var state: PlayerConstants.PlayerState = PlayerConstants.PlayerState.UNSTARTED

    lateinit var mController: MediaControllerCompat

    var notificationBuilder: NotificationCompat.Builder? = null

    private val listener = object : AbstractYouTubePlayerListener() {


        override fun onStateChange(
            youTubePlayer: YouTubePlayer,
            state: PlayerConstants.PlayerState
        ) {
            this@MediaSessionController.state = state
            buildNotification()
        }

        override fun onPlaybackRateChange(
            youTubePlayer: YouTubePlayer,
            playbackRate: PlayerConstants.PlaybackRate
        ) {
            this@MediaSessionController.playbackRate = when (playbackRate) {
                PlayerConstants.PlaybackRate.UNKNOWN -> 1F
                PlayerConstants.PlaybackRate.RATE_0_25 -> 0.25F
                PlayerConstants.PlaybackRate.RATE_0_5 -> 0.5F
                PlayerConstants.PlaybackRate.RATE_1 -> 1F
                PlayerConstants.PlaybackRate.RATE_1_5 -> 1.5F
                PlayerConstants.PlaybackRate.RATE_2 -> 2F
            }
            updateState()
        }

        override fun onCurrentSecond(youTubePlayer: YouTubePlayer, second: Float) {
            super.onCurrentSecond(youTubePlayer, second)
            this@MediaSessionController.second = second
            updateState()
        }

        override fun onReady(youTubePlayer: YouTubePlayer) {
            this@MediaSessionController.youTubePlayer = youTubePlayer
        }

        override fun onVideoDuration(youTubePlayer: YouTubePlayer, duration: Float) {
            this@MediaSessionController.duration = duration
            updateMetadata()
        }
    }

    val callback = object : MediaSessionCompat.Callback() {
        override fun onPlay() {
            if (state == PlayerConstants.PlayerState.PAUSED || state == PlayerConstants.PlayerState.UNSTARTED)
                youTubePlayer?.play()
            Log.d("SessionCallback", "onPlay")
        }

        override fun onPause() {
            youTubePlayer?.pause()
            Log.d("SessionCallback", "onPause")
        }

        override fun onStop() {
            youTubePlayer?.seekTo(0F)
            youTubePlayer?.pause()
            Log.d("SessionCallback", "onStop")
        }

        override fun onSeekTo(pos: Long) {
            val time = (pos / 1000.0).toFloat() // convert ms to s
            youTubePlayer?.seekTo(time)
            Log.d("SessionCallback", "onSeekTo " + pos)
        }

    }

    fun updateMetadata() {
        mediaSession.setMetadata(
            MediaMetadataCompat.Builder()

                // Title.
                .putString(MediaMetadata.METADATA_KEY_TITLE, "Title")

                // Artist.
                // Could also be the channel name or TV series.
                .putString(MediaMetadata.METADATA_KEY_ARTIST, "Artist")

                // Album art.
                // Could also be a screenshot or hero image for video content
                // The URI scheme needs to be "content", "file", or "android.resource".
//                    .putString(
//                        MediaMetadata.METADATA_KEY_ALBUM_ART_URI, currentTrack.albumArtUri
//                    )


                // Duration.
                // If duration isn't set, such as for live broadcasts, then the progress
                // indicator won't be shown on the seekbar.
                .putLong(MediaMetadata.METADATA_KEY_DURATION, (duration * 1000).toLong()) // 4

                .build()
        )
        mediaSession.isActive = true
//        showNotification()
        buildNotification()

//        showNotification()
    }

    private fun getPlaybackState(): Int {
        when (state) {
            PlayerConstants.PlayerState.UNKNOWN -> PlaybackStateCompat.STATE_NONE
            PlayerConstants.PlayerState.UNSTARTED -> PlaybackStateCompat.STATE_PAUSED
            PlayerConstants.PlayerState.ENDED -> PlaybackStateCompat.STATE_STOPPED
            PlayerConstants.PlayerState.PLAYING -> PlaybackStateCompat.STATE_PLAYING
            PlayerConstants.PlayerState.PAUSED -> PlaybackStateCompat.STATE_PAUSED
            PlayerConstants.PlayerState.BUFFERING -> PlaybackStateCompat.STATE_BUFFERING
            PlayerConstants.PlayerState.VIDEO_CUED -> PlaybackStateCompat.STATE_CONNECTING
        }
        return PlaybackStateCompat.STATE_NONE
    }

    var lastState = PlayerConstants.PlayerState.UNSTARTED

    fun updateState() {
        val playPauseAction =
            if (state == PlayerConstants.PlayerState.PLAYING) PlaybackStateCompat.ACTION_PAUSE
            else PlaybackStateCompat.ACTION_PLAY

        mediaSession.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setState(
                    getPlaybackState(),

                    // Playback position.
                    // Used to update the elapsed time and the progress bar.
                    (second * 1000).toLong(),

                    // Playback speed.
                    // Determines the rate at which the elapsed time changes.
                    playbackRate
                )

                // isSeekable.
                // Adding the SEEK_TO action indicates that seeking is supported
                // and makes the seekbar position marker draggable. If this is not
                // supplied seek will be disabled but progress will still be shown.
//                .setActions(
//                    PlaybackStateCompat.ACTION_SEEK_TO or playPauseAction or PlaybackStateCompat.ACTION_STOP
//                )
                .setActions(PlaybackStateCompat.ACTION_SEEK_TO)
                .build()
        )
//        showNotification()
        if (lastState != state) {
            lastState = state
            buildNotification()
        }
    }


    fun handleIntent(intent: Intent?) {
        if (intent == null || !intent.action.equals(ACTION_MEDIA_CONTROL)) return
        val action = intent.getStringExtra(EXTRA_MEDIA_CONTROL_ACTION) ?: return
        //        val action = intent.action
        when (action) {
            ACTION_PLAY -> mController.transportControls.play()
            ACTION_PAUSE -> mController.transportControls.pause()
            ACTION_FAST_FORWARD -> mController.transportControls.fastForward()
            ACTION_REWIND -> mController.transportControls.rewind()
            ACTION_PREVIOUS -> mController.transportControls.skipToPrevious()
            ACTION_NEXT -> mController.transportControls.skipToNext()
            ACTION_STOP -> mController.transportControls.stop()
        }
    }


    private fun generateAction(
        icon: Int,
        title: String,
        intentAction: String
    ): NotificationCompat.Action {
        val intent = Intent(ACTION_MEDIA_CONTROL)
        intent.putExtra(EXTRA_MEDIA_CONTROL_ACTION, intentAction)
        val pendingIntent =
            PendingIntent.getBroadcast(
                context,
                0,
                intent,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
            )
        return NotificationCompat.Action.Builder(icon, title, pendingIntent).build()
    }

    private var playAction: NotificationCompat.Action? = null

    @SuppressLint("RestrictedApi")
    private fun buildNotification() {

        val showPlayAction = state != PlayerConstants.PlayerState.PLAYING
        val action =
            if (showPlayAction) generateAction(
                android.R.drawable.ic_media_play,
                "Play",
                ACTION_PLAY
            ) else
                generateAction(android.R.drawable.ic_media_pause, "Pause", ACTION_PAUSE)

        if (notificationBuilder == null) {

            playAction = action

            val style = androidx.media.app.NotificationCompat.MediaStyle()
            style.setMediaSession(mediaSession.sessionToken)
//            style.setShowActionsInCompactView(2)

            val openIntent = Intent(context, clazz)
            openIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            val openPendingIntent =
                PendingIntent.getActivity(
                    context,
                    0,
                    openIntent,
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
                )

            val stopIntent = Intent(ACTION_MEDIA_CONTROL)
            stopIntent.putExtra(EXTRA_MEDIA_CONTROL_ACTION, ACTION_STOP)
            val stopPendingIntent =
                PendingIntent.getBroadcast(
                    context,
                    0,
                    stopIntent,
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
                )

            val builder: NotificationCompat.Builder =
                NotificationCompat.Builder(context, MyNotificationManager.CHANNEL_ID).apply {

                    setOnlyAlertOnce(true)
                    setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    setSmallIcon(R.drawable.yhtsfat_logo)
                    setContentTitle("Media Title")
                    setContentText("Media Artist")
                    setDeleteIntent(stopPendingIntent)
                    setContentIntent(openPendingIntent)
                    setStyle(style)
                    addAction(
                        generateAction(
                            android.R.drawable.ic_media_previous,
                            "Previous",
                            ACTION_PREVIOUS
                        )
                    )
                    addAction(
                        generateAction(
                            android.R.drawable.ic_media_rew,
                            "Rewind",
                            ACTION_REWIND
                        )
                    )
                    addAction(action)
                    addAction(
                        generateAction(
                            android.R.drawable.ic_media_ff, "Fast Forward", ACTION_FAST_FORWARD
                        )
                    )
                    addAction(generateAction(android.R.drawable.ic_media_next, "Next", ACTION_NEXT))
                }
            notificationBuilder = builder
        } else {
            notificationBuilder?.mActions?.set(2, action)
            playAction?.actionIntent = action.actionIntent
        }

        with(NotificationManagerCompat.from(context)) {
            // notificationId is a unique int for each notification that you must define
            notify(mediaNotificationId, notificationBuilder!!.build())
        }
    }

    var firstTime = true

    private fun showNotification() {
        if (!firstTime) return
        // Given a media session and its context (usually the component containing the session)
// Create a NotificationCompat.Builder

// Get the session's metadata
        val controller = mediaSession.controller
        val mediaMetadata = controller.metadata ?: return
        firstTime = false

        val description = mediaMetadata.description

        val builder = NotificationCompat.Builder(context, MyNotificationManager.CHANNEL_ID).apply {
            // Add the metadata for the currently playing track
            setContentTitle(description.title)
            setContentText(description.subtitle)
            setSubText(description.description)
            setLargeIcon(description.iconBitmap)

            // Enable launching the player by clicking the notification
            setContentIntent(controller.sessionActivity)

            // Stop the service when the notification is swiped away
            setDeleteIntent(
                MediaButtonReceiver.buildMediaButtonPendingIntent(
                    context,
                    PlaybackStateCompat.ACTION_STOP
                )
            )

            // Make the transport controls visible on the lockscreen
            setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

            // Add an app icon and set its accent color
            // Be careful about the color
            setSmallIcon(R.drawable.yhtsfat_logo)
            color = ContextCompat.getColor(context, R.color.colorPrimaryDark)

            // Add a pause button
            addAction(
                NotificationCompat.Action(
                    android.R.drawable.ic_media_pause,
                    "Pause",
                    MediaButtonReceiver.buildMediaButtonPendingIntent(
                        context,
                        PlaybackStateCompat.ACTION_PLAY_PAUSE
                    )
                )
            )

            // Take advantage of MediaStyle features
            setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0)

                    // Add a cancel button
                    .setShowCancelButton(true)
                    .setCancelButtonIntent(
                        MediaButtonReceiver.buildMediaButtonPendingIntent(
                            context,
                            PlaybackStateCompat.ACTION_STOP
                        )
                    )
            )
        }

// Display the notification and place the service in the foreground
//        context.startForeground(id, builder.build())

        with(NotificationManagerCompat.from(context)) {
            // notificationId is a unique int for each notification that you must define
            notify(mediaNotificationId, builder.build())
        }
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d("MediaSessionController", intent.toString())
            handleIntent(intent)
            intent?.getStringExtra(EXTRA_MEDIA_CONTROL_ACTION)?.let {
                Log.d("MediaSessionController", it)
            }
        }
    }


    init {
        ytVideoPlayerView.addYouTubePlayerListener(listener)

        mediaSession.setCallback(callback)
        mController = MediaControllerCompat(context, mediaSession)

        val filter = IntentFilter(ACTION_MEDIA_CONTROL)

        context.registerReceiver(receiver, filter)
    }

    fun finish() {
        context.unregisterReceiver(receiver)
    }
}