package com.google.android.settings.biometrics.fingerprint.modules.sfps.model

sealed class SfpsStageModel {
    data object Unknown : SfpsStageModel()

    data object NoAnimation : SfpsStageModel()

    data object Center : SfpsStageModel()

    data object Fingertip : SfpsStageModel()

    data object LeftEdge : SfpsStageModel()

    data object RightEdge : SfpsStageModel()
}
