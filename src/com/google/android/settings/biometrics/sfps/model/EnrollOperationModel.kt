package com.google.android.settings.biometrics.sfps.model

sealed class EnrollOperationModel {
    data object Idle : EnrollOperationModel()

    data object Running : EnrollOperationModel()
}
