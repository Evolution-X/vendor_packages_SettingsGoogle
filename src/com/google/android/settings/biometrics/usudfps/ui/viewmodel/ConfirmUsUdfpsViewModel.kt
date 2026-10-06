package com.google.android.settings.biometrics.usudfps.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.android.settings.biometrics.fingerprint2.lib.domain.interactor.CanEnrollFingerprintsInteractor
import com.android.settings.biometrics.fingerprint2.lib.domain.interactor.EnrolledFingerprintsInteractor
import com.android.settings.biometrics.fingerprint2.lib.domain.interactor.UserInteractor
import com.google.android.settings.biometrics.fingerprint.interactor.HareModeGetter
import com.google.android.settings.biometrics.fingerprint.model.HareMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull

abstract class ConfirmUsUdfpsViewModel : ViewModel() {
    abstract val isSuw: Boolean

    abstract suspend fun isEnrollable(): Boolean

    abstract suspend fun getEnrolledFingerprints(): Int

    abstract suspend fun getHareMode(): HareMode
}

class ConfirmUsUdfpsViewModelImpl(
    override val isSuw: Boolean,
    userId: Int,
    userInteractor: UserInteractor,
    private val enrolledFingerprintsInteractor: EnrolledFingerprintsInteractor,
    private val canEnrollFingerprintsInteractor: CanEnrollFingerprintsInteractor,
    private val hareModeGetter: HareModeGetter,
) : ConfirmUsUdfpsViewModel() {

    init {
        userInteractor.updateUser(userId)
        canEnrollFingerprintsInteractor.setShouldUseSettingsMaxFingerprints(false)
    }

    override suspend fun isEnrollable(): Boolean {
        val enrolledCount =
            enrolledFingerprintsInteractor.enrolledFingerprints.firstOrNull()?.size ?: 0
        return enrolledCount < canEnrollFingerprintsInteractor.maxFingerprintsEnrollable.first()
    }

    override suspend fun getEnrolledFingerprints(): Int {
        return enrolledFingerprintsInteractor.enrolledFingerprints.first()?.size ?: 1
    }

    override suspend fun getHareMode(): HareMode = hareModeGetter.getHareMode()
}
