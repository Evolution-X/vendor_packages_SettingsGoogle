package com.google.android.settings.fuelgauge.adaptivecharging;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;

import com.android.settings.overlay.FeatureFactory;

import com.google.android.systemui.googlebattery.AdaptiveChargingManager;

public abstract class AdaptiveChargingUtils {
    private static final String TAG = "AdaptiveChargingUtils";
    private static final String EXTRA_IS_AVAILABLE = "extra_is_available";
    private static final String EXTRA_IS_CHECKED = "extra_is_checked";
    private static final String EXTRA_PREVIOUS_IS_CHECKED = "extra_previous_is_checked";

    public static boolean isAvailable(AdaptiveChargingManager adaptiveChargingManager) {
        return adaptiveChargingManager != null
                && isSystemUser()
                && adaptiveChargingManager.isAvailable();
    }

    public static Bundle getIsAvailableBundle(AdaptiveChargingManager adaptiveChargingManager) {
        Bundle bundle = new Bundle(1);
        bundle.putBoolean(EXTRA_IS_AVAILABLE, isAvailable(adaptiveChargingManager));
        return bundle;
    }

    public static boolean isSystemUser() {
        return UserHandle.myUserId() == UserHandle.USER_SYSTEM;
    }

    public static boolean isChecked(AdaptiveChargingManager adaptiveChargingManager) {
        return adaptiveChargingManager != null && adaptiveChargingManager.isEnabled();
    }

    public static Bundle getIsCheckedBundle(AdaptiveChargingManager adaptiveChargingManager) {
        Bundle bundle = new Bundle(1);
        bundle.putBoolean(EXTRA_IS_CHECKED, isChecked(adaptiveChargingManager));
        return bundle;
    }

    public static void setChecked(
            Context context, AdaptiveChargingManager adaptiveChargingManager, Bundle bundle) {
        if (bundle == null) {
            Log.w(TAG, "Bundle is null!");
        } else {
            setChecked(
                    context,
                    adaptiveChargingManager,
                    bundle.getBoolean(EXTRA_PREVIOUS_IS_CHECKED),
                    bundle.getBoolean(EXTRA_IS_CHECKED));
        }
    }

    public static void setChecked(
            Context context,
            AdaptiveChargingManager adaptiveChargingManager,
            boolean previousChecked,
            boolean isChecked) {
        if (adaptiveChargingManager == null) {
            Log.w(TAG, "AdaptiveChargingManager is null!");
            return;
        }
        adaptiveChargingManager.setEnabled(isChecked);
        if (isChecked) {
            adaptiveChargingManager.setDefaultChargingPolicy();
            setChargingOptimizationMode(context, 0);
        } else {
            adaptiveChargingManager.setAdaptiveChargingDeadline(-1);
        }
        if (previousChecked != isChecked) {
            FeatureFactory.getFeatureFactory()
                    .getMetricsFeatureProvider()
                    .action(context, SettingsEnums.ACTION_ADAPTIVE_CHARGING_TOGGLE, isChecked);
        }
    }

    public static boolean isAdaptiveChargingVisible(Context context) {
        return Settings.Secure.getInt(context.getContentResolver(), "adaptive_charging_visible", 1)
                == 1;
    }

    private static void setChargingOptimizationMode(Context context, int mode) {
        Settings.Secure.putInt(context.getContentResolver(), "charge_optimization_mode", mode);
    }
}
