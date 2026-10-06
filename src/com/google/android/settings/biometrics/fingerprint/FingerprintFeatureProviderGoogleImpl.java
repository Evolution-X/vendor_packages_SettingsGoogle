package com.google.android.settings.biometrics.fingerprint;

import android.content.Context;
import android.content.Intent;
import android.hardware.fingerprint.FingerprintManager;
import android.hardware.fingerprint.FingerprintSensorProperties;
import android.hardware.fingerprint.FingerprintSensorPropertiesInternal;
import android.os.Bundle;
import android.util.Log;

import com.android.settings.biometrics.fingerprint.FingerprintEnrollActivityClassProvider;
import com.android.settings.biometrics.fingerprint.FingerprintEnrollParentalConsent;
import com.android.settings.biometrics.fingerprint.FingerprintFeatureProvider;
import com.android.settings.biometrics.fingerprint.FingerprintSettingsFeatureProvider;
import com.android.settings.biometrics.fingerprint.UdfpsEnrollCalibrator;
import com.android.settings.biometrics.fingerprint.feature.ChallengeGeneratedInvoker;
import com.android.settings.biometrics.fingerprint.feature.FingerprintExtPreferencesProvider;
import com.android.settings.biometrics.fingerprint.feature.SfpsEnrollmentFeature;

import com.google.android.settings.R;
import com.google.android.settings.biometrics.fingerprint.factory.DynamicClassLoader;
import com.google.android.settings.biometrics.fingerprint.feature.FingerprintActivityProviderWithoutFastEnroll;
import com.google.android.settings.biometrics.fingerprint.feature.FingerprintEnrollActivityClassProviderGoogleImpl;
import com.google.android.settings.biometrics.fingerprint.feature.SfpsEnrollmentFeatureGoogleImpl;
import com.google.android.settings.biometrics.fingerprint.feature.UdfpsEnrollCalibratorImpl;

import java.util.ArrayList;
import java.util.List;

public class FingerprintFeatureProviderGoogleImpl implements FingerprintFeatureProvider {
    private static final String TAG = "FingerprintFeatureProviderGoogleImpl";

    private SfpsEnrollmentFeature mSfpsEnrollmentFeatureImpl = null;

    @Override
    public SfpsEnrollmentFeature getSfpsEnrollmentFeature() {
        if (mSfpsEnrollmentFeatureImpl == null) {
            mSfpsEnrollmentFeatureImpl = new SfpsEnrollmentFeatureGoogleImpl();
            Log.v(
                    TAG,
                    "getSfpsEnrollmentFeature: impl=" + mSfpsEnrollmentFeatureImpl + ", flag=true");
        }
        return mSfpsEnrollmentFeatureImpl;
    }

    @Override
    public UdfpsEnrollCalibrator getUdfpsEnrollCalibrator(
            Context context, Bundle savedInstanceState, Intent intent) {
        if (context.getResources().getBoolean(R.bool.config_fingerprint_enroll_calibration)) {
            return UdfpsEnrollCalibratorImpl.getInstance(
                    context.getMainThreadHandler(), savedInstanceState, intent);
        }
        return null;
    }

    @Override
    public FingerprintEnrollActivityClassProvider getEnrollActivityClassProvider(Context context) {
        FingerprintManager fingerprintManager =
                com.android.settings.Utils.getFingerprintManagerOrNull(context);
        if (fingerprintManager != null
                && (fingerprintManager.isPowerbuttonFps() || isUdfps(fingerprintManager))) {
            return FingerprintEnrollActivityClassProviderGoogleImpl.INSTANCE;
        }
        return FingerprintActivityProviderWithoutFastEnroll.INSTANCE;
    }

    @Override
    public FingerprintExtPreferencesProvider getExtPreferenceProvider(Context context) {
        FingerprintManager fingerprintManager =
                com.android.settings.Utils.getFingerprintManagerOrNull(context);
        if (fingerprintManager != null) {
            List<FingerprintSensorPropertiesInternal> props =
                    fingerprintManager.getSensorPropertiesInternal();
            if (props != null
                    && !props.isEmpty()
                    && props.get(0).sensorType
                            == FingerprintSensorProperties.TYPE_UDFPS_ULTRASONIC) {
                FingerprintExtPreferencesProvider provider =
                        DynamicClassLoader.INSTANCE.newFingerprintExtPreferencesProvider(
                                "com.google.android.settings.biometrics.usudfps.feature"
                                        + ".UsudfpsExtPreferencesProvider",
                                context);
                if (provider != null) {
                    return provider;
                }
            }
        }
        return FingerprintFeatureProvider.super.getExtPreferenceProvider(context);
    }

    @Override
    public FingerprintSettingsFeatureProvider getFingerprintSettingsFeatureProvider() {
        return FingerprintSettingsFeatureProviderGoogle.INSTANCE;
    }

    @Override
    public List<ChallengeGeneratedInvoker> getChallengeGeneratedInvokers() {
        List<ChallengeGeneratedInvoker> invokers = new ArrayList<>(1);
        ChallengeGeneratedInvoker invoker =
                DynamicClassLoader.INSTANCE.newChallengeGeneratedInvoker(
                        "com.google.android.settings.biometrics.usudfps.feature"
                                + ".ScreenProtectorInvoker");
        if (invoker != null) {
            invokers.add(invoker);
        }
        return invokers;
    }

    @Override
    public Class<? extends FingerprintEnrollParentalConsent> getParentalConsentPage() {
        return FingerprintEnrollParentalConsentGoogle.class;
    }

    @Override
    public int[] getParentalConsentStringRes() {
        return FingerprintEnrollParentalConsentGoogle.CONSENT_STRING_RESOURCES;
    }

    private boolean isUdfps(FingerprintManager fingerprintManager) {
        for (FingerprintSensorPropertiesInternal props :
                fingerprintManager.getSensorPropertiesInternal()) {
            if (props.isAnyUdfpsType()) {
                return true;
            }
        }
        return false;
    }
}
