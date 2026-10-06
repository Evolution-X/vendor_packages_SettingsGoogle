package com.google.android.settings.biometrics.face

import android.app.Activity
import com.android.settings.biometrics.face.FaceEnrollActivityClassProvider

object FaceEnrollActivityClassProviderGoogle : FaceEnrollActivityClassProvider() {
    override val next: Class<out Activity>
        get() = FaceEnrollIntroductionGoogle::class.java
}
