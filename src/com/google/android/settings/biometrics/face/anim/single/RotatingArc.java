package com.google.android.settings.biometrics.face.anim.single;

import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.Paint;

public class RotatingArc {
    private float mAngle;
    private ValueAnimator mColorAnimator;
    private final int[] mColors;
    private final int mIndex;
    private final Paint mPaint;
    private float mRotateSpeed;
    private float mSweepAngle;

    public RotatingArc(int i, int i2, int[] iArr) {
        mIndex = i;
        mColors = iArr;
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeWidth(20.0f);
        mPaint.setColor(getColorForIndex(i));
        mAngle = (360 / i2) * i;
    }

    public void setSweepAngle(float f) {
        mSweepAngle = f;
    }

    public void setRotateSpeed(float f) {
        mRotateSpeed = f;
    }

    public int getColorForIndex(int i) {
        return mColors[i % mColors.length];
    }

    public void update(long j, long j2) {
        mAngle += (mRotateSpeed * j2) / 1000.0f;
    }

    public void draw(Canvas canvas) {
        float width = (canvas.getWidth() / 2) - (mPaint.getStrokeWidth() / 2.0f);
        canvas.drawArc(
                (canvas.getWidth() / 2) - width,
                (canvas.getHeight() / 2) - width,
                (canvas.getWidth() / 2) + width,
                (canvas.getWidth() / 2) + width,
                mAngle,
                mSweepAngle,
                false,
                mPaint);
    }

    public void stopCurrentAnimation() {
        if (mColorAnimator != null) {
            mColorAnimator.cancel();
        }
    }

    public void stopRotating(long j) {
        mColorAnimator = ValueAnimator.ofArgb(mPaint.getColor(), 0);
        mColorAnimator.setDuration(j);
        mColorAnimator.addUpdateListener(
                valueAnimator -> mPaint.setColor((Integer) valueAnimator.getAnimatedValue()));
        mColorAnimator.start();
    }

    public void startRotating(long j) {
        mColorAnimator = ValueAnimator.ofArgb(mPaint.getColor(), getColorForIndex(mIndex));
        mColorAnimator.setDuration(j);
        mColorAnimator.addUpdateListener(
                valueAnimator -> mPaint.setColor((Integer) valueAnimator.getAnimatedValue()));
        mColorAnimator.start();
    }

    public void startFinishing(long j) {
        startRotating(j);
    }
}
