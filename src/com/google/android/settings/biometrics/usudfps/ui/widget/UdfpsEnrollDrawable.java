package com.google.android.settings.biometrics.usudfps.ui.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.PathShape;
import android.os.SystemProperties;
import android.util.AttributeSet;
import android.util.PathParser;
import android.view.animation.AccelerateDecelerateInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.settings.R;

public class UdfpsEnrollDrawable extends Drawable {
    private int mAlpha;
    private final Paint mBlueFill;
    float mCurrentX;
    float mCurrentY;
    private UdfpsEnrollHelper mEnrollHelper;
    private int mEnrollIcon;
    private final ShapeDrawable mFingerprintDrawable;
    private int mMovingTargetFill;
    private final Drawable mMovingTargetFpIcon;
    private final Paint mSensorOutlinePaint;
    private RectF mSensorRect;
    private final Animator.AnimatorListener mTargetAnimListener;
    AnimatorSet mTargetAnimatorSet;
    private boolean mSkipDraw = false;
    float mCurrentScale = 1.0f;
    private boolean mShouldShowTipHint = false;
    private boolean mShouldShowEdgeHint = false;

    @Override
    public int getOpacity() {
        return PixelFormat.UNKNOWN;
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {}

    UdfpsEnrollDrawable(Context context, AttributeSet attrs) {
        mFingerprintDrawable = defaultFactory(context);
        loadResources(context, attrs);

        mSensorOutlinePaint = new Paint(0 /* flags */);
        mSensorOutlinePaint.setAntiAlias(true);
        mSensorOutlinePaint.setColor(mMovingTargetFill);
        mSensorOutlinePaint.setStyle(Paint.Style.FILL);

        mBlueFill = new Paint(0 /* flags */);
        mBlueFill.setAntiAlias(true);
        mBlueFill.setColor(mMovingTargetFill);
        mBlueFill.setStyle(Paint.Style.FILL);

        mMovingTargetFpIcon =
                context.getResources().getDrawable(R.drawable.ic_enrollment_fingerprint, null);
        mMovingTargetFpIcon.setTint(mEnrollIcon);
        mMovingTargetFpIcon.mutate();
        mFingerprintDrawable.setTint(mEnrollIcon);
        setAlpha(255);

        mTargetAnimListener =
                new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animator) {
                        updateTipHintVisibility();
                    }
                };
    }

    void onSensorRectUpdated(RectF rectF) {
        int inset = ((int) rectF.height()) / 8;
        updateFingerprintIconBounds(
                new Rect(
                        ((int) rectF.left) + inset,
                        ((int) rectF.top) + inset,
                        ((int) rectF.right) - inset,
                        ((int) rectF.bottom) - inset));
        mSensorRect = rectF;
    }

    void setEnrollHelper(UdfpsEnrollHelper udfpsEnrollHelper) {
        mEnrollHelper = udfpsEnrollHelper;
    }

    void setShouldSkipDraw(boolean skip) {
        if (mSkipDraw == skip) {
            return;
        }
        mSkipDraw = skip;
        invalidateSelf();
    }

    void updateFingerprintIconBounds(Rect rect) {
        mFingerprintDrawable.setBounds(rect);
        invalidateSelf();
        mMovingTargetFpIcon.setBounds(rect);
        invalidateSelf();
    }

    void onEnrollmentProgress(int remaining, int total) {
        if (mEnrollHelper == null) {
            return;
        }
        if (!mEnrollHelper.isCenterEnrollmentStage()) {
            if (mTargetAnimatorSet != null && mTargetAnimatorSet.isRunning()) {
                mTargetAnimatorSet.end();
            }
            PointF nextPoint = mEnrollHelper.getNextGuidedEnrollmentPoint();
            if (mCurrentX != nextPoint.x || mCurrentY != nextPoint.y) {
                ValueAnimator xAnimator = ValueAnimator.ofFloat(mCurrentX, nextPoint.x);
                xAnimator.addUpdateListener(
                        animation -> {
                            mCurrentX = (Float) animation.getAnimatedValue();
                            invalidateSelf();
                        });
                ValueAnimator yAnimator = ValueAnimator.ofFloat(mCurrentY, nextPoint.y);
                yAnimator.addUpdateListener(
                        animation -> {
                            mCurrentY = (Float) animation.getAnimatedValue();
                            invalidateSelf();
                        });
                long duration = (nextPoint.x == 0.0f && nextPoint.y == 0.0f) ? 600L : 800L;
                ValueAnimator scaleAnimator = ValueAnimator.ofFloat(0.0f, (float) Math.PI);
                scaleAnimator.setDuration(duration);
                scaleAnimator.addUpdateListener(
                        animation -> {
                            mCurrentScale =
                                    ((float) Math.sin((Float) animation.getAnimatedValue()) * 0.25f)
                                            + 1.0f;
                            invalidateSelf();
                        });

                mTargetAnimatorSet = new AnimatorSet();
                mTargetAnimatorSet.setInterpolator(new AccelerateDecelerateInterpolator());
                mTargetAnimatorSet.setDuration(duration);
                mTargetAnimatorSet.addListener(mTargetAnimListener);
                mTargetAnimatorSet.playTogether(xAnimator, yAnimator, scaleAnimator);
                mTargetAnimatorSet.start();
            } else {
                updateTipHintVisibility();
            }
        } else {
            updateTipHintVisibility();
        }
        updateEdgeHintVisibility();
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        if (mSkipDraw) {
            return;
        }
        if (SystemProperties.getBoolean("debug.udfps_show_sensor_bounds_outline", false)
                && mSensorRect != null) {
            visualizeFingerprintSensorOutline(canvas, mSensorRect);
        }
        if (mEnrollHelper != null && !mEnrollHelper.isCenterEnrollmentStage()) {
            canvas.save();
            canvas.translate(mCurrentX, mCurrentY);
            if (mSensorRect != null) {
                canvas.scale(
                        mCurrentScale, mCurrentScale, mSensorRect.centerX(), mSensorRect.centerY());
                canvas.drawOval(mSensorRect, mBlueFill);
            }
            mMovingTargetFpIcon.draw(canvas);
            canvas.restore();
            return;
        }
        if (mSensorRect != null) {
            canvas.drawOval(mSensorRect, mSensorOutlinePaint);
        }
        mFingerprintDrawable.draw(canvas);
        mFingerprintDrawable.setAlpha(getAlpha());
        mSensorOutlinePaint.setAlpha(getAlpha());
    }

    @Override
    public void setAlpha(int alpha) {
        mAlpha = alpha;
        mFingerprintDrawable.setAlpha(alpha);
        mSensorOutlinePaint.setAlpha(alpha);
        mBlueFill.setAlpha(alpha);
        mMovingTargetFpIcon.setAlpha(alpha);
        invalidateSelf();
    }

    @Override
    public int getAlpha() {
        return mAlpha;
    }

    public void updateTipHintVisibility() {
        boolean isTip = mEnrollHelper != null && mEnrollHelper.isTipEnrollmentStage();
        if (mShouldShowTipHint == isTip) {
            return;
        }
        mShouldShowTipHint = isTip;
    }

    private void updateEdgeHintVisibility() {
        boolean isEdge = mEnrollHelper != null && mEnrollHelper.isEdgeEnrollmentStage();
        if (mShouldShowEdgeHint == isEdge) {
            return;
        }
        mShouldShowEdgeHint = isEdge;
    }

    private ShapeDrawable defaultFactory(Context context) {
        ShapeDrawable shapeDrawable =
                new ShapeDrawable(
                        new PathShape(
                                PathParser.createPathFromPathData(
                                        context.getResources()
                                                .getString(R.string.config_udfpsIcon)),
                                72.0f,
                                72.0f));
        shapeDrawable.mutate();
        shapeDrawable.getPaint().setStyle(Paint.Style.STROKE);
        shapeDrawable.getPaint().setStrokeCap(Paint.Cap.ROUND);
        shapeDrawable.getPaint().setStrokeWidth(3.0f);
        return shapeDrawable;
    }

    private void loadResources(Context context, AttributeSet attrs) {
        TypedArray a =
                context.obtainStyledAttributes(
                        attrs,
                        R.styleable.BiometricsEnrollView,
                        R.attr.biometricsEnrollStyle,
                        R.style.BiometricsEnrollStyle);
        mEnrollIcon = a.getColor(R.styleable.BiometricsEnrollView_biometricsEnrollIcon, 0);
        mMovingTargetFill =
                a.getColor(R.styleable.BiometricsEnrollView_biometricsMovingTargetFill, 0);
        a.recycle();
    }

    private void visualizeFingerprintSensorOutline(Canvas canvas, RectF rectF) {
        Paint paint = new Paint();
        paint.setColor(0xFF00FF00);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2.0f);
        canvas.drawCircle(
                rectF.centerX(),
                rectF.centerY(),
                Math.min(rectF.width(), rectF.height()) / 2.0f,
                paint);
    }
}
