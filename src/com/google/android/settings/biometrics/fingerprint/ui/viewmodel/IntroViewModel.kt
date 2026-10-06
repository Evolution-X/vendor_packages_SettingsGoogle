package com.google.android.settings.biometrics.fingerprint.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.android.settingslib.RestrictedLockUtils
import com.google.android.settings.biometrics.fingerprint.interactor.FingerprintMaxTemplatesInteractor
import com.google.android.settings.biometrics.fingerprint.interactor.FingerprintNumOfEnrolledInteractor
import com.google.android.settings.biometrics.fingerprint.interactor.FingerprintSensorTypeInteractor
import com.google.android.settings.biometrics.fingerprint.ui.model.EnrollmentRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow

class IntroViewModel(
    parentalConsentRequiredEnforcedAdmin: RestrictedLockUtils.EnforcedAdmin?,
    fingerprintUnlockDisabledByAdminEnforcedAdmin: RestrictedLockUtils.EnforcedAdmin?,
    private val maxTemplatesInteractor: FingerprintMaxTemplatesInteractor,
    private val sensorTypeInteractor: FingerprintSensorTypeInteractor,
    private val numOfEnrolledInteractor: FingerprintNumOfEnrolledInteractor,
    val request: EnrollmentRequest,
) : ViewModel() {

    private val hasScrolledToBottomFlow = MutableStateFlow(false)

    private val enrollable: Flow<Boolean> = flow {
        emit(
            numOfEnrolledInteractor.getNumOfEnrolledFingerprints() <
                maxTemplatesInteractor.getMaxTemplates(
                    request.isSuw,
                    request.isAfterSuwOrSuwSuggestedAction,
                )
        )
    }

    val uiState: Flow<FingerprintEnrollIntroUiState> =
        combine(hasScrolledToBottomFlow, enrollable) { hasScrolledToBottom, enrollable ->
            FingerprintEnrollIntroUiState(hasScrolledToBottom, enrollable)
        }
    val isParentalConsentRequired: Boolean = parentalConsentRequiredEnforcedAdmin != null
    val isFingerprintUnlockDisabledByAdmin: Boolean =
        fingerprintUnlockDisabledByAdminEnforcedAdmin != null

    suspend fun getSensorType(): Int = sensorTypeInteractor.getType()

    suspend fun onScrollToBottom() {
        hasScrolledToBottomFlow.emit(true)
    }
}

data class FingerprintEnrollIntroUiState(
    val hasScrolledToBottom: Boolean,
    val enrollable: Boolean,
) {
    override fun toString(): String {
        return "${javaClass.simpleName}@${Integer.toHexString(hashCode())}{hasScrolledToBottom:$hasScrolledToBottom, enrollable:$enrollable}"
    }
}
