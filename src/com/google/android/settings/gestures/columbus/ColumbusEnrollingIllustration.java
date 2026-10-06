package com.google.android.settings.gestures.columbus;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.animation.DecelerateInterpolator;

import com.google.android.settings.R;
import com.google.android.setupdesign.view.Illustration;

/** Draws a half ring per detected Quick Tap during training. */
public class ColumbusEnrollingIllustration extends Illustration {
    private static final long ANIMATION_DURATION_MS = 500;

    private final int mInset;
    private final Paint mPaint;
    private Animator mAnimator;
    private float mGestureValue = 0f;

    public ColumbusEnrollingIllustration(Context context, AttributeSet attrs) {
        super(context, attrs);
        int strokeWidth =
                getResources()
                        .getDimensionPixelSize(R.dimen.columbus_enroll_illustration_stroke_width);
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(strokeWidth);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setColor(getContext().getColor(R.color.columbus_highlight));
        mInset = strokeWidth / 2;
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawArc(
                mInset,
                mInset,
                getWidth() - mInset,
                getHeight() - mInset,
                270f,
                mGestureValue * 180f,
                false,
                mPaint);
    }

    void setGestureCount(int count, Runnable onEnd) {
        if (mAnimator != null) {
            mAnimator.cancel();
        }
        ValueAnimator animator = ValueAnimator.ofFloat(mGestureValue, count);
        animator.setDuration(ANIMATION_DURATION_MS);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(
                animation -> {
                    mGestureValue = (float) animation.getAnimatedValue();
                    invalidate();
                });
        animator.addListener(
                new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mGestureValue = count;
                        if (count == 2) {
                            setBackgroundResource(R.drawable.ic_icon_check);
                        }
                        mAnimator = null;
                        if (onEnd != null) {
                            onEnd.run();
                        }
                    }
                });
        mAnimator = animator;
        animator.start();
    }
}
