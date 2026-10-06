package com.google.android.settings.biometrics.face;

import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.settings.biometrics.face.FaceEnrollParentalConsent;
import com.android.settingslib.widget.theme.R.color;

import com.google.android.settings.R;
import com.google.android.setupdesign.util.ThemeHelper;

public class FaceEnrollParentalConsentGoogle extends FaceEnrollParentalConsent {
    public static final int[] CONSENT_STRING_RESOURCES = {
        com.android.settings.R.string.security_settings_face_enroll_consent_introduction_title,
        R.string.security_settings_face_enroll_consent_secure_and_helpful_message_1,
        R.string.security_settings_face_enroll_introduction_secure_and_helpful_message_2,
        com.android.settings.R.string.security_settings_face_enroll_introduction_consent_message,
        com.android.settings.R.string
                .security_settings_face_enroll_introduction_info_consent_glasses,
        com.android.settings.R.string
                .security_settings_face_enroll_introduction_info_consent_looking,
        com.android.settings.R.string.security_settings_face_enroll_introduction_info_consent_gaze,
        R.string.security_settings_face_enroll_introduction_how_consent_message_1,
        com.android.settings.R.string
                .security_settings_face_enroll_introduction_control_consent_title,
        com.android.settings.R.string
                .security_settings_face_enroll_introduction_control_consent_message,
        com.android.settings.R.string.security_settings_face_enroll_introduction_consent_message_0,
        R.string.security_settings_face_enroll_consent_message,
        com.android.settings.R.string
                .security_settings_face_enroll_introduction_info_consent_less_secure,
        R.string.security_settings_face_enroll_introduction_learn_more_message,
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ImageView iconGlasses = findViewById(com.android.settings.R.id.icon_glasses);
        ImageView iconLooking = findViewById(com.android.settings.R.id.icon_looking);
        ImageView iconSecurityPrivacySafe = findViewById(R.id.icon_security_privacy_safe);
        ImageView iconPrivacyTip = findViewById(R.id.icon_privacy_tip);
        ImageView iconFamiliarFaceAndZone = findViewById(R.id.icon_familiar_face_and_zone);
        ImageView iconTrashCan = findViewById(com.android.settings.R.id.icon_trash_can);
        ImageView iconLink = findViewById(com.android.settings.R.id.icon_link);
        iconGlasses.getBackground().setColorFilter(getIconColorFilter());
        iconLooking.getBackground().setColorFilter(getIconColorFilter());
        iconSecurityPrivacySafe.getBackground().setColorFilter(getIconColorFilter());
        iconPrivacyTip.getBackground().setColorFilter(getIconColorFilter());
        iconFamiliarFaceAndZone.getBackground().setColorFilter(getIconColorFilter());
        iconTrashCan.getBackground().setColorFilter(getIconColorFilter());
        iconLink.getBackground().setColorFilter(getIconColorFilter());
        ((TextView) findViewById(R.id.footer_message_7))
                .setText(
                        R.string
                                .security_settings_face_enroll_consent_secure_and_helpful_message_1);
        TextView messageLearnMore = findViewById(R.id.message_learn_more);
        messageLearnMore.setText(
                Html.fromHtml(
                        getString(
                                R.string
                                        .security_settings_face_enroll_introduction_learn_more_message),
                        0));
        messageLearnMore.setMovementMethod(LinkMovementMethod.getInstance());
        if (!ThemeHelper.shouldApplyGlifExpressiveStyle(getApplicationContext())) {
            return;
        }
        ImageView illustrationImage = findViewById(com.android.settings.R.id.illustrationImage);
        if (illustrationImage != null) {
            illustrationImage.setImageResource(
                    R.drawable.face_enroll_intro_illustration_expressive);
        }
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.face_enroll_introduction_2;
    }

    @Override
    protected int getHowMessage() {
        return R.string.security_settings_face_enroll_introduction_how_consent_message_1;
    }

    @Override
    protected int getInControlMessage() {
        return R.string.security_settings_face_enroll_introduction_control_consent_message_1;
    }

    @Override
    protected void updateDescriptionText() {
        super.updateDescriptionText();
        if (isFaceStrong()) {
            setDescriptionText(getString(R.string.security_settings_face_enroll_consent_message));
        } else {
            setDescriptionText(
                    com.android.settings.R.string
                            .security_settings_face_enroll_introduction_consent_message_0);
        }
    }

    @Override
    protected PorterDuffColorFilter getIconColorFilter() {
        if (mIconColorFilter == null) {
            mIconColorFilter =
                    new PorterDuffColorFilter(
                            getColor(color.settingslib_materialColorOnSurfaceVariant),
                            PorterDuff.Mode.SRC_IN);
        }
        return mIconColorFilter;
    }
}
