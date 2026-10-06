package com.google.android.settings.biometrics.fingerprint.model

import android.hardware.fingerprint.Fingerprint

sealed class FingerprintRemoval {

    data class Error(val fp: Fingerprint?, val errMsgId: Int, val errString: CharSequence?) :
        FingerprintRemoval()

    data class Succeeded(val fp: Fingerprint?, val remaining: Int) : FingerprintRemoval()
}
