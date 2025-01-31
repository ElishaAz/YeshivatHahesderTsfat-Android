package tsfat.yeshivathahesder.channel.fragment


import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View

import tsfat.yeshivathahesder.channel.R
import tsfat.yeshivathahesder.channel.model.ChannelInfo
import tsfat.yeshivathahesder.channel.utils.DateTimeUtils
import tsfat.yeshivathahesder.channel.utils.Tools
import tsfat.yeshivathahesder.channel.viewmodel.AboutViewModel
import tsfat.yeshivathahesder.core.extensions.*
import tsfat.yeshivathahesder.core.helper.ResultWrapper
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintSet
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import coil.api.load
import org.koin.androidx.viewmodel.ext.android.viewModel
import tsfat.yeshivathahesder.channel.Channelify
import tsfat.yeshivathahesder.channel.databinding.FragmentAboutBinding

/**
 * A simple [Fragment] subclass.
 */
class AboutFragment : Fragment() {

    private val viewModel by viewModel<AboutViewModel>() // Lazy inject ViewModel

    private lateinit var channelInfoItem: ChannelInfo.Item

    private lateinit var binding: FragmentAboutBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAboutBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()

        setupObservables()
        fetchChannelInfo()

        binding.btnRetryAbout.setOnClickListener {
            fetchChannelInfo()
        }

        binding.btnSubscribeAbout.setOnClickListener {
            startYouTubeIntent()
        }
    }

    private fun setupToolbar() {
        binding.ablAbout.toolbarMain.apply {
            inflateMenu(R.menu.toolbar_menu_about)

            // Store configuration
            menu.findItem(R.id.miStoreAbout).isVisible = resources.getBoolean(R.bool.enable_store)

            menu.findItem(R.id.miCheckUpdate).isVisible = Channelify.isUpdateNotifyEnabled

            // MenuItem onclick
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.miStoreAbout -> {
                        context.openUrl(getString(R.string.store_url))
                    }
                    R.id.miCheckUpdate -> {
                        if (Channelify.isUpdateNotifyEnabled)
                            Tools.showUpdateDialog(context, false)
                    }
                    R.id.miThemeAbout -> {
                        findNavController().navigate(R.id.action_aboutFragment_to_settingsFragment)
                    }
                    R.id.miAppInfoAbout -> {
                        findNavController().navigate(R.id.action_aboutFragment_to_appInfoFragment)
                    }
                }
                false
            }
        }
    }

    /**
     * Fetches the info of channel
     */
    private fun fetchChannelInfo() {
        if (isInternetAvailable(requireContext())) {
            viewModel.getChannelInfo()
        } else {
            showChannelInfoErrorState()
        }
    }

    private fun setupObservables() {
        viewModel.channelInfoLiveData.observe(viewLifecycleOwner, Observer {
            when (it) {
                is ResultWrapper.Loading -> {
                    binding.pbAbout.makeVisible()
                }
                is ResultWrapper.Error -> {
                    binding.pbAbout.makeGone()
                    showChannelInfoErrorState()
                }
                is ResultWrapper.Success<*> -> {
                    binding.pbAbout.makeGone()
                    hideChannelInfoErrorState()
                    channelInfoItem = (it.data as ChannelInfo).items[0]
                    setChannelInfo()
                }
            }
        })
    }

    private fun setChannelInfo() {

        // Banner
        if (channelInfoItem.brandingSettings != null) {
            binding.ivBannerAbout.load(
                channelInfoItem.brandingSettings?.image?.bannerMobileHdImageUrl
                    ?: channelInfoItem.brandingSettings?.image?.bannerMobileMediumHdImageUrl
            )
        } else {
            binding.ivBannerAbout.makeGone()
            val constraintSet = ConstraintSet().apply {
                clone(binding.clAbout)
                connect(R.id.cvAbout, ConstraintSet.TOP, R.id.ablAbout, ConstraintSet.BOTTOM)
            }
            constraintSet.applyTo(binding.clAbout)
        }

        // Profile Image
        binding.ivProfileAbout.load(
            channelInfoItem.snippet.thumbnails.high?.url
                ?: channelInfoItem.snippet.thumbnails.medium.url
        )

        binding.tvNameAbout.text = channelInfoItem.snippet.title
        binding.tvJoinDateAbout.text = getString(
            R.string.text_channel_join_date,
            DateTimeUtils.getPublishedDate(channelInfoItem.snippet.publishedAt)
        )
        binding.tvSubscribersValueAbout.text =
            channelInfoItem.statistics.subscriberCount.toLong().getFormattedNumberInString()
        binding.tvVideosValueAbout.text =
            channelInfoItem.statistics.videoCount.toLong().getFormattedNumberInString()
        binding.tvViewsValueAbout.text =
            channelInfoItem.statistics.viewCount.toLong().getFormattedNumberInString()
        binding.tvDescAbout.text = channelInfoItem.snippet.description
    }

    private fun showChannelInfoErrorState() {
        binding.groupResultAbout.makeGone()
        binding.groupErrorAbout.makeVisible()
    }

    private fun hideChannelInfoErrorState() {
        binding.groupErrorAbout.makeGone()
        binding.groupResultAbout.makeVisible()
    }

    /**
     * Opens the YouTube app's Channel screen if YouTube app is installed otherwise opens the URL
     * in the browser.
     */
    private fun startYouTubeIntent() {
        try {
            val youtubeIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse(
                    getString(
                        R.string.text_youtube_channel_intent_url,
                        getString(R.string.channel_id)
                    )
                )
            )
            startActivity(youtubeIntent)
        } catch (exception: ActivityNotFoundException) {
            context?.openUrl(
                getString(
                    R.string.text_youtube_channel_intent_url,
                    getString(R.string.channel_id)
                )
            )
        }
    }

//    @SuppressLint("CheckResult")
//    private fun showThemeChooserDialog() {
//        val langList = resources.getStringArray(R.array.locales_values)
//        val langListNames = resources.getStringArray(R.array.locales_entries).asList()
//
//        val currentLangIndex = if (langList.contains(AppPref.localeOverride))
//            langList.indexOf(AppPref.localeOverride) else 0
//
//
//        MaterialDialog(requireContext()).show {
//            title(R.string.dialog_settings_title)
//            checkBoxPrompt(
//                R.string.dialog_theme_text_dark,
//                isCheckedDefault = !AppPref.isLightThemeEnabled(requireContext())
//            ) {
//                if (it) {
//                    setTheme(AppCompatDelegate.MODE_NIGHT_YES)
//                } else {
//                    setTheme(AppCompatDelegate.MODE_NIGHT_NO)
//                }
//            }
//            listItemsSingleChoice(
//                items = langListNames,
//                initialSelection = currentLangIndex
//            ) { dialog, index, text ->
//                AppPref.localeOverride = langList[index]
//                LocaleHelper.setLocale(context, AppPref.localeOverride)
//                val parent = activity
//                if (parent is MainActivity)
//                    parent.checkLocaleChange()
//            }
//
//            negativeButton(R.string.dialog_negative_button) {
//            }
//            positiveButton(R.string.dialog_positive_button) {
//            }
//
////            listItemsSingleChoice(
////                items = themeList,
////                initialSelection = currentThemeIndex
////            ) { dialog, index, text ->
////                when (text) {
////                    getString(R.string.dialog_theme_text_light) -> {
////                        setTheme(AppCompatDelegate.MODE_NIGHT_NO)
////                    }
////                    getString(R.string.dialog_theme_text_dark) -> {
////                        setTheme(AppCompatDelegate.MODE_NIGHT_YES)
////                    }
////                }
////            }
//        }
//    }
}

