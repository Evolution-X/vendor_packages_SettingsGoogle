package com.google.android.settings.biometrics.sfps.widget

import android.os.RemoteException
import android.os.ServiceManager
import android.util.Log
import com.google.hardware.biometrics.sidefps.IFingerprintExt
import java.util.function.Supplier

object FingerprintExtUtils {
    private const val TAG = "BiometricUtil"
    private const val SERVICE_NAME = "android.hardware.biometrics.fingerprint.IFingerprint/default"

    fun resumeEnroll() {
        val fingerprintExt = getFingerprintExtSupplier().get()
        if (fingerprintExt == null) {
            Log.e(TAG, "Failed to connect to the fingerprint extension")
            return
        }
        try {
            fingerprintExt.resumeEnroll()
        } catch (e: RemoteException) {
            Log.e(TAG, "RemoteException", e)
        }
    }

    fun getPauseEnrollThreshold(): Int {
        val fingerprintExt = getFingerprintExtSupplier().get()
        if (fingerprintExt == null) {
            Log.e(TAG, "Failed to connect to the fingerprint extension")
            return -1
        }
        return try {
            fingerprintExt.pauseEnrollThreshold
        } catch (e: RemoteException) {
            Log.e(TAG, "RemoteException", e)
            -1
        }
    }

    private fun getFingerprintExtSupplier(): Supplier<IFingerprintExt?> = Supplier {
        val binder = ServiceManager.waitForDeclaredService(SERVICE_NAME)
        if (binder == null) {
            Log.e(TAG, "Unable to get fingerprint service")
            return@Supplier null
        }
        try {
            IFingerprintExt.Stub.asInterface(binder.extension)
        } catch (e: RemoteException) {
            e.printStackTrace()
            null
        }
    }
}
