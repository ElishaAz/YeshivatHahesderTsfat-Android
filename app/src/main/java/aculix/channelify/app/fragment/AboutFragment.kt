package aculix.channelify.app.fragment


import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View

import aculix.channelify.app.R
import aculix.channelify.app.activity.MainActivity
import aculix.channelify.app.locales.LocaleHelper
import aculix.channelify.app.model.ChannelInfo
import aculix.channelify.app.sharedpref.AppPref
import aculix.channelify.app.utils.DateTimeUtils
import aculix.channelify.app.utils.Tools
import aculix.channelify.app.viewmodel.AboutViewModel
import aculix.core.extensions.*
import aculix.core.helper.ResultWrapper
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import coil.api.load
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.checkbox.checkBoxPrompt
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import com.github.javiersantos.appupdater.AppUpdater
import com.github.javiersantos.appupdater.enums.Display
import com.github.javiersantos.appupdater.enums.UpdateFrom
import kotlinx.android.synthetic.main.fragment_about.*
import kotlinx.android.synthetic.main.widget_toolbar.view.*
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * A simple [Fragment] subclass.
 */
class AboutFragment : Fragment(R.layout.fragment_about) {

    private val viewModel by viewModel<AboutViewModel>() // Lazy inject ViewModel

    private lateinit var channelInfoItem: ChannelInfo.Item

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()

        setupObservables()
        fetchChannelInfo()

        btnRetryAbout.setOnClickListener {
            fetchChannelInfo()
        }

        btnSubscribeAbout.setOnClickListener {
            startYouTubeIntent()
        }
    }

    private fun setupToolbar() {
        ablAbout.toolbarMain.apply {
            inflateMenu(R.menu.toolbar_menu_about)

            // Change theme menu item icon based on current theme
            val themeDrawable = if (AppPref.isLightThemeEnabled) ContextCompat.getDrawable(
                requireContext(),
                R.drawable.ic_theme_light
            ) else ContextCompat.getDrawable(requireContext(), R.drawable.ic_theme_dark)
            menu.findItem(R.id.miThemeAbout).icon = themeDrawable

            // Store configuration
            menu.findItem(R.id.miStoreAbout).isVisible = resources.getBoolean(R.bool.enable_store)


            // MenuItem onclick
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.miStoreAbout -> {
                        context.openUrl(getString(R.string.store_url), R.color.defaultBgColor)
                    }
                    R.id.miCheckUpdate -> {
                        Tools.showUpdateDialog(context, false)
                    }
                    R.id.miThemeAbout -> {
                        showThemeChooserDialog()
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
                    pbAbout.makeVisible()
                }
                is ResultWrapper.Error -> {
                    pbAbout.makeGone()
                    showChannelInfoErrorState()
                }
                is ResultWrapper.Success<*> -> {
                    pbAbout.makeGone()
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
            ivBannerAbout.load(
                channelInfoItem.brandingSettings?.image?.bannerMobileHdImageUrl
                    ?: channelInfoItem.brandingSettings?.image?.bannerMobileMediumHdImageUrl
            )
        } else {
            ivBannerAbout.makeGone()
            val constraintSet = ConstraintSet().apply {
                clone(clAbout)
                connect(R.id.cvAbout, ConstraintSet.TOP, R.id.ablAbout, ConstraintSet.BOTTOM)
            }
            constraintSet.applyTo(clAbout)
        }

        // Profile Image
        ivProfileAbout.load(
            channelInfoItem.snippet.thumbnails.high?.url
                ?: channelInfoItem.snippet.thumbnails.medium.url
        )

        tvNameAbout.text = channelInfoItem.snippet.title
        tvJoinDateAbout.text = getString(
            R.string.text_channel_join_date,
            DateTimeUtils.getPublishedDate(channelInfoItem.snippet.publishedAt)
        )
        tvSubscribersValueAbout.text =
            channelInfoItem.statistics.subscriberCount.toLong().getFormattedNumberInString()
        tvVideosValueAbout.text =
            channelInfoItem.statistics.videoCount.toLong().getFormattedNumberInString()
        tvViewsValueAbout.text =
            channelInfoItem.statistics.viewCount.toLong().getFormattedNumberInString()
        tvDescAbout.text = channelInfoItem.snippet.description
    }

    private fun showChannelInfoErrorState() {
        groupResultAbout.makeGone()
        groupErrorAbout.makeVisible()
    }

    private fun hideChannelInfoErrorState() {
        groupErrorAbout.makeGone()
        groupResultAbout.makeVisible()
    }

    /**
     * Opens the YouTube app's Channel screen if YouTube app is installed otherwise opens the URL
     * in Chrome Custom Tab.
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
                ),
                R.color.defaultBgColor
            )
        }
    }

    @SuppressLint("CheckResult")
    private fun showThemeChooserDialog() {
        val langList = resources.getStringArray(R.array.locales)
        val langListNames = resources.getStringArray(R.array.settings_language_values).asList()

        val currentLangIndex = if (langList.contains(AppPref.localeOverride))
            langList.indexOf(AppPref.localeOverride) else 0


        MaterialDialog(requireContext()).show {
            title(R.string.dialog_settings_title)
            checkBoxPrompt(R.string.dialog_theme_text_dark, isCheckedDefault = !AppPref.isLightThemeEnabled) {
                if (it) {
                    setTheme(AppCompatDelegate.MODE_NIGHT_YES)
                } else {
                    setTheme(AppCompatDelegate.MODE_NIGHT_NO)
                }
            }
            listItemsSingleChoice(
                items = langListNames,
                initialSelection = currentLangIndex
            ) { dialog, index, text ->
                AppPref.localeOverride = langList[index]
                LocaleHelper.setLocale(context, AppPref.localeOverride)
                val parent = activity
                if (parent is MainActivity)
                    parent.checkLocaleChange()
            }

            negativeButton(R.string.dialog_negative_button) {
            }
            positiveButton(R.string.dialog_positive_button) {
            }

//            listItemsSingleChoice(
//                items = themeList,
//                initialSelection = currentThemeIndex
//            ) { dialog, index, text ->
//                when (text) {
//                    getString(R.string.dialog_theme_text_light) -> {
//                        setTheme(AppCompatDelegate.MODE_NIGHT_NO)
//                    }
//                    getString(R.string.dialog_theme_text_dark) -> {
//                        setTheme(AppCompatDelegate.MODE_NIGHT_YES)
//                    }
//                }
//            }
        }
    }

    private fun setTheme(themeMode: Int) {
        AppCompatDelegate.setDefaultNightMode(themeMode)
        AppPref.isLightThemeEnabled = themeMode == AppCompatDelegate.MODE_NIGHT_NO
    }
}

