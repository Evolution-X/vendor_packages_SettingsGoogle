package com.google.android.settings.biometrics.face.anim;

import android.animation.TimeAnimator;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.media.AudioAttributes;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.util.SparseIntArray;
import android.widget.ImageView;

import com.google.android.settings.R;
import com.google.android.settings.biometrics.face.Debouncer;
import com.google.android.settings.biometrics.face.FaceEnrollSidecar;
import com.google.android.settings.biometrics.face.FaceUtils;
import com.google.android.settings.biometrics.face.Utils;

public abstract class FaceEnrollAnimationBase extends Drawable
        implements FaceEnrollSidecar.Listener {
    private static final AudioAttributes SONIFICATION_AUDIO_ATTRIBUTES =
            new AudioAttributes.Builder().setContentType(4).setUsage(13).build();
    private boolean mCenterAcquired;
    private final Paint mCircleCutoutPaint;
    private final Context mContext;
    private final Debouncer mDebouncer;
    private final ImageView mFaceIcon;
    private final FaceOutlineIndicatorController mFaceOutlineIndicatorController;
    private boolean mFinishing;
    private boolean mFromSetupWizard;
    private Bitmap mInverseCutoutBitmap;
    private final AnimationListener mListener;
    private final Paint mScrimPaint;
    private final Paint mSquarePaint;
    private TimeAnimator mTimeAnimator;
    private final Vibrator mVibrator;
    private int mFOVState = 2;
    private final VibrationEffect mVibrationEffect = VibrationEffect.get(2);

    public interface AnimationListener {
        void clearHelp();

        void onEnrollAnimationFinished();

        void onEnrollAnimationStarted();

        void showHelp(CharSequence charSequence);
    }

    protected void bucketAcquiredWhileScrimShowing(int i) {}

    protected boolean isBucket(int i) {
        return i >= 1101 && i <= 1125;
    }

    public void onSaveInstanceState(Bundle bundle) {}

    protected void update(long j, long j2) {}

    public FaceEnrollAnimationBase(
            Context context, AnimationListener animationListener, ImageView imageView, boolean z) {
        mContext = context;
        mListener = animationListener;
        mFromSetupWizard = z;
        mVibrator = (Vibrator) context.getSystemService(Vibrator.class);
        mSquarePaint = new Paint();
        mSquarePaint.setColor(-1);
        mSquarePaint.setAntiAlias(true);
        mCircleCutoutPaint = new Paint();
        mCircleCutoutPaint.setColor(0);
        mCircleCutoutPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        mCircleCutoutPaint.setAntiAlias(true);
        mScrimPaint = new Paint();
        mScrimPaint.setAntiAlias(true);
        mScrimPaint.setStyle(Paint.Style.FILL);
        mScrimPaint.setColor(0);
        mFaceIcon = (ImageView) ((Activity) context).findViewById(R.id.face_smiley);
        mFaceIcon.setImageDrawable(context.getDrawable(R.drawable.face_smiley));
        mFaceOutlineIndicatorController = new FaceOutlineIndicatorController(context, imageView);
        SparseIntArray sparseIntArray = new SparseIntArray();
        sparseIntArray.append(1, 6);
        sparseIntArray.append(2, 10);
        sparseIntArray.append(3, 10);
        mDebouncer = new Debouncer(sparseIntArray);
    }

    protected void vibrate() {
        mVibrator.vibrate(mVibrationEffect, SONIFICATION_AUDIO_ATTRIBUTES);
    }

    protected AnimationListener getListener() {
        return mListener;
    }

    protected boolean isCenterAcquired() {
        return mCenterAcquired;
    }

    protected void onUserEnterGood() {
        Log.i("FaceEnroll/AnimationBase", "onUserEnterGood");
        getListener().clearHelp();
        mFaceOutlineIndicatorController.clear();
    }

    protected void onUserLeaveGood(CharSequence charSequence) {
        Log.i("FaceEnroll/AnimationBase", "onUserLeaveGood");
        getListener().showHelp(charSequence);
        mFaceOutlineIndicatorController.show();
    }

    public void onFirstFrameReceived() {
        onUserLeaveGood(null);
        mFOVState = 2;
    }

    @Override
    protected void onBoundsChange(Rect rect) {
        if (mTimeAnimator == null) {
            TimeAnimator timeAnimator = new TimeAnimator();
            mTimeAnimator = timeAnimator;
            timeAnimator.setTimeListener(
                    (animation, frameTime, deltaTime) -> {
                        update(frameTime, deltaTime);
                        invalidateSelf();
                    });
            mTimeAnimator.start();
        }
    }

    @Override
    public void draw(Canvas canvas) {
        canvas.save();
        int iDpToPx = (int) Utils.dpToPx(mContext, 30);
        if (mInverseCutoutBitmap == null) {
            mInverseCutoutBitmap =
                    Cutout.createCutoutBitmap(
                            mContext, getBounds().width() + (iDpToPx * 2), getBounds().width() / 2);
        }
        float f = -iDpToPx;
        canvas.drawBitmap(mInverseCutoutBitmap, f, f, null);
        canvas.drawCircle(
                canvas.getWidth() / 2, canvas.getHeight() / 2, canvas.getWidth() / 2, mScrimPaint);
        canvas.restore();
    }

    protected boolean outOfFOVScrimShowing() {
        return mFOVState != 1;
    }

    @Override
    public void onEnrollmentHelp(int i, CharSequence charSequence) {
        if (mFinishing) {
            return;
        }
        if (isBucket(i) && mFOVState != 1) {
            mDebouncer.updateBuffer(1);
            if (!mDebouncer.passesDebounce(1)) {
                if (!FaceUtils.isOneOfCenterBuckets(i)) {
                    bucketAcquiredWhileScrimShowing(i);
                }
            } else {
                mFOVState = 1;
                onUserEnterGood();
            }
        } else if (i == 11) {
            handleOutOfFovState(2, charSequence);
        } else {
            switch (i) {
                case 4:
                case 5:
                case 6:
                case 7:
                case 8:
                case 9:
                    handleOutOfFovState(2, charSequence);
                    break;
                default:
                    switch (i) {
                        case 1126:
                        case 1127:
                        case 1128:
                        case 1129:
                        case 1130:
                        case 1131:
                        case 1132:
                        case 1133:
                            handleOutOfFovState(3, charSequence);
                            break;
                    }
                    break;
            }
        }
        if (mCenterAcquired || !FaceUtils.isOneOfCenterBuckets(i)) {
            return;
        }
        mCenterAcquired = true;
    }

    private void handleOutOfFovState(int i, CharSequence charSequence) {
        mDebouncer.updateBuffer(i);
        if (mFOVState == i || !mDebouncer.passesDebounce(i)) {
            return;
        }
        onUserLeaveGood(charSequence);
        mFOVState = i;
    }

    @Override
    public void onEnrollmentProgressChange(int i, int i2) {
        if (i2 == 0) {
            if (mFOVState != 1) {
                mFOVState = 1;
                onUserEnterGood();
            }
            mListener.onEnrollAnimationStarted();
            startFinishing();
            mListener.clearHelp();
        }
    }

    protected void startFinishing() {
        mFinishing = true;
    }

    protected boolean isFinishing() {
        return mFinishing;
    }
}
