package com.google.android.settings.gestures.columbus;

import android.app.settings.SettingsEnums;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import com.android.settings.SetupWizardUtils;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.settings.R;
import com.google.android.setupcompat.template.FooterBarMixin;
import com.google.android.setupcompat.template.FooterButton;
import com.google.android.setupdesign.GlifLayout;

public class ColumbusGestureTrainingEnrollingActivity extends ColumbusGestureTrainingBase {
    private static final int REQUEST_ACTION = 1;

    private final Handler mHandler = new Handler(Looper.myLooper());
    private LottieAnimationView mAnimation;
    private boolean mFirstGestureDetected;
    private ColumbusEnrollingIllustration mIllustration;
    private GlifLayout mLayout;

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SETTINGS_COLUMBUS_GESTURE_TRAINING_ENROLLING;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(SetupWizardUtils.getTheme(this, getIntent()));
        setContentView(R.layout.columbus_gesture_training_enrolling_activity);
        super.onCreate(savedInstanceState);
        mLayout = findViewById(R.id.layout);
        mAnimation = mLayout.findViewById(R.id.animation);
        mIllustration = mLayout.findViewById(R.id.columbus_gesture_illustration);
        mLayout.setDescriptionText(R.string.columbus_gesture_training_enrolling_text);
        mLayout.getMixin(FooterBarMixin.class)
                .setSecondaryButton(
                        new FooterButton.Builder(this)
                                .setText(R.string.columbus_gesture_enrollment_do_it_later)
                                .setListener(
                                        v -> {
                                            setResult(RESULT_SKIP);
                                            finishAndRemoveTask();
                                        })
                                .setButtonType(FooterButton.ButtonType.CANCEL)
                                .setTheme(
                                        com.google.android.setupdesign.R.style
                                                .SudGlifButton_Secondary)
                                .build());
    }

    @Override
    public void onTrigger() {
        if (mFirstGestureDetected) {
            mHandler.post(this::onSecondGesture);
        } else {
            mFirstGestureDetected = true;
            mHandler.post(this::onFirstGesture);
        }
    }

    private void onFirstGesture() {
        mLayout.setHeaderText(R.string.columbus_gesture_training_enrolling_first_gesture_title);
        mLayout.setDescriptionText(R.string.columbus_gesture_training_enrolling_first_gesture_text);
        mIllustration.setGestureCount(1, null);
        mLayout.requestAccessibilityFocus();
    }

    private void onSecondGesture() {
        mLayout.setHeaderText(R.string.columbus_gesture_training_enrolling_second_gesture_title);
        mLayout.setDescriptionText(
                R.string.columbus_gesture_training_enrolling_second_gesture_text);
        mLayout.getMixin(FooterBarMixin.class)
                .setPrimaryButton(
                        new FooterButton.Builder(this)
                                .setText(com.android.settings.R.string.wizard_next)
                                .setListener(
                                        v -> {
                                            startNextStep(
                                                    ColumbusGestureTrainingActionActivity.class,
                                                    REQUEST_ACTION);
                                            finishAndRemoveTask();
                                        })
                                .setButtonType(FooterButton.ButtonType.NEXT)
                                .setTheme(
                                        com.google.android.setupdesign.R.style
                                                .SudGlifButton_Primary)
                                .build());
        mIllustration.setGestureCount(
                2,
                () -> {
                    mAnimation.cancelAnimation();
                    mAnimation.setVisibility(View.GONE);
                });
        mLayout.requestAccessibilityFocus();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ACTION) {
            setResult(resultCode, data);
            finishAndRemoveTask();
        }
    }
}
