package com.google.android.settings.fuelgauge;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public final class BatteryBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "BatteryBroadcastReceiver";

    static final String ACTION_FETCH_BLUETOOTH_BATTERY_DATA =
            "settings.intelligence.battery.action.FETCH_BLUETOOTH_BATTERY_DATA";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }
        Log.d(TAG, "onReceive:" + intent.getAction());
        // Stock also handles ENABLE_POWER_MONITOR_RECEIVER for userdebug power monitor
        // tooling, which is not ported.
        if (ACTION_FETCH_BLUETOOTH_BATTERY_DATA.equals(intent.getAction())) {
            try {
                BluetoothBatteryMetadataFetcher.returnBluetoothDevices(context, intent);
            } catch (Exception e) {
                Log.e(TAG, "returnBluetoothDevices() error", e);
            }
        }
    }
}
