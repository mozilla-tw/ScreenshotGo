package org.mozilla.scryer.setting

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.preference.Preference
import android.support.v7.preference.PreferenceFragmentCompat
import android.support.v7.preference.SwitchPreferenceCompat
import org.mozilla.scryer.Observer
import org.mozilla.scryer.R
import org.mozilla.scryer.ScryerApplication
import org.mozilla.scryer.ScryerService

class SettingsFragment : PreferenceFragmentCompat(), Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {

    private val enableCaptureService: SwitchPreferenceCompat by lazy { findPreference(getString(R.string.pref_key_enable_capture_service)) as SwitchPreferenceCompat }
    private val enableFloatingScreenshotButton: SwitchPreferenceCompat by lazy { findPreference(getString(R.string.pref_key_enable_floating_screenshot_button)) as SwitchPreferenceCompat }
    private val shareWithFriendsPreference: Preference by lazy { findPreference(getString(R.string.pref_key_share_with_friends)) }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings, rootKey)

        enableCaptureService.onPreferenceChangeListener = this
        ScryerApplication.getSettingsRepository().serviceEnabledObserver.observe(this, Observer {
            enableCaptureService.isChecked = it
        })

        enableFloatingScreenshotButton.onPreferenceChangeListener = this
        ScryerApplication.getSettingsRepository().floatingEnableObservable.observe(this, Observer {
            enableFloatingScreenshotButton.isChecked = it
        })
        
        shareWithFriendsPreference.onPreferenceClickListener = this
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
        val repository = ScryerApplication.getSettingsRepository()

        if (preference == enableCaptureService) {
            val enable = newValue as Boolean
            val intent = Intent(activity, ScryerService::class.java)
            if (enable) {
                activity?.startService(intent)
            } else {
                activity?.stopService(intent)
            }

            repository.serviceEnabled = enable

            return true
        } else if (preference == enableFloatingScreenshotButton) {
            repository.floatingEnable = newValue as Boolean

            return true
        }

        return false
    }

    override fun onPreferenceClick(preference: Preference?): Boolean {
        when (preference) {
            shareWithFriendsPreference -> context?.let { showShareAppDialog(it); return true }
        }

        return false
    }

    private fun showShareAppDialog(context: Context) {
        val sendIntent = Intent(Intent.ACTION_SEND)
        sendIntent.type = "text/plain"
        sendIntent.putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.app_name))
        sendIntent.putExtra(Intent.EXTRA_TEXT, context.getString(R.string.share_app_promotion_text))
        context.startActivity(Intent.createChooser(sendIntent, null))
    }
}
