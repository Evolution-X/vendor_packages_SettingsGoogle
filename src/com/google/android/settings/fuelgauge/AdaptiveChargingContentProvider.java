package com.google.android.settings.fuelgauge;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.android.settings.R;

import com.google.android.settings.fuelgauge.adaptivecharging.AdaptiveChargingUtils;
import com.google.android.systemui.googlebattery.AdaptiveChargingManager;

public class AdaptiveChargingContentProvider extends ContentProvider {
    private static final String TAG = "AdaptiveChargingContentProvider";
    static final String METHOD_GET_ADAPTIVE_CHARGING_AVAILABILITY =
            "get_adaptive_charging_availability";
    static final String METHOD_IS_ADAPTIVE_CHARGING_CHECKED = "is_adaptive_charging_checked";
    static final String METHOD_SET_ADAPTIVE_CHARGING_CHECKED = "set_adaptive_charging_checked";
    AdaptiveChargingManager mAdaptiveChargingManager;

    @Override
    public boolean onCreate() {
        mAdaptiveChargingManager =
                new AdaptiveChargingManager(getContext().getApplicationContext());
        return true;
    }

    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        if (method == null || method.isEmpty()) {
            return Bundle.EMPTY;
        }
        if (!AdaptiveChargingUtils.isSystemUser()) {
            Log.w(TAG, "call: ignore non-system users for " + method);
            return Bundle.EMPTY;
        }
        Log.d(TAG, "method: " + method);
        if (!isCalledFromSI()) {
            Log.w(TAG, "caller is invalid from " + getCallingPackageNonFinal());
            return null;
        }
        Context applicationContext = getContext().getApplicationContext();
        if (METHOD_GET_ADAPTIVE_CHARGING_AVAILABILITY.equals(method)) {
            return AdaptiveChargingUtils.getIsAvailableBundle(mAdaptiveChargingManager);
        }
        if (METHOD_IS_ADAPTIVE_CHARGING_CHECKED.equals(method)) {
            return AdaptiveChargingUtils.getIsCheckedBundle(mAdaptiveChargingManager);
        }
        if (METHOD_SET_ADAPTIVE_CHARGING_CHECKED.equals(method)) {
            AdaptiveChargingUtils.setChecked(applicationContext, mAdaptiveChargingManager, extras);
        }
        return null;
    }

    @Override
    public Cursor query(
            Uri uri,
            String[] projection,
            String selection,
            String[] selectionArgs,
            String sortOrder) {
        Log.w(TAG, "unsupported query() from " + getCallingPackageNonFinal());
        return null;
    }

    @Override
    public String getType(Uri uri) {
        Log.w(TAG, "unsupported getType() from " + getCallingPackageNonFinal());
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        Log.w(TAG, "unsupported insert() from " + getCallingPackageNonFinal());
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        Log.w(TAG, "unsupported delete() from " + getCallingPackageNonFinal());
        return -1;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        Log.w(TAG, "unsupported update() from " + getCallingPackageNonFinal());
        return -1;
    }

    private boolean isCalledFromSI() {
        String callerPackage = getCallingPackageNonFinal();
        Log.d(TAG, "callerPackage: " + callerPackage);
        return getContext()
                        .getString(R.string.config_settingsintelligence_package_name)
                        .equals(callerPackage)
                && Utils.isPreinstalledApp(getContext(), callerPackage);
    }

    String getCallingPackageNonFinal() {
        return getCallingPackage();
    }
}
