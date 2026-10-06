package com.google.android.settings.biometrics.fingerprint.feature

import android.app.Activity
import com.android.settings.biometrics.fingerprint.FingerprintEnrollActivityClassProvider
import com.google.android.settings.biometrics.fingerprint.ui.view.FingerprintEnrollmentActivity

object FingerprintActivityProviderWithoutFastEnroll : FingerprintEnrollActivityClassProvider() {
    override val default: Class<out Activity> = FingerprintEnrollmentActivity::class.java
    override val setup: Class<out Activity> =
        FingerprintEnrollmentActivity.SetupActivity::class.java
    override val internal: Class<out Activity> =
        FingerprintEnrollmentActivity.InternalActivity::class.java
    override val addAnother: Class<out Activity>
        get() = super.addAnother

    override val setupSkipIntro: Class<out Activity>
        get() = super.setupSkipIntro

    override val skipIntro: Class<out Activity>
        get() = super.skipIntro
}
