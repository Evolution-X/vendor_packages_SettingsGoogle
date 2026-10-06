package com.google.android.settings.biometrics.fingerprint.data.repository

import android.hardware.fingerprint.FingerprintManager
import android.hardware.fingerprint.FingerprintSensorPropertiesInternal
import com.google.android.settings.biometrics.fingerprint.data.datasource.FingerprintManagerDataSource
import com.google.android.settings.biometrics.fingerprint.data.datasource.ResourcesDataSource
import com.google.android.settings.biometrics.fingerprint.model.FingerprintRemoval
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapNotNull

interface FingerprintsRepository {
    fun generateChallenge(
        userId: Int,
        generateChallengeCallback: FingerprintManager.GenerateChallengeCallback,
    )

    fun generateChallenge2(userId: Int): Flow<Long>

    fun getEnrollStageThreshold(stage: Int): Float

    fun getFingerprintSensor(): SharedFlow<FingerprintSensorPropertiesInternal>

    fun getMaxTemplates(isSuw: Boolean, isFastEnroll: Boolean): Flow<Int>

    fun getNumOfEnrolledFingerprints(userId: Int): Int

    fun removeAll(userId: Int): Flow<FingerprintRemoval>

    fun revokeChallenge(userId: Int, challenge: Long)
}

class FingerprintsRepositoryImpl
private constructor(
    private val fingerprintManagerDataSource: FingerprintManagerDataSource,
    private val resourcesDataSource: ResourcesDataSource,
) : FingerprintsRepository {

    override fun getFingerprintSensor(): SharedFlow<FingerprintSensorPropertiesInternal> =
        fingerprintManagerDataSource.getFingerprintSensor()

    override fun getNumOfEnrolledFingerprints(userId: Int): Int =
        fingerprintManagerDataSource.getNumOfEnrolledFingerprints(userId)

    override fun getMaxTemplates(isSuw: Boolean, isFastEnroll: Boolean): Flow<Int> {
        if (isSuw && !isFastEnroll) {
            return flowOf(resourcesDataSource.getSuwMaxFingerprintsEnrollable())
        }
        return fingerprintManagerDataSource.getFingerprintSensor().mapNotNull { sensorProperty ->
            sensorProperty.maxEnrollmentsPerUser
        }
    }

    override fun generateChallenge(
        userId: Int,
        generateChallengeCallback: FingerprintManager.GenerateChallengeCallback,
    ) {
        fingerprintManagerDataSource.generateChallenge(userId, generateChallengeCallback)
    }

    override fun generateChallenge2(userId: Int): Flow<Long> =
        fingerprintManagerDataSource.generateChallenge2(userId)

    override fun removeAll(userId: Int): Flow<FingerprintRemoval> =
        fingerprintManagerDataSource.removeAll(userId)

    override fun revokeChallenge(userId: Int, challenge: Long) {
        fingerprintManagerDataSource.revokeChallenge(userId, challenge)
    }

    override fun getEnrollStageThreshold(stage: Int): Float =
        fingerprintManagerDataSource.getEnrollStageThreshold(stage)

    companion object {
        @Volatile private var instance: FingerprintsRepository? = null

        @JvmStatic
        fun getInstance(
            fingerprintManagerDataSource: FingerprintManagerDataSource,
            resourcesDataSource: ResourcesDataSource,
        ): FingerprintsRepository {
            return instance
                ?: synchronized(this) {
                    instance
                        ?: FingerprintsRepositoryImpl(
                                fingerprintManagerDataSource,
                                resourcesDataSource,
                            )
                            .also { instance = it }
                }
        }
    }
}
