package com.google.android.settings.biometrics.fingerprint.interactor

import com.google.android.settings.biometrics.fingerprint.data.repository.FingerprintsRepository

interface FingerprintNumOfEnrolledInteractor {
    fun getNumOfEnrolledFingerprints(): Int
}

class FingerprintNumOfEnrolledInteractorImpl(
    private val userId: Int,
    private val fingerprintsRepository: FingerprintsRepository,
) : FingerprintNumOfEnrolledInteractor {

    override fun getNumOfEnrolledFingerprints(): Int {
        return fingerprintsRepository.getNumOfEnrolledFingerprints(userId)
    }
}
