package com.google.android.settings.biometrics.udfps.ui.widget;

import android.content.Context;
import android.graphics.PointF;
import android.hardware.fingerprint.FingerprintManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.accessibility.AccessibilityManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class UdfpsEnrollHelper {
    private static final String TAG = "UdfpsEnrollHelper";
    private final boolean mAccessibilityEnabled;
    private final FingerprintManager mFingerprintManager;
    private final List<PointF> mGuidedEnrollmentPoints;
    Listener mListener;
    private int mTotalSteps = -1;
    private int mRemainingSteps = -1;
    private int mLocationsEnrolled = 0;
    private int mCenterTouchCount = 0;
    private int mPace = 1;

    interface Listener {
        void onAcquired(boolean success);

        void onEnrollmentHelp(int remaining, int total);

        void onEnrollmentProgress(int remaining, int total);

        void onPointerDown(int pointerId);

        void onPointerUp(int pointerId);
    }

    public UdfpsEnrollHelper(Context context) {
        mFingerprintManager =
                Objects.requireNonNull(
                        (FingerprintManager) context.getSystemService(FingerprintManager.class));
        AccessibilityManager accessibilityManager =
                Objects.requireNonNull(
                        (AccessibilityManager)
                                context.getSystemService(AccessibilityManager.class));
        mAccessibilityEnabled = accessibilityManager.isEnabled();
        mGuidedEnrollmentPoints = new ArrayList<>();
        float dim =
                TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_MM,
                        1.0f,
                        context.getResources().getDisplayMetrics());
        Log.v(TAG, "Using old coordinates");
        mGuidedEnrollmentPoints.add(new PointF(2.0f * dim, 0.0f * dim));
        mGuidedEnrollmentPoints.add(new PointF(0.87f * dim, (-2.7f) * dim));
        float x18 = (-1.8f) * dim;
        mGuidedEnrollmentPoints.add(new PointF(x18, (-1.31f) * dim));
        mGuidedEnrollmentPoints.add(new PointF(x18, 1.31f * dim));
        mGuidedEnrollmentPoints.add(new PointF(0.88f * dim, 2.7f * dim));
        mGuidedEnrollmentPoints.add(new PointF(3.94f * dim, (-1.06f) * dim));
        mGuidedEnrollmentPoints.add(new PointF(2.9f * dim, (-4.14f) * dim));
        mGuidedEnrollmentPoints.add(new PointF((-0.52f) * dim, (-5.95f) * dim));
        float x333 = (-3.33f) * dim;
        mGuidedEnrollmentPoints.add(new PointF(x333, x333));
        mGuidedEnrollmentPoints.add(new PointF((-3.99f) * dim, (-0.35f) * dim));
        mGuidedEnrollmentPoints.add(new PointF((-3.62f) * dim, 2.54f * dim));
        mGuidedEnrollmentPoints.add(new PointF((-1.49f) * dim, 5.57f * dim));
        mGuidedEnrollmentPoints.add(new PointF(2.29f * dim, 4.92f * dim));
        mGuidedEnrollmentPoints.add(new PointF(3.82f * dim, dim * 1.78f));
    }

    public void onEnrollmentProgress(int total, int remaining) {
        if (mTotalSteps == -1) {
            mTotalSteps = total;
        }
        if (remaining != mRemainingSteps) {
            mLocationsEnrolled++;
            if (isCenterEnrollmentStage()) {
                mCenterTouchCount++;
            }
        }
        if (mRemainingSteps > remaining) {
            mPace = mRemainingSteps - remaining;
        }
        mRemainingSteps = remaining;
        Listener listener = mListener;
        if (listener == null || mTotalSteps == -1) {
            return;
        }
        listener.onEnrollmentProgress(remaining, mTotalSteps);
    }

    public void onEnrollmentHelp() {
        Listener listener = mListener;
        if (listener != null) {
            listener.onEnrollmentHelp(mRemainingSteps, mTotalSteps);
        }
    }

    public void onAcquired(boolean success) {
        Listener listener = mListener;
        if (listener != null) {
            listener.onAcquired(success && animateIfLastStep());
        }
    }

    public void onPointerDown(int pointerId) {
        Listener listener = mListener;
        if (listener != null) {
            listener.onPointerDown(pointerId);
        }
    }

    public void onPointerUp(int pointerId) {
        Listener listener = mListener;
        if (listener != null) {
            listener.onPointerUp(pointerId);
        }
    }

    void setListener(Listener listener) {
        mListener = listener;
        if (listener == null || mTotalSteps == -1) {
            return;
        }
        listener.onEnrollmentProgress(mRemainingSteps, mTotalSteps);
    }

    boolean isCenterEnrollmentStage() {
        int totalSteps = mTotalSteps;
        return totalSteps == -1
                || mRemainingSteps == -1
                || totalSteps - mRemainingSteps < getStageThresholdSteps(totalSteps, 0);
    }

    boolean isTipEnrollmentStage() {
        int totalSteps = mTotalSteps;
        if (totalSteps == -1 || mRemainingSteps == -1) {
            return false;
        }
        int stepsCompleted = totalSteps - mRemainingSteps;
        return stepsCompleted >= getStageThresholdSteps(totalSteps, 1)
                && stepsCompleted < getStageThresholdSteps(mTotalSteps, 2);
    }

    boolean isEdgeEnrollmentStage() {
        int totalSteps = mTotalSteps;
        return totalSteps != -1
                && mRemainingSteps != -1
                && totalSteps - mRemainingSteps >= getStageThresholdSteps(totalSteps, 2);
    }

    PointF getNextGuidedEnrollmentPoint() {
        if (mAccessibilityEnabled || !isGuidedEnrollmentStage()) {
            return new PointF(0.0f, 0.0f);
        }
        int index = mLocationsEnrolled - mCenterTouchCount;
        PointF point = mGuidedEnrollmentPoints.get(index % mGuidedEnrollmentPoints.size());
        return new PointF(point.x * 0.5f, point.y * 0.5f);
    }

    boolean animateIfLastStep() {
        if (mListener == null) {
            Log.e(TAG, "animateIfLastStep, null listener");
            return false;
        }
        return mRemainingSteps <= mPace && mRemainingSteps >= 0;
    }

    private int getStageThresholdSteps(int totalSteps, int stage) {
        return Math.round(totalSteps * mFingerprintManager.getEnrollStageThreshold(stage));
    }

    private boolean isGuidedEnrollmentStage() {
        int totalSteps = mTotalSteps;
        if (mAccessibilityEnabled || totalSteps == -1 || mRemainingSteps == -1) {
            return false;
        }
        int stepsCompleted = totalSteps - mRemainingSteps;
        return stepsCompleted >= getStageThresholdSteps(totalSteps, 0)
                && stepsCompleted < getStageThresholdSteps(mTotalSteps, 1);
    }
}
