package com.google.android.settings.biometrics.face;

import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.settings.biometrics.BiometricsOnboardingProto.FromSource;
import com.android.settings.biometrics.BiometricsOnboardingProto.Modality;
import com.android.settings.biometrics.ParentalControlsUtils;
import com.android.settings.biometrics.face.FaceEnrollIntroduction;
import com.android.settings.biometrics.metrics.BiometricsLogger;
import com.android.settings.biometrics.metrics.OnboardingEvent;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.widget.theme.R.color;

import com.google.android.settings.R;
import com.google.android.setupcompat.util.WizardManagerHelper;
import com.google.android.setupdesign.util.ThemeHelper;

public class FaceEnrollIntroductionGoogle extends FaceEnrollIntroduction {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (mBiometricsLogger != null && mOnboardingEvent == null) {
            boolean isAnySetupWizard = WizardManagerHelper.isAnySetupWizard(getIntent());
            boolean fromSafetySource =
                    getIntent().getBooleanExtra("launch_from_safety_source_issue", false);
            int fromSource;
            if (isAnySetupWizard) {
                fromSource = FromSource.FROM_SUW_VALUE;
            } else {
                fromSource =
                        fromSafetySource
                                ? FromSource.FROM_SAFETY_ISSUE_VALUE
                                : FromSource.FROM_SETTINGS_VALUE;
            }
            int capybaraStatus =
                    FeatureFactory.getFeatureFactory()
                                            .getFingerprintFeatureProvider()
                                            .getExtPreferenceProvider(getApplicationContext())
                                            .getSize()
                                    > 0
                            ? 1
                            : 0;
            OnboardingEvent onboardingEvent = new OnboardingEvent();
            mOnboardingEvent = onboardingEvent;
            onboardingEvent.setModality(Modality.MODALITY_FACE_VALUE);
            mOnboardingEvent.setFromSource(fromSource);
            mOnboardingEvent.setUserId(mUserId);
            mOnboardingEvent.setCapybaraStatus(capybaraStatus);
            if (BiometricsLogger.LOGGABLE) {
                Log.d(
                        BiometricsLogger.TAG,
                        getClass().getSimpleName() + ": create event=" + mOnboardingEvent);
            }
        }
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
        ((TextView) findViewById(com.android.settings.R.id.message_in_control))
                .setText(R.string.security_settings_face_enroll_introduction_control_message_2);
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        OnboardingEvent onboardingEventFromIntent = getOnboardingEventFromIntent(data);
        if (BiometricsLogger.LOGGABLE) {
            Log.d(
                    BiometricsLogger.TAG,
                    getClass().getSimpleName()
                            + ": current event="
                            + mOnboardingEvent
                            + ", eventFromData="
                            + onboardingEventFromIntent);
        }
        if (onboardingEventFromIntent != null) {
            mOnboardingEvent = onboardingEventFromIntent;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.face_enroll_introduction_2;
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

    @Override
    protected void updateDescriptionText() {
        if (isPrivateProfile()) {
            setDescriptionText(
                    getString(
                            com.android.settings.R.string
                                    .private_space_face_enroll_introduction_message));
        } else if (isFaceStrong()) {
            setDescriptionText(
                    getString(
                            R.string.security_settings_face_enroll_introduction_message_class3_2));
        }
        boolean parentalConsentRequired =
                ParentalControlsUtils.parentConsentRequired(this, getModality()) != null;
        if (!isDisabledByAdmin() || parentalConsentRequired) {
            return;
        }
        setDescriptionText(getDescriptionDisabledByAdmin());
    }

    @Override
    protected Intent getEnrollingIntent() {
        Intent intent = new Intent(this, FaceEnrollEducationGoogle.class);
        WizardManagerHelper.copyWizardManagerExtras(getIntent(), intent);
        intent.putExtra("enroll_reason", getIntent().getIntExtra("enroll_reason", -1));
        return intent;
    }
}
