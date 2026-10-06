package com.google.android.settings.biometrics.fingerprint.ui.model

import android.content.Intent
import android.os.Bundle
import com.android.settings.SetupWizardUtils
import com.google.android.setupcompat.util.WizardManagerHelper
import java.util.UUID

interface EnrollmentRequest {
    var calibratorUuid: UUID?

    val isSuw: Boolean
    val isFastEnroll: Boolean
    val isAfterSuwOrSuwSuggestedAction: Boolean
    val isFromSafetySource: Boolean
    val isFromFrrNotification: Boolean
    val isSkipIntro: Boolean
    val enrollReason: Int
    val suwExtras: Bundle
    val nextIntentExtra: Bundle
}

class EnrollmentRequestImpl(intent: Intent, isSetup: Boolean, isAddAnother: Boolean) :
    EnrollmentRequest {

    override var calibratorUuid: UUID? =
        intent.getSerializableExtra("calibrator_uuid", UUID::class.java)

    override val isSuw: Boolean = isSetup && WizardManagerHelper.isAnySetupWizard(intent)
    override val isFastEnroll: Boolean = isAddAnother
    override val isAfterSuwOrSuwSuggestedAction: Boolean =
        isSetup &&
            (WizardManagerHelper.isDeferredSetupWizard(intent) ||
                WizardManagerHelper.isPortalSetupWizard(intent) ||
                intent.getBooleanExtra("isSuwSuggestedActionFlow", false))
    override val isFromSafetySource: Boolean =
        intent.getBooleanExtra("launch_from_safety_source_issue", false)
    override val isFromFrrNotification: Boolean = intent.getBooleanExtra("isFromFrr", false)
    override val isSkipIntro: Boolean = intent.getBooleanExtra("skip_intro", false)
    override val enrollReason: Int = intent.getIntExtra("enroll_reason", if (isSuw) 3 else 2)

    private val _suwExtras: Bundle = getSuwExtras(isSuw, intent)

    override val suwExtras: Bundle
        get() = Bundle(_suwExtras)

    override val nextIntentExtra: Bundle
        get() =
            Bundle(_suwExtras).apply {
                putInt("enroll_reason", enrollReason)
                putSerializable("calibrator_uuid", calibratorUuid)
            }

    override fun toString(): String {
        return "${javaClass.simpleName}:{isSuw:$isSuw, isAfterSuwOrSuwSuggestedAction:$isAfterSuwOrSuwSuggestedAction, isFromSafetySource:$isFromSafetySource, isFromFrrNotification:$isFromFrrNotification}"
    }

    companion object {
        private fun getSuwExtras(isSuw: Boolean, intent: Intent): Bundle {
            val dest = Intent()
            if (isSuw) {
                SetupWizardUtils.copySetupExtras(intent, dest)
            }
            return dest.extras ?: Bundle()
        }
    }
}
