package com.google.android.settings.biometrics.fingerprint.data.repository

import android.util.Log
import com.google.android.settings.biometrics.fingerprint.model.HareMode
import com.google.hardware.biometrics.fingerprint.IFingerprintExt
import com.google.hardware.biometrics.fingerprint.IHareConfigCallback
import java.util.function.Supplier
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async

interface HareRepository {
    suspend fun getHareMode(): HareMode
}

class HareRepositoryImpl
private constructor(
    private val fingerprintExtSupplier: Supplier<IFingerprintExt?>,
    internal val externalScope: CoroutineScope,
    internal val dispatcher: CoroutineDispatcher,
) : HareRepository {

    private val hareModeDeferred: Deferred<HareMode> =
        externalScope.async(dispatcher) { fetchHareModeFromHal() }

    suspend fun fetchHareModeFromHal(): HareMode {
        Log.d(TAG, "Start requesting HareMode from HAL...")
        val result = CompletableDeferred<HareMode>()
        val callback =
            object : IHareConfigCallback.Stub() {
                override fun getInterfaceVersion(): Int = INTERFACE_VERSION

                override fun onHareConfig(enabled: Boolean, param1: Int, param2: Int) {
                    Log.d(TAG, "onHareConfig($enabled, $param1, $param2)")
                    result.complete(HareMode.newInstance(enabled, param1, param2))
                }

                override fun getInterfaceHash(): String = INTERFACE_HASH
            }
        try {
            val fingerprintExt = fingerprintExtSupplier.get()
            if (fingerprintExt == null) {
                Log.e(TAG, "Empty fingerprintExt")
                result.complete(HareMode.Disabled)
            } else {
                fingerprintExt.requestHareConfig(callback)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Got exception during getHareMode $e")
            result.complete(HareMode.Disabled)
        }
        return result.await()
    }

    override suspend fun getHareMode(): HareMode = hareModeDeferred.await()

    companion object {
        private const val TAG = "HareRepositoryImpl"
        private const val INTERFACE_VERSION = 3
        private const val INTERFACE_HASH = "fb35cef863421d4bb91eb447fcaf7717460c5e6c"

        @Volatile private var instance: HareRepositoryImpl? = null

        @Synchronized
        fun getInstance(
            fingerprintExtSupplier: Supplier<IFingerprintExt?>,
            externalScope: CoroutineScope,
            dispatcher: CoroutineDispatcher,
        ): HareRepository {
            val current = instance
            if (current == null) {
                instance = HareRepositoryImpl(fingerprintExtSupplier, externalScope, dispatcher)
            } else {
                if (current.dispatcher != dispatcher) {
                    Log.w(TAG, "new $dispatcher != previous ${current.dispatcher}")
                }
                if (current.externalScope != externalScope) {
                    Log.w(TAG, "new $externalScope != previous ${current.externalScope}")
                }
            }
            return instance!!
        }
    }
}
