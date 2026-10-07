package com.google.android.settings.fuelgauge;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.util.Log;

import com.android.settingslib.bluetooth.BatteryLevelsInfo;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothManager;

import com.google.android.settings.R;
import com.google.android.settings.experiments.PhenotypeProxy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

abstract class BluetoothBatteryMetadataFetcher {

    private static final String TAG = "BluetoothBatteryMetadataFetcher";

    static boolean sBatteryAllTheTimeEnabled = false;

    static void returnBluetoothDevices(Context context, Intent intent) {
        Log.i(TAG, String.format("isBattEnabled: %b", isBatteryAllTheTimeEnabled(context, true)));
        AsyncTask.execute(() -> returnBluetoothDevicesInner(context, intent));
    }

    private static void returnBluetoothDevicesInner(Context context, Intent intent) {
        ResultReceiver resultReceiver =
                intent.getParcelableExtra(Intent.EXTRA_RESULT_RECEIVER, ResultReceiver.class);
        if (resultReceiver == null) {
            Log.w(TAG, "No result receiver found from intent");
            return;
        }
        LocalBluetoothManager localBluetoothManager =
                LocalBluetoothManager.getInstance(context, null);
        BluetoothAdapter adapter = context.getSystemService(BluetoothManager.class).getAdapter();
        if (adapter == null || !adapter.isEnabled() || localBluetoothManager == null) {
            Log.w(TAG, "BluetoothAdapter not present or not enabled");
            resultReceiver.send(1, null);
        } else {
            sendAndFilterBluetoothData(
                    context,
                    resultReceiver,
                    localBluetoothManager,
                    intent.getBooleanExtra("extra_fetch_icon", false));
        }
    }

    static void sendAndFilterBluetoothData(
            Context context,
            ResultReceiver resultReceiver,
            LocalBluetoothManager localBluetoothManager,
            boolean fetchIcon) {
        long startTime = System.currentTimeMillis();
        Collection<CachedBluetoothDevice> cachedDevices =
                localBluetoothManager.getCachedDeviceManager().getCachedDevicesCopy();
        Log.d(TAG, "cachedDevices:" + cachedDevices);
        if (cachedDevices == null || cachedDevices.isEmpty()) {
            resultReceiver.send(0, Bundle.EMPTY);
            return;
        }
        Collection<CachedBluetoothDevice> filteredDevices =
                cachedDevices.stream()
                        .filter(device -> shouldReturnDevice(context, device))
                        .collect(Collectors.toList());
        Log.d(TAG, "filteredDevices:" + filteredDevices);
        if (filteredDevices.isEmpty()) {
            resultReceiver.send(0, Bundle.EMPTY);
            return;
        }
        ArrayList<ContentValues> wrapDataList = new ArrayList<>();
        ArrayList<BluetoothDevice> deviceList = new ArrayList<>();
        for (CachedBluetoothDevice cachedDevice : filteredDevices) {
            BluetoothDevice device = cachedDevice.getDevice();
            deviceList.add(device);
            try {
                wrapDataList.add(
                        BluetoothUtils.wrapBluetoothData(context, cachedDevice, fetchIcon));
            } catch (Exception e) {
                Log.e(TAG, "wrapBluetoothData() failed: " + device, e);
            }
        }
        Bundle bundle = new Bundle();
        bundle.putParcelableArrayList("bluetoothParcelableListKey", deviceList);
        if (!wrapDataList.isEmpty()) {
            bundle.putParcelableArrayList("bluetoothWrapDataListKey", wrapDataList);
        }
        resultReceiver.send(0, bundle);
        Log.d(
                TAG,
                String.format(
                        "sendAndFilterBluetoothData() size=%d in %d/ms",
                        wrapDataList.size(), System.currentTimeMillis() - startTime));
    }

    private static boolean shouldReturnDevice(Context context, CachedBluetoothDevice cachedDevice) {
        if (cachedDevice.getDevice().getBondState() != BluetoothDevice.BOND_BONDED) {
            return false;
        }
        if (cachedDevice.isConnected()) {
            return true;
        }
        if (!isBatteryAllTheTimeEnabled(context, false)
                || !BluetoothUtils.isBatteryAllTheTimeSupported(cachedDevice)) {
            return false;
        }
        BatteryLevelsInfo batteryLevelsInfo = cachedDevice.getBatteryLevelsInfo();
        return batteryLevelsInfo != null && hasValidBattBattery(batteryLevelsInfo);
    }

    static boolean isBatteryAllTheTimeEnabled(Context context, boolean forceRefresh) {
        if (forceRefresh) {
            sBatteryAllTheTimeEnabled =
                    PhenotypeProxy.getBooleanFlagByPackageAndKey(
                            context,
                            context.getString(R.string.config_settingsintelligence_package_name),
                            "BatteryWidget__is_batt_enabled",
                            false);
        }
        Log.i(
                TAG,
                String.format(
                        "isBattEnabled: %b, forceRefresh: %b",
                        sBatteryAllTheTimeEnabled, forceRefresh));
        return sBatteryAllTheTimeEnabled;
    }

    private static boolean hasValidBattBattery(BatteryLevelsInfo batteryLevelsInfo) {
        return batteryLevelsInfo.getLeftBatteryLevel() != BluetoothDevice.BATTERY_LEVEL_UNKNOWN
                || batteryLevelsInfo.getRightBatteryLevel() != BluetoothDevice.BATTERY_LEVEL_UNKNOWN
                || batteryLevelsInfo.getCaseBatteryLevel() != BluetoothDevice.BATTERY_LEVEL_UNKNOWN;
    }
}
