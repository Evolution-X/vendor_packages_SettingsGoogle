package com.google.android.settings.biometrics.fingerprint;

import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.settings.biometrics.fingerprint.FingerprintEnrollParentalConsent;

import com.google.android.settings.R;
import com.google.android.setupdesign.util.ThemeHelper;

public class FingerprintEnrollParentalConsentGoogle extends FingerprintEnrollParentalConsent {
    public static final int[] CONSENT_STRING_RESOURCES = {
        com.android.settings.R.string
                .security_settings_fingerprint_enroll_consent_introduction_title,
        R.string.security_settings_fingerprint_enroll_consent_message,
        R.string.security_settings_fingerprint_enroll_consent_secure_and_helpful_message_1,
        R.string.security_settings_face_enroll_introduction_secure_and_helpful_message_2,
        com.android.settings.R.string
                .security_settings_fingerprint_enroll_introduction_footer_title_consent_1,
        com.android.settings.R.string
                .security_settings_fingerprint_v2_enroll_introduction_footer_message_consent_2,
        com.android.settings.R.string
                .security_settings_fingerprint_v2_enroll_introduction_footer_message_consent_3,
        com.android.settings.R.string
                .security_settings_fingerprint_v2_enroll_introduction_footer_message_consent_4,
        com.android.settings.R.string
                .security_settings_fingerprint_v2_enroll_introduction_footer_message_consent_5,
        com.android.settings.R.string
                .security_settings_fingerprint_v2_enroll_introduction_footer_message_consent_6
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setDescriptionText(R.string.security_settings_fingerprint_enroll_consent_message);
        ((TextView) findViewById(R.id.footer_message_7))
                .setText(
                        R.string
                                .security_settings_fingerprint_enroll_consent_secure_and_helpful_message_1);
        TextView learnMoreView =
                (TextView) findViewById(com.android.settings.R.id.footer_learn_more);
        learnMoreView.setMovementMethod(LinkMovementMethod.getInstance());
        learnMoreView.setText(
                Html.fromHtml(
                        getString(
                                R.string
                                        .security_settings_fingerprint_v2_enroll_introduction_message_learn_more_2),
                        0));
        ImageView securityPrivacySafeIcon =
                (ImageView) findViewById(R.id.icon_security_privacy_safe);
        ImageView privacyTipIcon = (ImageView) findViewById(R.id.icon_privacy_tip);
        securityPrivacySafeIcon.setColorFilter(getIconColorFilter());
        privacyTipIcon.setColorFilter(getIconColorFilter());
        if (!ThemeHelper.shouldApplyGlifExpressiveStyle(getApplicationContext())) {
            return;
        }
        ImageView illustrationView =
                (ImageView) findViewById(com.android.settings.R.id.illustrationImage);
        if (illustrationView != null) {
            illustrationView.setImageResource(
                    R.drawable.fingerprint_enroll_introduction_expressive);
        }
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.fingerprint_enroll_introduction_2;
    }

    @Override
    protected PorterDuffColorFilter getIconColorFilter() {
        if (mIconColorFilter == null) {
            mIconColorFilter =
                    new PorterDuffColorFilter(
                            getColor(
                                    com.android.settingslib.widget.theme.R.color
                                            .settingslib_materialColorOnSurfaceVariant),
                            PorterDuff.Mode.SRC_IN);
        }
        return mIconColorFilter;
    }

    @Override
    protected void updateDescriptionText() {
        super.updateDescriptionText();
        setDescriptionText(R.string.security_settings_fingerprint_enroll_consent_message);
    }
}
