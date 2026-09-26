package com.google.android.settings.fuelgauge.batterysaver

import android.content.Context
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.UI_ONLY_PREFERENCE
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability
import com.android.settingslib.preference.PreferenceBinding
import com.android.settingslib.widget.TopIntroPreference
import com.google.android.settings.R

class AdaptiveBatteryTopIntroPreference :
    PreferenceMetadata, PreferenceBinding, PreferenceAvailabilityProvider {

    override val key: String
        get() = "adaptive_battery_top_intro"

    override val purpose: Int
        get() = R.string.adaptive_battery_top_intro_purpose

    override val title: Int
        get() = R.string.smart_battery_summary

    override val indexable: Boolean
        get() = false

    override val availabilityDescription: String
        get() = UI_ONLY_PREFERENCE

    override fun getAvailabilityStability(): PreconditionStability =
        PreconditionStability.STABLE_UNTIL_APK_UPDATE

    override fun tags(context: Context): Array<String> = arrayOf(UI_ONLY_PREFERENCE)

    override fun createWidget(context: Context): TopIntroPreference = TopIntroPreference(context)

    override fun isAvailable(context: Context): Boolean =
        AdaptiveBatteryScreen.isAdaptiveBatteryAvailable(context)
}
