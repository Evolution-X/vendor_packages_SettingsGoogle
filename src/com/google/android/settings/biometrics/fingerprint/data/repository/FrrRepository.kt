package com.google.android.settings.biometrics.fingerprint.data.repository

import android.os.SystemProperties
import android.util.Log
import com.google.android.settings.biometrics.fingerprint.model.SpHal
import com.google.hardware.biometrics.fingerprint.IFingerprintExt
import java.util.function.Supplier

interface FrrRepository {
    val screenProtector: SpHal?

    fun setScreenProtector(spHal: SpHal)
}

class FrrRepositoryImpl
private constructor(private val fingerprintExtSupplier: Supplier<IFingerprintExt?>) :
    FrrRepository {

    override val screenProtector: SpHal?
        get() {
            if (fingerprintExtSupplier.get() == null) {
                Log.d(TAG, "get(), fingerprintExt is null")
                return null
            }
            return try {
                val config = SystemProperties.get(KEY_SP)
                if (config.isNotEmpty()) SpHal.getInstance(config) else null
            } catch (e: Exception) {
                Log.e(TAG, "Got exception during get", e)
                null
            }
        }

    override fun setScreenProtector(spHal: SpHal) {
        val fingerprintExt = fingerprintExtSupplier.get()
        if (fingerprintExt == null) {
            Log.d(TAG, "set(), fingerprintExt is null")
        } else {
            try {
                Log.d(TAG, "setScreenProtector $spHal")
                fingerprintExt.onUserProvideScreenProtectorInfo(spHal.hex.toInt())
                SystemProperties.set(KEY_SP, spHal.hex.toString(16))
                return
            } catch (e: Exception) {
                Log.e(TAG, "Got exception during set", e)
            }
        }
        throw SecurityException("Fail to setup")
    }

    companion object {
        private const val TAG = "FrrRepository"
        private const val KEY_SP = "persist.fingerprint.screenprotector.config"

        @Volatile private var instance: FrrRepository? = null

        @JvmStatic
        @Synchronized
        fun getInstance(fingerprintExtSupplier: Supplier<IFingerprintExt?>): FrrRepository {
            return instance ?: FrrRepositoryImpl(fingerprintExtSupplier).also { instance = it }
        }
    }
}
