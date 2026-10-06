package com.google.android.settings.biometrics;

import android.content.Context;
import android.safetycenter.SafetySourceIssue;

import com.android.settings.biometrics.BiometricsFeatureProvider;
import com.android.settings.biometrics.metrics.BiometricsLogger;

import com.google.android.settings.biometrics.metrics.BiometricsLoggerImpl;

public class BiometricsFeatureProviderGoogleImpl implements BiometricsFeatureProvider {
    private Context mContext;
    private BiometricsSafetySourceIssueController mBiometricsSafetySourceIssueController = null;
    private BiometricsLogger mBiometricsLogger = null;

    public BiometricsFeatureProviderGoogleImpl(Context context) {
        mContext = context;
    }

    @Override
    public SafetySourceIssue getSafetySourceIssue(String sourceId) {
        return getBiometricsSafetySourceIssueController().getSafetySourceIssue(sourceId);
    }

    @Override
    public void notifySafetyIssueActionLaunched() {
        getBiometricsSafetySourceIssueController().notifySafetyIssueActionLaunched();
    }

    private BiometricsSafetySourceIssueController getBiometricsSafetySourceIssueController() {
        if (mBiometricsSafetySourceIssueController == null) {
            mBiometricsSafetySourceIssueController =
                    new BiometricsSafetySourceIssueController(mContext);
        }
        return mBiometricsSafetySourceIssueController;
    }

    @Override
    public BiometricsLogger getBiometricsLogger() {
        if (mBiometricsLogger == null) {
            mBiometricsLogger = new BiometricsLoggerImpl();
        }
        return mBiometricsLogger;
    }
}
