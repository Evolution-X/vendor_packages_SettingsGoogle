package com.google.android.settings.gestures.columbus;

import android.app.ActivityManager;
import android.app.settings.SettingsEnums;
import android.content.Intent;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.android.settings.SetupWizardUtils;

import com.google.android.settings.R;
import com.google.android.setupcompat.template.FooterBarMixin;
import com.google.android.setupcompat.template.FooterButton;
import com.google.android.setupdesign.GlifLayout;

import java.util.Comparator;
import java.util.List;

public class ColumbusGestureTrainingLaunchActivity extends ColumbusGestureTrainingBase {
    private static final int REQUEST_FINISHED = 1;

    private RadioGroup mRadioGroup;

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SETTINGS_COLUMBUS_GESTURE_TRAINING_APP;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(SetupWizardUtils.getTheme(this, getIntent()));
        setContentView(R.layout.columbus_gesture_training_launch_activity);
        super.onCreate(savedInstanceState);
        mRadioGroup = findViewById(R.id.apps);
        List<LauncherActivityInfo> apps =
                getSystemService(LauncherApps.class)
                        .getActivityList(null, UserHandle.of(ActivityManager.getCurrentUser()));
        apps.sort(Comparator.comparing(app -> app.getLabel().toString()));
        LayoutInflater inflater = LayoutInflater.from(mRadioGroup.getContext());
        int iconSize = getResources().getDimensionPixelSize(R.dimen.columbus_app_icon_size);
        for (LauncherActivityInfo app : apps) {
            ColumbusRadioButton button =
                    (ColumbusRadioButton)
                            inflater.inflate(R.layout.columbus_app_list_item, mRadioGroup, false);
            button.setText(app.getLabel());
            Drawable icon = app.getIcon(0);
            icon.setBounds(0, 0, iconSize, iconSize);
            button.setCompoundDrawablesRelative(icon, null, null, null);
            button.setSecureValue(app.getComponentName().flattenToString());
            mRadioGroup.addView(button);
        }
        GlifLayout layout = findViewById(R.id.layout);
        layout.setDescriptionText(R.string.columbus_gesture_training_launch_text);
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
        if (requestCode == REQUEST_FINISHED) {
            setResult(resultCode, data);
            finishAndRemoveTask();
        }
    }

    private void onNextButtonClicked() {
        ColumbusRadioButton button =
                mRadioGroup.findViewById(mRadioGroup.getCheckedRadioButtonId());
        String app = button == null ? null : button.getSecureValue();
        if (app == null) {
            Toast.makeText(
                            this,
                            R.string.columbus_gesture_training_launch_no_selection_error,
                            Toast.LENGTH_SHORT)
                    .show();
            return;
        }
        Settings.Secure.putStringForUser(
                getContentResolver(), "columbus_launch_app", app, ActivityManager.getCurrentUser());
        Settings.Secure.putStringForUser(
                getContentResolver(),
                "columbus_launch_app_shortcut",
                app,
                ActivityManager.getCurrentUser());
        startNextStep(ColumbusGestureTrainingFinishedActivity.class, REQUEST_FINISHED);
    }
}
