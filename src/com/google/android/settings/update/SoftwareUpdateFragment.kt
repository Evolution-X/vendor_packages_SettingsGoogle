package com.google.android.settings.update

import android.app.settings.SettingsEnums
import android.content.Context
import com.android.settings.dashboard.DashboardFragment
import com.android.settings.search.BaseSearchIndexProvider
import com.google.android.settings.R

class SoftwareUpdateFragment : DashboardFragment() {

    override fun getMetricsCategory(): Int = SettingsEnums.SETTINGS_SOFTWARE_UPDATES

    override fun getPreferenceScreenResId(): Int = R.xml.software_update

    override fun getLogTag(): String = TAG

    override fun getPreferenceScreenBindingKey(context: Context): String = SoftwareUpdateScreen.KEY

    companion object {
        private const val TAG = "SoftwareUpdate"

        @JvmField val SEARCH_INDEX_DATA_PROVIDER = BaseSearchIndexProvider(R.xml.software_update)
    }
}
