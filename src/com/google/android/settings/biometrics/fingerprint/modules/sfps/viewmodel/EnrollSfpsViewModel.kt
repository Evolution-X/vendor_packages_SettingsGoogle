package com.google.android.settings.biometrics.fingerprint.modules.sfps.viewmodel

import android.content.res.Configuration
import android.hardware.fingerprint.FingerprintEnrollOptions
import android.util.Log
import android.view.Surface
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.settings.biometrics.fingerprint2.domain.interactor.AccessibilityInteractor
import com.android.settings.biometrics.fingerprint2.domain.interactor.FingerprintVibrationEffects
import com.android.settings.biometrics.fingerprint2.domain.interactor.FoldStateInteractor
import com.android.settings.biometrics.fingerprint2.domain.interactor.OrientationInteractor
import com.android.settings.biometrics.fingerprint2.domain.interactor.VibrationInteractor
import com.android.settings.biometrics.fingerprint2.lib.domain.interactor.CanEnrollFingerprintsInteractor
import com.android.settings.biometrics.fingerprint2.lib.domain.interactor.EnrollFingerprintInteractor
import com.android.settings.biometrics.fingerprint2.lib.domain.interactor.EnrolledFingerprintsInteractor
import com.android.settings.biometrics.fingerprint2.lib.domain.interactor.UserInteractor
import com.android.settings.biometrics.fingerprint2.lib.model.EnrollReason
import com.android.settings.biometrics.fingerprint2.lib.model.FingerEnrollState
import com.google.android.settings.biometrics.fingerprint.modules.sfps.model.SfpsStageModel
import com.google.android.settings.biometrics.fingerprint.ui.model.CredentialModel
import com.google.android.settings.biometrics.fingerprint.ui.model.EnrollmentRequest
import com.google.android.settings.biometrics.sfps.model.EnrollOperationModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combineTransform
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class EnrollSfpsViewModel(
    val isFastEnroll: Boolean,
    private val foldStateInteractor: FoldStateInteractor,
    private val orientationInteractor: OrientationInteractor,
    private val accessibilityInteractor: AccessibilityInteractor,
    private val vibrationInteractor: VibrationInteractor,
    private val enrollInteractor: EnrollFingerprintInteractor,
    private val enrollRequest: EnrollmentRequest,
    private val credentialModel: CredentialModel,
    private val enrollStageThresholds: FloatArray,
    private val userInteractor: UserInteractor,
    private val enrolledFingerprintsInteractor: EnrolledFingerprintsInteractor,
    private val canEnrollFingerprintsInteractor: CanEnrollFingerprintsInteractor,
) : ViewModel() {

    init {
        userInteractor.updateUser(credentialModel.userId)
        Log.d(TAG, "Enrolling sfps for user ${credentialModel.userId}")
    }

    private val _orientation = MutableStateFlow<Int?>(null)

    private val _enrollOperationState =
        MutableStateFlow<EnrollOperationModel>(EnrollOperationModel.Idle)
    val enrollOperationState: StateFlow<EnrollOperationModel> = _enrollOperationState.asStateFlow()

    private val _orientationCombined =
        merge(_orientation.filterNotNull(), orientationInteractor.rotationFromDefault)

    private val rotationAndFoldInfo: Flow<Pair<Boolean, Int>> =
        combineTransform(
            foldStateInteractor.isFolded.distinctUntilChanged(),
            _orientationCombined.distinctUntilChanged(),
        ) { isFolded, rotation ->
            emit(Pair(isFolded, rotation))
        }

    val isSuw: Boolean = enrollRequest.isSuw

    private val enrollState: SharedFlow<FingerEnrollState> =
        _enrollOperationState
            .flatMapLatest {
                flow {
                    if (it == EnrollOperationModel.Running) {
                        val options =
                            FingerprintEnrollOptions.Builder()
                                .setEnrollReason(enrollRequest.enrollReason)
                                .build()
                        emitAll(
                            enrollInteractor.enroll(
                                credentialModel.token,
                                EnrollReason.EnrollEnrolling,
                                options,
                            )
                        )
                    }
                }
            }
            .shareIn(viewModelScope, SharingStarted.Lazily, 0)

    val enrollError: Flow<FingerEnrollState.EnrollError> =
        enrollState.filterIsInstance<FingerEnrollState.EnrollError>()

    val isTalkbackEnabled: StateFlow<Boolean> =
        accessibilityInteractor
            .isEnabledFlow(viewModelScope)
            .stateIn(viewModelScope, SharingStarted.Eagerly, accessibilityInteractor.isEnabled)

    private val enrollStage: SharedFlow<SfpsStageModel> =
        enrollState
            .filterIsInstance<FingerEnrollState.EnrollProgress>()
            .distinctUntilChanged()
            .map { progress ->
                Log.e(STAGE_TAG, "got progress $progress")
                val progressPercent =
                    1.0f - progress.remainingSteps.toFloat() / progress.totalStepsRequired.toFloat()
                if (progressPercent <= enrollStageThresholds[0]) {
                    SfpsStageModel.Unknown
                } else if (progressPercent <= enrollStageThresholds[1]) {
                    SfpsStageModel.Center
                } else if (progressPercent <= enrollStageThresholds[2]) {
                    SfpsStageModel.Fingertip
                } else if (progressPercent <= enrollStageThresholds[3]) {
                    SfpsStageModel.LeftEdge
                } else {
                    SfpsStageModel.RightEdge
                }
            }
            .shareIn(viewModelScope, SharingStarted.Eagerly, 0)

    val enrollHelp: Flow<FingerEnrollState.EnrollHelp> =
        enrollState.filterIsInstance<FingerEnrollState.EnrollHelp>()

    val enrollGoodAcquired: Flow<FingerEnrollState.Acquired> =
        enrollState
            .filterIsInstance<FingerEnrollState.Acquired>()
            .map { if (it.acquiredGood) it else null }
            .filterNotNull()

    private val enrollStageNonFiltered: SharedFlow<SfpsStageModel> =
        enrollStage
            .onStart { emit(SfpsStageModel.Unknown) }
            .shareIn(viewModelScope, SharingStarted.Eagerly, 1)

    val helpActual: Flow<Any> = merge(enrollHelp, enrollStageNonFiltered).sample(HELP_SAMPLE_MS)

    val enrollStageLottie: Flow<SfpsStageModel> = enrollStageNonFiltered.distinctUntilChanged()

    val lastProgress = MutableStateFlow<FingerEnrollState.EnrollProgress?>(null)

    val enrollProgress: SharedFlow<FingerEnrollState.EnrollProgress> =
        enrollState
            .filterIsInstance<FingerEnrollState.EnrollProgress>()
            .map { progress ->
                lastProgress.update { progress }
                progress
            }
            .distinctUntilChanged()
            .shareIn(viewModelScope, SharingStarted.Eagerly, 0)

    val shouldVibrateForError: Flow<Boolean> =
        combineTransform(enrollError, isTalkbackEnabled) { _, isTalkbackEnabled ->
            if (isTalkbackEnabled) {
                emit(true)
            }
        }

    val shouldUseMaxSizeText: Flow<Boolean> = rotationAndFoldInfo.map { (isFolded, rotation) ->
        !isFolded || rotation == Surface.ROTATION_180
    }

    fun onUpdateOrientation(orientation: Int) {
        _orientation.update { orientationInteractor.getRotationFromDefault(orientation) }
    }

    fun onConfigurationChanged(newConfig: Configuration) {
        foldStateInteractor.onConfigurationChange(newConfig)
    }

    fun doVibrate() {
        vibrationInteractor.vibrate(FingerprintVibrationEffects.UdfpsError, "$TAG::doVibrate")
    }

    fun readyForEnrollment() {
        if (_enrollOperationState.value == EnrollOperationModel.Idle) {
            _enrollOperationState.update { EnrollOperationModel.Running }
        }
    }

    fun finishEnrollment() {
        _enrollOperationState.update { EnrollOperationModel.Idle }
    }

    fun interruptAccessibility() {
        accessibilityInteractor.interrupt()
    }

    suspend fun isEnrollable(): Boolean {
        val enrolledCount =
            enrolledFingerprintsInteractor.enrolledFingerprints.firstOrNull()?.size ?: 0
        return enrolledCount < canEnrollFingerprintsInteractor.maxFingerprintsEnrollable.first()
    }

    companion object {
        private const val TAG = "EnrollSfpsViewModel"
        private const val STAGE_TAG = "EnrollSfpsFrag"
        private const val HELP_SAMPLE_MS = 200L
    }
}
