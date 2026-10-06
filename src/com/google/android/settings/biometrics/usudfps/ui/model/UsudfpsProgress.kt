package com.google.android.settings.biometrics.usudfps.ui.model

import com.android.settings.biometrics.fingerprint2.lib.model.FingerEnrollState

data class UsudfpsProgress(val remainingSteps: Int, val totalStepsRequired: Int, val taps: Int) {
    companion object {
        fun newInstance(enrollProgress: FingerEnrollState.EnrollProgress, taps: Int) =
            UsudfpsProgress(enrollProgress.remainingSteps, enrollProgress.totalStepsRequired, taps)
    }
}
