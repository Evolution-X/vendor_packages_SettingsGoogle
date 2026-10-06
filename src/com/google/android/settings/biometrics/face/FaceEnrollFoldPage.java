/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.settings.biometrics.face;

import android.content.ComponentName;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.window.embedding.ActivityFilter;
import androidx.window.embedding.ActivityRule;
import androidx.window.embedding.RuleController;

import com.android.settings.SetupWizardUtils;
import com.android.settings.biometrics.BiometricUtils;
import com.android.settingslib.widget.LottieColorUtils;
import com.android.systemui.unfold.compat.ScreenSizeFoldProvider;
import com.android.systemui.unfold.updates.FoldProvider;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.settings.R;
import com.google.android.setupcompat.template.FooterBarMixin;
import com.google.android.setupcompat.template.FooterButton;
import com.google.android.setupdesign.GlifLayout;
import com.google.android.setupdesign.util.ThemeHelper;

import java.util.HashSet;
import java.util.Set;

/** Activity for posture guidance on foldables during face enrollment. */
public class FaceEnrollFoldPage extends FragmentActivity implements FoldProvider.FoldCallback {

    private static final String KEY_POSTURE_STATE = "posture_state";
    private static final long TIMEOUT_MS = 60000L;

    private int mDevicePostureState;
    private FooterBarMixin mFooterBarMixin;
    private GlifLayout mGlifLayout;
    private LottieAnimationView mIllustrationLottie;
    private boolean mKeepScreenOn;
    private int mOrientation;
    private ScreenSizeFoldProvider mScreenSizeFoldProvider;

    private final Runnable mTimeoutRunnable = () -> onSkipButtonClick(null);

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mDevicePostureState = savedInstanceState.getInt(KEY_POSTURE_STATE);
        }
        Set<ActivityFilter> filters = new HashSet<>();
        filters.add(new ActivityFilter(new ComponentName(this, FaceEnrollFoldPage.class), null));
        RuleController.getInstance(this)
                .addRule(new ActivityRule.Builder(filters).setAlwaysExpand(true).build());
        setTheme(SetupWizardUtils.getTheme(this, getIntent()));
        ThemeHelper.trySetDynamicColor(this);
        BiometricUtils.setDevicePosturesAllowEnroll(
                getResources()
                        .getInteger(
                                com.android.settings.R.integer
                                        .config_face_enroll_supported_posture));
        getOnBackPressedDispatcher()
                .addCallback(
                        this,
                        new OnBackPressedCallback(true) {
                            @Override
                            public void handleOnBackPressed() {
                                onBackInvoked();
                            }
                        });
        relayout();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_POSTURE_STATE, mDevicePostureState);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (mScreenSizeFoldProvider != null) {
            mScreenSizeFoldProvider.onConfigurationChange(newConfig);
        }
        if (newConfig.orientation != getCurrentOrientation()) {
            relayout();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (BiometricUtils.isPostureAllowEnrollment(mDevicePostureState)) {
            onFinishPostureGuidance();
        } else {
            setupPostureChangeListener();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mScreenSizeFoldProvider != null) {
            mScreenSizeFoldProvider.unregisterCallback(this);
            mScreenSizeFoldProvider = null;
        }
    }

    @Override
    public void onFoldUpdated(boolean isFolded) {
        getMainThreadHandler().removeCallbacks(mTimeoutRunnable);
        int postureState = isFolded ? 1 : 3;
        mDevicePostureState = postureState;
        if (BiometricUtils.isPostureAllowEnrollment(postureState)) {
            onFinishPostureGuidance();
        }
    }

    @Override
    protected void onApplyThemeResource(Resources.Theme theme, int resid, boolean first) {
        theme.applyStyle(com.android.settings.R.style.SetupWizardPartnerResource, true);
        super.onApplyThemeResource(theme, resid, first);
    }

    private void setupPostureChangeListener() {
        if (mScreenSizeFoldProvider == null) {
            ScreenSizeFoldProvider screenSizeFoldProvider =
                    new ScreenSizeFoldProvider(getApplicationContext());
            mScreenSizeFoldProvider = screenSizeFoldProvider;
            screenSizeFoldProvider.registerCallback(this, getMainExecutor());
        }
    }

    private void onFinishPostureGuidance() {
        if (isFinishing()) {
            return;
        }
        setResult(RESULT_FIRST_USER);
        onRemoveCallbacksAndFinish();
    }

    private void onRemoveCallbacksAndFinish() {
        getMainThreadHandler().removeCallbacks(mTimeoutRunnable);
        finish();
        overridePendingTransition(0, 0);
    }

    private void onBackInvoked() {
        setResult(RESULT_CANCELED);
        onRemoveCallbacksAndFinish();
    }

    private void onSkipButtonClick(View view) {
        setResult(RESULT_FIRST_USER + 1, getIntent());
        onRemoveCallbacksAndFinish();
    }

    @Override
    public void onResume() {
        super.onResume();
        setKeepScreenOn(true);
        getMainThreadHandler().postDelayed(mTimeoutRunnable, TIMEOUT_MS);
    }

    @Override
    public void onPause() {
        super.onPause();
        setKeepScreenOn(false);
    }

    private void setKeepScreenOn(boolean keepScreenOn) {
        if (keepScreenOn == mKeepScreenOn) {
            return;
        }
        if (keepScreenOn) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            mKeepScreenOn = true;
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            mKeepScreenOn = false;
        }
    }

    void relayout() {
        setCurrentOrientation(getResources().getConfiguration().orientation);
        setContentView(R.layout.face_enroll_fold_page);
        mGlifLayout = findViewById(R.id.setup_wizard_layout);
        mGlifLayout.setHeaderText(R.string.face_enrolling_close_to_continue);
        mGlifLayout.setDescriptionText(R.string.face_enrolling_close_to_continue_description);
        mIllustrationLottie = findViewById(R.id.illustration_lottie);
        LottieColorUtils.applyDynamicColors(getApplicationContext(), mIllustrationLottie);
        mIllustrationLottie.setAnimation(R.raw.face_posture_guidance_lottie);
        mIllustrationLottie.setVisibility(View.VISIBLE);
        mIllustrationLottie.playAnimation();
        mFooterBarMixin = mGlifLayout.getMixin(FooterBarMixin.class);
        mFooterBarMixin.setSecondaryButton(
                new FooterButton.Builder(this)
                        .setText(R.string.face_enrolling_do_it_later)
                        .setListener(this::onSkipButtonClick)
                        .setButtonType(FooterButton.ButtonType.SKIP)
                        .setTheme(com.google.android.setupdesign.R.style.SudGlifButton_Secondary)
                        .build());
    }

    void setCurrentOrientation(int orientation) {
        mOrientation = orientation;
    }

    int getCurrentOrientation() {
        return mOrientation;
    }
}
