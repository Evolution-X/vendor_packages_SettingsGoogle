package com.google.android.settings.biometrics.face.anim;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.os.Handler;
import android.widget.ImageView;

import com.google.android.settings.biometrics.face.anim.single.ArcCollection;

public class FaceEnrollAnimationSingleCaptureDrawable extends FaceEnrollAnimationBase {
    private final Handler mHandler;
    private final ArcCollection mRotatingArcs;

    @Override
    public int getOpacity() {
        return -3;
    }

    @Override
    public void onEnrollmentError(int i, CharSequence charSequence) {}

    @Override
    public void setAlpha(int i) {}

    @Override
    public void setColorFilter(ColorFilter colorFilter) {}

    public FaceEnrollAnimationSingleCaptureDrawable(
            Context context,
            FaceEnrollAnimationBase.AnimationListener animationListener,
            ImageView imageView,
            boolean z) {
        super(context, animationListener, imageView, z);
        mHandler = new Handler();
        mRotatingArcs = new ArcCollection(context, mHandler);
    }

    @Override
    protected void startFinishing() {
        super.startFinishing();
        mRotatingArcs.startFinishing(() -> getListener().onEnrollAnimationFinished());
    }

    @Override
    protected void update(long j, long j2) {
        mRotatingArcs.update(j, j2);
    }

    @Override
    protected void onUserLeaveGood(CharSequence charSequence) {
        super.onUserLeaveGood(charSequence);
        mRotatingArcs.stopRotating();
    }

    @Override
    protected void onUserEnterGood() {
        super.onUserEnterGood();
        mRotatingArcs.startRotating();
    }

    @Override
    public void onEnrollmentProgressChange(int i, int i2) {
        super.onEnrollmentProgressChange(i, i2);
        if (i2 == 0) {
            vibrate();
        }
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        mRotatingArcs.draw(canvas);
    }
}
