package com.google.android.settings.biometrics.fingerprint.ui.model

import com.google.android.settings.biometrics.fingerprint.model.SpProductInfo

sealed class SpUiData {

    data object None : SpUiData()

    data object ThirdParty : SpUiData()

    data object Unset : SpUiData()

    data class FirstParty(val productInfo: SpProductInfo) : SpUiData()
}
