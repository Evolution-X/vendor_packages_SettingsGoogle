package com.google.android.settings.biometrics.usudfps.ui.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.os.Process;
import android.os.VibrationAttributes;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.view.accessibility.AccessibilityManager;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.OvershootInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.settings.R;

public class UdfpsEnrollProgressBarDrawable extends Drawable {
    private static final Interpolator DEACCEL = new DecelerateInterpolator();
    private static final VibrationEffect VIBRATE_EFFECT_ERROR =
            VibrationEffect.createWaveform(new long[] {0, 5, 55, 60}, -1);
    private static final VibrationAttributes FINGERPRINT_ENROLLING_SONIFICATION_ATTRIBUTES =
            VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ACCESSIBILITY);
    private static final VibrationAttributes HARDWARE_FEEDBACK_VIBRATION_ATTRIBUTES =
            new VibrationAttributes.Builder()
                    .setUsage(VibrationAttributes.USAGE_HARDWARE_FEEDBACK)
                    .build();
    private static final VibrationEffect SUCCESS_VIBRATION_EFFECT =
            VibrationEffect.get(VibrationEffect.EFFECT_CLICK);

    private boolean mAfterFirstTouch;
    private ValueAnimator mBackgroundColorAnimator;
    private final ValueAnimator.AnimatorUpdateListener mBackgroundColorUpdateListener;
    private final Paint mBackgroundPaint;
    private ValueAnimator mCheckmarkAnimator;
    private final Drawable mCheckmarkDrawable;
    private final Interpolator mCheckmarkInterpolator;
    private final ValueAnimator.AnimatorUpdateListener mCheckmarkUpdateListener;
    private final Context mContext;
    private int mEnrollProgress;
    private int mEnrollProgressHelp;
    private int mEnrollProgressHelpWithTalkback;
    private ValueAnimator mFillColorAnimator;
    private final ValueAnimator.AnimatorUpdateListener mFillColorUpdateListener;
    final Paint mFillPaint;
    private final int mHelpColor;
    private final boolean mIsAccessibilityEnabled;
    private int mMovingTargetFill;
    private int mMovingTargetFillError;
    private OnDrawFinishedListener mOnDrawFinishedListener;
    private final int mOnFirstBucketFailedColor;
    private ValueAnimator mProgressAnimator;
    private final int mProgressColor;
    private final ValueAnimator.AnimatorUpdateListener mProgressUpdateListener;
    private final float mStrokeWidthPx;
    private final Vibrator mVibrator;

    private int mRemainingSteps = 0;
    private int mTotalSteps = 0;
    private float mProgress = 0.0f;
    private boolean mComplete = false;
    private float mCheckmarkScale = 0.0f;

    interface OnDrawFinishedListener {
        void onDrawFinished();
    }

    public UdfpsEnrollProgressBarDrawable(Context context, AttributeSet attrs) {
        mContext = context;
        loadResources(context, attrs);
        mStrokeWidthPx = (context.getResources().getDisplayMetrics().densityDpi / 160.0f) * 12.0f;
        mProgressColor = mEnrollProgress;

        mIsAccessibilityEnabled =
                context.getSystemService(AccessibilityManager.class).isTouchExplorationEnabled();
        mOnFirstBucketFailedColor = mMovingTargetFillError;
        mHelpColor =
                !mIsAccessibilityEnabled ? mEnrollProgressHelp : mEnrollProgressHelpWithTalkback;

        mCheckmarkDrawable = context.getDrawable(R.drawable.udfps_enroll_checkmark);
        mCheckmarkDrawable.mutate();
        mCheckmarkInterpolator = new OvershootInterpolator();

        mBackgroundPaint = new Paint();
        mBackgroundPaint.setStrokeWidth(mStrokeWidthPx);
        mBackgroundPaint.setColor(mMovingTargetFill);
        mBackgroundPaint.setAntiAlias(true);
        mBackgroundPaint.setStyle(Paint.Style.STROKE);
        mBackgroundPaint.setStrokeCap(Paint.Cap.ROUND);

        mFillPaint = new Paint();
        mFillPaint.setStrokeWidth(mStrokeWidthPx);
        mFillPaint.setColor(mProgressColor);
        mFillPaint.setAntiAlias(true);
        mFillPaint.setStyle(Paint.Style.STROKE);
        mFillPaint.setStrokeCap(Paint.Cap.ROUND);

        mVibrator = context.getSystemService(Vibrator.class);

        mProgressUpdateListener =
                animation -> {
                    mProgress = (Float) animation.getAnimatedValue();
                    invalidateSelf();
                };
        mFillColorUpdateListener =
                animation -> {
                    mFillPaint.setColor((Integer) animation.getAnimatedValue());
                    invalidateSelf();
                };
        mCheckmarkUpdateListener =
                animation -> {
                    mCheckmarkScale = (Float) animation.getAnimatedValue();
                    invalidateSelf();
                };
        mBackgroundColorUpdateListener =
                animation -> {
                    mBackgroundPaint.setColor((Integer) animation.getAnimatedValue());
                    invalidateSelf();
                };
    }

    void onEnrollmentProgress(int remaining, int total) {
        mAfterFirstTouch = true;
        performHaptic(remaining, total, false);
        updateState(remaining, total, false);
    }

    void onEnrollmentHelp(int remaining, int total) {
        performHaptic(remaining, total, true);
        updateState(remaining, total, true);
    }

    void onLastStepAcquired() {
        updateState(0, mTotalSteps, false);
    }

    private void updateState(int remaining, int total, boolean isHelp) {
        updateProgress(remaining, total);
        updateFillColor(isHelp);
    }

    private void performHaptic(int remaining, int total, boolean isHelp) {
        if (mRemainingSteps == remaining && mTotalSteps == total) {
            return;
        }
        if (mVibrator == null) {
            return;
        }
        if (isHelp) {
            if (mIsAccessibilityEnabled) {
                mVibrator.vibrate(
                        Process.myUid(),
                        mContext.getOpPackageName(),
                        VIBRATE_EFFECT_ERROR,
                        getClass().getSimpleName() + "::onEnrollmentHelp",
                        FINGERPRINT_ENROLLING_SONIFICATION_ATTRIBUTES);
            }
            return;
        }
        if (remaining == -1 && mIsAccessibilityEnabled) {
            mVibrator.vibrate(
                    Process.myUid(),
                    mContext.getOpPackageName(),
                    VIBRATE_EFFECT_ERROR,
                    getClass().getSimpleName() + "::onFirstTouchError",
                    FINGERPRINT_ENROLLING_SONIFICATION_ATTRIBUTES);
        } else if (remaining != -1 && !mIsAccessibilityEnabled) {
            mVibrator.vibrate(
                    Process.myUid(),
                    mContext.getOpPackageName(),
                    SUCCESS_VIBRATION_EFFECT,
                    getClass().getSimpleName() + "::OnEnrollmentProgress",
                    HARDWARE_FEEDBACK_VIBRATION_ATTRIBUTES);
        }
    }

    private void updateProgress(int remaining, int total) {
        if (mRemainingSteps == remaining && mTotalSteps == total) {
            return;
        }
        mRemainingSteps = remaining;
        mTotalSteps = total;
        int completed = Math.max(0, total - remaining);
        if (mAfterFirstTouch) {
            completed++;
        }
        int totalWithTouch = total;
        if (mAfterFirstTouch) {
            totalWithTouch++;
        }
        float targetProgress = Math.min(1.0f, (float) completed / totalWithTouch);
        if (mProgressAnimator != null && mProgressAnimator.isRunning()) {
            mProgressAnimator.cancel();
        }
        mProgressAnimator = ValueAnimator.ofFloat(mProgress, targetProgress);
        mProgressAnimator.setDuration(400L);
        mProgressAnimator.addUpdateListener(mProgressUpdateListener);
        mProgressAnimator.start();
        if (remaining == 0) {
            startCompletionAnimation();
        } else if (remaining > 0) {
            rollBackCompletionAnimation();
        }
    }

    private void animateBackgroundColor() {
        if (mBackgroundColorAnimator != null && mBackgroundColorAnimator.isRunning()) {
            mBackgroundColorAnimator.end();
        }
        mBackgroundColorAnimator =
                ValueAnimator.ofArgb(mBackgroundPaint.getColor(), mOnFirstBucketFailedColor);
        mBackgroundColorAnimator.setDuration(350L);
        mBackgroundColorAnimator.setRepeatCount(1);
        mBackgroundColorAnimator.setRepeatMode(ValueAnimator.REVERSE);
        mBackgroundColorAnimator.setInterpolator(DEACCEL);
        mBackgroundColorAnimator.addUpdateListener(mBackgroundColorUpdateListener);
        mBackgroundColorAnimator.start();
    }

    private void updateFillColor(boolean isHelp) {
        if (!mAfterFirstTouch && isHelp) {
            animateBackgroundColor();
            return;
        }
        if (mFillColorAnimator != null && mFillColorAnimator.isRunning()) {
            mFillColorAnimator.end();
        }
        mFillColorAnimator =
                ValueAnimator.ofArgb(mFillPaint.getColor(), isHelp ? mHelpColor : mProgressColor);
        mFillColorAnimator.setDuration(350L);
        mFillColorAnimator.setRepeatCount(1);
        mFillColorAnimator.setRepeatMode(ValueAnimator.REVERSE);
        mFillColorAnimator.setInterpolator(DEACCEL);
        mFillColorAnimator.addUpdateListener(mFillColorUpdateListener);
        mFillColorAnimator.start();
    }

    private void startCompletionAnimation() {
        if (mComplete) {
            return;
        }
        mComplete = true;
        if (mCheckmarkAnimator != null && mCheckmarkAnimator.isRunning()) {
            mCheckmarkAnimator.cancel();
        }
        mCheckmarkAnimator = ValueAnimator.ofFloat(mCheckmarkScale, 1.0f);
        mCheckmarkAnimator.setStartDelay(200L);
        mCheckmarkAnimator.setDuration(300L);
        mCheckmarkAnimator.setInterpolator(mCheckmarkInterpolator);
        mCheckmarkAnimator.addUpdateListener(mCheckmarkUpdateListener);
        mCheckmarkAnimator.start();
    }

    private void rollBackCompletionAnimation() {
        if (!mComplete) {
            return;
        }
        mComplete = false;
        long duration =
                Math.round(
                        (mCheckmarkAnimator != null
                                        ? mCheckmarkAnimator.getAnimatedFraction()
                                        : 0.0f)
                                * 200.0f);
        if (mCheckmarkAnimator != null && mCheckmarkAnimator.isRunning()) {
            mCheckmarkAnimator.cancel();
        }
        mCheckmarkAnimator = ValueAnimator.ofFloat(mCheckmarkScale, 0.0f);
        mCheckmarkAnimator.setDuration(duration);
        mCheckmarkAnimator.addUpdateListener(mCheckmarkUpdateListener);
        mCheckmarkAnimator.start();
    }

    private void loadResources(Context context, AttributeSet attrs) {
        TypedArray a =
                context.obtainStyledAttributes(
                        attrs,
                        R.styleable.BiometricsEnrollView,
                        R.attr.biometricsEnrollStyle,
                        R.style.BiometricsEnrollStyle);
        mMovingTargetFill =
                a.getColor(R.styleable.BiometricsEnrollView_biometricsMovingTargetFill, 0);
        mMovingTargetFillError =
                a.getColor(R.styleable.BiometricsEnrollView_biometricsMovingTargetFillError, 0);
        mEnrollProgress = a.getColor(R.styleable.BiometricsEnrollView_biometricsEnrollProgress, 0);
        mEnrollProgressHelp =
                a.getColor(R.styleable.BiometricsEnrollView_biometricsEnrollProgressHelp, 0);
        mEnrollProgressHelpWithTalkback =
                a.getColor(
                        R.styleable.BiometricsEnrollView_biometricsEnrollProgressHelpWithTalkback,
                        0);
        a.recycle();
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        canvas.save();
        canvas.rotate(-90.0f, getBounds().centerX(), getBounds().centerY());
        float halfStroke = mStrokeWidthPx / 2.0f;
        if (mProgress < 1.0f) {
            canvas.drawArc(
                    halfStroke,
                    halfStroke,
                    getBounds().right - halfStroke,
                    getBounds().bottom - halfStroke,
                    0.0f,
                    360.0f,
                    false,
                    mBackgroundPaint);
        }
        if (mProgress > 0.0f) {
            canvas.drawArc(
                    halfStroke,
                    halfStroke,
                    getBounds().right - halfStroke,
                    getBounds().bottom - halfStroke,
                    0.0f,
                    mProgress * 360.0f,
                    false,
                    mFillPaint);
        }
        canvas.restore();
        if (mCheckmarkScale > 0.0f) {
            float sqrt = (float) Math.sqrt(2.0d) / 2.0f;
            float halfW = ((getBounds().width() - mStrokeWidthPx) / 2.0f) * sqrt;
            float halfH = ((getBounds().height() - mStrokeWidthPx) / 2.0f) * sqrt;
            float cx = getBounds().centerX() + halfW;
            float cy = getBounds().centerY() + halfH;
            float iw = (mCheckmarkDrawable.getIntrinsicWidth() / 2.0f) * mCheckmarkScale;
            float ih = (mCheckmarkDrawable.getIntrinsicHeight() / 2.0f) * mCheckmarkScale;
            mCheckmarkDrawable.setBounds(
                    Math.round(cx - iw),
                    Math.round(cy - ih),
                    Math.round(cx + iw),
                    Math.round(cy + ih));
            mCheckmarkDrawable.draw(canvas);
        }
        if (mOnDrawFinishedListener != null) {
            mOnDrawFinishedListener.onDrawFinished();
        }
    }

    void addOnDrawFinishedListener(OnDrawFinishedListener listener) {
        mOnDrawFinishedListener = listener;
    }

    void deleteOnDrawFinishedListener() {
        mOnDrawFinishedListener = null;
    }

    @Override
    public void setAlpha(int alpha) {}

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {}

    @Override
    public int getOpacity() {
        return PixelFormat.UNKNOWN;
    }
}
