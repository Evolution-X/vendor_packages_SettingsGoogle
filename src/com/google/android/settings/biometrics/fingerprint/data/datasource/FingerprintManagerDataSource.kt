package com.google.android.settings.biometrics.fingerprint.data.datasource

import android.hardware.fingerprint.Fingerprint
import android.hardware.fingerprint.FingerprintManager
import android.hardware.fingerprint.FingerprintSensorPropertiesInternal
import android.hardware.fingerprint.IFingerprintAuthenticatorsRegisteredCallback
import android.util.Log
import com.google.android.settings.biometrics.fingerprint.model.FingerprintRemoval
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.withContext

interface FingerprintManagerDataSource {
    fun generateChallenge(
        userId: Int,
        generateChallengeCallback: FingerprintManager.GenerateChallengeCallback,
    )

    fun generateChallenge2(userId: Int): Flow<Long>

    fun getEnrollStageThreshold(stage: Int): Float

    fun getFingerprintSensor(): SharedFlow<FingerprintSensorPropertiesInternal>

    fun getNumOfEnrolledFingerprints(userId: Int): Int

    fun removeAll(userId: Int): Flow<FingerprintRemoval>

    fun revokeChallenge(userId: Int, challenge: Long)
}

class FingerprintManagerDataSourceImpl
private constructor(
    private val fingerprintManager: FingerprintManager,
    coroutineScope: CoroutineScope,
    backgroundDispatcher: CoroutineDispatcher,
) : FingerprintManagerDataSource {

    private val fingerprintSensor: SharedFlow<FingerprintSensorPropertiesInternal> =
        callbackFlow {
                val callback =
                    object : IFingerprintAuthenticatorsRegisteredCallback.Stub() {
                        override fun onAllAuthenticatorsRegistered(
                            sensorProperties: List<FingerprintSensorPropertiesInternal>
                        ) {
                            if (sensorProperties.isEmpty()) {
                                Log.e(TAG, "empty sensors from onAllAuthenticatorsRegistered")
                                return
                            }
                            trySend(sensorProperties[0])
                        }
                    }
                withContext(backgroundDispatcher) {
                    fingerprintManager.addAuthenticatorsRegisteredCallback(callback)
                }
                awaitClose {}
            }
            .shareIn(coroutineScope, SharingStarted.Eagerly, 1)

    override fun getFingerprintSensor(): SharedFlow<FingerprintSensorPropertiesInternal> =
        fingerprintSensor

    override fun getNumOfEnrolledFingerprints(userId: Int): Int =
        fingerprintManager.getEnrolledFingerprints(userId).size

    override fun generateChallenge(
        userId: Int,
        generateChallengeCallback: FingerprintManager.GenerateChallengeCallback,
    ) {
        fingerprintManager.generateChallenge(userId, generateChallengeCallback)
    }

    override fun generateChallenge2(userId: Int): Flow<Long> = callbackFlow {
        val callback =
            object : FingerprintManager.GenerateChallengeCallback {
                override fun onChallengeGenerated(sensorId: Int, userId: Int, challenge: Long) {
                    trySend(challenge)
                    close()
                }
            }
        fingerprintManager.generateChallenge(userId, callback)
        awaitClose {}
    }

    override fun removeAll(userId: Int): Flow<FingerprintRemoval> = callbackFlow {
        val removalCallback =
            object : FingerprintManager.RemovalCallback() {
                override fun onRemovalError(
                    fp: Fingerprint?,
                    errMsgId: Int,
                    errString: CharSequence?,
                ) {
                    trySend(FingerprintRemoval.Error(fp, errMsgId, errString))
                    close()
                }

                override fun onRemovalSucceeded(fp: Fingerprint?, remaining: Int) {
                    trySend(FingerprintRemoval.Succeeded(fp, remaining))
                    if (remaining == 0) {
                        close()
                    }
                }
            }
        fingerprintManager.removeAll(userId, removalCallback)
        awaitClose {}
    }

    override fun revokeChallenge(userId: Int, challenge: Long) {
        fingerprintManager.revokeChallenge(userId, challenge)
    }

    override fun getEnrollStageThreshold(stage: Int): Float =
        fingerprintManager.getEnrollStageThreshold(stage)

    companion object {
        private const val TAG = "FingerprintManagerRepository"

        @Volatile private var instance: FingerprintManagerDataSource? = null

        @Synchronized
        @JvmStatic
        fun getInstance(
            fingerprintManager: FingerprintManager,
            coroutineScope: CoroutineScope,
            backgroundDispatcher: CoroutineDispatcher,
        ): FingerprintManagerDataSource {
            return instance
                ?: FingerprintManagerDataSourceImpl(
                        fingerprintManager,
                        coroutineScope,
                        backgroundDispatcher,
                    )
                    .also { instance = it }
        }
    }
}
