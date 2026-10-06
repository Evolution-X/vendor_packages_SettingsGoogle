package com.google.android.settings.biometrics.fingerprint.interactor

import android.os.Handler
import com.google.android.settings.biometrics.fingerprint.feature.UdfpsEnrollCalibratorImpl
import java.util.UUID

class UsUdfpsCalibratorInteractor(private val mainHandler: Handler, private val initUuid: UUID?) {

    val calibrator: UdfpsEnrollCalibratorImpl? by lazy {
        UdfpsEnrollCalibratorImpl.getInstance(mainHandler, initUuid)
    }
}
