package com.google.android.settings.biometrics.face.anim.curve;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.animation.AccelerateDecelerateInterpolator;

import com.google.android.settings.R;
import com.google.android.settings.biometrics.face.anim.FaceEnrollAnimationMultiAngleDrawable;

public class CellState {
    private final boolean mAlternateCursor;
    private final FaceEnrollAnimationMultiAngleDrawable.BucketListener mBucketListener;
    private CellConfig mCellConfig;
    private ValueAnimator mCursorAnimator;
    private ValueAnimator.AnimatorUpdateListener mCursorAnimatorListener;
    private final int mCursorColorAcquired;
    private final int mCursorColorGone;
    private Paint mCursorEdgePaint;
    private int mCursorState;
    private final boolean mDisableCursor;
    private boolean mDone;
    private final Handler mHandler =
            new Handler(Looper.getMainLooper()) {
                @Override
                public void handleMessage(Message message) {
                    if (message.what != 1) {
                        return;
                    }
                    CellState.this.handleFadeCursor();
                }
            };
    private final int mIndex;
    private ValueAnimator mNoActivityAnimator;
    private ValueAnimator.AnimatorUpdateListener mNoActivityAnimatorListener;
    private Paint mNoActivityPaint;
    private boolean mNoActivityPulseShouldRepeat;
    private int mScrimAnimationState;
    private ValueAnimator mScrimAnimator;
    private ValueAnimator.AnimatorUpdateListener mScrimAnimatorListener;
    private final int mScrimColorEnrolled;
    private final int mScrimColorNoActivityEnd;
    private final int mScrimColorNoActivityStart;
    private int mScrimColorNotEnrolled;
    private Paint mScrimPaint;

    public CellState(
            Context context,
            int i,
            FaceEnrollAnimationMultiAngleDrawable.BucketListener bucketListener,
            int i2) {
        mIndex = i;
        mBucketListener = bucketListener;
        mScrimColorNotEnrolled = i2;
        mScrimColorEnrolled = context.getColor(R.color.face_enroll_cell_enrolled);
        mCursorColorAcquired = context.getColor(R.color.face_enroll_cursor_acquired);
        mCursorColorGone = context.getColor(R.color.face_enroll_cursor_gone);
        mScrimColorNoActivityStart = context.getColor(R.color.face_enroll_cell_no_activity_start);
        mScrimColorNoActivityEnd = context.getColor(R.color.face_enroll_cell_no_activity_end);
        mScrimAnimationState = 0;
        mCursorState = 0;
        mScrimPaint = new Paint();
        mScrimPaint.setAntiAlias(true);
        mScrimPaint.setAlpha(0);
        boolean z =
                Settings.Secure.getInt(
                                context.getContentResolver(),
                                "com.google.android.settings.future.biometrics.face.anim.curve.alternate_cursor",
                                0)
                        != 0;
        mAlternateCursor = z;
        mDisableCursor =
                Settings.Secure.getInt(
                                context.getContentResolver(),
                                "com.google.android.settings.future.biometrics.face.anim.curve.disable_cursor",
                                0)
                        != 0;
        int shadowColor = z ? -65536 : context.getColor(R.color.face_enroll_cursor_shadow);
        mCursorEdgePaint = new Paint();
        mCursorEdgePaint.setColor(mCursorColorGone);
        mCursorEdgePaint.setAntiAlias(true);
        mCursorEdgePaint.setShadowLayer(6.0f, 0.0f, 0.0f, shadowColor);
        mCursorEdgePaint.setStrokeCap(Paint.Cap.ROUND);
        mCursorEdgePaint.setStyle(Paint.Style.STROKE);
        mCursorEdgePaint.setStrokeWidth(12.0f);
        mNoActivityPaint = new Paint();
        mNoActivityPaint.setAntiAlias(true);
        mNoActivityPaint.setColor(mScrimColorNoActivityStart);
        mScrimAnimatorListener =
                valueAnimator -> mScrimPaint.setColor((Integer) valueAnimator.getAnimatedValue());
        mCursorAnimatorListener =
                valueAnimator ->
                        mCursorEdgePaint.setColor((Integer) valueAnimator.getAnimatedValue());
        mNoActivityAnimatorListener =
                valueAnimator ->
                        mNoActivityPaint.setColor((Integer) valueAnimator.getAnimatedValue());
    }

    public void updateConfig(CellConfig cellConfig) {
        mCellConfig = cellConfig;
    }

    public void draw(Canvas canvas) {
        canvas.save();
        CellConfig cellConfig = mCellConfig;
        if (cellConfig == null) {
            return;
        }
        if (cellConfig.mFlipVertical) {
            canvas.scale(1.0f, -1.0f, 0.0f, 0.0f);
        }
        canvas.rotate(mCellConfig.mRotation);
        canvas.drawPath(mCellConfig.mPath, mScrimPaint);
        canvas.drawPath(mCellConfig.mPath, mNoActivityPaint);
        canvas.restore();
    }

    public void drawCursor(Canvas canvas) {
        canvas.save();
        CellConfig cellConfig = mCellConfig;
        if (cellConfig == null) {
            return;
        }
        if (cellConfig.mFlipVertical) {
            canvas.scale(1.0f, -1.0f, 0.0f, 0.0f);
        }
        canvas.rotate(mCellConfig.mRotation);
        if (!mDisableCursor) {
            canvas.drawPath(mCellConfig.mPath, mCursorEdgePaint);
        }
        canvas.restore();
    }

    public boolean isDone() {
        return mDone;
    }

    public void setEarlyDone() {
        mDone = true;
    }

    public void stopPulseForNoActivity() {
        mNoActivityPulseShouldRepeat = false;
    }

    public void pulseForNoActivity(int times) {
        mNoActivityPulseShouldRepeat = true;
        if (isAnimating(mNoActivityAnimator)) {
            return;
        }
        ValueAnimator valueAnimator =
                ValueAnimator.ofArgb(
                        mScrimColorNoActivityStart,
                        mScrimColorNoActivityEnd,
                        mScrimColorNoActivityEnd,
                        mScrimColorNoActivityStart);
        mNoActivityAnimator = valueAnimator;
        valueAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        mNoActivityAnimator.addUpdateListener(mNoActivityAnimatorListener);
        mNoActivityAnimator.setDuration(1233L);
        mNoActivityAnimator.addListener(
                new AnimatorListenerAdapter() {
                    private int curPulses = 1;
                    private final int numPulses;

                    {
                        numPulses = times;
                    }

                    @Override
                    public void onAnimationEnd(Animator animator) {
                        super.onAnimationEnd(animator);
                        if (CellState.this.mNoActivityPulseShouldRepeat && curPulses < numPulses) {
                            CellState.this.mNoActivityAnimator.start();
                            curPulses++;
                        } else {
                            CellState.this.mBucketListener.onNoActivityAnimationFinished();
                        }
                    }
                });
        mNoActivityAnimator.start();
    }

    public void fadeScrimOut(int i) {
        animateScrimColor(
                i == 2 ? 0 : (mDone ? mScrimColorEnrolled : mScrimColorNotEnrolled), 200L, 1);
    }

    public void fadeScrimIn() {
        fadeScrimIn(200L);
    }

    private void fadeScrimIn(long j) {
        animateScrimColor(mDone ? mScrimColorEnrolled : mScrimColorNotEnrolled, j, 2);
    }

    public void onAcquired() {
        if (mHandler.hasMessages(1)) {
            mHandler.removeMessages(1);
            mHandler.sendEmptyMessageDelayed(1, 300L);
        }
        if (mCursorState == 0 || !mDone) {
            mCursorState = 1;
            if (!mDone) {
                mBucketListener.onStartFinishing();
            }
            mDone = true;
            ValueAnimator valueAnimator =
                    ValueAnimator.ofArgb(mCursorEdgePaint.getColor(), mCursorColorAcquired);
            mCursorAnimator = valueAnimator;
            valueAnimator.setDuration(300L);
            mCursorAnimator.addUpdateListener(mCursorAnimatorListener);
            mCursorAnimator.addListener(
                    new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animator) {
                            CellState.this.mCursorState = 3;
                            int i = CellState.this.mScrimAnimationState;
                            CellState cellState = CellState.this;
                            if (i != 1) {
                                cellState.fadeScrimOut(1);
                            } else {
                                Log.w(
                                        "FaceEnroll/CellState",
                                        "Index "
                                                + cellState.mIndex
                                                + " intentionally not going to"
                                                + " SCRIM_FADE_REASON_DONE");
                            }
                            CellState.this.mHandler.sendEmptyMessageDelayed(1, 300L);
                        }
                    });
            mCursorAnimator.start();
        }
    }

    public void updateScrimNotEnrolledColor(int i, boolean z) {
        mScrimColorNotEnrolled = i;
        if (z) {
            int i2 = mScrimAnimationState;
            if (i2 == 0) {
                animateScrimNotEnrolledColor(200L);
            } else if (i2 == 2) {
                fadeScrimIn(getRemainingAnimationTime(mScrimAnimator));
            } else {
                if (i2 != 3) {
                    return;
                }
                animateScrimNotEnrolledColor(getRemainingAnimationTime(mScrimAnimator));
            }
        }
    }

    private void animateScrimNotEnrolledColor(long j) {
        if (mDone) {
            return;
        }
        int color = mScrimPaint.getColor();
        if (color == mScrimColorNotEnrolled) {
            return;
        }
        animateScrimColor(mScrimColorNotEnrolled, j, 3);
    }

    private void animateScrimColor(int i, long j, int i2) {
        if (j <= 0) {
            return;
        }
        if (isAnimating(mScrimAnimator)) {
            mScrimAnimator.cancel();
        }
        mScrimAnimationState = i2;
        ValueAnimator valueAnimator = ValueAnimator.ofArgb(mScrimPaint.getColor(), i);
        mScrimAnimator = valueAnimator;
        valueAnimator.addUpdateListener(mScrimAnimatorListener);
        mScrimAnimator.addListener(
                new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationCancel(Animator animator) {
                        CellState.this.mScrimAnimationState = 0;
                    }

                    @Override
                    public void onAnimationEnd(Animator animator) {
                        CellState.this.mScrimAnimationState = 0;
                    }
                });
        mScrimAnimator.setDuration(j);
        mScrimAnimator.start();
    }

    public void fadeCursorNow() {
        handleFadeCursor();
    }

    private void handleFadeCursor() {
        mCursorState = 2;
        if (isAnimating(mCursorAnimator)) {
            mCursorAnimator.cancel();
        }
        ValueAnimator valueAnimator =
                ValueAnimator.ofArgb(mCursorEdgePaint.getColor(), mCursorColorGone);
        mCursorAnimator = valueAnimator;
        valueAnimator.setDuration(200L);
        mCursorAnimator.addUpdateListener(mCursorAnimatorListener);
        mCursorAnimator.addListener(
                new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animator) {
                        CellState.this.mCursorState = 0;
                    }
                });
        mCursorAnimator.start();
    }

    private static boolean isAnimating(ValueAnimator valueAnimator) {
        return valueAnimator != null && valueAnimator.isRunning();
    }

    private static long getRemainingAnimationTime(ValueAnimator valueAnimator) {
        return Math.round(
                (1.0f - valueAnimator.getAnimatedFraction()) * valueAnimator.getDuration());
    }
}
