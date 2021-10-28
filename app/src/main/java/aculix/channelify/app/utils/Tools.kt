package aculix.channelify.app.utils

import aculix.channelify.app.R
import aculix.channelify.app.sharedpref.AppPref
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.afollestad.materialdialogs.MaterialDialog
import com.github.javiersantos.appupdater.enums.AppUpdaterError

import com.github.javiersantos.appupdater.AppUpdaterUtils
import com.github.javiersantos.appupdater.AppUpdaterUtils.UpdateListener
import com.github.javiersantos.appupdater.enums.UpdateFrom
import com.github.javiersantos.appupdater.objects.Update


object Tools {
    fun showUpdateDialog(context: Context, autoCheck: Boolean) {

//        val updater = AppUpdater(context)
//            .setUpdateFrom(UpdateFrom.JSON)
//            .setUpdateJSON(context.getString(R.string.text_update_changelog_link))
//            .setDisplay(Display.DIALOG)
//            .setCancelable(false)
//            .showAppUpdated(true)
//            .setTitleOnUpdateAvailable(R.string.text_update_available)
//            .setTitleOnUpdateNotAvailable(R.string.text_update_unavailable)
//            .setButtonDismiss(R.string.text_update_dismiss)
//            .setButtonUpdate(R.string.text_update_ok)
//            .setContentOnUpdateAvailable(R.string.text_update_available_content)
//            .setContentOnUpdateNotAvailable(R.string.text_update_unavailable_content)
//
//        if (showHideButton)
//            updater.setButtonDoNotShowAgain(R.string.text_update_hide)
//        else
//            updater
//                .setButtonDoNotShowAgain("")
//                .setButtonDoNotShowAgainClickListener { _: DialogInterface, _: Int -> }
//        updater.start()
        val appUpdaterUtils = AppUpdaterUtils(context) //.setUpdateFrom(UpdateFrom.AMAZON)
            .setUpdateFrom(UpdateFrom.JSON)
            .setUpdateJSON(context.getString(R.string.text_update_changelog_link))
            .withListener(object : UpdateListener {
                override fun onSuccess(update: Update, isUpdateAvailable: Boolean?) {
                    if ((update.latestVersionCode > AppPref.lastDismissedUpdate && isUpdateAvailable!!)
                        || !autoCheck
                    ) {
                        MaterialDialog(context).show {
                            if (isUpdateAvailable!!) {
                                title(R.string.text_update_available)
                                message(R.string.text_update_available_content)
                                positiveButton(R.string.text_update_ok) {
                                    val intent = Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse(update.getUrlToDownload().toString())
                                    )
                                    context.startActivity(intent)
                                }
                                negativeButton(R.string.text_update_dismiss)
                                if (autoCheck)
                                    neutralButton(R.string.text_update_hide) {
                                        AppPref.lastDismissedUpdate = update.latestVersionCode
                                    }
                            } else {
                                title(R.string.text_update_unavailable)
                                message(R.string.text_update_unavailable_content)
                                positiveButton(R.string.text_update_dismiss)
                            }
                        }
                    }
//                    Log.d("Latest Version", update.getLatestVersion())
//                    Log.d("Latest Version Code", update.getLatestVersionCode().toString())
//                    Log.d("Release notes", update.getReleaseNotes())
//                    Log.d("URL", update.getUrlToDownload().toString())
//                    Log.d("Is update available?", java.lang.Boolean.toString(isUpdateAvailable!!))
                }

                override fun onFailed(error: AppUpdaterError) {
                    Log.d("AppUpdater Error", "Something went wrong")
                }
            })
        appUpdaterUtils.start()
    }
}