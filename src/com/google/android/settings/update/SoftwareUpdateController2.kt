package com.google.android.settings.update

import android.content.Context
import com.android.settings.core.BasePreferenceController
import com.android.settingslib.widget.SettingsThemeHelper
import com.google.android.settings.R

class SoftwareUpdateController2(context: Context, preferenceKey: String) :
    BasePreferenceController(context, preferenceKey) {

    override fun getAvailabilityStatus(): Int =
        if (SettingsThemeHelper.isExpressiveTheme(mContext)) {
            AVAILABLE
        } else {
            CONDITIONALLY_UNAVAILABLE
        }

    override fun getSummary(): CharSequence =
        if (SystemUpdatePreferenceController.isSystemUpdatable(mContext)) {
            mContext.getString(R.string.software_update_can_be_updated_header)
        } else {
            mContext.getString(R.string.software_update_up_to_date_header)
        }
}
