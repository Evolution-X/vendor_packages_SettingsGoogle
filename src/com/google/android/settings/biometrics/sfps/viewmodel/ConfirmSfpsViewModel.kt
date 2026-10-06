package com.google.android.settings.biometrics.sfps.viewmodel

import androidx.lifecycle.ViewModel
import com.android.settings.biometrics.fingerprint2.lib.domain.interactor.CanEnrollFingerprintsInteractor
import com.android.settings.biometrics.fingerprint2.lib.domain.interactor.EnrolledFingerprintsInteractor
import com.android.settings.biometrics.fingerprint2.lib.domain.interactor.UserInteractor
import com.google.android.settings.biometrics.fingerprint.interactor.PrivateProfileInteractor
import com.google.android.settings.biometrics.fingerprint.ui.model.EnrollmentRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull

class ConfirmSfpsViewModel(
    userId: Int,
    userInteractor: UserInteractor,
    private val enrolledFingerprintsInteractor: EnrolledFingerprintsInteractor,
    private val canEnrollFingerprintsInteractor: CanEnrollFingerprintsInteractor,
    privateProfileInteractor: PrivateProfileInteractor,
    val request: EnrollmentRequest,
) : ViewModel() {

    init {
        userInteractor.updateUser(userId)
        canEnrollFingerprintsInteractor.setShouldUseSettingsMaxFingerprints(false)
    }

    val isPrivateProfile: Flow<Boolean> = privateProfileInteractor.isPrivateProfile

    suspend fun canEnrollMoreFingerprints(): Boolean {
        val enrolledCount =
            enrolledFingerprintsInteractor.enrolledFingerprints.firstOrNull()?.size ?: 0
        return enrolledCount < canEnrollFingerprintsInteractor.maxFingerprintsEnrollable.first()
    }

    suspend fun getEnrolledFingerprints(): Int {
        return enrolledFingerprintsInteractor.enrolledFingerprints.first()?.size ?: 1
    }
}
