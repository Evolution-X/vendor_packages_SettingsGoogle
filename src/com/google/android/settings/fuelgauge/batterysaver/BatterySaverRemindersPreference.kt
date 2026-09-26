package com.google.android.settings.fuelgauge.batterysaver

import android.content.Context
import android.provider.Settings
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.Permissions
import com.android.settingslib.datastore.SettingsGlobalStore
import com.android.settingslib.datastore.SettingsSystemStore
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.metadata.SwitchPreference
import com.google.android.settings.R

class BatterySaverRemindersPreference :
    SwitchPreference(
        key = KEY,
        purpose = R.string.low_power_mode_reminder_enabled_purpose,
        title = R.string.battery_saver_reminder_switch_title,
        summary = R.string.battery_saver_reminder_switch_summary,
    ) {

    override val sensitivityLevel: Int
        get() = SensitivityLevel.NO_SENSITIVITY

    override fun storage(context: Context): KeyValueStore =
        getBatterySaverRemindersDataStorage(context)

    override fun tags(context: Context): Array<String> = arrayOf("battery_saver_reminders")

    override fun getReadPermissions(context: Context): Permissions =
        SettingsSystemStore.getReadPermissions()

    override fun getWritePermissions(context: Context): Permissions =
        SettingsSystemStore.getWritePermissions()

    override fun getReadPermit(context: Context, callingPid: Int, callingUid: Int): Int =
        ReadWritePermit.ALLOW

    override fun getWritePermit(
        context: Context,
        value: Boolean?,
        callingPid: Int,
        callingUid: Int,
    ): Int = ReadWritePermit.ALLOW

    companion object {
        const val KEY = Settings.Global.LOW_POWER_MODE_REMINDER_ENABLED

        fun getBatterySaverRemindersDataStorage(context: Context): KeyValueStore {
            val settingsGlobalStore = SettingsGlobalStore.get(context)
            settingsGlobalStore.setDefaultValue(KEY, true)
            return settingsGlobalStore
        }
    }
}
