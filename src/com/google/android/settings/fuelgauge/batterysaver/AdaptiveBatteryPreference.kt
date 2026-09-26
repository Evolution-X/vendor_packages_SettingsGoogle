package com.google.android.settings.fuelgauge.batterysaver

import android.app.settings.SettingsEnums
import android.content.Context
import android.provider.Settings
import com.android.settings.metrics.PreferenceActionMetricsProvider
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.Permissions
import com.android.settingslib.datastore.SettingsGlobalStore
import com.android.settingslib.metadata.BooleanValuePreference
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability
import com.android.settingslib.widget.MainSwitchPreferenceBinding
import com.google.android.settings.R

class AdaptiveBatteryPreference(private val dataStore: KeyValueStore) :
    BooleanValuePreference,
    MainSwitchPreferenceBinding,
    PreferenceActionMetricsProvider,
    PreferenceAvailabilityProvider {

    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.adaptive_battery_switch_title

    override val purpose: Int
        get() = R.string.adaptive_battery_management_enabled_purpose

    override val preferenceActionMetrics: Int
        get() = SettingsEnums.ACTION_ADAPTIVE_BATTERY

    override val sensitivityLevel: Int
        get() = SensitivityLevel.NO_SENSITIVITY

    override val availabilityDescription: String
        get() = "The device must support adaptive battery."

    override val supportsWrite: Boolean
        get() = true

    override fun tags(context: Context): Array<String> = arrayOf("adaptive_battery")

    override fun isAvailable(context: Context): Boolean =
        AdaptiveBatteryScreen.isAdaptiveBatteryAvailable(context)

    override fun getAvailabilityStability(): PreconditionStability =
        PreconditionStability.STABLE_UNTIL_APK_UPDATE

    override fun storage(context: Context): KeyValueStore = dataStore

    override fun getReadPermit(context: Context, callingPid: Int, callingUid: Int): Int =
        ReadWritePermit.ALLOW

    override fun getWritePermit(
        context: Context,
        value: Boolean?,
        callingPid: Int,
        callingUid: Int,
    ): Int = ReadWritePermit.ALLOW

    override fun getReadPermissions(context: Context): Permissions =
        SettingsGlobalStore.getReadPermissions()

    override fun getWritePermissions(context: Context): Permissions =
        SettingsGlobalStore.getWritePermissions()

    companion object {
        const val KEY = Settings.Global.ADAPTIVE_BATTERY_MANAGEMENT_ENABLED

        fun getAdaptiveBatteryDataStore(context: Context): KeyValueStore =
            SettingsGlobalStore.get(context).apply { setDefaultValue(KEY, true) }
    }
}
