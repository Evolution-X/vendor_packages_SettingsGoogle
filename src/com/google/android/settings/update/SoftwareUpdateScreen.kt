package com.google.android.settings.update

import android.app.settings.SettingsEnums
import android.content.Context
import android.content.Intent
import androidx.fragment.app.Fragment
import com.android.settings.R as AR
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.utils.makeLaunchIntent
import com.android.settingslib.R as SLR
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.metadata.METADATA_IN_UI
import com.android.settingslib.metadata.PersistentPreference
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceHierarchy
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.preferenceHierarchy
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability
import com.android.settingslib.widget.SettingsThemeHelper
import com.google.android.settings.R
import kotlinx.coroutines.CoroutineScope

class SoftwareUpdateScreen :
    PreferenceScreenMixin, PreferenceAvailabilityProvider, PreferenceSummaryProvider {

    override val key: String
        get() = KEY

    override val purpose: Int
        get() = R.string.software_update_settings_v2_purpose

    override val title: Int
        get() = R.string.software_update_entry_title

    override val icon: Int
        get() = SLR.drawable.ic_system_update

    override val keywords: Int
        get() = AR.string.keywords_system_update_settings

    override val highlightMenuKey: Int
        get() = AR.string.menu_key_system

    override val availabilityDescription: String =
        "Requires Android 16 (Baklava) or higher with the 'Material Expressive Design' feature enabled."

    override fun getAvailabilityStability(): PreconditionStability = PreconditionStability.UNSTABLE

    override fun getMetricsCategory(): Int = SettingsEnums.SETTINGS_SOFTWARE_UPDATES

    override fun hasCompleteHierarchy(): Boolean = false

    override fun fragmentClass(): Class<out Fragment> = SoftwareUpdateFragment::class.java

    override fun isAvailable(context: Context): Boolean =
        SettingsThemeHelper.isExpressiveTheme(context)

    override fun getSummary(context: Context): CharSequence =
        if (SystemUpdatePreferenceController.isSystemUpdatable(context)) {
            context.getString(R.string.software_update_can_be_updated_header)
        } else {
            context.getString(R.string.software_update_up_to_date_header)
        }

    override fun getPreferenceHierarchy(
        context: Context,
        coroutineScope: CoroutineScope,
    ): PreferenceHierarchy =
        preferenceHierarchy(context) { +SoftwareUpdateScreenPreference(this@SoftwareUpdateScreen) }

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?): Intent =
        makeLaunchIntent(context, SoftwareUpdateActivity::class.java, metadata?.key)

    inner class SoftwareUpdateScreenPreference(private val screenMetadata: SoftwareUpdateScreen) :
        PreferenceMetadata,
        PreferenceSummaryProvider,
        PreferenceAvailabilityProvider,
        PersistentPreference<String> {

        override val key: String
            get() = KEY_PREFERENCE

        override val purpose: Int
            get() = screenMetadata.purpose

        override val availabilityDescription: String
            get() = screenMetadata.availabilityDescription

        override val indexable: Boolean
            get() = false

        override val supportsWrite: Boolean
            get() = false

        override val valueType: Class<String>
            get() = String::class.java

        override fun tags(context: Context): Array<String> = arrayOf(METADATA_IN_UI)

        override fun isEnabled(context: Context): Boolean = screenMetadata.isEnabled(context)

        override fun isAvailable(context: Context): Boolean = screenMetadata.isAvailable(context)

        override fun getAvailabilityStability(): PreconditionStability =
            screenMetadata.getAvailabilityStability()

        override fun getSummary(context: Context): CharSequence? =
            screenMetadata.getSummary(context)

        override fun storage(context: Context): KeyValueStore = createSummaryStorage(context, key)
    }

    companion object {
        const val KEY = "software_update_settings_v2"
        const val KEY_PREFERENCE = "software_update_settings_v2_preference"
    }
}
