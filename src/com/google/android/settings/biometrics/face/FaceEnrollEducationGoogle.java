package com.google.android.settings.biometrics.face;

import android.app.settings.SettingsEnums;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.android.settings.biometrics.BiometricsOnboardingProto.OnboardingAction;
import com.android.settings.biometrics.face.FaceEnrollAccessibilityToggle;
import com.android.settings.biometrics.face.FaceEnrollEducation;
import com.android.settings.biometrics.metrics.BiometricsLogger;
import com.android.settings.biometrics.metrics.OnboardingEvent;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

import com.google.android.settings.R;

public class FaceEnrollEducationGoogle extends FaceEnrollEducation {
    private boolean mGazeEnabled;
    private MetricsFeatureProvider mMetricsFeatureProvider;
    private FaceEnrollAccessibilityToggle mSwitchGaze;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mGazeEnabled = getResources().getBoolean(com.android.settings.R.bool.config_gazeEnabled);
        if (mGazeEnabled) {
            mMetricsFeatureProvider =
                    FeatureFactory.getFeatureFactory().getMetricsFeatureProvider();
            mSwitchGaze = findViewById(R.id.toggle_gaze);
            mSwitchGaze.setOnClickListener(view -> onGazeToggleClick());
            mSwitchGaze.setChecked(!isAccessibilityEnabled());
            mSwitchGaze.setVisibility(isAccessibilityEnabled() ? View.VISIBLE : View.GONE);
            ((TextView) mSwitchGaze.findViewById(R.id.subtitle))
                    .setText(
                            com.android.settings.R.string
                                    .security_settings_face_settings_gaze_details);
        }
    }

    private void onGazeToggleClick() {
        updateOnboardingScreenInfoActions(
                mSwitchGaze.isChecked()
                        ? OnboardingAction.ACTION_FACE_GAZE_ON_VALUE
                        : OnboardingAction.ACTION_FACE_GAZE_OFF_VALUE);
        mSwitchGaze.getSwitch().toggle();
        mMetricsFeatureProvider.action(
                getApplicationContext(),
                SettingsEnums.ACTION_FACE_REQUIRE_ATTENTION_FROM_SUW,
                mSwitchGaze.isChecked());
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
    protected void onAccessibilityButtonClicked(View view) {
        super.onAccessibilityButtonClicked(view);
        updateOnboardingScreenInfoActions(OnboardingAction.ACTION_SETUP_FOR_FACE_A11Y_VALUE);
        if (mGazeEnabled) {
            mSwitchGaze.setChecked(false);
            mSwitchGaze.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onNextButtonClick(View view) {
        if (mGazeEnabled) {
            Intent intent = new Intent();
            mExtraInfoIntent = intent;
            intent.putExtra("gaze_enabled", mSwitchGaze.isChecked());
        }
        super.onNextButtonClick(view);
    }
}
