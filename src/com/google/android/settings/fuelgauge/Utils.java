package com.google.android.settings.fuelgauge;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;

public abstract class Utils {
    private static final String TAG = "Utils";

    public static boolean isPreinstalledApp(Context context, String packageName) {
        try {
            ApplicationInfo applicationInfo =
                    context.getPackageManager().getApplicationInfo(packageName, 0);
            if (applicationInfo != null) {
                return (applicationInfo.flags
                                & (ApplicationInfo.FLAG_SYSTEM
                                        | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP))
                        != 0;
            }
            Log.w(TAG, "PackageName information is null for " + packageName);
            return false;
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "PackageName not found for " + packageName, e);
            return false;
        }
    }
}
