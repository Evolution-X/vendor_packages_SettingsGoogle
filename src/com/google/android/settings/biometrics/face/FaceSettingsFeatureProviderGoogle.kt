package com.google.android.settings.biometrics.face

import com.android.settings.biometrics.face.FaceSettingsFeatureProvider
import com.google.android.settings.R

object FaceSettingsFeatureProviderGoogle : FaceSettingsFeatureProvider() {
    override fun getSettingPageDescription(): Int = R.string.security_settings_face_description

    override fun getSettingPageFooterDescriptionClass3(): Int =
        R.string.security_settings_face_footer_description_class3

    override fun getSettingPageFooterLearnMoreDescription(): Int =
        R.string.security_settings_face_footer_learn_more_description

    override fun getSettingPageFooterLearnMoreUrl(): Int =
        R.string.security_settings_face_footer_learn_more_url
}
