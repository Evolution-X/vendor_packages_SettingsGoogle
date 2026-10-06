package com.google.android.settings.biometrics.fingerprint;

import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;

import com.google.hardware.biometrics.sidefps.IFingerprintExt;

import java.util.function.Supplier;

public abstract class Utils {
    private static final String TAG = "BiometricUtil";

    public static void resumeEnroll() {
        IFingerprintExt fingerprintExt = getFingerprintExtSupplier().get();
        if (fingerprintExt == null) {
            Log.e(TAG, "Failed to connect to the fingerprint extension");
            return;
        }
        try {
            fingerprintExt.resumeEnroll();
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException", e);
        }
    }

    private static Supplier<IFingerprintExt> getFingerprintExtSupplier() {
        return () -> {
            IBinder binder =
                    ServiceManager.waitForDeclaredService(
                            "android.hardware.biometrics.fingerprint.IFingerprint/default");
            if (binder == null) {
                Log.e(TAG, "Unable to get fingerprint service");
                return null;
            }
            try {
                return IFingerprintExt.Stub.asInterface(binder.getExtension());
            } catch (RemoteException e) {
                e.printStackTrace();
                return null;
            }
        };
    }
}
