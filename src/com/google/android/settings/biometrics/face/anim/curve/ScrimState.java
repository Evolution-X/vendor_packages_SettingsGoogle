package com.google.android.settings.biometrics.face.anim.curve;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.Paint;

public class ScrimState {
    private final int mGoneColor;
    private final Paint mPaint;
    private ValueAnimator mScrimAnimator;
    private ValueAnimator.AnimatorUpdateListener mScrimAnimatorListener;
    private final int mShowingColor;
    private int mState = 0;

    public ScrimState(int i, int i2) {
        mGoneColor = i;
        mShowingColor = i2;
        mPaint = new Paint();
        mPaint.setColor(i);
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.FILL);
        mScrimAnimatorListener =
                valueAnimator -> mPaint.setColor((Integer) valueAnimator.getAnimatedValue());
    }

    public boolean isShowing() {
        return mState != 0;
    }

    public void fadeOut() {
        int i = mState;
        if (i == 0 || i == 2) {
            return;
        }
        mState = 2;
        mScrimAnimator = ValueAnimator.ofArgb(mPaint.getColor(), mGoneColor);
        mScrimAnimator.addUpdateListener(mScrimAnimatorListener);
        mScrimAnimator.setDuration(200L);
        mScrimAnimator.addListener(
                new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animator) {
                        super.onAnimationEnd(animator);
                        ScrimState.this.mState = 0;
                    }
                });
        mScrimAnimator.start();
    }

    public void draw(Canvas canvas) {
        canvas.drawCircle(0.0f, 0.0f, canvas.getWidth() / 2, mPaint);
    }
}
