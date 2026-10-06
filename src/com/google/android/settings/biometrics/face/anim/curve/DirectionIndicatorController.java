package com.google.android.settings.biometrics.face.anim.curve;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Animatable2;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioAttributes;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.widget.ImageView;

import com.google.android.settings.R;

public class DirectionIndicatorController {
    private static final AudioAttributes SONIFICATION_AUDIO_ATTRIBUTES =
            new AudioAttributes.Builder().setContentType(4).setUsage(13).build();
    private Rect mBounds;
    private final Context mContext;
    private final ImageView mImageView;
    private final Paint mLargeAnglePaint;
    private boolean mShouldRepeat;
    private ValueAnimator mStrokeAnimator;
    private final VibrationEffect mVibrationEffect = VibrationEffect.get(1);
    private final Vibrator mVibrator;

    public void draw(Canvas canvas) {}

    public DirectionIndicatorController(Context context, ImageView imageView) {
        mContext = context;
        mImageView = imageView;
        mVibrator = (Vibrator) context.getSystemService(Vibrator.class);
        mLargeAnglePaint = new Paint();
        mLargeAnglePaint.setAntiAlias(true);
        mLargeAnglePaint.setColor(context.getColor(R.color.blue_500));
        mLargeAnglePaint.setStrokeWidth(0.0f);
        mLargeAnglePaint.setStrokeCap(Paint.Cap.ROUND);
        mLargeAnglePaint.setStyle(Paint.Style.STROKE);
        mStrokeAnimator = ValueAnimator.ofFloat(0.0f, 20.0f, 0.0f);
        mStrokeAnimator.setDuration(1233L);
        mStrokeAnimator.addUpdateListener(
                valueAnimator ->
                        mLargeAnglePaint.setStrokeWidth((Float) valueAnimator.getAnimatedValue()));
    }

    public void stopCurrentIndication() {
        mShouldRepeat = false;
    }

    public void pulseForNoActivity(int i, int i2) {
        pulseAnimation(i, i2, false);
    }

    private void pulseAnimation(int i, int times, boolean z) {
        if (mBounds == null) {
            return;
        }
        mShouldRepeat = true;
        AnimatedVectorDrawable animatedVectorDrawable =
                (AnimatedVectorDrawable) mImageView.getDrawable();
        if (animatedVectorDrawable == null || !animatedVectorDrawable.isRunning()) {
            AnimatedVectorDrawable animation =
                    (AnimatedVectorDrawable)
                            mContext.getDrawable(R.drawable.face_indicator_triangle);
            mImageView.setImageDrawable(animation);
            double radians = Math.toRadians(i);
            int iCenterX =
                    (int)
                            (((double)
                                            (mBounds.centerX()
                                                    + ((mImageView.getMeasuredWidth() * 0.15f)
                                                            / 2.0f)))
                                    * Math.sin(radians));
            int iCenterY =
                    (int)
                            (((double)
                                            (mBounds.centerY()
                                                    + ((mImageView.getMeasuredWidth() * 0.15f)
                                                            / 2.0f)))
                                    * Math.cos(radians));
            mImageView.setScaleX(0.15f);
            mImageView.setScaleY(0.15f);
            ImageView imageView = mImageView;
            if (z) {
                imageView.setRotation(i - 180);
            } else {
                imageView.setRotation(i);
            }
            mImageView.setTranslationX(iCenterX);
            mImageView.setTranslationY(-iCenterY);
            animation.registerAnimationCallback(
                    new Animatable2.AnimationCallback() {
                        private int curPulses = 1;
                        private final int numPulses;

                        {
                            numPulses = times;
                        }

                        @Override
                        public void onAnimationEnd(Drawable drawable) {
                            super.onAnimationEnd(drawable);
                            if (!DirectionIndicatorController.this.mShouldRepeat
                                    || curPulses >= numPulses) {
                                return;
                            }
                            animation.start();
                            curPulses++;
                        }
                    });
            animation.start();
        }
    }

    public void onBoundsChange(Rect rect) {
        mBounds = rect;
    }
}
