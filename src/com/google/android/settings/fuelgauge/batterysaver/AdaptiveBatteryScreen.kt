package com.google.android.settings.fuelgauge.batterysaver

import android.app.settings.SettingsEnums
import android.content.Context
import android.content.Intent
import androidx.fragment.app.Fragment
import com.android.settings.CatalystFragment
import com.android.settings.CatalystSettingsActivity
import com.android.settings.R as AR
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.utils.makeLaunchIntent
import com.android.settingslib.datastore.HandlerExecutor
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.KeyedObserver
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceHierarchy
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.preferenceHierarchy
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability
import com.google.android.settings.R
import kotlinx.coroutines.CoroutineScope

class AdaptiveBatteryScreen(context: Context) :
    PreferenceScreenMixin,
    PreferenceAvailabilityProvider,
    PreferenceSummaryProvider,
    PreferenceLifecycleProvider {

    private val adaptiveBatteryStore: KeyValueStore =
        AdaptiveBatteryPreference.getAdaptiveBatteryDataStore(context)
    private var keyedObserver: KeyedObserver<String>? = null

    override val key: String
        get() = KEY

    override val title: Int
        get() = AR.string.smart_battery_title

    override val purpose: Int
        get() = R.string.adaptive_battery_entry_purpose

    override val keywords: Int
        get() = AR.string.keywords_battery_adaptive_preferences

    override val highlightMenuKey: Int
        get() = AR.string.menu_key_battery

    override val indexable: Boolean
        get() = true

    override val availabilityDescription: String
        get() = "The device must support adaptive battery."

    override fun getMetricsCategory(): Int = SettingsEnums.FUELGAUGE_ADAPTIVE_BATTERY

    override fun isFlagEnabled(context: Context): Boolean = true

    override fun hasCompleteHierarchy(): Boolean = true

    override fun fragmentClass(): Class<out Fragment>? = CatalystFragment::class.java

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?): Intent =
        makeLaunchIntent(context, AdaptiveBatteryTrampoline::class.java, metadata?.key)

    override fun isAvailable(context: Context): Boolean = isAdaptiveBatteryAvailable(context)

    override fun getAvailabilityStability(): PreconditionStability =
        PreconditionStability.STABLE_UNTIL_APK_UPDATE

    override fun getSummary(context: Context): CharSequence =
        context.getString(
            if (adaptiveBatteryStore.getBoolean(AdaptiveBatteryPreference.KEY) == false)
                R.string.smart_battery_summary_off
            else R.string.smart_battery_summary_on
        )

    override fun getPreferenceHierarchy(
        context: Context,
        coroutineScope: CoroutineScope,
    ): PreferenceHierarchy =
        preferenceHierarchy(context) {
            +AdaptiveBatteryTopIntroPreference()
            +AdaptiveBatteryIllustrationPreference()
            +AdaptiveBatteryPreference(adaptiveBatteryStore)
        }

    override fun onCreate(context: PreferenceLifecycleContext) {
        if (isEntryPoint(context)) {
            val observer = KeyedObserver<String> { _, _ -> context.notifyPreferenceChange(KEY) }
            keyedObserver = observer
            adaptiveBatteryStore.addObserver(
                AdaptiveBatteryPreference.KEY,
                observer,
                HandlerExecutor.main,
            )
        }
    }

    override fun onDestroy(context: PreferenceLifecycleContext) {
        if (isEntryPoint(context)) {
            keyedObserver?.let {
                adaptiveBatteryStore.removeObserver(AdaptiveBatteryPreference.KEY, it)
            }
        }
    }

    companion object {
        const val KEY = "adaptive_battery_entry"

        fun isAdaptiveBatteryAvailable(context: Context): Boolean =
            context.resources.getBoolean(com.android.internal.R.bool.config_smart_battery_available)
    }
}

class AdaptiveBatteryTrampoline : CatalystSettingsActivity(AdaptiveBatteryScreen.KEY)
