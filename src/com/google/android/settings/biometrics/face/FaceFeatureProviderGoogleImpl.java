package com.google.android.settings.biometrics.face;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.android.settings.biometrics.face.FaceEnrollActivityClassProvider;
import com.android.settings.biometrics.face.FaceFeatureProvider;
import com.android.settings.biometrics.face.FaceSettingsFeatureProvider;

import com.google.android.settings.R;

public class FaceFeatureProviderGoogleImpl implements FaceFeatureProvider {
    @Override
    public Intent getPostureGuidanceIntent(Context context) {
        String postureGuidanceActivity =
                context.getString(R.string.config_face_enroll_guidance_page);
        ComponentName componentName =
                TextUtils.isEmpty(postureGuidanceActivity)
                        ? null
                        : ComponentName.unflattenFromString(postureGuidanceActivity);
        if (componentName == null) {
            return null;
        }
        Intent intent = new Intent();
        intent.setComponent(componentName);
        return intent;
    }

    @Override
    public boolean isAttentionSupported(Context context) {
        return context.getResources().getBoolean(R.bool.config_face_settings_attention_supported);
    }

    @Override
    public boolean isSetupWizardSupported(Context context) {
        return context.getResources()
                .getBoolean(com.android.settings.R.bool.config_suw_support_face_enroll);
    }

    @NonNull
    @Override
    public FaceEnrollActivityClassProvider getEnrollActivityClassProvider() {
        return FaceEnrollActivityClassProviderGoogle.INSTANCE;
    }

    @NonNull
    @Override
    public FaceSettingsFeatureProvider getFaceSettingsFeatureProvider() {
        return FaceSettingsFeatureProviderGoogle.INSTANCE;
    }

    @Override
    public int getMaxEnrollableCount(Context context) {
        return context.getResources().getInteger(R.integer.settings_max_face_enrollable);
    }

    @Override
    public Class getParentalConsentPage() {
        return FaceEnrollParentalConsentGoogle.class;
    }

    @Override
    public int[] getParentalConsentStringRes() {
        return FaceEnrollParentalConsentGoogle.CONSENT_STRING_RESOURCES;
    }
}
