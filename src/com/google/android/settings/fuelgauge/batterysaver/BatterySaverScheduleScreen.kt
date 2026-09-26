package com.google.android.settings.fuelgauge.batterysaver

import android.app.settings.SettingsEnums
import android.content.Context
import android.content.Intent
import androidx.fragment.app.Fragment
import com.android.settings.CatalystSettingsActivity
import com.android.settings.R as AR
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.fuelgauge.batterysaver.BatterySaverSchedulePreferenceController
import com.android.settings.utils.makeLaunchIntent
import com.android.settingslib.metadata.PreferenceCategory
import com.android.settingslib.metadata.PreferenceHierarchy
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.preferenceHierarchy
import com.google.android.settings.R
import kotlinx.coroutines.CoroutineScope

class BatterySaverScheduleScreen : PreferenceScreenMixin {

    override val key: String
        get() = BatterySaverSchedulePreferenceController.KEY_BATTERY_SAVER_SCHEDULE

    override val purpose: Int
        get() = R.string.battery_saver_schedule_purpose

    override val title: Int
        get() = R.string.battery_schedule_title

    override val summary: Int
        get() = R.string.battery_schedule_summary

    override val keywords: Int
        get() = AR.string.keywords_battery_saver_schedule

    override val highlightMenuKey: Int
        get() = AR.string.menu_key_battery

    override fun getMetricsCategory(): Int = SettingsEnums.FUELGAUGE_BATTERY_SAVER_SCHEDULE

    override fun hasCompleteHierarchy(): Boolean = false

    override fun fragmentClass(): Class<out Fragment>? =
        BatterySaverScheduleAndRemindersSettings::class.java

    override fun getPreferenceHierarchy(
        context: Context,
        coroutineScope: CoroutineScope,
    ): PreferenceHierarchy =
        preferenceHierarchy(context) {
            +PreferenceCategory(
                key = "battery_saver_reminder_entry",
                purpose = R.string.battery_saver_reminder_entry_purpose,
                title = R.string.battery_saver_reminder_category,
            ) order 90 +=
                {
                    +BatterySaverRemindersPreference()
                }
        }

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?): Intent =
        makeLaunchIntent(context, BatterySaverScheduleTrampoline::class.java, metadata?.key)
}

class BatterySaverScheduleTrampoline :
    CatalystSettingsActivity(
        BatterySaverSchedulePreferenceController.KEY_BATTERY_SAVER_SCHEDULE,
        BatterySaverScheduleAndRemindersSettings::class.java,
    )
