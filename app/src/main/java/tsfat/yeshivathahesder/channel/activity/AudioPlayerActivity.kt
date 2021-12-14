package tsfat.yeshivathahesder.channel.activity

import tsfat.yeshivathahesder.channel.R
import tsfat.yeshivathahesder.channel.locales.LocaleHelper
import tsfat.yeshivathahesder.channel.viewmodel.AudioPlayerViewModel
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.koin.androidx.viewmodel.ext.android.viewModel

class AudioPlayerActivity : AppCompatActivity(R.layout.activity_video_player) {
    companion object {
        const val AUDIO_ID = "audio_id"

        fun startActivity(context: Context?, audioId: String) {
            val intent = Intent(context, AudioPlayerActivity::class.java).apply {
                putExtra(AUDIO_ID, audioId)
            }
            context?.startActivity(intent)
        }
    }


    private val viewModel by viewModel<AudioPlayerViewModel>() // Lazy inject ViewModel

    private lateinit var audioId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        audioId = intent.getStringExtra(AUDIO_ID)!!

    }

    // Locale changes
    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }
}
