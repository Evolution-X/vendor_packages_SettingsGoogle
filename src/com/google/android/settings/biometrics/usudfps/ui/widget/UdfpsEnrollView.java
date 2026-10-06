package com.google.android.settings.biometrics.usudfps.ui.widget;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.hardware.fingerprint.FingerprintSensorProperties;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.RotationUtils;
import android.view.Gravity;
import android.view.Surface;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.android.settings.R;
import com.android.systemui.biometrics.shared.model.UdfpsOverlayParams;

public class UdfpsEnrollView extends FrameLayout implements UdfpsEnrollHelper.Listener {
    private int mDefaultProgressBarRadius;
    private final UdfpsEnrollDrawable mFingerprintDrawable;
    private final UdfpsEnrollProgressBarDrawable mFingerprintProgressDrawable;
    private ImageView mFingerprintProgressView;
    private final Handler mHandler;
    private UdfpsOverlayParams mOverlayParams;
    private int mProgressBarRadius;
    private Rect mSensorRect;

    public UdfpsEnrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mFingerprintDrawable = new UdfpsEnrollDrawable(context, attrs);
        mFingerprintProgressDrawable = new UdfpsEnrollProgressBarDrawable(context, attrs);
        mHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    protected void onFinishInflate() {
        ((ImageView) findViewById(R.id.udfps_enroll_animation_fp_view))
                .setImageDrawable(mFingerprintDrawable);
        mFingerprintProgressView = findViewById(R.id.udfps_enroll_animation_fp_progress_view);
        mFingerprintProgressView.setImageDrawable(mFingerprintProgressDrawable);
    }

    @Override
    public void onEnrollmentProgress(final int remaining, final int total) {
        mHandler.post(
                () -> {
                    mFingerprintProgressDrawable.onEnrollmentProgress(remaining, total);
                    mFingerprintDrawable.onEnrollmentProgress(remaining, total);
                });
    }

    @Override
    public void onEnrollmentHelp(final int remaining, final int total) {
        mHandler.post(() -> mFingerprintProgressDrawable.onEnrollmentHelp(remaining, total));
    }

    @Override
    public void onAcquired(final boolean isLastStep) {
        mHandler.post(
                () -> {
                    onFingerUp();
                    if (isLastStep) {
                        mFingerprintProgressDrawable.onLastStepAcquired();
                    }
                });
    }

    @Override
    public void onPointerDown(int sensorId) {
        onFingerDown();
    }

    @Override
    public void onPointerUp(int sensorId) {
        onFingerUp();
    }

    public UdfpsOverlayParams getOverlayParams() {
        return mOverlayParams;
    }

    public void setOverlayParams(UdfpsOverlayParams overlayParams) {
        mOverlayParams = overlayParams;
        post(
                () -> {
                    int scaleFactor =
                            (int)
                                    (mOverlayParams.getScaleFactor()
                                            * getContext()
                                                    .getResources()
                                                    .getInteger(
                                                            R.integer
                                                                    .config_udfpsEnrollProgressBar));
                    mDefaultProgressBarRadius = scaleFactor;
                    if (mProgressBarRadius == 0) {
                        mProgressBarRadius = scaleFactor;
                    }
                    mSensorRect = new Rect(mOverlayParams.getSensorBounds());
                    onSensorRectUpdated();
                });
    }

    public void setEnrollHelper(UdfpsEnrollHelper helper) {
        mFingerprintDrawable.setEnrollHelper(helper);
        helper.setListener(this);
    }

    void setDecreasePadding(int decrease) {
        mProgressBarRadius -= decrease;
        onSensorRectUpdated();
    }

    Drawable getFingerprintProgressDrawable() {
        return mFingerprintProgressDrawable;
    }

    private void onSensorRectUpdated() {
        updateDimensions();
        mSensorRect.set(
                getPaddingX(),
                getPaddingY(),
                mOverlayParams.getSensorBounds().width() + getPaddingX(),
                mOverlayParams.getSensorBounds().height() + getPaddingY());
        mFingerprintDrawable.onSensorRectUpdated(new RectF(mSensorRect));
    }

    private void updateDimensions() {
        Rect rect = new Rect(mOverlayParams.getSensorBounds());
        int rotation = mOverlayParams.getRotation();
        if (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) {
            RotationUtils.rotateBounds(
                    rect,
                    mOverlayParams.getNaturalDisplayWidth(),
                    mOverlayParams.getNaturalDisplayHeight(),
                    rotation);
        }
        ViewGroup parent = (ViewGroup) getParent();
        ViewGroup.MarginLayoutParams marginLayoutParams =
                (ViewGroup.MarginLayoutParams) getLayoutParams();
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) getLayoutParams();
        if (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180) {
            int[] location = new int[2];
            parent.getLocationOnScreen(location);
            int width = location[0] + parent.getWidth();
            layoutParams.gravity = Gravity.TOP | Gravity.RIGHT;
            int paddingX = (width - rect.right) - getPaddingX();
            int paddingY = (rect.top - location[1]) - getPaddingY();
            if (marginLayoutParams.rightMargin == paddingX
                    && marginLayoutParams.topMargin == paddingY) {
                return;
            }
            marginLayoutParams.rightMargin = paddingX;
            marginLayoutParams.topMargin = paddingY;
            setLayoutParams(layoutParams);
        } else {
            int[] location = new int[2];
            parent.getLocationOnScreen(location);
            int width = parent.getWidth() + location[0];
            int height = location[1] + parent.getHeight();
            if (rotation == Surface.ROTATION_90) {
                layoutParams.gravity = Gravity.BOTTOM | Gravity.RIGHT;
                marginLayoutParams.rightMargin = (width - rect.right) - getPaddingX();
                marginLayoutParams.bottomMargin = (height - rect.bottom) - getPaddingY();
            } else if (rotation == Surface.ROTATION_270) {
                layoutParams.gravity = Gravity.BOTTOM | Gravity.LEFT;
                marginLayoutParams.leftMargin = (rect.left - location[0]) - getPaddingX();
                marginLayoutParams.bottomMargin = (height - rect.bottom) - getPaddingY();
            }
        }
        layoutParams.height = rect.height() + (getPaddingX() * 2);
        layoutParams.width = rect.width() + (getPaddingY() * 2);
        setLayoutParams(layoutParams);
    }

    private void onFingerDown() {
        if (mOverlayParams.getSensorType() == FingerprintSensorProperties.TYPE_UDFPS_OPTICAL) {
            mFingerprintDrawable.setShouldSkipDraw(true);
        }
        mFingerprintDrawable.invalidateSelf();
    }

    private void onFingerUp() {
        mFingerprintDrawable.setShouldSkipDraw(false);
        mFingerprintDrawable.invalidateSelf();
    }

    private int getPaddingX() {
        return mProgressBarRadius;
    }

    private int getPaddingY() {
        return mProgressBarRadius;
    }
}
