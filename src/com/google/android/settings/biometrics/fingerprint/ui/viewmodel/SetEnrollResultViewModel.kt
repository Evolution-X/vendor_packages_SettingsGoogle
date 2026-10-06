package com.google.android.settings.biometrics.fingerprint.ui.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class SetEnrollResultViewModel : ViewModel() {

    private val _resultFlow = MutableSharedFlow<FingerprintEnrollResult>()

    val resultFlow: SharedFlow<FingerprintEnrollResult> = _resultFlow.asSharedFlow()

    suspend fun emit(result: FingerprintEnrollResult) {
        _resultFlow.emit(result)
    }
}

enum class FingerprintEnrollResult {
    GENERATE_CHALLENGE_FAILED,
    ACTIVITY_ON_PAUSE_UNEXPECTED,
    INTRO_FRAGMENT_SKIP_OR_CANCEL_BUTTON,
    INTRO_FRAGMENT_DONE_AND_FINISH_BUTTON,
    INTRO_FRAGMENT_CONTINUE_ENROLL,
    SPLIT_DIALOG_DISMISS,
    FIND_SENSOR_SKIP_BUTTON,
    FIND_SENSOR_ERROR_TIMEOUT,
    FIND_SENSOR_ERROR_FINISH,
    ENROLL_ERROR_DIALOG_OK_BUTTON_TIMEOUT,
    ENROLL_ERROR_DIALOG_OK_BUTTON_FINISH,
    FIND_SENSOR_NEXT_SCREEN,
    ENROLL_SKIP_BUTTON,
    CONFIRMATION_NEXT_BUTTON,
}
