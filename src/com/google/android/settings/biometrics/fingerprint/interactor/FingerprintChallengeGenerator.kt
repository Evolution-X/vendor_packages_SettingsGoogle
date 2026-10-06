package com.google.android.settings.biometrics.fingerprint.interactor

import android.hardware.fingerprint.FingerprintManager
import com.google.android.settings.biometrics.fingerprint.data.repository.FingerprintsRepository
import kotlinx.coroutines.flow.Flow

interface FingerprintChallengeGenerator {
    fun generateChallenge(generateChallengeCallback: FingerprintManager.GenerateChallengeCallback)

    fun generateChallenge2(): Flow<Long>

    fun revokeChallenge(challenge: Long)
}

class FingerprintChallengeGeneratorImpl(
    private val userId: Int,
    private val fingerprintsRepository: FingerprintsRepository,
) : FingerprintChallengeGenerator {

    override fun generateChallenge(
        generateChallengeCallback: FingerprintManager.GenerateChallengeCallback
    ) {
        fingerprintsRepository.generateChallenge(userId, generateChallengeCallback)
    }

    override fun generateChallenge2(): Flow<Long> {
        return fingerprintsRepository.generateChallenge2(userId)
    }

    override fun revokeChallenge(challenge: Long) {
        fingerprintsRepository.revokeChallenge(userId, challenge)
    }
}
