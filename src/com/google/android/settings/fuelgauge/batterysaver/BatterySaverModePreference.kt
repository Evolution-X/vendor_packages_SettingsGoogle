package com.google.android.settings.fuelgauge.batterysaver

import android.app.settings.SettingsEnums
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.View
import androidx.preference.Preference
import com.android.settings.metrics.PreferenceActionMetricsProvider
import com.android.settingslib.datastore.Permissions
import com.android.settingslib.metadata.BooleanValuePreference
import com.android.settingslib.metadata.MUSTPASS_SET
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.preference.PreferenceBinding
import com.android.settingslib.preference.forEachRecursively
import com.android.settingslib.widget.SelectorWithWidgetPreference
import com.google.android.settings.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

abstract class BatterySaverModePreference(protected val dataStore: BatterySaverModeDataStore) :
    BooleanValuePreference,
    PreferenceBinding,
    Preference.OnPreferenceClickListener,
    SelectorWithWidgetPreference.OnClickListener,
    PreferenceLifecycleProvider {

    override val supportsWrite: Boolean
        get() = true

    override fun storage(context: Context) = dataStore

    override fun getReadPermissions(context: Context) = Permissions.EMPTY

    override fun getWritePermissions(context: Context) = Permissions.EMPTY

    override fun getReadPermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override fun getWritePermit(
        context: Context,
        value: Boolean?,
        callingPid: Int,
        callingUid: Int,
    ) = ReadWritePermit.ALLOW

    override fun createWidget(context: Context) = SelectorWithWidgetPreference(context)

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        preference.isPersistent = false
        val selector = preference as SelectorWithWidgetPreference
        selector.isChecked = dataStore.getBoolean(key) == true
        selector.onPreferenceClickListener = this
        selector.setOnClickListener(this)
    }

    override fun onPreferenceClick(preference: Preference): Boolean = true

    override fun onPause(context: PreferenceLifecycleContext) {
        context.findPreference<Preference>(key)?.isPersistent = true
    }

    override fun onRadioButtonClicked(preference: SelectorWithWidgetPreference) {
        preference.parent?.forEachRecursively { child ->
            if (child is SelectorWithWidgetPreference) {
                child.isChecked = child == preference
            }
        }
    }
}

class BasicBatterySaverPreference(dataStore: BatterySaverModeDataStore) :
    BatterySaverModePreference(dataStore) {
    override val key: String
        get() = KEY

    override val purpose: Int
        get() = R.string.basic_battery_saver_purpose

    override val title: Int
        get() = R.string.basic_battery_saver_title

    override val summary: Int
        get() = R.string.basic_battery_saver_summary

    override val sensitivityLevel: Int
        get() = SensitivityLevel.NO_SENSITIVITY

    companion object {
        const val KEY = "basic_battery_saver"
    }
}

class ExtremeBatterySaverPreference(dataStore: BatterySaverModeDataStore) :
    BatterySaverModePreference(dataStore), PreferenceActionMetricsProvider, View.OnClickListener {

    override val key: String
        get() = KEY

    override val purpose: Int
        get() = R.string.extreme_battery_saver_purpose

    override val title: Int
        get() = R.string.extreme_battery_saver_title

    override val summary: Int
        get() = R.string.extreme_battery_saver_summary

    override val sensitivityLevel: Int
        get() = SensitivityLevel.NO_SENSITIVITY

    override val preferenceActionMetrics: Int
        get() = SettingsEnums.ACTION_EXTREME_BATTERY_SAVER

    override fun tags(context: Context) = arrayOf(KEY, MUSTPASS_SET)

    override fun intent(context: Context): Intent =
        Intent("android.settings.batterysaver.flipendo").setPackage("com.google.android.flipendo")

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        (preference as SelectorWithWidgetPreference).setExtraWidgetOnClickListener(this)
    }

    override fun onClick(view: View) {
        try {
            val context = view.context
            context.startActivity(intent(context))
        } catch (e: Exception) {
            Log.e(TAG, "launch Flipendo failed", e)
        }
    }

    override fun onStart(context: PreferenceLifecycleContext) {
        dataStore.refreshFlipendoStates(true)
    }

    override fun onPause(context: PreferenceLifecycleContext) {
        super.onPause(context)
        persistBatterySaverMode(context)
    }

    private fun persistBatterySaverMode(context: PreferenceLifecycleContext) {
        val preference = context.findPreference<SelectorWithWidgetPreference>(KEY) ?: return
        val isChecked = preference.isChecked
        if (isChecked == dataStore.getBoolean(KEY)) {
            return
        }
        CoroutineScope(Dispatchers.IO).launch {
            Log.i(TAG, "Update $KEY to $isChecked")
            dataStore.setBoolean(KEY, isChecked)
        }
    }

    companion object {
        const val KEY = "extreme_battery_saver"
        private const val TAG = "BatterySaverMode"
    }
}
