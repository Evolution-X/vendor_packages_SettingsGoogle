package com.google.android.settings.fuelgauge;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;
import android.os.SystemProperties;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.settings.fuelgauge.BatterySettingsFeatureProviderImpl;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.utils.AnnotationSpan;
import com.android.settingslib.HelpUtils;
import com.android.settingslib.utils.PowerUtil;

import com.google.android.settings.R;

public class BatterySettingsFeatureProviderGoogleImpl extends BatterySettingsFeatureProviderImpl {
    private static final String TAG = "BatterySettingsFeatureProviderGoogleImpl";
    static final int CHARGE_LIMIT_PERCENTAGE = 80;
    static final String EU_SKU = "EMA";
    static final String SKU_KEY = "ro.boot.warranty.sku";

    @Override
    public boolean isChargingOptimizationMode(@NonNull Context context, boolean isLongLife) {
        return isLongLife
                && Settings.Secure.getInt(
                                context.getContentResolver(), "charge_optimization_mode", 0)
                        == 1;
    }

    @Override
    @Nullable
    public CharSequence getChargingOptimizationRemainingLabel(
            @NonNull Context context,
            int batteryLevel,
            int pluggedStatus,
            long chargeRemainingTimeMs,
            long currentTimeMs) {
        boolean isLotXWireless =
                pluggedStatus == BatteryManager.BATTERY_PLUGGED_WIRELESS && shouldApplyLotX();
        if (batteryLevel >= CHARGE_LIMIT_PERCENTAGE) {
            if (isLotXWireless) {
                return TextUtils.concat(
                        context.getString(
                                R.string
                                        .charging_optimization_wireless_reach_limit_remaining_time_label),
                        getLinkify(context));
            }
            return context.getString(
                    R.string.charging_optimization_reach_limit_remaining_time_label);
        }
        if (chargeRemainingTimeMs <= 0) {
            return null;
        }
        String targetTimeShortString =
                PowerUtil.getTargetTimeShortString(context, chargeRemainingTimeMs, currentTimeMs);
        if (isLotXWireless) {
            return TextUtils.concat(
                    context.getString(
                            R.string.charging_optimization_wireless_remaining_time_label,
                            targetTimeShortString),
                    getLinkify(context));
        }
        return context.getString(
                R.string.charging_optimization_remaining_time_label, targetTimeShortString);
    }

    @Override
    @Nullable
    public CharSequence getChargingOptimizationChargeLabel(
            @NonNull Context context,
            int batteryLevel,
            String batteryPercentageString,
            long chargeRemainingTimeMs,
            long currentTimeMs) {
        if (batteryLevel >= CHARGE_LIMIT_PERCENTAGE) {
            return context.getString(
                    R.string.charging_optimization_reach_limit_charge_label,
                    batteryPercentageString);
        }
        if (chargeRemainingTimeMs > 0) {
            return context.getString(
                    R.string.charging_optimization_charge_label,
                    batteryPercentageString,
                    PowerUtil.getTargetTimeShortString(
                            context, chargeRemainingTimeMs, currentTimeMs));
        }
        return null;
    }

    @Override
    public boolean isForceFullCharge(@NonNull Context context) {
        BatteryManager batteryManager = context.getSystemService(BatteryManager.class);
        if (batteryManager == null) {
            return false;
        }
        int chargingPolicy =
                batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGING_POLICY);
        Log.d(TAG, "charging policy = " + chargingPolicy);
        return chargingPolicy == BatteryManager.CHARGING_POLICY_FORCE_FULL_CHARGE;
    }

    @Override
    @Nullable
    public CharSequence getForceFullChargeLabel(@NonNull Context context) {
        return context.getString(R.string.charging_optimization_bypass_limit_remaining_time_label);
    }

    private static boolean shouldApplyLotX() {
        return SystemProperties.getBoolean("charging_string.apply_lotx", false)
                && TextUtils.equals(SystemProperties.get(SKU_KEY), EU_SKU);
    }

    private static CharSequence getLinkify(final Context context) {
        final Intent helpIntent =
                HelpUtils.getHelpIntent(
                        context,
                        context.getString(R.string.help_url_wireless_charging),
                        context.getClass().getName());
        return AnnotationSpan.linkify(
                context.getText(R.string.wireless_charging_learn_more),
                new AnnotationSpan.LinkInfo(
                        "url",
                        view -> {
                            FeatureFactory.getFeatureFactory()
                                    .getMetricsFeatureProvider()
                                    .action(
                                            context,
                                            SettingsEnums.ACTION_WIRELESS_CHARGING_LEARN_MORE);
                            view.startActivityForResult(helpIntent, 0);
                        }));
    }
}
