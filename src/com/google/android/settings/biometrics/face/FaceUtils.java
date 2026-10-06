package com.google.android.settings.biometrics.face;

import android.content.Context;

import com.android.internal.util.FrameworkStatsLog;

public abstract class FaceUtils {
    public static final int[] CENTER_BUCKETS = {1108, 1112, 1113, 1114, 1118};

    private FaceUtils() {}

    public static void writeVendorLog(int userId, int vendorCode) {
        FrameworkStatsLog.write(
                FrameworkStatsLog.BIOMETRIC_ACQUIRED,
                /* modality= */ 4,
                userId,
                false,
                1,
                0,
                22,
                vendorCode,
                false,
                -1,
                0,
                0,
                false,
                false,
                0,
                0,
                0,
                -1,
                0);
    }

    public static boolean isOneOfCenterBuckets(int acquired) {
        for (int bucket : CENTER_BUCKETS) {
            if (acquired == bucket) {
                return true;
            }
        }
        return false;
    }

    public static boolean isFoldable(Context context) {
        return context != null
                && context.getResources()
                                .getIntArray(com.android.internal.R.array.config_foldedDeviceStates)
                                .length
                        > 0;
    }
}
