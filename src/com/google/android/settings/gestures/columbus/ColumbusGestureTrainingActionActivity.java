package com.google.android.settings.gestures.columbus;

import android.app.ActivityManager;
import android.app.settings.SettingsEnums;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.android.settings.SetupWizardUtils;

import com.google.android.settings.R;
import com.google.android.setupcompat.template.FooterBarMixin;
import com.google.android.setupcompat.template.FooterButton;
import com.google.android.setupdesign.GlifLayout;

public class ColumbusGestureTrainingActionActivity extends ColumbusGestureTrainingBase {
    private static final int REQUEST_LAUNCH = 1;
    private static final int REQUEST_FINISHED = 2;

    private RadioGroup mRadioGroup;

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SETTINGS_COLUMBUS_GESTURE_TRAINING_ACTION;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(SetupWizardUtils.getTheme(this, getIntent()));
        setContentView(R.layout.columbus_gesture_training_action_activity);
        super.onCreate(savedInstanceState);
        mRadioGroup = findViewById(R.id.actions);
        GlifLayout layout = findViewById(R.id.layout);
        layout.setDescriptionText(R.string.columbus_gesture_training_action_text);
        FooterBarMixin footerBarMixin = layout.getMixin(FooterBarMixin.class);
        footerBarMixin.setPrimaryButton(
                new FooterButton.Builder(this)
                        .setText(com.android.settings.R.string.wizard_next)
                        .setListener(v -> onNextButtonClicked())
                        .setButtonType(FooterButton.ButtonType.NEXT)
                        .setTheme(com.google.android.setupdesign.R.style.SudGlifButton_Primary)
                        .build());
        footerBarMixin.setSecondaryButton(
                new FooterButton.Builder(this)
                        .setText(R.string.columbus_gesture_enrollment_do_it_later)
                        .setListener(
                                v -> {
                                    setResult(RESULT_SKIP);
                                    finishAndRemoveTask();
                                })
                        .setButtonType(FooterButton.ButtonType.CANCEL)
                        .setTheme(com.google.android.setupdesign.R.style.SudGlifButton_Secondary)
                        .build());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_LAUNCH || requestCode == REQUEST_FINISHED) {
            setResult(resultCode, data);
            finishAndRemoveTask();
        }
    }

    private void onNextButtonClicked() {
        int checkedId = mRadioGroup.getCheckedRadioButtonId();
        // Stock compares against 0, which RadioGroup never returns, and then crashes on the
        // missing button when nothing is selected.
        if (checkedId == View.NO_ID) {
            Toast.makeText(
                            this,
                            R.string.columbus_gesture_training_action_no_selection_error,
                            Toast.LENGTH_SHORT)
                    .show();
            return;
        }
        if (checkedId == R.id.launch) {
            Settings.Secure.putStringForUser(
                    getContentResolver(),
                    "columbus_action",
                    getString(R.string.columbus_setting_action_launch_value),
                    ActivityManager.getCurrentUser());
            startNextStep(ColumbusGestureTrainingLaunchActivity.class, REQUEST_LAUNCH);
        } else {
            ColumbusRadioButton button = findViewById(checkedId);
            Settings.Secure.putStringForUser(
                    getContentResolver(),
                    "columbus_action",
                    button.getSecureValue(),
                    ActivityManager.getCurrentUser());
            startNextStep(ColumbusGestureTrainingFinishedActivity.class, REQUEST_FINISHED);
        }
    }
}
