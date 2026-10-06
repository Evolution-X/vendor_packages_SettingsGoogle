package com.google.android.settings.biometrics.face.anim.single;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.os.Handler;

import com.google.android.settings.R;

import java.util.ArrayList;
import java.util.List;

public class ArcCollection {
    private final List<RotatingArc> mArcs;
    private final Handler mHandler;
    private float mSpeed;
    private ValueAnimator mSpeedAnimator;
    private int mState = 0;
    private float mSweepAngle;
    private ValueAnimator mSweepAnimator;

    public ArcCollection(Context context, Handler handler) {
        mHandler = handler;
        int[] iArr = {
            context.getResources().getColor(R.color.face_enroll_single_capture_rotating_4),
            context.getResources().getColor(R.color.face_enroll_single_capture_rotating_3),
            context.getResources().getColor(R.color.face_enroll_single_capture_rotating_2),
            context.getResources().getColor(R.color.face_enroll_single_capture_rotating_1)
        };
        ArrayList<RotatingArc> arrayList = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            arrayList.add(new RotatingArc(i, 4, iArr));
        }
        mArcs = arrayList;
    }

    public void update(long j, long j2) {
        for (int i = 0; i < mArcs.size(); i++) {
            mArcs.get(i).update(j, j2);
        }
    }

    public void draw(Canvas canvas) {
        for (int i = 0; i < mArcs.size(); i++) {
            mArcs.get(i).draw(canvas);
        }
    }

    public void setSweepAngle(float f) {
        mSweepAngle = f;
        for (int i = 0; i < mArcs.size(); i++) {
            mArcs.get(i).setSweepAngle(f);
        }
    }

    public void setSpeed(float f) {
        mSpeed = f;
        for (int i = 0; i < mArcs.size(); i++) {
            mArcs.get(i).setRotateSpeed(f);
        }
    }

    public void stopCurrentAnimation() {
        if (mSweepAnimator != null && mSweepAnimator.isRunning()) {
            mSweepAnimator.cancel();
        }
        if (mSpeedAnimator != null && mSpeedAnimator.isRunning()) {
            mSpeedAnimator.cancel();
        }
        for (int i = 0; i < mArcs.size(); i++) {
            mArcs.get(i).stopCurrentAnimation();
        }
    }

    public void stopRotating() {
        int i = mState;
        if (i == 1 || i == 3) {
            return;
        }
        stopCurrentAnimation();
        mState = 3;
        mSweepAnimator = ValueAnimator.ofFloat(mSweepAngle, 0.0f);
        mSweepAnimator.setDuration(1100L);
        mSweepAnimator.addUpdateListener(
                valueAnimator -> setSweepAngle((Float) valueAnimator.getAnimatedValue()));
        mSweepAnimator.addListener(
                new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animator) {
                        super.onAnimationEnd(animator);
                        ArcCollection.this.mState = 1;
                    }
                });
        mSpeedAnimator = ValueAnimator.ofFloat(mSpeed, 0.0f);
        mSpeedAnimator.setDuration(1100L);
        mSpeedAnimator.addUpdateListener(
                valueAnimator -> setSpeed((Float) valueAnimator.getAnimatedValue()));
        mSweepAnimator.start();
        for (int i2 = 0; i2 < mArcs.size(); i2++) {
            mArcs.get(i2).stopRotating(1100L);
        }
    }

    public void startRotating() {
        if (mState == 2) {
            return;
        }
        stopCurrentAnimation();
        mState = 2;
        mSweepAnimator = ValueAnimator.ofFloat(mSweepAngle, 90.0f);
        mSweepAnimator.setDuration(800L);
        mSweepAnimator.addUpdateListener(
                valueAnimator -> setSweepAngle((Float) valueAnimator.getAnimatedValue()));
        mSpeedAnimator = ValueAnimator.ofFloat(mSpeed, 200.0f);
        mSpeedAnimator.setDuration(800L);
        mSpeedAnimator.addUpdateListener(
                valueAnimator -> setSpeed((Float) valueAnimator.getAnimatedValue()));
        mSweepAnimator.start();
        mSpeedAnimator.start();
        for (int i = 0; i < mArcs.size(); i++) {
            mArcs.get(i).startRotating(800L);
        }
    }

    public void startFinishing(final Runnable runnable) {
        int i = mState;
        if (i == 4 || i == 5) {
            return;
        }
        stopCurrentAnimation();
        mState = 4;
        mSweepAnimator = ValueAnimator.ofFloat(mSweepAngle, 360.0f);
        mSweepAnimator.setDuration(800L);
        mSweepAnimator.addUpdateListener(
                valueAnimator -> setSweepAngle((Float) valueAnimator.getAnimatedValue()));
        mSweepAnimator.addListener(
                new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animator) {
                        super.onAnimationEnd(animator);
                        ArcCollection.this.mHandler.post(runnable);
                    }
                });
        mSpeedAnimator = ValueAnimator.ofFloat(mSpeed, 200.0f);
        mSpeedAnimator.setDuration(800L);
        mSpeedAnimator.addUpdateListener(
                valueAnimator -> setSpeed((Float) valueAnimator.getAnimatedValue()));
        mSweepAnimator.start();
        mSpeedAnimator.start();
        for (int i2 = 0; i2 < mArcs.size(); i2++) {
            mArcs.get(i2).startFinishing(800L);
        }
    }
}
