package com.google.android.settings.update

import android.content.Context
import com.android.settings.system.SystemUpdatePreferenceController

open class GoogleSystemUpdatePreferenceController(context: Context, key: String) :
    SystemUpdatePreferenceController(context, key) {

    override fun getAvailabilityStatus(): Int {
        if (isFeaturePropertyOn()) {
            return CONDITIONALLY_UNAVAILABLE
        }
        return super.getAvailabilityStatus()
    }

    protected open fun isFeaturePropertyOn(): Boolean =
        SoftwareUpdateUtils.canShowSoftwareUpdateUi()
}
