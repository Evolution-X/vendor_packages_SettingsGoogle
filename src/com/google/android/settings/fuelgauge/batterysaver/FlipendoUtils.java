package com.google.android.settings.fuelgauge.batterysaver;

import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;

public abstract class FlipendoUtils {
    private static final String TAG = "FlipendoUtils";

    static final Uri FLIPENDO_ENABLED_OBSERVABLE_URI =
            Uri.parse("content://com.google.android.flipendo.api/get_flipendo_state");
    public static final String FLIPENDO_IS_AGGRESSIVE_KEY = "is_flipendo_aggressive";
    public static final String FLIPENDO_STATE_AUTHORITY = "com.google.android.flipendo.api";
    public static final String FLIPENDO_STATE_METHOD = "get_flipendo_state";

    public static Pair<Boolean, Boolean> getFlipendoState(Context context) {
        Bundle bundle = null;
        try {
            bundle =
                    context.getApplicationContext()
                            .getContentResolver()
                            .call(FLIPENDO_STATE_AUTHORITY, FLIPENDO_STATE_METHOD, null, null);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "getFlipendoState() failed", e);
        }
        if (bundle == null) {
            bundle = new Bundle();
        }
        return new Pair<>(
                bundle.getBoolean(FLIPENDO_IS_AGGRESSIVE_KEY, false),
                bundle.getBoolean("flipendo_state", false));
    }

    public static Boolean isFlipendoInstalled(Context context) {
        try {
            context.getPackageManager().getPackageInfo("com.google.android.flipendo", 0);
            return Boolean.TRUE;
        } catch (PackageManager.NameNotFoundException unused) {
            Log.i(TAG, "Flipendo app not installed on this device");
            return Boolean.FALSE;
        }
    }
}
