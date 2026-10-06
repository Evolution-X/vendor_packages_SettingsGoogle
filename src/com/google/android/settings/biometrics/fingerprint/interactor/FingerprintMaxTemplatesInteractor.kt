package com.google.android.settings.biometrics.fingerprint.interactor

import com.google.android.settings.biometrics.fingerprint.data.repository.FingerprintsRepository
import kotlinx.coroutines.flow.first

interface FingerprintMaxTemplatesInteractor {
    suspend fun getMaxTemplates(isSuw: Boolean, isFastEnroll: Boolean): Int
}

class FingerprintMaxTemplatesInteractorImpl(
    private val fingerprintsRepository: FingerprintsRepository
) : FingerprintMaxTemplatesInteractor {

    override suspend fun getMaxTemplates(isSuw: Boolean, isFastEnroll: Boolean): Int {
        return fingerprintsRepository.getMaxTemplates(isSuw, isFastEnroll).first()
    }
}
