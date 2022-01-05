package tsfat.yeshivathahesder.channel.fragment

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import tsfat.yeshivathahesder.channel.R

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.sharedPreferencesName = getString(R.string.preference_file_name)
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
    }
}