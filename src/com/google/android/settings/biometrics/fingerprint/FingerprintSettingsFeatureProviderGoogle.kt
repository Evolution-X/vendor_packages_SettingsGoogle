package com.google.android.settings.biometrics.fingerprint

import com.android.settings.R as AR
import com.android.settings.biometrics.fingerprint.FingerprintSettingsFeatureProvider

object FingerprintSettingsFeatureProviderGoogle : FingerprintSettingsFeatureProvider() {
    override fun getSettingPageDescription(): Int {
        return AR.string.security_settings_fingerprint_description
    }

    override fun getSettingPageFooterLearnMoreDescription(): Int {
        return AR.string.security_settings_fingerprint_settings_footer_learn_more
    }
}
