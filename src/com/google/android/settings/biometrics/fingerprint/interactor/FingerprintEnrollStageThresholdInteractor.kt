package com.google.android.settings.biometrics.fingerprint.interactor

import com.google.android.settings.biometrics.fingerprint.data.repository.FingerprintsRepository

interface FingerprintEnrollStageThresholdInteractor {
    fun getThreshold(stage: Int): Float
}

class FingerprintEnrollStageThresholdInteractorImpl(
    private val fingerprintsRepository: FingerprintsRepository
) : FingerprintEnrollStageThresholdInteractor {
    override fun getThreshold(stage: Int): Float {
        return fingerprintsRepository.getEnrollStageThreshold(stage)
    }
}
