package com.google.android.settings.fuelgauge.batterysaver

import android.os.Bundle
import android.util.Log
import com.android.settings.R as AR
import com.android.settings.fuelgauge.batterysaver.BatterySaverSettings
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen
import com.android.settingslib.metadata.preferencesapi.category.Category
import com.android.settingslib.metadata.preferencesapi.preconditions.Allowed
import com.android.settingslib.metadata.preferencesapi.preconditions.HardwareUnsupported
import com.android.settingslib.metadata.preferencesapi.types.CustomEnum
import com.android.settingslib.metadata.preferencesapi.types.EnumApiWithRes
import com.google.android.settings.R

enum class BatterySaverType(override val asApiValue: String, override val purpose: Int) :
    EnumApiWithRes<String> {
    STANDARD(BasicBatterySaverPreference.KEY, R.string.basic_battery_saver_purpose),
    EXTREME(ExtremeBatterySaverPreference.KEY, R.string.extreme_battery_saver_purpose),
}

class BatterySaverGoogleApiScreen :
    PreferencesApiScreen(
        key = KEY,
        topLevelSettingsCategory = Category.BATTERY,
        fragment = BatterySaverSettings::class,
        purpose = AR.string.battery_saver_screen_purpose,
    ) {

    init {
        flag { true }
        tags(APP_FUNCTION_BATTERY)
        preference<BatterySaverType, String>(
            key = "battery_saver_type",
            purpose = R.string.battery_saver_type_purpose,
            type = CustomEnum(BatterySaverType::class, R.string.battery_saver_type_options_purpose),
        ) {
            sensitivityLevel(SensitivityLevel.NO_SENSITIVITY)
            get {
                execute {
                    if (!FlipendoUtils.isFlipendoInstalled(context)) {
                        BatterySaverType.STANDARD
                    } else {
                        val flipendoState = FlipendoUtils.getFlipendoState(context)
                        if (!flipendoState.first && !flipendoState.second) {
                            BatterySaverType.STANDARD
                        } else {
                            BatterySaverType.EXTREME
                        }
                    }
                }
            }
            set {
                preconditions(R.string.battery_saver_type_preconditions) {
                    if (!FlipendoUtils.isFlipendoInstalled(context)) {
                        HardwareUnsupported(R.string.battery_saver_type_feature_disabled)
                    } else {
                        Allowed
                    }
                }
                execute { type: BatterySaverType ->
                    val mode =
                        when (type) {
                            BatterySaverType.STANDARD -> 0
                            BatterySaverType.EXTREME -> 1
                        }
                    val bundle = Bundle().apply { putInt("update_flipendo_mode", mode) }
                    try {
                        context.contentResolver.call(
                            FlipendoUtils.FLIPENDO_STATE_AUTHORITY,
                            "update_flipendo_mode_method",
                            null,
                            bundle,
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "setValue failed", e)
                    }
                }
            }
        }
    }

    companion object {
        const val KEY = "api_battery_saver_screen"
        private const val TAG = "BatterySaverGoogleApiScreen"
    }
}
