package com.google.android.settings.biometrics.face.anim;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.widget.ImageView;

import com.google.android.settings.R;
import com.google.android.settings.biometrics.face.anim.curve.DirectionIndicationHelper;
import com.google.android.settings.biometrics.face.anim.curve.DirectionIndicatorController;
import com.google.android.settings.biometrics.face.anim.curve.GridController;

public class FaceEnrollAnimationMultiAngleDrawable extends FaceEnrollAnimationBase {
    private final BucketListener mBucketListener;
    private boolean[] mBucketsCompleted;
    private final Context mContext;
    private final DirectionIndicationHelper mDirectionIndicationHelper;
    private final DirectionIndicatorController mDirectionIndicatorController;
    private Paint mFinishingArcPaint;
    private final GridController mGridController;
    private final Handler mHandler;
    private long mLastVibrationMs;

    public interface BucketListener {
        void onNoActivityAnimationFinished();

        void onStartFinishing();
    }

    private boolean isLargeAngle(int i) {
        return i >= 1126 && i <= 1133;
    }

    @Override
    public int getOpacity() {
        return -3;
    }

    @Override
    public void setAlpha(int i) {}

    @Override
    public void setColorFilter(ColorFilter colorFilter) {}

    private void handleUserNoActivityAnimation() {
        mGridController.pulseForNoActivity(
                mDirectionIndicationHelper.getNoProgressBucket(mBucketsCompleted),
                Integer.MAX_VALUE);
        mDirectionIndicatorController.pulseForNoActivity(
                mDirectionIndicationHelper.getNoProgressPulseAngle(mBucketsCompleted),
                Integer.MAX_VALUE);
        getListener().showHelp(mContext.getString(R.string.face_enrolling_turn_head_to_arrow));
    }

    public FaceEnrollAnimationMultiAngleDrawable(
            Context context,
            FaceEnrollAnimationBase.AnimationListener animationListener,
            ImageView imageView,
            ImageView imageView2,
            boolean z,
            Bundle bundle) {
        super(context, animationListener, imageView2, z);
        mBucketsCompleted = new boolean[25];
        mHandler =
                new Handler(Looper.getMainLooper()) {
                    @Override
                    public void handleMessage(Message message) {
                        if (message.what == 1) {
                            FaceEnrollAnimationMultiAngleDrawable.this
                                    .handleUserNoActivityAnimation();
                            return;
                        }
                        Log.w("FaceEnroll/AnimationDrawable", "Unknown message: " + message.what);
                    }
                };
        BucketListener bucketListener =
                new BucketListener() {
                    @Override
                    public void onStartFinishing() {
                        boolean z2;
                        synchronized (this) {
                            if (SystemClock.uptimeMillis()
                                            - FaceEnrollAnimationMultiAngleDrawable.this
                                                    .mLastVibrationMs
                                    > 50) {
                                FaceEnrollAnimationMultiAngleDrawable.this.mLastVibrationMs =
                                        SystemClock.uptimeMillis();
                                z2 = true;
                            } else {
                                z2 = false;
                            }
                        }
                        if (z2) {
                            FaceEnrollAnimationMultiAngleDrawable.this.vibrate();
                        }
                    }

                    @Override
                    public void onNoActivityAnimationFinished() {
                        FaceEnrollAnimationMultiAngleDrawable.this.mHandler.removeMessages(1);
                        FaceEnrollAnimationMultiAngleDrawable.this.getListener().clearHelp();
                    }
                };
        mBucketListener = bucketListener;
        mContext = context;
        mFinishingArcPaint = new Paint();
        mFinishingArcPaint.setAntiAlias(true);
        mFinishingArcPaint.setStyle(Paint.Style.STROKE);
        mFinishingArcPaint.setStrokeCap(Paint.Cap.ROUND);
        mFinishingArcPaint.setStrokeWidth(20.0f);
        mFinishingArcPaint.setColor(
                context.getResources().getColor(R.color.face_enroll_single_capture_rotating_1));
        mDirectionIndicationHelper = new DirectionIndicationHelper();
        mGridController = new GridController(context, bucketListener);
        mDirectionIndicatorController = new DirectionIndicatorController(context, imageView);
        if (bundle == null) {
            return;
        }
        mBucketsCompleted = bundle.getBooleanArray("key_bucket_status");
        for (int i = 0; i < mBucketsCompleted.length; i++) {
            mGridController.restoreState(i, mBucketsCompleted[i]);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putBooleanArray("key_bucket_status", mBucketsCompleted);
    }

    private boolean isNewBucketAcquired(int i) {
        return isBucket(i) && !mBucketsCompleted[i - 1101];
    }

    private void stopCurrentDirectionIndication() {
        mDirectionIndicatorController.stopCurrentIndication();
        mGridController.stopPulseForNoActivity();
    }

    @Override
    protected void onUserLeaveGood(CharSequence charSequence) {
        super.onUserLeaveGood(charSequence);
        mGridController.onUserLeaveGood();
        stopCurrentDirectionIndication();
        mHandler.removeMessages(1);
    }

    @Override
    protected void onUserEnterGood() {
        super.onUserEnterGood();
        mGridController.onUserEnterGood();
    }

    @Override
    protected void bucketAcquiredWhileScrimShowing(int i) {
        int i2 = i - 1101;
        mBucketsCompleted[i2] = true;
        mGridController.setEarlyDone(i2);
    }

    @Override
    public void onEnrollmentHelp(int i, CharSequence charSequence) {
        super.onEnrollmentHelp(i, charSequence);
        if (isFinishing() || outOfFOVScrimShowing()) {
            return;
        }
        if (isCenterAcquired() && isBucket(i) && !isNewBucketAcquired(i)) {
            addDelayedUserNoActivityAnimation();
        } else if (isNewBucketAcquired(i) || isLargeAngle(i) || i != 0) {
            mHandler.removeMessages(1);
            stopCurrentDirectionIndication();
        }
        if (isBucket(i)) {
            int i2 = i - 1101;
            mBucketsCompleted[i2] = true;
            if (isCenterAcquired()) {
                mGridController.onAcquired(i2);
            }
        }
    }

    @Override
    public void onEnrollmentError(int i, CharSequence charSequence) {
        mHandler.removeMessages(1);
    }

    @Override
    public void onEnrollmentProgressChange(int i, int i2) {
        super.onEnrollmentProgressChange(i, i2);
        if (i2 == 0) {
            mHandler.removeMessages(1);
        }
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        canvas.save();
        mGridController.draw(canvas);
        mDirectionIndicatorController.draw(canvas);
        canvas.restore();
    }

    @Override
    public void onBoundsChange(Rect rect) {
        super.onBoundsChange(rect);
        mGridController.onBoundsChange(rect);
        mDirectionIndicatorController.onBoundsChange(rect);
    }

    private void addDelayedUserNoActivityAnimation() {
        if (mHandler.hasMessages(1)) {
            return;
        }
        mHandler.sendEmptyMessageDelayed(1, 4000L);
    }
}
