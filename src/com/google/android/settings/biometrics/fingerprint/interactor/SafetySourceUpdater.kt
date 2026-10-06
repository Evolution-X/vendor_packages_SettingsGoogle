package com.google.android.settings.biometrics.fingerprint.interactor

import android.content.Context
import com.android.settings.safetycenter.FingerprintSafetySource

interface SafetySourceUpdater {
    fun onBiometricsChanged()
}

class SafetySourceUpdaterImpl(private val context: Context) : SafetySourceUpdater {

    override fun onBiometricsChanged() {
        FingerprintSafetySource.onBiometricsChanged(context)
    }
}
