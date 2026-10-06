package com.google.android.settings.biometrics.face;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

public class SquareFrameLayout extends FrameLayout {
    private int mOuterRegionChild;
    private int mPaddingDp;

    public SquareFrameLayout(Context context) {
        super(context);
    }

    public SquareFrameLayout(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public SquareFrameLayout(Context context, AttributeSet attributeSet, int defStyleAttr) {
        super(context, attributeSet, defStyleAttr);
    }

    public SquareFrameLayout(
            Context context, AttributeSet attributeSet, int defStyleAttr, int defStyleRes) {
        super(context, attributeSet, defStyleAttr, defStyleRes);
    }

    public void setOuterRegion(int outerRegionChild, int paddingDp) {
        mOuterRegionChild = outerRegionChild;
        mPaddingDp = paddingDp;
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int size = View.MeasureSpec.getSize(widthMeasureSpec);
        setMeasuredDimension(size, size);
        int padding = (int) Utils.dpToPx(getContext(), mPaddingDp);
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            int measureSpec = View.MeasureSpec.makeMeasureSpec(size, View.MeasureSpec.EXACTLY);
            View childAt = getChildAt(i);
            childAt.measure(measureSpec, measureSpec);
            if (childAt.getId() != mOuterRegionChild) {
                childAt.setPadding(padding, padding, padding, padding);
            }
        }
    }
}
