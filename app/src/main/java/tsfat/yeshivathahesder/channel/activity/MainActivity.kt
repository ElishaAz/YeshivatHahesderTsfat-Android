package tsfat.yeshivathahesder.channel.activity

import tsfat.yeshivathahesder.channel.R
import tsfat.yeshivathahesder.channel.locales.LocaleHelper
import tsfat.yeshivathahesder.channel.sharedpref.AppPref
import tsfat.yeshivathahesder.channel.utils.Tools
import tsfat.yeshivathahesder.channel.utils.media.MyNotificationManager
import android.content.Context
import android.media.AudioManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.Transformations
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.gms.cast.framework.CastContext
import kotlinx.android.synthetic.main.activity_main.*
import org.koin.android.ext.android.inject
import timber.log.Timber
import tsfat.yeshivathahesder.channel.di.AudioConnector
import tsfat.yeshivathahesder.channel.uamp.AudioItem
import tsfat.yeshivathahesder.channel.uamp.fragments.AudioItemFragment
import tsfat.yeshivathahesder.channel.uamp.utils.InjectorUtils
import tsfat.yeshivathahesder.channel.uamp.viewmodels.MainActivityViewModel
import tsfat.yeshivathahesder.channel.uamp.viewmodels.MediaItemFragmentViewModel

class MainActivity : AppCompatActivity() {

    private var initialLayoutComplete = false
    private lateinit var initialLocale: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initialLocale = AppPref.localeOverride

        audioOnCreate(savedInstanceState)

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


    /********************** UAMP code ********************************/

    private val viewModel by viewModels<MainActivityViewModel> {
        InjectorUtils.provideMainActivityViewModel(this)
    }

    //    private val mediaItemFragmentViewModel by viewModels<MediaItemFragmentViewModel> {
//        InjectorUtils.provideMediaItemFragmentViewModel(this, viewModel.rootMediaId.value!!)
//    }
    private lateinit var mediaItemViewModel: LiveData<MediaItemFragmentViewModel>

    //    private val _audioItems: MutableLiveData<List<AudioItem>> = MutableLiveData<List<AudioItem>>()
    private val audioConnector: AudioConnector by inject()

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
            Timber.e(e.stackTraceToString())
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

        mediaItemViewModel.observe(this) {
            if (it == null) {
                audioConnector.audioItems.value = null
            } else {
                it.audioItems.observe(this) { audioConnector.audioItems.value = it }
            }
        }

        audioConnector.playItem = {
            viewModel.playMedia(it, pauseAllowed = false)
        }

//        viewModel.isConnected.observe(this, Observer {
//            if (it) {
//                viewModel.playMedia(audioId, pauseAllowed = false)
//                viewModel.showFragment(NowPlayingFragment.newInstance())
//            }
//        })
//
//        /**
//         * Observe [MainActivityViewModel.navigateToFragment] for [Event]s that request a
//         * fragment swap.
//         */
//        viewModel.navigateToFragment.observe(this, Observer {
//            it?.getContentIfNotHandled()?.let { fragmentRequest ->
////                val transaction = supportFragmentManager.beginTransaction()
////                transaction.replace(
////                    R.id.fragmentContainer, fragmentRequest.fragment, fragmentRequest.tag
////                )
////                if (fragmentRequest.backStack) transaction.addToBackStack(null)
////                transaction.commit()
////                if (fragmentRequest.play)
//                findNavController(R.id.navHostFragment)
//                    .navigate(R.id.action_homeFragment_to_searchFragment)
//            }
//        })
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
//        /**
//         * Observe [MainActivityViewModel.navigateToMediaItem] for [Event]s indicating
//         * the user has requested to browse to a different [MediaItemData].
//         */
//        viewModel.navigateToMediaItem.observe(this, Observer {
//            it?.getContentIfNotHandled()?.let { mediaId ->
//                navigateToMediaItem(mediaId)
//            }
//        })
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
