package tsfat.yeshivathahesder.channel.fragment

import tsfat.yeshivathahesder.channel.BuildConfig
import tsfat.yeshivathahesder.channel.R
import tsfat.yeshivathahesder.core.extensions.openAppInGooglePlay
import tsfat.yeshivathahesder.core.extensions.openUrl
import tsfat.yeshivathahesder.core.extensions.startEmailIntent
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import coil.api.load
import tsfat.yeshivathahesder.channel.databinding.FragmentAppInfoBinding

class AppInfoFragment : Fragment() {

    private lateinit var binding: FragmentAppInfoBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAppInfoBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        binding.ivLogoAppInfo.load(R.drawable.yhtsfat_logo)

        onWebsiteClick()
        onGooglePlayClick()
//        onInstagramClick()
        onPhoneClick()
        onFacebookClick()
        onEmailClick()
        onNavigationViewMenuItemClick()
    }

    private fun setupToolbar() {
        val navController = findNavController()
        val appBarConfiguration = AppBarConfiguration(navController.graph)
        binding.toolbarAppInfo.setupWithNavController(navController, appBarConfiguration)
    }

    private fun onWebsiteClick() {
        binding.ivWebsiteAppInfo.setOnClickListener {
            context?.openUrl(getString(R.string.text_website_url), R.color.defaultBgColor)
        }
    }

    private fun onGooglePlayClick() {
        binding.ivGooglePlayAppInfo.setOnClickListener {
            try {
                // Try to open in the Google Play app
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("market://search?q=pub:${getString(R.string.text_google_play_developer_name)}")
                    )
                )
            } catch (exception: Throwable) {
                // Google Play app is not installed. Open URL in the browser.
                context?.openUrl(
                    "https://play.google.com/store/apps/dev?id=${getString(R.string.text_google_play_developer_id)}",
                    R.color.defaultBgColor
                )
            }
        }
    }

    private fun onPhoneClick() {
        binding.ivPhoneAppInfo.setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL)
            intent.data = Uri.parse(
                "tel:${
                    getString(R.string.text_phone_number)
                }"
            )
            startActivity(intent)
        }
    }

    private fun onFacebookClick() {
        binding.ivFacebookAppInfo.setOnClickListener {
            try {
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("fb://profile/${getString(R.string.text_facebook_user_id)}")
                )
                startActivity(intent)
            } catch (_: android.content.ActivityNotFoundException) {
                context?.openUrl(
                    "https://www.facebook.com/${getString(R.string.text_facebook_username)}",
                    R.color.defaultBgColor
                )
            }
        }
    }

//    private fun onInstagramClick() {
//        ivInstagramAppInfo.setOnClickListener {
//            try {
//                // Try to open in the Instagram app
//                startActivity(
//                    Intent(
//                        Intent.ACTION_VIEW,
//                        Uri.parse("https://instagram.com/_u/${getString(R.string.text_instagram_user_name)}")
//                    )
//                )
//            } catch (exception: android.content.ActivityNotFoundException) {
//                // Instagram app is not installed. Open URL in the browser.
//                context?.openUrl(
//                    "https://instagram.com/${getString(R.string.text_instagram_user_name)}",
//                    R.color.defaultBgColor
//                )
//            }
//        }
//    }

    private fun onEmailClick() {
        binding.ivEmailAppInfo.setOnClickListener {
            context?.startEmailIntent(getString(R.string.text_contact_email), null)
        }
    }

    private fun onNavigationViewMenuItemClick() {
        binding.nvAppInfo.setNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.miRateAppInfo -> {
                    context?.openAppInGooglePlay(BuildConfig.APPLICATION_ID)
                }
                R.id.miFeedbackAppInfo -> {
                    context?.startEmailIntent(
                        getString(R.string.text_contact_email),
                        getString(R.string.text_app_feedback_email_subject)
                    )
                }
                R.id.miTosAppInfo -> {
                    context?.openUrl(getString(R.string.text_tos_url), R.color.defaultBgColor)
                }
                R.id.miPrivacyPolicyAppInfo -> {
                    context?.openUrl(
                        getString(R.string.text_privacy_policy_url),
                        R.color.defaultBgColor
                    )
                }
                R.id.miSourceCodeAppInfo -> {
                    context?.openUrl(getString(R.string.text_source_code_url),
                    R.color.defaultBgColor)
                }
            }

            true
        }
    }
}
