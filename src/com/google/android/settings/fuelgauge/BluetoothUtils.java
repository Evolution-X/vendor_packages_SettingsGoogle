package com.google.android.settings.fuelgauge;

import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.ContentValues;
import android.content.Context;
import android.util.Log;

import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.metadata.BundlesKt;

abstract class BluetoothUtils {

    private static final String TAG = "BluetoothUtils";

    static ContentValues wrapBluetoothData(
            Context context, CachedBluetoothDevice cachedDevice, boolean fetchIcon) {
        BluetoothDevice device = cachedDevice.getDevice();
        ContentValues values = new ContentValues();
        values.put("type", device.getType());
        values.put("name", emptyIfNull(device.getName()));
        values.put("alias", emptyIfNull(device.getAlias()));
        values.put("address", emptyIfNull(device.getAddress()));
        values.put("batteryLevel", device.getBatteryLevel());
        putStringMetadata(
                values, "isConnected", Boolean.toString(cachedDevice.isConnected()).getBytes());
        putStringMetadata(
                values,
                "hardwareVersion",
                device.getMetadata(BluetoothDevice.METADATA_HARDWARE_VERSION));
        putStringMetadata(
                values, "deviceType", device.getMetadata(BluetoothDevice.METADATA_DEVICE_TYPE));
        putStringMetadata(
                values,
                "batteryLevelRight",
                device.getMetadata(BluetoothDevice.METADATA_UNTETHERED_RIGHT_BATTERY));
        putStringMetadata(
                values,
                "batteryLevelLeft",
                device.getMetadata(BluetoothDevice.METADATA_UNTETHERED_LEFT_BATTERY));
        putStringMetadata(
                values,
                "batteryLevelCase",
                device.getMetadata(BluetoothDevice.METADATA_UNTETHERED_CASE_BATTERY));
        putStringMetadata(
                values,
                "batteryLevelMain",
                device.getMetadata(BluetoothDevice.METADATA_MAIN_BATTERY));
        putStringMetadata(
                values,
                "batteryChargingRight",
                device.getMetadata(BluetoothDevice.METADATA_UNTETHERED_RIGHT_CHARGING));
        putStringMetadata(
                values,
                "batteryChargingLeft",
                device.getMetadata(BluetoothDevice.METADATA_UNTETHERED_LEFT_CHARGING));
        putStringMetadata(
                values,
                "batteryChargingCase",
                device.getMetadata(BluetoothDevice.METADATA_UNTETHERED_CASE_CHARGING));
        putStringMetadata(
                values,
                "batteryChargingMain",
                device.getMetadata(BluetoothDevice.METADATA_MAIN_CHARGING));
        if (fetchIcon) {
            putStringMetadata(
                    values,
                    "deviceIconMain",
                    device.getMetadata(BluetoothDevice.METADATA_MAIN_ICON));
            putStringMetadata(
                    values,
                    "deviceIconCase",
                    device.getMetadata(BluetoothDevice.METADATA_UNTETHERED_CASE_ICON));
            putStringMetadata(
                    values,
                    "deviceIconLeft",
                    device.getMetadata(BluetoothDevice.METADATA_UNTETHERED_LEFT_ICON));
            putStringMetadata(
                    values,
                    "deviceIconRight",
                    device.getMetadata(BluetoothDevice.METADATA_UNTETHERED_RIGHT_ICON));
        }
        BluetoothClass bluetoothClass = device.getBluetoothClass();
        if (bluetoothClass != null) {
            values.put("bluetoothClass", BundlesKt.marshallParcel(bluetoothClass));
        }
        return values;
    }

    static boolean isBatteryAllTheTimeSupported(CachedBluetoothDevice cachedDevice) {
        String batt =
                com.android.settingslib.bluetooth.BluetoothUtils.getFastPairCustomizedField(
                        cachedDevice.getDevice(), "BATT");
        Log.d(TAG, "BATT: " + batt);
        return Boolean.parseBoolean(batt);
    }

    private static void putStringMetadata(ContentValues values, String key, byte[] data) {
        if (data == null || data.length == 0) {
            return;
        }
        values.put(key, new String(data));
    }

    private static String emptyIfNull(String value) {
        return value == null ? "" : value;
    }
}
