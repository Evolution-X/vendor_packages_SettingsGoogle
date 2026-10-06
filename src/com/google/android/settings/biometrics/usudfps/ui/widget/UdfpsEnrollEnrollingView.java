package com.google.android.settings.biometrics.usudfps.ui.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Handler;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.DisplayInfo;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityManager;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.android.settings.R;
import com.android.systemui.biometrics.UdfpsUtils;
import com.android.systemui.biometrics.shared.model.FingerprintSensor;
import com.android.systemui.biometrics.shared.model.FingerprintSensorTypeKt;
import com.android.systemui.biometrics.shared.model.UdfpsOverlayParams;

import com.google.android.setupcompat.template.FooterBarMixin;
import com.google.android.setupdesign.GlifLayout;
import com.google.android.setupdesign.view.BottomScrollView;

import java.util.Locale;

public class UdfpsEnrollEnrollingView extends GlifLayout {
    private final AccessibilityManager mAccessibilityManager;
    private int mDefaultHeaderScrollViewIndicator;
    private ObjectAnimator mHeaderScrollAnimator;
    private View mHeaderView;
    private boolean mIsLandscape;
    private UdfpsEnrollProgressBarDrawable.OnDrawFinishedListener mOnDrawFinishedListener;
    private ValueAnimator mProgressBarEnterAnimator;
    private int mRotation;
    private boolean mShouldShowLottie;
    private boolean mShouldUseReverseLandscape;
    private UdfpsEnrollHelper mUdfpsEnrollHelper;
    private UdfpsEnrollView mUdfpsEnrollView;
    private FingerprintSensor mUdfpsProps;
    private final UdfpsUtils mUdfpsUtils;
    private WindowManager mWindowManager;

    public UdfpsEnrollEnrollingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mDefaultHeaderScrollViewIndicator = 0;
        updateLandscapeInfo();
        mUdfpsUtils = new UdfpsUtils();
        mAccessibilityManager = getContext().getSystemService(AccessibilityManager.class);
        mWindowManager = mContext.getSystemService(WindowManager.class);
    }

    private void updateLandscapeInfo() {
        int rotation = getContext().getDisplay().getRotation();
        mRotation = rotation;
        mIsLandscape = rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270;
        boolean isRtl =
                TextUtils.getLayoutDirectionFromLocale(Locale.getDefault())
                        == View.LAYOUT_DIRECTION_RTL;
        mShouldUseReverseLandscape =
                (mRotation == Surface.ROTATION_90 && isRtl)
                        || (mRotation == Surface.ROTATION_270 && !isRtl);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mHeaderView = findViewById(com.google.android.setupdesign.R.id.sud_landscape_header_area);
        mUdfpsEnrollView = findViewById(R.id.udfps_animation_view);
    }

    @Override
    protected View onInflateTemplate(LayoutInflater inflater, int template) {
        if (inflater.getContext().getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_PORTRAIT) {
            template = R.layout.biometrics_glif_compact;
        }
        return super.onInflateTemplate(inflater, template);
    }

    void setDecreasePadding(int decrease) {
        if (mUdfpsEnrollView != null) {
            mUdfpsEnrollView.setDecreasePadding(decrease);
        }
    }

    void onUdfpsSensorRectUpdated() {
        mProgressBarEnterAnimator = ValueAnimator.ofFloat(0.0f, 1.0f);
        mProgressBarEnterAnimator.setInterpolator(new LinearInterpolator());
        mProgressBarEnterAnimator.setDuration(500L);
        mProgressBarEnterAnimator.addUpdateListener(
                animator -> setAlpha((Float) animator.getAnimatedValue()));
        mProgressBarEnterAnimator.addListener(
                new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animator) {
                        removeAllEnrollProgressAnimatorListener();
                    }

                    @Override
                    public void onAnimationCancel(Animator animator) {
                        removeAllEnrollProgressAnimatorListener();
                    }
                });
        mProgressBarEnterAnimator.start();
    }

    public void removeAllEnrollProgressAnimatorListener() {
        mProgressBarEnterAnimator.removeAllUpdateListeners();
        mProgressBarEnterAnimator.removeAllListeners();
    }

    private int getScrollableGlifHeaderHeight(boolean showLottie) {
        mShouldShowLottie = showLottie;
        TypedValue typedValue = new TypedValue();
        if (isLargeDisplaySizeOrFontSize() && !showLottie) {
            getResources()
                    .getValue(R.dimen.biometrics_glif_header_height_ratio_large, typedValue, true);
        } else {
            getResources().getValue(R.dimen.biometrics_glif_header_height_ratio, typedValue, true);
        }
        return (int) (getResources().getDisplayMetrics().heightPixels * typedValue.getFloat());
    }

    public void adjustScrollableHeaderHeight(ScrollView scrollView, boolean showLottie) {
        ViewGroup.LayoutParams params = scrollView.getLayoutParams();
        params.height = getScrollableGlifHeaderHeight(showLottie);
        scrollView.setLayoutParams(params);
    }

    public boolean isLargeDisplaySizeOrFontSize() {
        return getResources().getConfiguration().fontScale > 1.3f || getLargeDisplayScale() >= 2.8f;
    }

    private float getLargeDisplayScale() {
        Display defaultDisplay = mWindowManager.getDefaultDisplay();
        DisplayMetrics displayMetrics = new DisplayMetrics();
        defaultDisplay.getMetrics(displayMetrics);
        return displayMetrics.scaledDensity;
    }

    public void adjustProgressBarView() {
        if (mUdfpsEnrollView != null) {
            mUdfpsEnrollView.postDelayed(this::adjustUdfpsViewWithFooterBar, 100L);
        }
    }

    public void adjustUdfpsViewWithFooterBar() {
        updateSensorOverlayParams(mUdfpsProps, mUdfpsEnrollHelper);
        ImageView progressView = findViewById(R.id.udfps_enroll_animation_fp_progress_view);
        final int onScreenPositionTop =
                ((getOnScreenPositionTop(progressView)
                                        + progressView.getDrawable().getBounds().height())
                                - progressView.getPaddingBottom())
                        + 2;
        final LinearLayout buttonContainer = getMixin(FooterBarMixin.class).getButtonContainer();
        buttonContainer.requestLayout();
        final Rect rect = new Rect();
        buttonContainer
                .getViewTreeObserver()
                .addOnGlobalLayoutListener(
                        new ViewTreeObserver.OnGlobalLayoutListener() {
                            @Override
                            public void onGlobalLayout() {
                                buttonContainer.getGlobalVisibleRect(rect);
                                int top = rect.top;
                                if (onScreenPositionTop > top
                                        && (!mIsLandscape || !isLargeDisplaySizeOrFontSize())) {
                                    setDecreasePadding(onScreenPositionTop - top);
                                }
                                buttonContainer
                                        .getViewTreeObserver()
                                        .removeOnGlobalLayoutListener(this);
                            }
                        });
        onUdfpsSensorRectUpdated();
    }

    private int getOnScreenPositionTop(View view) {
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        return location[1];
    }

    public void headerVerticalScrolling(final ScrollView scrollView, final long duration) {
        scrollView.post(
                () -> {
                    mHeaderScrollAnimator =
                            ObjectAnimator.ofInt(
                                    scrollView,
                                    "scrollY",
                                    scrollView.getChildAt(0).getMeasuredHeight()
                                            - scrollView.getMeasuredHeight());
                    mHeaderScrollAnimator.setDuration(duration);
                    mHeaderScrollAnimator.addListener(
                            new AnimatorListenerAdapter() {
                                @Override
                                public void onAnimationEnd(Animator animator) {
                                    scrollView.post(
                                            () -> {
                                                mHeaderScrollAnimator.removeAllListeners();
                                                if (mAccessibilityManager.isEnabled()) {
                                                    new Handler()
                                                            .postDelayed(
                                                                    () -> {
                                                                        if (mHeaderScrollAnimator
                                                                                .isRunning()) {
                                                                            return;
                                                                        }
                                                                        setFocusOnDescription();
                                                                    },
                                                                    duration + 200);
                                                }
                                            });
                                }
                            });
                    mHeaderScrollAnimator.start();
                });
    }

    public void setFocusOnDescription() {
        final ScrollView scrollView =
                findViewById(com.google.android.setupdesign.R.id.sud_header_scroll_view);
        final TextView descriptionTextView = getDescriptionTextView();
        if (descriptionTextView == null || descriptionTextView.getText().isEmpty()) {
            return;
        }
        descriptionTextView.post(
                () -> {
                    Rect hitRect = new Rect();
                    scrollView.getHitRect(hitRect);
                    if (descriptionTextView.getLocalVisibleRect(hitRect)) {
                        return;
                    }
                    descriptionTextView.setFocusable(true);
                    descriptionTextView.setFocusableInTouchMode(true);
                    descriptionTextView.requestFocus();
                });
    }

    public void initView(FingerprintSensor fingerprintSensor, UdfpsEnrollHelper udfpsEnrollHelper) {
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        mUdfpsProps = fingerprintSensor;
        mUdfpsEnrollHelper = udfpsEnrollHelper;
        initUdfpsEnrollView(fingerprintSensor, udfpsEnrollHelper);
        FrameLayout frameLayout = findViewById(R.id.layout_container);
        frameLayout.setClipChildren(false);
        frameLayout.setClipToPadding(false);
        ViewGroup parent = (ViewGroup) frameLayout.getParent();
        parent.setClipChildren(false);
        parent.setClipToPadding(false);
        if (!mIsLandscape) {
            adjustPortraitPaddings();
            showPortraitSubTitle();
        } else if (mShouldUseReverseLandscape) {
            relayoutForReversedLandscape();
            showSubTitle();
        } else {
            showSubTitle();
        }
        setOnHoverListener();
        adjustProgressBarViewIfNeeded();
    }

    private void adjustProgressBarViewIfNeeded() {
        if (mUdfpsEnrollView != null) {
            final UdfpsEnrollProgressBarDrawable progressDrawable =
                    (UdfpsEnrollProgressBarDrawable)
                            mUdfpsEnrollView.getFingerprintProgressDrawable();
            mOnDrawFinishedListener =
                    () -> {
                        progressDrawable.deleteOnDrawFinishedListener();
                        mUdfpsEnrollView.setVisibility(View.INVISIBLE);
                        adjustProgressBarView();
                    };
            progressDrawable.addOnDrawFinishedListener(mOnDrawFinishedListener);
        }
    }

    public void relayoutForFingerprintSensor() {
        updateLandscapeInfo();
        if (mIsLandscape) {
            if (mShouldUseReverseLandscape) {
                relayoutForReversedLandscape();
            } else {
                relayoutForLandscape();
            }
        }
    }

    private void initUdfpsEnrollView(
            FingerprintSensor fingerprintSensor, UdfpsEnrollHelper udfpsEnrollHelper) {
        updateSensorOverlayParams(fingerprintSensor, udfpsEnrollHelper);
    }

    private void updateSensorOverlayParams(
            FingerprintSensor fingerprintSensor, UdfpsEnrollHelper udfpsEnrollHelper) {
        DisplayInfo displayInfo = new DisplayInfo();
        getContext().getDisplay().getDisplayInfo(displayInfo);
        float scaleFactor = mUdfpsUtils.getScaleFactor(displayInfo);
        Rect sensorBounds = Rect.copyOrNull(fingerprintSensor.getSensorBounds());
        sensorBounds.scale(scaleFactor);
        mUdfpsEnrollView.setOverlayParams(
                new UdfpsOverlayParams(
                        sensorBounds,
                        new Rect(
                                0,
                                displayInfo.getNaturalHeight() / 2,
                                displayInfo.getNaturalWidth(),
                                displayInfo.getNaturalHeight()),
                        displayInfo.getNaturalWidth(),
                        displayInfo.getNaturalHeight(),
                        scaleFactor,
                        displayInfo.rotation,
                        FingerprintSensorTypeKt.toInt(fingerprintSensor.getSensorType())));
        mUdfpsEnrollView.setEnrollHelper(udfpsEnrollHelper);
        mUdfpsEnrollView.setClipToPadding(false);
        mUdfpsEnrollView.setClipChildren(false);
        mUdfpsEnrollView.setVisibility(View.VISIBLE);
    }

    private void adjustPortraitPaddings() {
        FrameLayout frameLayout = findViewById(R.id.layout_container);
        int topPadding = (int) getResources().getDimension(R.dimen.udfps_lottie_padding_top);
        frameLayout.setPadding(0, topPadding, 0, 0);
        ((ImageView) mUdfpsEnrollView.findViewById(R.id.udfps_enroll_animation_fp_progress_view))
                .setPadding(0, -topPadding, 0, topPadding);
        ((ImageView) mUdfpsEnrollView.findViewById(R.id.udfps_enroll_animation_fp_view))
                .setPadding(0, -topPadding, 0, topPadding);
    }

    private void setOnHoverListener() {
        if (mAccessibilityManager.isEnabled()) {
            int contentAreaId =
                    mIsLandscape
                            ? com.google.android.setupdesign.R.id.sud_landscape_content_area
                            : com.google.android.setupdesign.R.id.sud_layout_content;
            final ViewGroup viewGroup = (ViewGroup) findManagedViewById(contentAreaId);
            viewGroup.setClipChildren(false);
            viewGroup.setClipToPadding(false);
            viewGroup.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
            viewGroup.setAccessibilityLiveRegion(ACCESSIBILITY_LIVE_REGION_ASSERTIVE);
            viewGroup.setOnHoverListener(
                    (v, event) -> {
                        if (event.getAction() == MotionEvent.ACTION_HOVER_ENTER
                                || event.getAction() == MotionEvent.ACTION_HOVER_MOVE) {
                            onHoverEnterOrMove(viewGroup, event);
                            return false;
                        }
                        if (event.getAction() != MotionEvent.ACTION_HOVER_EXIT) {
                            return false;
                        }
                        onHoverExit(viewGroup);
                        return false;
                    });
        }
    }

    private void onHoverEnterOrMove(View view, MotionEvent event) {
        Point nativeCoords =
                mUdfpsUtils.getTouchInNativeCoordinates(
                        event.getPointerId(0), event, mUdfpsEnrollView.getOverlayParams());
        if (mUdfpsUtils.isWithinSensorArea(
                event.getPointerId(0), event, mUdfpsEnrollView.getOverlayParams())) {
            return;
        }
        String description =
                mUdfpsUtils.onTouchOutsideOfSensorArea(
                        mAccessibilityManager.isTouchExplorationEnabled(),
                        getContext(),
                        nativeCoords.x,
                        nativeCoords.y,
                        mUdfpsEnrollView.getOverlayParams());
        if (description == null) {
            return;
        }
        view.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        view.setContentDescription(null);
        view.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
        view.setContentDescription(description);
    }

    private void onHoverExit(final View view) {
        view.postDelayed(
                () -> view.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO), 600L);
    }

    private void relayoutForReversedLandscape() {
        ViewGroup parent = (ViewGroup) mHeaderView.getParent();
        if (parent.getChildAt(0) == mHeaderView) {
            parent.removeView(mHeaderView);
            parent.addView(mHeaderView);
        }
        BottomScrollView scrollView =
                mHeaderView.findViewById(
                        com.google.android.setupdesign.R.id.sud_header_scroll_view);
        mDefaultHeaderScrollViewIndicator = scrollView.getScrollIndicators();
        scrollView.setScrollIndicators(0);
    }

    private void relayoutForLandscape() {
        ViewGroup parent = (ViewGroup) mHeaderView.getParent();
        if (parent.getChildAt(parent.getChildCount() - 1) == mHeaderView) {
            parent.removeView(mHeaderView);
            parent.addView(mHeaderView, 0);
        }
        ((BottomScrollView)
                        mHeaderView.findViewById(
                                com.google.android.setupdesign.R.id.sud_header_scroll_view))
                .setScrollIndicators(mDefaultHeaderScrollViewIndicator);
    }

    private void showSubTitle() {
        mHeaderView
                .findViewById(com.google.android.setupdesign.R.id.sud_layout_subtitle)
                .setVisibility(View.VISIBLE);
    }

    private void showPortraitSubTitle() {
        if (mAccessibilityManager.isEnabled() || !mShouldShowLottie) {
            findViewById(com.google.android.setupdesign.R.id.sud_layout_subtitle)
                    .setVisibility(View.VISIBLE);
        }
    }
}
