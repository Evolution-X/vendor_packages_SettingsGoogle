package com.google.android.settings.biometrics.fingerprint.feature;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Dialog;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.TouchDelegate;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.Interpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.view.animation.PathInterpolatorCompat;
import androidx.fragment.app.DialogFragment;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;

import com.android.settings.biometrics.fingerprint.FingerprintEnrollEnrolling;
import com.android.settings.biometrics.fingerprint.feature.SfpsEnrollmentFeature;
import com.android.settingslib.widget.LottieColorUtils;

import com.airbnb.lottie.LottieAnimationView;
import com.airbnb.lottie.LottieProperty;
import com.airbnb.lottie.model.KeyPath;
import com.google.android.settings.R;
import com.google.android.settings.biometrics.fingerprint.Utils;

import java.util.function.Function;
import java.util.function.Supplier;

public class SfpsEnrollmentFeatureGoogleImpl implements SfpsEnrollmentFeature {
    private static final String TAG = "SfpsEnrollmentFeatureGoogleImpl";
    private float[] mEnrollStageThresholds = null;
    private boolean mIsAcquiredGood = false;
    private boolean mIsAcquiredImmobile = false;

    @Override
    public CharSequence getFeaturedVendorString(
            Context context, int helpMsgId, CharSequence helpString) {
        if (context == null || helpMsgId != 1000 || TextUtils.isEmpty(helpString)) {
            return helpString;
        }
        return getVendorString(context, 0);
    }

    @Override
    public void handleOnEnrollmentLottieComposition(LottieAnimationView lottieAnimationView) {
        Context context = lottieAnimationView.getContext();
        lottieAnimationView.setSpeed(1.0f);
        LottieColorUtils.applyDynamicColors(context, lottieAnimationView);
        if (isDarkMode(context)) {
            return;
        }
        final int color = context.getColor(R.color.sfps_enroll_grey300_light);
        lottieAnimationView.addValueCallback(
                new KeyPath("**", ".grey300", "**"),
                LottieProperty.COLOR_FILTER,
                frameInfo -> new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_ATOP));
    }

    private boolean isDarkMode(Context context) {
        return (context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
    }

    @Override
    public int getCurrentSfpsEnrollStage(int steps, Function<Integer, Integer> stageStepsGetter) {
        if (steps < stageStepsGetter.apply(0)) {
            return 0;
        }
        if (steps < stageStepsGetter.apply(1)) {
            return 1;
        }
        if (steps < stageStepsGetter.apply(2)) {
            return 2;
        }
        if (steps < stageStepsGetter.apply(3)) {
            return 3;
        }
        return 4;
    }

    @Override
    public int getFeaturedStageHeaderResource(int stage) {
        switch (stage) {
            case 0:
                return R.string.security_settings_fingerprint_enroll_repeat_title_immobile_overlay;
            case 1:
                return R.string.security_settings_sfps_enroll_finger_center_title_immobile_overlay;
            case 2:
                return R.string.security_settings_sfps_enroll_fingertip_title_immobile_overlay;
            case 3:
                return R.string.security_settings_sfps_enroll_left_edge_title_immobile_overlay;
            case 4:
                return R.string.security_settings_sfps_enroll_right_edge_title_immobile_overlay;
            default:
                throw new IllegalArgumentException("Invalid stage: " + stage);
        }
    }

    @Override
    public int getSfpsEnrollLottiePerStage(int stage) {
        switch (stage) {
            case 0:
            case 1:
            case 2:
            case 3:
                return R.raw.sfps_lift_then_touch_lottie;
            case 4:
                return R.raw.sfps_reposition_finger_right_lottie;
            default:
                throw new IllegalArgumentException("Invalid stage: " + stage);
        }
    }

    @Override
    public float getEnrollStageThreshold(Context context, int stage) {
        if (mEnrollStageThresholds == null) {
            String[] thresholds =
                    context.getResources()
                            .getStringArray(R.array.config_sfps_enroll_stage_thresholds);
            mEnrollStageThresholds = new float[thresholds.length];
            for (int i = 0; i < thresholds.length; i++) {
                mEnrollStageThresholds[i] = Float.parseFloat(thresholds[i]);
            }
        }
        if (stage >= 0 && stage <= mEnrollStageThresholds.length) {
            if (stage == mEnrollStageThresholds.length) {
                return 1.0f;
            }
            return mEnrollStageThresholds[stage];
        }
        Log.w(TAG, "Unsupported enroll stage index: " + stage);
        return stage < 0 ? 0.0f : 1.0f;
    }

    @Override
    public boolean shouldUpdateTitleAndDescription() {
        return mIsAcquiredGood || !mIsAcquiredImmobile;
    }

    @Override
    public void handleOnAcquired(boolean isGood) {
        mIsAcquiredGood = isGood;
        mIsAcquiredImmobile = false;
    }

    @Override
    public void handleOnEnrollmentProgressChange(int steps, int remaining) {
        Log.d(
                TAG,
                "handleOnEnrollmentProgressChange: good="
                        + mIsAcquiredGood
                        + ", immobile="
                        + mIsAcquiredImmobile
                        + ", remaining="
                        + remaining);
    }

    @Override
    public void handleOnEnrollmentHelp(
            int helpMsgId,
            CharSequence helpString,
            Supplier<FingerprintEnrollEnrolling> activitySupplier) {
        mIsAcquiredImmobile = (helpMsgId == 9 || helpMsgId == 1000);
        Log.d(
                TAG,
                "handleOnEnrollmentHelp: good="
                        + mIsAcquiredGood
                        + ", immobile="
                        + mIsAcquiredImmobile
                        + ", helpMsgId="
                        + helpMsgId);
        if (activitySupplier == null || TextUtils.isEmpty(helpString)) {
            return;
        }
        showPauseEnrollmentDialogIfNecessary(helpMsgId, helpString.toString(), activitySupplier);
    }

    private void showPauseEnrollmentDialogIfNecessary(
            int helpMsgId,
            String helpString,
            Supplier<FingerprintEnrollEnrolling> activitySupplier) {
        if (helpMsgId != 1000) {
            return;
        }
        String vendorString = getVendorString(activitySupplier.get(), 0);
        if (!TextUtils.isEmpty(vendorString) && vendorString.equals(helpString)) {
            getImmobileDialog()
                    .show(activitySupplier.get().getSupportFragmentManager(), "immobile_dialog");
        } else {
            Log.d(
                    TAG,
                    "ImmobileHelpDialog not showing: has vendor string="
                            + (!TextUtils.isEmpty(vendorString))
                            + ", msg matches="
                            + (!TextUtils.isEmpty(vendorString)
                                    && vendorString.equals(helpString)));
        }
    }

    public ImmobileHelpDialog getImmobileDialog() {
        return new ImmobileHelpDialog();
    }

    public String getVendorString(Context context, int index) {
        if (index < 0) {
            return null;
        }
        String[] stringArray =
                context.getResources().getStringArray(R.array.fingerprint_acquired_vendor);
        if (stringArray == null || index >= stringArray.length) {
            return null;
        }
        return stringArray[index];
    }

    public static class ImmobileHelpDialog extends DialogFragment {
        @NonNull
        @Override
        public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
            return new AlertDialog.Builder(getContext())
                    .setView(R.layout.biometric_enrollment_immobile_dialog)
                    .create();
        }

        @Override
        public void onResume() {
            Dialog dialog = getDialog();
            if (dialog != null) {
                dialog.setCancelable(false);
                dialog.setCanceledOnTouchOutside(false);
                View continueButton = dialog.findViewById(R.id.immobile_continue_btn);
                continueButton.setOnClickListener(
                        v -> {
                            Utils.resumeEnroll();
                            getDialog().dismiss();
                        });
                setUpTouchDelegate(
                        dialog.findViewById(R.id.biometric_enrollment_immobile_help),
                        continueButton);
                adjustDimensionsIfNeeded(dialog);
            } else {
                Log.w(SfpsEnrollmentFeatureGoogleImpl.TAG, "No dialog created!");
                Utils.resumeEnroll();
            }
            super.onResume();
        }

        private void setUpTouchDelegate(final View container, final View target) {
            container
                    .getViewTreeObserver()
                    .addOnGlobalLayoutListener(
                            new ViewTreeObserver.OnGlobalLayoutListener() {
                                @Override
                                public void onGlobalLayout() {
                                    container.setTouchDelegate(
                                            new TouchDelegate(
                                                    new Rect(
                                                            container.getLeft(),
                                                            target.getTop(),
                                                            container.getRight(),
                                                            target.getBottom()),
                                                    target));
                                    container
                                            .getViewTreeObserver()
                                            .removeOnGlobalLayoutListener(this);
                                }
                            });
        }

        private void adjustDimensionsIfNeeded(Dialog dialog) {
            int displayWidth = getResources().getDisplayMetrics().widthPixels;
            int dialogWidth = getResources().getDimensionPixelSize(R.dimen.immobile_dialog_width);
            dialog.getWindow()
                    .setLayout(
                            dialogWidth > displayWidth
                                    ? ViewGroup.LayoutParams.WRAP_CONTENT
                                    : dialogWidth,
                            ViewGroup.LayoutParams.WRAP_CONTENT);
            Log.d(
                    SfpsEnrollmentFeatureGoogleImpl.TAG,
                    "Immobile dialog: dialogWidth="
                            + dialogWidth
                            + ", displayWidth="
                            + displayWidth);
        }
    }

    @Override
    public Animator getHelpAnimator(View view) {
        float distance =
                TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        10.0f,
                        view.getContext().getResources().getDisplayMetrics());
        float negativeDistance = -distance;
        float doubleDistance = 2.0f * distance;
        ObjectAnimator moveLeftAnimator =
                ObjectAnimator.ofFloat(view, "translationX", negativeDistance).setDuration(67L);
        ObjectAnimator moveRightAnimator =
                ObjectAnimator.ofFloat(view, "translationX", doubleDistance).setDuration(100L);
        ObjectAnimator moveLeftFarAnimator =
                ObjectAnimator.ofFloat(view, "translationX", -2.0f * distance).setDuration(100L);
        ObjectAnimator moveRightAgainAnimator =
                ObjectAnimator.ofFloat(view, "translationX", doubleDistance).setDuration(100L);
        ObjectAnimator moveLeftReturnAnimator =
                ObjectAnimator.ofFloat(view, "translationX", negativeDistance).setDuration(150L);

        Interpolator interpolator = PathInterpolatorCompat.create(0.6f, 0.0f, 0.4f, 1.0f);
        moveLeftAnimator.setInterpolator(interpolator);
        moveRightAnimator.setInterpolator(interpolator);
        moveLeftFarAnimator.setInterpolator(interpolator);
        moveRightAgainAnimator.setInterpolator(interpolator);
        moveLeftReturnAnimator.setInterpolator(new FastOutSlowInInterpolator());

        moveLeftAnimator.setAutoCancel(false);
        moveRightAnimator.setAutoCancel(false);
        moveLeftFarAnimator.setAutoCancel(false);
        moveRightAgainAnimator.setAutoCancel(false);
        moveLeftReturnAnimator.setAutoCancel(false);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playSequentially(
                moveLeftAnimator,
                moveRightAnimator,
                moveLeftFarAnimator,
                moveRightAgainAnimator,
                moveLeftReturnAnimator);
        return animatorSet;
    }

    @Override
    public boolean shouldAdjustHeaderText(Configuration configuration, boolean isFolded) {
        return !isFolded || configuration.orientation == Configuration.ORIENTATION_LANDSCAPE;
    }
}
