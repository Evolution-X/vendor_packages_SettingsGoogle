package com.google.android.settings.fuelgauge.adaptivecharging;

import android.content.Context;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

import com.google.android.systemui.googlebattery.AdaptiveChargingManager;

public class AdaptiveChargingPreferenceController extends BasePreferenceController {
    AdaptiveChargingManager mAdaptiveChargingManager;

    public AdaptiveChargingPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mAdaptiveChargingManager = new AdaptiveChargingManager(context.getApplicationContext());
    }

    @Override
    public int getAvailabilityStatus() {
        return (mAdaptiveChargingManager.isAvailable()
                        && AdaptiveChargingUtils.isAdaptiveChargingVisible(mContext))
                ? AVAILABLE_UNSEARCHABLE
                : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        preference.setSummary(
                mAdaptiveChargingManager.isEnabled()
                        ? R.string.battery_saver_on_summary
                        : R.string.battery_saver_off_summary);
    }
}
