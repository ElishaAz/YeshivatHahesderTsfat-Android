package tsfat.yeshivathahesder.channel.activity

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.view.View
import android.widget.SeekBar
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.Transformations
import androidx.navigation.Navigation
import androidx.navigation.ui.setupWithNavController
import coil.api.load
import com.google.android.gms.cast.framework.CastContext
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber
import tsfat.yeshivathahesder.channel.Channelify
import tsfat.yeshivathahesder.channel.R
import tsfat.yeshivathahesder.channel.databinding.ActivityMainBinding
import tsfat.yeshivathahesder.channel.di.AudioConnector
import tsfat.yeshivathahesder.channel.di.PlayVideo
import tsfat.yeshivathahesder.channel.locales.LocaleHelper
import tsfat.yeshivathahesder.channel.model.FavoritesEntry
import tsfat.yeshivathahesder.channel.paging.datasource.PLAYLIST_TYPE_AUDIO
import tsfat.yeshivathahesder.channel.sharedpref.AppPref
import tsfat.yeshivathahesder.channel.uamp.fragments.AudioItemFragment
import tsfat.yeshivathahesder.channel.uamp.media.library.UAMP_INFO_ID
import tsfat.yeshivathahesder.channel.uamp.utils.InjectorUtils
import tsfat.yeshivathahesder.channel.uamp.viewmodels.MainActivityViewModel
import tsfat.yeshivathahesder.channel.uamp.viewmodels.MediaItemFragmentViewModel
import tsfat.yeshivathahesder.channel.uamp.viewmodels.NowPlayingFragmentViewModel
import tsfat.yeshivathahesder.channel.utils.Tools
import tsfat.yeshivathahesder.channel.viewmodel.VideoDetailsViewModel
import tsfat.yeshivathahesder.core.extensions.*

class MainActivity : AppCompatActivity() {

    private var initialLayoutComplete = false
    private lateinit var initialLocale: String
    private var initialIsLight: Boolean = true

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initialLocale = AppPref.localeOverride

        AppPref.preferences.registerOnSharedPreferenceChangeListener(preferenceChangeListener)

        audioOnCreate(savedInstanceState)


        if (Channelify.isUpdateNotifyEnabled)
            Tools.showUpdateDialog(this, true)
//        MyNotificationManager.createNotificationChannel(this)

        val navController = Navigation.findNavController(this, R.id.navHostFragment)

        binding.bottomNavView.setupWithNavController(navController)

        binding.nowPlayingCard.makeInvisible()
    }

    override fun onResume() {
        super.onResume()
        checkLocaleChange()
    }

    private val preferenceChangeListener =
        SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
            when (key) {
                getString(R.string.preference_locale_key) -> checkLocaleChange()
                getString(R.string.preference_theme_key) -> checkThemeChange()
            }
        }

    private fun checkThemeChange() {
//        val isLight = AppPref.isLightThemeEnabled(this)
//        if (initialIsLight != isLight) {
//            if (isLight) {
//                setTheme(AppCompatDelegate.MODE_NIGHT_NO)
//            } else {
//                setTheme(AppCompatDelegate.MODE_NIGHT_YES)
//            }
//            initialIsLight = isLight
//            Timber.d("Theme changed!")
//        }
        AppCompatDelegate.setDefaultNightMode(AppPref.modeNight)
        setTheme(AppPref.modeNight)
    }

    private fun checkLocaleChange() {
        // Locale changes
        if (initialLocale != AppPref.localeOverride) {
            recreate();
            initialLocale = AppPref.localeOverride
            Timber.d("Locale changed!")
        }
    }

    // Locale changes
    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }


    /********************** UAMP code ********************************/

    private val viewModel by viewModels<MainActivityViewModel> {
        InjectorUtils.provideMainActivityViewModel(this)
    }
    private val nowPlayingViewModel by viewModels<NowPlayingFragmentViewModel> {
        InjectorUtils.provideNowPlayingFragmentViewModel(this)
    }

    //    private val mediaItemFragmentViewModel by viewModels<MediaItemFragmentViewModel> {
//        InjectorUtils.provideMediaItemFragmentViewModel(this, viewModel.rootMediaId.value!!)
//    }
    private lateinit var mediaItemViewModel: LiveData<MediaItemFragmentViewModel>

    //    private val _audioItems: MutableLiveData<List<AudioItem>> = MutableLiveData<List<AudioItem>>()
    private val audioConnector: AudioConnector by inject()
    private val playVideo: PlayVideo by inject()

    private var castContext: CastContext? = null

    //    private lateinit var audioId: String
    private var castEnable: Boolean = false;

    private fun audioOnCreate(savedInstanceState: Bundle?) {
        // Initialize the Cast context. This is required so that the media route button can be
        // created in the AppBar
        try {
            castContext = CastContext.getSharedInstance(this)
            castEnable = true
        } catch (e: RuntimeException) {
            Timber.e(e)
        }

//        setContentView(R.layout.activity_audio_main)

        mediaItemViewModel =
            Transformations.map(viewModel.rootMediaId) {
                if (it != null) {
                    val mediaItemFragmentViewModel by viewModels<MediaItemFragmentViewModel> {
                        InjectorUtils.provideMediaItemFragmentViewModel(
                            this,
                            it
                        )
                    }
                    mediaItemFragmentViewModel
                } else {
                    null
                }
            }

//        audioId = intent.getStringExtra(AudioPlayerActivity.AUDIO_ID)!!

        // Since UAMP is a music player, the volume controls should adjust the music volume while
        // in the app.
        volumeControlStream = AudioManager.STREAM_MUSIC

        mediaItemViewModel.observe(this) { mediaItemFragmentViewModel ->
            if (mediaItemFragmentViewModel == null) {
                audioConnector.audioItems.value = null
                audioConnector.playlists.value = null
            } else {
                mediaItemFragmentViewModel.audioItems.observe(this) {
                    Timber.d("%s", it)
                    if (it.isNotEmpty()) {
                        audioConnector.hashItems.value = true
                        audioConnector.audioItems.value =
                            it.filter { item -> item.mediaId != UAMP_INFO_ID }
                                .sortedBy { item -> item.publishedAt }
                    } else {
                        audioConnector.hashItems.value = false
                        audioConnector.audioItems.value = it
                    }
                }
                mediaItemFragmentViewModel.mediaPlaylists.observe(this) {
                    audioConnector.playlists.value = it
                }
            }
        }

        audioConnector.playItem = {
            Timber.d("Playing audio. Id: $it")
            nowPlayingMaximized.postValue(false)
            viewModel.playMediaId(it)
            binding.nowPlayingCard.makeVisible()
            sendBroadcast(VideoPlayerActivity.getPauseIntent())
        }
        playVideo.play = { context, id ->
            viewModel.pause()
            VideoPlayerActivity.startActivity(context, id)
        }

//        viewModel.isConnected.observe(this, Observer {
//            if (it) {
//                viewModel.playMedia(audioId, pauseAllowed = false)
//                viewModel.showFragment(NowPlayingFragment.newInstance())
//            }
//        })
//
        /**
         * Observe [MainActivityViewModel.navigateToFragment] for [Event]s that request a
         * fragment swap.
         */
        viewModel.navigateToFragment.observe(this, Observer {
            it?.getContentIfNotHandled()?.let { fragmentRequest ->
//                val transaction = supportFragmentManager.beginTransaction()
//                transaction.replace(
//                    R.id.fragmentContainer, fragmentRequest.fragment, fragmentRequest.tag
//                )
//                if (fragmentRequest.backStack) transaction.addToBackStack(null)
//                transaction.commit()
//                if (fragmentRequest.play)
//                findNavController(R.id.navHostFragment)
//                    .navigate(R.id.action_homeFragment_to_searchFragment)
                Timber.d("Navigate to fragment")
                binding.nowPlayingCard.makeVisible()
            }
        })
//
//        /**
//         * Observe changes to the [MainActivityViewModel.rootMediaId]. When the app starts,
//         * and the UI connects to [MusicService], this will be updated and the app will show
//         * the initial list of media items.
//         */
//        viewModel.rootMediaId.observe(this,
//            Observer<String> { rootMediaId ->
//                rootMediaId?.let { navigateToMediaItem(it) }
//            })
//
        /**
         * Observe [MainActivityViewModel.navigateToMediaItem] for [Event]s indicating
         * the user has requested to browse to a different [MediaItemData].
         */
        viewModel.navigateToMediaItem.observe(this, Observer {
//            it?.getContentIfNotHandled()?.let { mediaId ->
//                navigateToMediaItem(mediaId)
//            }

            Timber.d("Navigate to media item")
        })

        nowPlayingOnCreate()
    }

    private var updateSeekBar: Boolean = true
    private var nowPlayingMaximized = MutableLiveData(false)

    private fun nowPlayingOnCreate() {
        binding.nowPlayingCard.isClickable = true

        nowPlayingViewModel.mediaState.observe(this) {
            when (it) {
                PlaybackStateCompat.STATE_NONE, PlaybackStateCompat.STATE_STOPPED -> binding.nowPlayingCard.makeInvisible()
                else -> binding.nowPlayingCard.makeVisible()
            }
            when (it) {
                PlaybackStateCompat.STATE_CONNECTING, PlaybackStateCompat.STATE_BUFFERING -> {
                    binding.pbNowPlayingLoading.makeVisible()
                    binding.pbNowPlayingLoadingMax.makeVisible()
                }
                else -> {
                    binding.pbNowPlayingLoading.makeGone()
                    binding.pbNowPlayingLoadingMax.makeGone()
                }
            }
        }
        // Attach observers to the LiveData coming from this ViewModel
        nowPlayingViewModel.mediaMetadata.observe(this,
            Observer { mediaItem -> updateUI(mediaItem) })
        nowPlayingViewModel.mediaButtonRes.observe(this,
            Observer { res ->
                binding.playPauseButton.setImageResource(res)
                binding.playPauseButtonMax.setImageResource(res)
            })
        nowPlayingViewModel.mediaPosition.observe(this,
            Observer { pos ->
                binding.progressBar.isIndeterminate = false
                binding.seekBar.isIndeterminate = false
                binding.progressBar.progress = pos.toInt()
                if (updateSeekBar)
                    binding.seekBar.progress = pos.toInt()
                binding.position.text =
                    NowPlayingFragmentViewModel.NowPlayingMetadata.timestampToMSS(this, pos)
            })

        nowPlayingMaximized.observe(this) {
            binding.nowPlayingMaximized.visibility = if (it) View.VISIBLE else View.GONE
            binding.nowPlayingMinimized.visibility = if (it) View.GONE else View.VISIBLE
        }

        // Setup UI handlers for buttons
        binding.playPauseButton.setOnClickListener {
            nowPlayingViewModel.mediaMetadata.value?.let { viewModel.playMediaId(it.id) }
        }
        binding.playPauseButtonMax.setOnClickListener {
            nowPlayingViewModel.mediaMetadata.value?.let { viewModel.playMediaId(it.id) }
        }

        binding.stopButtonMax.setOnClickListener { viewModel.stop() }
        binding.forwardButtonMax.setOnClickListener { viewModel.skipBy(10 * 1000) }
        binding.replayButtonMax.setOnClickListener { viewModel.skipBy(10 * 1000) }

        binding.skipNextButtonMax.setOnClickListener { viewModel.skipToNext() }
        binding.skipPrevButtonMax.setOnClickListener { viewModel.skipToPrevious() }

        // seekbar
        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    viewModel.seekTo(progress.toLong())
                    Timber.d("Skiped to: $progress")
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                updateSeekBar = false
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                updateSeekBar = true
            }
        })

        binding.nowPlayingCard.setOnClickListener {
            nowPlayingMaximized.postValue(nowPlayingMaximized.value == false)
        }

        binding.nowPlayingCard.setOnFocusChangeListener { v, hasFocus ->
            if (!hasFocus) nowPlayingMaximized.postValue(
                false
            )
        }

        // Initialize playback duration and position to zero
        binding.duration.text =
            NowPlayingFragmentViewModel.NowPlayingMetadata.timestampToMSS(this, 0L)
        binding.position.text =
            NowPlayingFragmentViewModel.NowPlayingMetadata.timestampToMSS(this, 0L)
        binding.progressBar.isIndeterminate = true
        binding.seekBar.isIndeterminate = true

        // bind maximized to minimized
        binding.title.doOnTextChanged { text, start, before, count ->
            binding.titleMax.text = text
        }
        binding.subtitle.doOnTextChanged { text, start, before, count ->
            binding.subtitleMax.text = text
        }
        binding.duration.doOnTextChanged { text, start, before, count ->
            binding.durationMax.text = text
        }
        binding.position.doOnTextChanged { text, start, before, count ->
            binding.positionMax.text = text
        }

        linksOnCreate()
    }

    private val favoritesViewModel by viewModel<VideoDetailsViewModel>()
    private var isAudioAddedToFavorite = false

    private fun linksOnCreate() {
        favoritesViewModel.favoriteVideoLiveData.observe(
            this,
            Observer { isFavorite ->
                isAudioAddedToFavorite = isFavorite

                if (isAudioAddedToFavorite) {
                    binding.favoriteButton.setImageDrawable(
                        ContextCompat.getDrawable(
                            this,
                            R.drawable.ic_favorite_filled
                        )
                    )
                } else {
                    binding.favoriteButton.setImageDrawable(
                        ContextCompat.getDrawable(
                            this,
                            R.drawable.ic_favorite_border
                        )
                    )
                }
            })

        nowPlayingViewModel.mediaMetadata.observe(this, Observer { media ->
            favoritesViewModel.getVideoInfo(media.id)
            favoritesViewModel.getVideoFavoriteStatus(media.id)
        })

        binding.favoriteButton.setOnClickListener {
            val metadata = nowPlayingViewModel.mediaMetadata.value!!
            val favoriteVideo = FavoritesEntry(
                metadata.id,
                metadata.title ?: "", PLAYLIST_TYPE_AUDIO,
                metadata.albumArtUri.toString(),
                System.currentTimeMillis(),
                true
            )

            isAudioAddedToFavorite = if (isAudioAddedToFavorite) {
                binding.favoriteButton.setImageDrawable(
                    ContextCompat.getDrawable(
                        this,
                        R.drawable.ic_favorite_border
                    )
                )

                favoritesViewModel.removeVideoFromFavorites(favoriteVideo)
                false
            } else {
                // Add video to favorites
                binding.favoriteButton.setImageDrawable(
                    ContextCompat.getDrawable(
                        this,
                        R.drawable.ic_favorite_filled
                    )
                )
                favoritesViewModel.addVideoToFavorites(favoriteVideo)
                true
            }
        }
        binding.shareButton.setOnClickListener {
            this.startShareTextIntent(
                getString(R.string.text_share_video),
                getString(
                    R.string.audio_share_url,
                    nowPlayingViewModel.mediaMetadata.value!!.id
                )
            )
        }
        binding.openInBrowserButton.setOnClickListener {
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse(
                    getString(
                        R.string.audio_open_url,
                        nowPlayingViewModel.mediaMetadata.value!!.id
                    )
                )
            )
            startActivity(intent)
        }
    }

    /**
     * Internal function used to update all UI elements except for the current item playback
     */
    private fun updateUI(metadata: NowPlayingFragmentViewModel.NowPlayingMetadata) {
        with(binding) {
            //            if (metadata.albumArtUri == Uri.EMPTY) {
            //                albumArt.setImageResource(R.drawable.ic_album_black_24dp)
            //            } else {
            //                Glide.with(view)
            //                    .load(metadata.albumArtUri)
            //                    .into(albumArt)
            //            }
            title.text = metadata.title
            subtitle.text = metadata.subtitle
            duration.text = NowPlayingFragmentViewModel.NowPlayingMetadata.timestampToMSS(
                this@MainActivity,
                metadata.duration
            )
            with(progressBar) {
                max = metadata.duration.toInt()
                progress = 0
                isIndeterminate = true
            }
            with(seekBar) {
                max = metadata.duration.toInt()
                progress = 0
                isIndeterminate = true
            }

        }
    }

//    @Override
//    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
//        super.onCreateOptionsMenu(menu)
//        menuInflater.inflate(R.menu.main_activity_menu, menu)
//
//        /**
//         * Set up a MediaRouteButton to allow the user to control the current media playback route
//         */
//        if (castEnable)
//            CastButtonFactory.setUpMediaRouteButton(this, menu, R.id.media_route_menu_item)
//        return true
//    }

    private fun navigateToMediaItem(mediaId: String) {
        var fragment: AudioItemFragment? = getBrowseFragment(mediaId)
        if (fragment == null) {
            fragment = AudioItemFragment.newInstance(mediaId)
            // If this is not the top level media (root), we add it to the fragment
            // back stack, so that actionbar toggle and Back will work appropriately:
            viewModel.showFragment(fragment, !isRootId(mediaId), mediaId)
        }
    }

    private fun isRootId(mediaId: String) = mediaId == viewModel.rootMediaId.value

    private fun getBrowseFragment(mediaId: String): AudioItemFragment? {
        return supportFragmentManager.findFragmentByTag(mediaId) as? AudioItemFragment
    }
}
