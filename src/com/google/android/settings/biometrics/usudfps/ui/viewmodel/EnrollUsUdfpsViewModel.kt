package com.google.android.settings.biometrics.usudfps.ui.viewmodel

import android.hardware.fingerprint.FingerprintEnrollOptions
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.settings.biometrics.fingerprint2.domain.interactor.AccessibilityInteractor
import com.android.settings.biometrics.fingerprint2.domain.interactor.FingerprintSensorInteractor
import com.android.settings.biometrics.fingerprint2.domain.interactor.OrientationInteractor
import com.android.settings.biometrics.fingerprint2.lib.domain.interactor.CanEnrollFingerprintsInteractor
import com.android.settings.biometrics.fingerprint2.lib.domain.interactor.EnrollFingerprintInteractor
import com.android.settings.biometrics.fingerprint2.lib.domain.interactor.EnrolledFingerprintsInteractor
import com.android.settings.biometrics.fingerprint2.lib.domain.interactor.UserInteractor
import com.android.settings.biometrics.fingerprint2.lib.model.EnrollReason
import com.android.settings.biometrics.fingerprint2.lib.model.FingerEnrollState
import com.android.systemui.biometrics.shared.model.FingerprintSensor
import com.google.android.settings.biometrics.fingerprint.interactor.FingerprintEnrollStageThresholdInteractor
import com.google.android.settings.biometrics.fingerprint.interactor.HareModeGetter
import com.google.android.settings.biometrics.fingerprint.interactor.SafetySourceUpdater
import com.google.android.settings.biometrics.fingerprint.model.HareMode
import com.google.android.settings.biometrics.fingerprint.ui.model.CredentialModel
import com.google.android.settings.biometrics.usudfps.ui.model.EnrollStage
import com.google.android.settings.biometrics.usudfps.ui.model.HareStage
import com.google.android.settings.biometrics.usudfps.ui.model.UsudfpsProgress
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.launch

abstract class EnrollUsUdfpsViewModel : ViewModel() {
    abstract val progressFlow: StateFlow<UsudfpsProgress?>
    abstract val stageFlow: StateFlow<EnrollStage>
    abstract val hareStageFlow: StateFlow<HareStage>
    abstract val nextHareStageFlow: StateFlow<HareStage>
    abstract val isStageHalfCompletedFlow: StateFlow<Boolean>
    abstract val helpFlow: SharedFlow<FingerEnrollState.EnrollHelp?>
    abstract val errorFlow: SharedFlow<FingerEnrollState.EnrollError?>
    abstract val acquiredFlow: SharedFlow<FingerEnrollState.Acquired?>
    abstract val pointerDownFlow: SharedFlow<FingerEnrollState.PointerDown?>
    abstract val pointerUpFlow: SharedFlow<FingerEnrollState.PointerUp?>
    abstract val rotation: Flow<Int>
    abstract val isSuw: Boolean
    abstract val isFastEnroll: Boolean

    abstract fun startEnroll(): Boolean

    abstract fun cancelEnroll()

    abstract fun isEnrolling(): Boolean

    abstract suspend fun getSensorProp(): FingerprintSensor

    abstract fun isAccessibilityEnabled(): Boolean

    abstract suspend fun isEnrollable(): Boolean

    abstract suspend fun getHareMode(): HareMode
}

class EnrollUsUdfpsViewModelImpl(
    private val safetySourceUpdater: SafetySourceUpdater,
    override val isSuw: Boolean,
    override val isFastEnroll: Boolean,
    private val enrollReason: Int,
    private val credentialModel: CredentialModel,
    userInteractor: UserInteractor,
    private val sensorInteractor: FingerprintSensorInteractor,
    private val enrollStateThresholdInteractor: FingerprintEnrollStageThresholdInteractor,
    orientationInteractor: OrientationInteractor,
    private val accessibilityInteractor: AccessibilityInteractor,
    private val enroll2Interactor: EnrollFingerprintInteractor,
    private val enrolledFingerprintsInteractor: EnrolledFingerprintsInteractor,
    private val canEnrollFingerprintsInteractor: CanEnrollFingerprintsInteractor,
    private val hareModeGetter: HareModeGetter,
) : EnrollUsUdfpsViewModel() {

    private val _progressFlow = MutableStateFlow<UsudfpsProgress?>(null)
    override val progressFlow: StateFlow<UsudfpsProgress?> = _progressFlow.asStateFlow()

    override val stageFlow: StateFlow<EnrollStage> =
        _progressFlow
            .transform { progress ->
                if (progress == null) {
                    emit(EnrollStage.INIT_STAGE)
                    return@transform
                }
                val completedSteps = progress.totalStepsRequired - progress.remainingSteps
                for (stage in EnrollStage.POSITIVE_STAGES) {
                    if (completedSteps < getThresholdSteps(progress, stage)) {
                        emit(stage)
                        return@transform
                    }
                }
                emit(EnrollStage.LAST_STAGE)
            }
            .stateIn(viewModelScope, SharingStarted.Eagerly, EnrollStage.UNKNOWN)

    override val hareStageFlow: StateFlow<HareStage> =
        _progressFlow
            .transform { progress ->
                emit(if (progress == null) HareStage.INIT else toHareStage(progress))
            }
            .stateIn(viewModelScope, SharingStarted.Eagerly, HareStage.INIT)

    override val nextHareStageFlow: StateFlow<HareStage> =
        _progressFlow
            .transform { progress ->
                emit(if (progress == null) HareStage.TAP_TO_STAGE[1] else toNextHareStage(progress))
            }
            .stateIn(viewModelScope, SharingStarted.Eagerly, HareStage.TAP_TO_STAGE[1])

    override val isStageHalfCompletedFlow: StateFlow<Boolean> =
        _progressFlow
            .transform { progress ->
                if (progress == null) {
                    emit(false)
                    return@transform
                }
                if (getHareMode() != HareMode.Disabled) {
                    emit(false)
                    Log.d(TAG, "Not supported in HareMode")
                    return@transform
                }
                val completedSteps = progress.totalStepsRequired - progress.remainingSteps
                var previousThreshold = 0
                for (stage in EnrollStage.POSITIVE_STAGES) {
                    val threshold = getThresholdSteps(progress, stage)
                    if (previousThreshold <= completedSteps && completedSteps < threshold) {
                        emit(
                            completedSteps - previousThreshold >=
                                (threshold - previousThreshold) / 2
                        )
                        return@transform
                    }
                    previousThreshold = threshold
                }
                emit(true)
            }
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private fun toHareStage(progress: UsudfpsProgress): HareStage {
        val index =
            if (progress.taps < 0) {
                0
            } else if (progress.taps >= HareStage.TAP_TO_STAGE.size) {
                HareStage.TAP_TO_STAGE.size - 1
            } else {
                progress.taps
            }
        return HareStage.TAP_TO_STAGE[index]
    }

    private fun toNextHareStage(progress: UsudfpsProgress): HareStage {
        val index =
            if (progress.taps < 0) {
                1
            } else if (progress.taps + 1 >= HareStage.TAP_TO_STAGE.size) {
                HareStage.TAP_TO_STAGE.size - 1
            } else {
                progress.taps + 1
            }
        return HareStage.TAP_TO_STAGE[index]
    }

    private fun getThresholdSteps(progress: UsudfpsProgress, stage: EnrollStage): Int =
        (progress.totalStepsRequired * enrollStateThresholdInteractor.getThreshold(stage.value))
            .roundToInt()

    private val _helpFlow = MutableSharedFlow<FingerEnrollState.EnrollHelp?>(replay = 1)
    override val helpFlow: SharedFlow<FingerEnrollState.EnrollHelp?> = _helpFlow.asSharedFlow()

    private val _errorFlow = MutableSharedFlow<FingerEnrollState.EnrollError?>(replay = 1)
    override val errorFlow: SharedFlow<FingerEnrollState.EnrollError?> = _errorFlow.asSharedFlow()

    private val _acquiredFlow = MutableSharedFlow<FingerEnrollState.Acquired?>(replay = 1)
    override val acquiredFlow: SharedFlow<FingerEnrollState.Acquired?> =
        _acquiredFlow.asSharedFlow()

    private val _pointerDownFlow = MutableSharedFlow<FingerEnrollState.PointerDown?>(replay = 1)
    override val pointerDownFlow: SharedFlow<FingerEnrollState.PointerDown?> =
        _pointerDownFlow.asSharedFlow()

    private val _pointerUpFlow = MutableSharedFlow<FingerEnrollState.PointerUp?>(replay = 1)
    override val pointerUpFlow: SharedFlow<FingerEnrollState.PointerUp?> =
        _pointerUpFlow.asSharedFlow()

    override val rotation: Flow<Int> = orientationInteractor.rotation.distinctUntilChanged()

    private var enrollingJob: Job? = null

    init {
        userInteractor.updateUser(credentialModel.userId)
        viewModelScope.launch { _helpFlow.emit(null) }
    }

    override fun startEnroll(): Boolean {
        val token = credentialModel.token
        if (token == null) {
            Log.e(TAG, "Null hardware auth token for enroll")
            return false
        }
        enrollingJob?.cancel(CancellationException("RestartEnroll"))
        enrollingJob = viewModelScope.launch {
            Log.e(TAG, "startEnroll()")
            _progressFlow.emit(null)
            _helpFlow.emit(null)
            _errorFlow.emit(null)
            _pointerDownFlow.emit(null)
            _pointerUpFlow.emit(null)
            enroll2Interactor
                .enroll(
                    token,
                    EnrollReason.EnrollEnrolling,
                    FingerprintEnrollOptions.Builder().setEnrollReason(enrollReason).build(),
                )
                .collect { state ->
                    when (state) {
                        is FingerEnrollState.EnrollProgress -> {
                            Log.d(
                                TAG,
                                "EnrollProgress(${state.totalStepsRequired},${state.remainingSteps})",
                            )
                            _helpFlow.emit(null)
                            _progressFlow.emit(
                                UsudfpsProgress.newInstance(
                                    state,
                                    (_progressFlow.firstOrNull()?.taps ?: 0) + 1,
                                )
                            )
                            if (state.remainingSteps == 0) {
                                safetySourceUpdater.onBiometricsChanged()
                            }
                        }
                        is FingerEnrollState.EnrollHelp -> _helpFlow.emit(state)
                        is FingerEnrollState.EnrollError -> _errorFlow.emit(state)
                        is FingerEnrollState.Acquired -> _acquiredFlow.emit(state)
                        is FingerEnrollState.PointerDown -> _pointerDownFlow.emit(state)
                        is FingerEnrollState.PointerUp -> _pointerUpFlow.emit(state)
                        else -> Log.d(TAG, "enroll() get unexpected $state")
                    }
                }
        }
        return true
    }

    override fun cancelEnroll() {
        enrollingJob?.cancel(CancellationException("CancelEnroll"))
        enrollingJob = null
    }

    override fun isEnrolling(): Boolean = enrollingJob != null

    override suspend fun getSensorProp(): FingerprintSensor {
        return sensorInteractor.fingerprintSensor.first()
    }

    override fun isAccessibilityEnabled(): Boolean {
        return accessibilityInteractor.isEnabled
    }

    override suspend fun getHareMode(): HareMode = hareModeGetter.getHareMode()

    override suspend fun isEnrollable(): Boolean {
        val enrolledList = enrolledFingerprintsInteractor.enrolledFingerprints.firstOrNull()
        val count = enrolledList?.size ?: 0
        val max = canEnrollFingerprintsInteractor.maxFingerprintsEnrollable.first()
        return count < max
    }

    companion object {
        private const val TAG = "EnrollUsUdfpsViewModel"
    }
}
