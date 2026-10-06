package com.google.android.settings.gestures.columbus;

import android.app.ActivityManager;
import android.app.settings.SettingsEnums;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;

import com.android.settings.SetupWizardUtils;

import com.google.android.settings.R;
import com.google.android.setupcompat.template.FooterBarMixin;
import com.google.android.setupcompat.template.FooterButton;
import com.google.android.setupcompat.util.WizardManagerHelper;
import com.google.android.setupdesign.GlifLayout;

public class ColumbusGestureTrainingIntroActivity extends ColumbusGestureTrainingBase {
    private static final int REQUEST_ENROLLING = 1;

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SETTINGS_COLUMBUS_GESTURE_TRAINING_INTRO;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(SetupWizardUtils.getTheme(this, getIntent()));
        super.onCreate(savedInstanceState);
        if (!ColumbusPreferenceController.isColumbusSupported(this)) {
            finish();
            return;
        }
        if (Settings.Secure.getIntForUser(
                        getContentResolver(),
                        "columbus_suw_complete",
                        0,
                        ActivityManager.getCurrentUser())
                != 0) {
            launchColumbusGestureSettings(SettingsEnums.PAGE_UNKNOWN);
            finish();
            return;
        }
        setContentView(R.layout.columbus_gesture_training_intro_activity);
        GlifLayout layout = findViewById(R.id.layout);
        layout.setDescriptionText(R.string.columbus_gesture_training_intro_text_suw);
        FooterBarMixin footerBarMixin = layout.getMixin(FooterBarMixin.class);
        footerBarMixin.setPrimaryButton(
                new FooterButton.Builder(this)
                        .setText(R.string.columbus_gesture_enrollment_try_it)
                        .setListener(v -> startEnrollingActivity())
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
        if (requestCode == REQUEST_ENROLLING) {
            setResult(resultCode, data);
            finishAndRemoveTask();
        }
    }

    @Override
    public void onTrigger() {}

    private void startEnrollingActivity() {
        Intent intent = new Intent(this, ColumbusGestureTrainingEnrollingActivity.class);
        intent.putExtra(EXTRA_LAUNCHED_FROM, getFlowType());
        SetupWizardUtils.copySetupExtras(getIntent(), intent);
        startActivityForResult(intent, REQUEST_ENROLLING);
    }

    // Stock also maps a ColumbusGestureSuggestion alias to "settings_suggestion", but no such
    // component is declared, so every non-setup launch lands here as an accidental trigger.
    private String getFlowType() {
        Intent intent = getIntent();
        if (WizardManagerHelper.isSetupWizardIntent(intent)) {
            return FLOW_SETUP;
        }
        if (WizardManagerHelper.isDeferredSetupWizard(intent)) {
            return FLOW_DEFERRED_SETUP;
        }
        if (ColumbusGestureTrainingIntroActivity.class
                .getName()
                .contentEquals(intent.getComponent().getClassName())) {
            return FLOW_ACCIDENTAL_TRIGGER;
        }
        return null;
    }
}
