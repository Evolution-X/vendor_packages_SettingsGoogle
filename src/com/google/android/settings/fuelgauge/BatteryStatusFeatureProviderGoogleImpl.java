package com.google.android.settings.fuelgauge;

import android.content.Context;
import android.database.ContentObserver;
import android.provider.Settings;

import com.android.settings.fuelgauge.BatteryInfo;
import com.android.settings.fuelgauge.BatteryPreferenceController;
import com.android.settings.fuelgauge.BatteryStatusFeatureProviderImpl;
import com.android.settings.fuelgauge.BatteryUtils;
import com.android.settingslib.utils.ThreadUtils;

import com.google.android.settings.R;
import com.google.android.systemui.googlebattery.AdaptiveChargingManager;

import java.util.concurrent.TimeUnit;

public class BatteryStatusFeatureProviderGoogleImpl extends BatteryStatusFeatureProviderImpl {
    private boolean mAdaptiveChargingEnabledInSettings;
    private AdaptiveChargingManager mAdaptiveChargingManager;

    public BatteryStatusFeatureProviderGoogleImpl(Context context) {
        super(context);
        mAdaptiveChargingManager = new AdaptiveChargingManager(context);
        mContext.getContentResolver()
                .registerContentObserver(
                        Settings.Secure.getUriFor("adaptive_charging_enabled"),
                        false,
                        new ContentObserver(null) {
                            @Override
                            public void onChange(boolean selfChange) {
                                refreshAdaptiveChargingEnabled();
                            }
                        });
        refreshAdaptiveChargingEnabled();
    }

    private void refreshAdaptiveChargingEnabled() {
        mAdaptiveChargingEnabledInSettings =
                mAdaptiveChargingManager.isAvailable() && mAdaptiveChargingManager.isEnabled();
    }

    // Reverse charging (ReverseChargingManager) is not ported.
    @Override
    public boolean triggerBatteryStatusUpdate(
            final BatteryPreferenceController batteryPreferenceController,
            final BatteryInfo batteryInfo) {
        if (batteryInfo.discharging
                || BatteryUtils.isBatteryDefenderOn(batteryInfo)
                || !mAdaptiveChargingEnabledInSettings) {
            return false;
        }
        mAdaptiveChargingManager.queryStatus(
                new AdaptiveChargingManager.AdaptiveChargingStatusReceiver() {
                    private boolean mSetStatus;

                    @Override
                    public void onReceiveStatus(String stage, int deadlineSecs) {
                        if (AdaptiveChargingManager.isActive(stage, deadlineSecs)) {
                            final String estimateStr =
                                    mContext.getString(
                                            R.string.adaptive_charging_time_estimate,
                                            mAdaptiveChargingManager.formatTimeToFull(
                                                    System.currentTimeMillis()
                                                            + TimeUnit.SECONDS.toMillis(
                                                                    deadlineSecs + 29)));
                            mSetStatus = true;
                            ThreadUtils.postOnMainThread(
                                    () ->
                                            batteryPreferenceController.updateBatteryStatus(
                                                    estimateStr, batteryInfo));
                        }
                    }

                    @Override
                    public void onDestroyInterface() {
                        if (mSetStatus) {
                            return;
                        }
                        ThreadUtils.postOnMainThread(
                                () ->
                                        batteryPreferenceController.updateBatteryStatus(
                                                null, batteryInfo));
                    }
                });
        return true;
    }
}
