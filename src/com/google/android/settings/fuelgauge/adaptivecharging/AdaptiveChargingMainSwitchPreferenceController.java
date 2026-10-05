package com.google.android.settings.fuelgauge.adaptivecharging;

import android.content.Context;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;

import com.google.android.systemui.googlebattery.AdaptiveChargingManager;

public class AdaptiveChargingMainSwitchPreferenceController extends TogglePreferenceController {
    AdaptiveChargingManager mAdaptiveChargingManager;
    private boolean mChecked;

    public AdaptiveChargingMainSwitchPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mAdaptiveChargingManager = new AdaptiveChargingManager(context.getApplicationContext());
    }

    @Override
    public int getAvailabilityStatus() {
        if (AdaptiveChargingUtils.isSystemUser()) {
            return AdaptiveChargingUtils.isAvailable(mAdaptiveChargingManager)
                    ? AVAILABLE
                    : CONDITIONALLY_UNAVAILABLE;
        }
        return DISABLED_FOR_USER;
    }

    @Override
    public boolean isChecked() {
        return AdaptiveChargingUtils.isChecked(mAdaptiveChargingManager);
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        AdaptiveChargingUtils.setChecked(mContext, mAdaptiveChargingManager, mChecked, isChecked);
        mChecked = isChecked;
        return true;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_battery;
    }
}
