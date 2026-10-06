package com.google.android.settings.biometrics.udfps.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.android.settings.biometrics.fingerprint2.lib.domain.interactor.CanEnrollFingerprintsInteractor
import com.android.settings.biometrics.fingerprint2.lib.domain.interactor.EnrolledFingerprintsInteractor
import com.android.settings.biometrics.fingerprint2.lib.domain.interactor.UserInteractor
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull

class ConfirmUdfpsViewModel(
    val isSuw: Boolean,
    userId: Int,
    userInteractor: UserInteractor,
    private val enrolledFingerprintsInteractor: EnrolledFingerprintsInteractor,
    private val canEnrollFingerprintsInteractor: CanEnrollFingerprintsInteractor,
) : ViewModel() {

    init {
        userInteractor.updateUser(userId)
        canEnrollFingerprintsInteractor.setShouldUseSettingsMaxFingerprints(false)
    }

    suspend fun isEnrollable(): Boolean {
        val enrolledCount =
            enrolledFingerprintsInteractor.enrolledFingerprints.firstOrNull()?.size ?: 0
        return enrolledCount < canEnrollFingerprintsInteractor.maxFingerprintsEnrollable.first()
    }

    suspend fun getEnrolledFingerprints(): Int {
        return enrolledFingerprintsInteractor.enrolledFingerprints.first()?.size ?: 1
    }
}
