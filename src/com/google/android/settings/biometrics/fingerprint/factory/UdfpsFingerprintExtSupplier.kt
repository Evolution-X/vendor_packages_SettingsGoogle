package com.google.android.settings.biometrics.fingerprint.factory

import android.os.RemoteException
import android.os.ServiceManager
import android.util.Log
import com.google.hardware.biometrics.fingerprint.IFingerprintExt
import java.util.function.Supplier

object UdfpsFingerprintExtSupplier : Supplier<IFingerprintExt?> {

    private const val TAG = "UdfpsFingerprintExtSupplier"

    private const val SERVICE_NAME = "android.hardware.biometrics.fingerprint.IFingerprint/default"

    override fun get(): IFingerprintExt? {
        return getSupplier().get()
    }

    private fun getSupplier(): Supplier<IFingerprintExt?> = Supplier {
        val binder = ServiceManager.waitForDeclaredService(SERVICE_NAME)
        if (binder == null) {
            Log.e(TAG, "Unable to get fingerprint service")
            return@Supplier null
        }
        try {
            IFingerprintExt.Stub.asInterface(binder.extension)
        } catch (e: RemoteException) {
            Log.e(TAG, "IFingerprintExt.Stub.asInterface RemoteException $e")
            null
        }
    }
}
