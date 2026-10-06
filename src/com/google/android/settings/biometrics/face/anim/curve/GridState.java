package com.google.android.settings.biometrics.face.anim.curve;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;

import com.google.android.settings.R;

public class GridState {
    private ValueAnimator mAnimator;
    private ValueAnimator.AnimatorUpdateListener mAnimatorUpdateListener;
    private final Paint mEdgePaint;
    private final Handler mHandler;
    private int mState;

    public GridState(Context context, Handler handler) {
        mHandler = handler;
        mEdgePaint = new Paint();
        mEdgePaint.setColor(context.getColor(R.color.face_enroll_grid));
        mEdgePaint.setAntiAlias(true);
        mEdgePaint.setStyle(Paint.Style.STROKE);
        mEdgePaint.setStrokeWidth(3.0f);
        mEdgePaint.setAlpha(0);
        mState = 0;
        mAnimatorUpdateListener =
                valueAnimator -> mEdgePaint.setAlpha((Integer) valueAnimator.getAnimatedValue());
    }

    public void fadeIn() {
        if (mState == 1) {
            return;
        }
        mState = 2;
        mAnimator = ValueAnimator.ofInt(mEdgePaint.getAlpha(), 64);
        mAnimator.removeAllUpdateListeners();
        mAnimator.addUpdateListener(mAnimatorUpdateListener);
        mAnimator.removeAllListeners();
        mAnimator.addListener(
                new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animator) {
                        super.onAnimationEnd(animator);
                        GridState.this.mState = 1;
                    }
                });
        mAnimator.start();
    }

    public void fadeOut(final Runnable runnable) {
        if (mState == 0) {
            mHandler.post(runnable);
            return;
        }
        mState = 2;
        mAnimator = ValueAnimator.ofInt(mEdgePaint.getAlpha(), 0);
        mAnimator.addUpdateListener(mAnimatorUpdateListener);
        mAnimator.removeAllListeners();
        mAnimator.addListener(
                new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animator) {
                        super.onAnimationEnd(animator);
                        GridState.this.mState = 0;
                        GridState.this.mHandler.post(runnable);
                    }
                });
        mAnimator.start();
    }

    public void draw(Canvas canvas) {
        int width = canvas.getWidth() / 2;
        int height = canvas.getHeight() / 2;
        float f = width;
        canvas.drawCircle(0.0f, 0.0f, f - (mEdgePaint.getStrokeWidth() / 2.0f), mEdgePaint);
        float width2 = canvas.getWidth() * 0.32f;
        float width3 = canvas.getWidth() * 0.78f;
        float f2 = (-width2) / 2.0f;
        float f3 = -height;
        float f4 = width2 / 2.0f;
        float f5 = height;
        canvas.drawArc(new RectF(f2, f3, f4, f5), 0.0f, 360.0f, false, mEdgePaint);
        float f6 = -width;
        canvas.drawArc(new RectF(f6, f2, f, f4), 0.0f, 360.0f, false, mEdgePaint);
        float f7 = (-width3) / 2.0f;
        float f8 = width3 / 2.0f;
        canvas.drawArc(new RectF(f7, f3, f8, f5), 0.0f, 360.0f, false, mEdgePaint);
        canvas.drawArc(new RectF(f6, f7, f, f8), 0.0f, 360.0f, false, mEdgePaint);
    }
}
