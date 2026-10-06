package com.google.android.settings.biometrics.fingerprint.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.android.settings.biometrics.fingerprint.UdfpsEnrollCalibrator
import com.google.android.settings.biometrics.fingerprint.interactor.UsUdfpsCalibratorInteractor
import com.google.android.settings.biometrics.fingerprint.ui.model.EnrollmentRequest
import java.util.UUID

class UsUdfpsCalibratorViewModel(
    private val calibratorInteractor: UsUdfpsCalibratorInteractor,
    private val request: EnrollmentRequest,
) : ViewModel() {
    init {
        if (calibratorInteractor.calibrator != null) {
            request.calibratorUuid = calibratorInteractor.calibrator?.uuid
        }
    }

    val calibratorUuid: UUID?
        get() = request.calibratorUuid

    val calibrator: UdfpsEnrollCalibrator?
        get() = calibratorInteractor.calibrator
}
