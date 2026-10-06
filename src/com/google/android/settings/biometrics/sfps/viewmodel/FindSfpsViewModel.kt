package com.google.android.settings.biometrics.sfps.viewmodel

import android.content.res.Configuration
import android.hardware.biometrics.SensorLocationInternal
import android.hardware.fingerprint.FingerprintEnrollOptions
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.settings.biometrics.fingerprint2.domain.interactor.AccessibilityInteractor
import com.android.settings.biometrics.fingerprint2.domain.interactor.FoldStateInteractor
import com.android.settings.biometrics.fingerprint2.domain.interactor.OrientationInteractor
import com.android.settings.biometrics.fingerprint2.lib.domain.interactor.EnrollFingerprintInteractor
import com.android.settings.biometrics.fingerprint2.lib.domain.interactor.UserInteractor
import com.android.settings.biometrics.fingerprint2.lib.model.EnrollReason
import com.android.settings.biometrics.fingerprint2.lib.model.FingerEnrollState
import com.google.android.settings.biometrics.fingerprint.interactor.FingerprintSensorTypeInteractor
import com.google.android.settings.biometrics.fingerprint.ui.model.CredentialModel
import com.google.android.settings.biometrics.fingerprint.ui.model.EnrollmentRequest
import com.google.android.settings.biometrics.sfps.model.EnrollOperationModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.combineTransform
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update

@OptIn(ExperimentalCoroutinesApi::class)
class FindSfpsViewModel(
    private val enrollRequest: EnrollmentRequest,
    private val foldStateInteractor: FoldStateInteractor,
    private val orientationInteractor: OrientationInteractor,
    private val enrollInteractor: EnrollFingerprintInteractor,
    private val credentialModel: CredentialModel,
    private val userInteractor: UserInteractor,
    private val accessibilityInteractor: AccessibilityInteractor,
    private val sensorTypeInteractor: FingerprintSensorTypeInteractor,
    private val options: FingerprintEnrollOptions,
    private val scope: CoroutineScope? = null,
) : ViewModel() {

    init {
        userInteractor.updateUser(credentialModel.userId)
        Log.d(TAG, "Enrolling sfps for user ${credentialModel.userId}")
    }

    private val _orientation = MutableStateFlow<Int?>(null)

    private val _enrollOperationState =
        MutableStateFlow<EnrollOperationModel>(EnrollOperationModel.Idle)
    val enrollOperationState: StateFlow<EnrollOperationModel> = _enrollOperationState.asStateFlow()

    private val _pauseLottieAnimation = MutableStateFlow(false)

    val enrollState: SharedFlow<FingerEnrollState> =
        _enrollOperationState
            .flatMapLatest {
                flow {
                    if (it == EnrollOperationModel.Running) {
                        emitAll(
                            enrollInteractor.enroll(
                                credentialModel.token,
                                EnrollReason.FindSensor,
                                options,
                            )
                        )
                    }
                }
            }
            .shareIn(getScope(), SharingStarted.Lazily, 0)

    val shouldAnimateLottie: Flow<Boolean> =
        combine(accessibilityInteractor.isEnabledFlow(viewModelScope), _pauseLottieAnimation) {
                _,
                isPaused ->
                !isPaused
            }
            .onStart { emit(true) }
            .distinctUntilChanged()

    private val _orientationCombined =
        merge(_orientation.filterNotNull(), orientationInteractor.rotationFromDefault)

    val sfpsLottieInfo: Flow<Pair<Boolean, Int>> =
        combineTransform(
            foldStateInteractor.isFolded.distinctUntilChanged(),
            _orientationCombined.distinctUntilChanged(),
        ) { isFolded, rotation ->
            emit(Pair(isFolded, rotation))
        }

    val isSuw: Boolean = enrollRequest.isSuw

    private fun getScope(): CoroutineScope = scope ?: viewModelScope

    suspend fun getSensorLocation(): SensorLocationInternal =
        sensorTypeInteractor.getSensorLocation()

    fun onConfigurationChanged(newConfig: Configuration) {
        foldStateInteractor.onConfigurationChange(newConfig)
    }

    fun onUpdateOrientation(orientation: Int) {
        _orientation.update { orientationInteractor.getRotationFromDefault(orientation) }
    }

    fun readyForEnrollment() {
        Log.d(TAG, "readyForEnrollment(${_enrollOperationState.value})")
        if (_enrollOperationState.value == EnrollOperationModel.Idle) {
            _enrollOperationState.update { EnrollOperationModel.Running }
        }
    }

    fun finishEnrollment() {
        Log.d(TAG, "finishEnrollment()")
        _enrollOperationState.update { EnrollOperationModel.Idle }
    }

    fun didTouchLottie() {
        _pauseLottieAnimation.update { !it }
    }

    companion object {
        private const val TAG = "FindSensorViewModel"
    }
}
