/*
 * Copyright 2017 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package tsfat.yeshivathahesder.channel.uamp

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.view.Menu
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import tsfat.yeshivathahesder.channel.uamp.fragments.AudioItemFragment
import tsfat.yeshivathahesder.channel.uamp.utils.InjectorUtils
import tsfat.yeshivathahesder.channel.uamp.viewmodels.MainActivityViewModel
import com.google.android.gms.cast.framework.CastButtonFactory
import com.google.android.gms.cast.framework.CastContext
import timber.log.Timber
import tsfat.yeshivathahesder.channel.R
import tsfat.yeshivathahesder.channel.activity.AudioPlayerActivity

class AudioMainActivity : AppCompatActivity() {

    companion object {
        private const val AUDIO_ID = "audio_id"

        fun startActivity(context: Context?, audioId: String) {
            val intent = Intent(context, AudioMainActivity::class.java).apply {
                putExtra(AUDIO_ID, audioId)
            }
            context?.startActivity(intent)
        }
    }

    private val viewModel by viewModels<MainActivityViewModel> {
        InjectorUtils.provideMainActivityViewModel(this)
    }
    private var castContext: CastContext? = null

    private lateinit var audioId: String
    private var castEnable: Boolean = false;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize the Cast context. This is required so that the media route button can be
        // created in the AppBar
        try {
            castContext = CastContext.getSharedInstance(this)
            castEnable = true
        } catch (e: RuntimeException) {
            Timber.e(e.stackTraceToString())
        }

        setContentView(R.layout.activity_audio_main)

        audioId = intent.getStringExtra(AudioPlayerActivity.AUDIO_ID)!!

        // Since UAMP is a music player, the volume controls should adjust the music volume while
        // in the app.
        volumeControlStream = AudioManager.STREAM_MUSIC

//        viewModel.isConnected.observe(this, Observer {
//            if (it) {
//                viewModel.playMedia(audioId, pauseAllowed = false)
//                viewModel.showFragment(NowPlayingFragment.newInstance())
//            }
//        })

        /**
         * Observe [MainActivityViewModel.navigateToFragment] for [Event]s that request a
         * fragment swap.
         */
        viewModel.navigateToFragment.observe(this, Observer {
            it?.getContentIfNotHandled()?.let { fragmentRequest ->
                val transaction = supportFragmentManager.beginTransaction()
                transaction.replace(
                    R.id.fragmentContainer, fragmentRequest.fragment!!, fragmentRequest.tag
                )
                if (fragmentRequest.backStack) transaction.addToBackStack(null)
                transaction.commit()
            }
        })

        /**
         * Observe changes to the [MainActivityViewModel.rootMediaId]. When the app starts,
         * and the UI connects to [MusicService], this will be updated and the app will show
         * the initial list of media items.
         */
        viewModel.rootMediaId.observe(this,
            Observer<String> { rootMediaId ->
                rootMediaId?.let { navigateToMediaItem(it) }
            })

        /**
         * Observe [MainActivityViewModel.navigateToMediaItem] for [Event]s indicating
         * the user has requested to browse to a different [MediaItemData].
         */
        viewModel.navigateToMediaItem.observe(this, Observer {
            it?.getContentIfNotHandled()?.let { mediaId ->
                navigateToMediaItem(mediaId)
            }
        })
    }

    @Override
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.main_activity_menu, menu)

        /**
         * Set up a MediaRouteButton to allow the user to control the current media playback route
         */
        if (castEnable)
            CastButtonFactory.setUpMediaRouteButton(this, menu, R.id.media_route_menu_item)
        return true
    }

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
