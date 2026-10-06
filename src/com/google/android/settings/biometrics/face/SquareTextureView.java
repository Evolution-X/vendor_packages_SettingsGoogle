package com.google.android.settings.biometrics.face;

import android.content.Context;
import android.util.AttributeSet;
import android.view.TextureView;
import android.view.View;

public class SquareTextureView extends TextureView {
    public SquareTextureView(Context context) {
        this(context, null);
    }

    public SquareTextureView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public SquareTextureView(Context context, AttributeSet attributeSet, int defStyleAttr) {
        super(context, attributeSet, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int widthSize = View.MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = View.MeasureSpec.getSize(heightMeasureSpec);
        if (widthSize < heightSize) {
            setMeasuredDimension(widthSize, widthSize);
        } else {
            setMeasuredDimension(heightSize, heightSize);
        }
    }
}
