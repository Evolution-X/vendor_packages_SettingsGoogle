package com.google.android.settings.biometrics.fingerprint.interactor

import com.google.android.settings.biometrics.fingerprint.data.repository.FingerprintsRepository
import com.google.android.settings.biometrics.fingerprint.model.FingerprintRemoval
import kotlinx.coroutines.flow.Flow

interface FingerprintRemovalInteractor {
    fun removeAll(): Flow<FingerprintRemoval>
}

class FingerprintRemovalInteractorImpl(
    private val userId: Int,
    private val fingerprintsRepository: FingerprintsRepository,
) : FingerprintRemovalInteractor {

    override fun removeAll(): Flow<FingerprintRemoval> {
        return fingerprintsRepository.removeAll(userId)
    }
}
