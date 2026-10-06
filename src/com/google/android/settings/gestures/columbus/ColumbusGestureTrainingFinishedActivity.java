package com.google.android.settings.gestures.columbus;

import android.app.ActivityManager;
import android.app.settings.SettingsEnums;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;

import com.android.settings.SetupWizardUtils;

import com.google.android.settings.R;
import com.google.android.setupcompat.template.FooterBarMixin;
import com.google.android.setupcompat.template.FooterButton;
import com.google.android.setupdesign.GlifLayout;

public class ColumbusGestureTrainingFinishedActivity extends ColumbusGestureTrainingBase {
    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SETTINGS_COLUMBUS_GESTURE_TRAINING_FINISHED;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(SetupWizardUtils.getTheme(this, getIntent()));
        setContentView(R.layout.columbus_gesture_training_finished_activity);
        GlifLayout layout = findViewById(R.id.layout);
        super.onCreate(savedInstanceState);
        layout.setHeaderText(R.string.columbus_gesture_training_finished_title);
        layout.setDescriptionText(R.string.columbus_gesture_training_finished_text);
        boolean setupFlow = flowTypeDeferredSetup() || flowTypeSetup();

        FooterBarMixin footerBarMixin = layout.getMixin(FooterBarMixin.class);
        footerBarMixin.setSecondaryButton(
                new FooterButton.Builder(this)
                        .setText(R.string.columbus_gesture_enrollment_settings)
                        .setListener(v -> launchColumbusGestureSettings(getMetricsCategory()))
                        .setButtonType(FooterButton.ButtonType.OTHER)
                        .setTheme(com.google.android.setupdesign.R.style.SudGlifButton_Secondary)
                        .build());
        if (setupFlow) {
            footerBarMixin.getSecondaryButton().setVisibility(View.INVISIBLE);
        }

        footerBarMixin.setPrimaryButton(
                new FooterButton.Builder(this)
                        .setText(com.android.settings.R.string.done)
                        .setListener(v -> handleDone())
                        .setButtonType(FooterButton.ButtonType.NEXT)
                        .setTheme(com.google.android.setupdesign.R.style.SudGlifButton_Primary)
                        .build());
        FooterButton primaryButton = footerBarMixin.getPrimaryButton();
        if (setupFlow) {
            primaryButton.setText(this, com.android.settings.R.string.next_label);
        } else if (flowTypeAccidentalTrigger()) {
            primaryButton.setText(this, R.string.columbus_gesture_enrollment_complete);
        }

        Settings.Secure.putIntForUser(
                getContentResolver(), "columbus_suw_complete", 1, ActivityManager.getCurrentUser());
        setEnableColumbusOnPause();
    }
}
