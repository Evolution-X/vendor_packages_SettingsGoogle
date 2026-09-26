package com.google.android.settings.update

import android.content.Context
import android.os.SystemUpdateManager
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import com.android.settings.core.BasePreferenceController
import com.android.settingslib.DeviceInfoUtils
import com.android.settingslib.widget.SettingsThemeHelper
import com.android.settingslib.widget.StatusBannerPreference
import com.android.settingslib.widget.theme.R as SLR
import com.google.android.settings.R

class SystemUpdatePreferenceController(context: Context, preferenceKey: String) :
    BasePreferenceController(context, preferenceKey), DefaultLifecycleObserver {

    private var mPreferenceScreen: PreferenceScreen? = null

    override fun getAvailabilityStatus(): Int =
        if (SettingsThemeHelper.isExpressiveTheme(mContext)) {
            AVAILABLE
        } else {
            CONDITIONALLY_UNAVAILABLE
        }

    override fun displayPreference(screen: PreferenceScreen) {
        super.displayPreference(screen)
        updateUi(screen)
    }

    override fun onResume(owner: LifecycleOwner) {
        updateUi(mPreferenceScreen)
    }

    private fun updateUi(preferenceScreen: PreferenceScreen?) {
        if (preferenceScreen == null) {
            Log.d(TAG, "No PreferenceScreen.")
            return
        }
        mPreferenceScreen = preferenceScreen
        updateSystemPreference(preferenceScreen.findPreference(KEY_SYSTEM_UPDATE_PREFERENCE))
        updateStatusBanner(preferenceScreen.findPreference(KEY_STATUS_BANNER))
    }

    private fun updateStatusBanner(preference: Preference?) {
        if (preference == null) {
            Log.d(TAG, "Can not update banner due to no preference.")
            return
        }
        var title = R.string.software_update_banner_up_to_date
        var icon = R.drawable.software_update_banner_up_to_date
        var bannerStatus = StatusBannerPreference.BannerStatus.LOW
        if (isSystemUpdatable(preference.context)) {
            title = R.string.software_update_banner_update_available
            icon = R.drawable.software_update_banner_pending_update
            bannerStatus = StatusBannerPreference.BannerStatus.MEDIUM
        }
        preference.setTitle(title)
        preference.setIcon(icon)
        (preference as? StatusBannerPreference)?.iconLevel = bannerStatus
    }

    private fun updateSystemPreference(preference: Preference?) {
        if (preference == null) {
            Log.d(TAG, "Can not update system preference due to no preference.")
            return
        }
        var summary: String
        var icon = SLR.drawable.settingslib_expressive_icon_level_medium
        if (!isSystemUpdatable(preference.context)) {
            val securityPatch = DeviceInfoUtils.getSecurityPatch()
            summary =
                if (securityPatch != null) {
                    preference.context.getString(
                        R.string.software_update_up_to_specific_date_summary,
                        securityPatch,
                    )
                } else {
                    ""
                }
            icon = SLR.drawable.settingslib_expressive_icon_level_low
        } else {
            summary = preference.context.getString(R.string.software_update_pending_update_summary)
        }
        preference.setIcon(icon)
        preference.summary = summary
    }

    companion object {
        const val KEY_STATUS_BANNER = "key_status_banner"
        const val KEY_SYSTEM_UPDATE_PREFERENCE = "key_system_update_preference"
        private const val TAG = "SoftwareUpdatePreferencesController"

        fun isSystemUpdatable(context: Context): Boolean {
            val systemUpdateManager = context.getSystemService(SystemUpdateManager::class.java)
            if (systemUpdateManager == null) {
                Log.d(TAG, "Can not get SystemUpdateManager.")
                return false
            }
            val status =
                try {
                    systemUpdateManager
                        .retrieveSystemUpdateInfo()
                        .getInt(SystemUpdateManager.KEY_STATUS)
                } catch (e: Exception) {
                    Log.w(TAG, "Error getting system update info. $e")
                    SystemUpdateManager.STATUS_UNKNOWN
                }
            return status in
                SystemUpdateManager.STATUS_WAITING_DOWNLOAD..SystemUpdateManager
                        .STATUS_WAITING_REBOOT
        }
    }
}
