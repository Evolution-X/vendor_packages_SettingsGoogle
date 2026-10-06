package com.google.android.settings.biometrics.fingerprint.feature

import android.app.Activity
import com.android.settings.biometrics.fingerprint.FingerprintEnrollActivityClassProvider
import com.google.android.settings.biometrics.fingerprint.ui.view.FingerprintEnrollmentActivity

object FingerprintEnrollActivityClassProviderGoogleImpl : FingerprintEnrollActivityClassProvider() {
    override val default: Class<out Activity> = FingerprintEnrollmentActivity::class.java
    override val setup: Class<out Activity> =
        FingerprintEnrollmentActivity.SetupActivity::class.java
    override val internal: Class<out Activity> =
        FingerprintEnrollmentActivity.InternalActivity::class.java
    override val addAnother: Class<out Activity> =
        FingerprintEnrollmentActivity.AddAnother::class.java
    override val setupSkipIntro: Class<out Activity> =
        FingerprintEnrollmentActivity.SetupActivity::class.java
    override val skipIntro: Class<out Activity> = FingerprintEnrollmentActivity::class.java
}
