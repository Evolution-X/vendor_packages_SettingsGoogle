package com.google.android.settings.fuelgauge;

import android.content.Context;

import com.android.settings.fuelgauge.PowerUsageFeatureProviderImpl;

import com.google.android.settings.fuelgauge.adaptivecharging.AdaptiveChargingUtils;
import com.google.android.systemui.googlebattery.AdaptiveChargingManager;

public class PowerUsageFeatureProviderGoogleImpl extends PowerUsageFeatureProviderImpl {
    private AdaptiveChargingManager mAdaptiveChargingManager;

    public PowerUsageFeatureProviderGoogleImpl(Context context) {
        super(context);
    }

    private AdaptiveChargingManager getAdaptiveChargingManager() {
        if (mAdaptiveChargingManager == null) {
            mAdaptiveChargingManager = new AdaptiveChargingManager(mContext);
        }
        return mAdaptiveChargingManager;
    }

    @Override
    public boolean isSmartBatterySupported() {
        return AdaptiveChargingUtils.isAvailable(getAdaptiveChargingManager())
                && AdaptiveChargingUtils.isAdaptiveChargingVisible(mContext);
    }
}
