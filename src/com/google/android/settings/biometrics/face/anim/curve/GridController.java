package com.google.android.settings.biometrics.face.anim.curve;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.util.ArraySet;

import com.android.internal.graphics.ColorUtils;

import com.google.android.settings.R;
import com.google.android.settings.biometrics.face.anim.FaceEnrollAnimationMultiAngleDrawable;

public class GridController {
    private static final int[] SCRIM_OPACITY_THRESHOLDS = {0, 5, 10, 15, 20};
    private static final float[] SCRIM_OPACITY_VALUES = {0.55f, 0.6f, 0.65f, 0.7f, 0.75f};
    private final FaceEnrollAnimationMultiAngleDrawable.BucketListener mBucketListener;
    private CellConfig[] mCellConfigs;
    private final CellState[] mCellStates;
    private final GridState mGridState;
    private final ScrimState mNoActivityScrimState;
    private final ArraySet<Integer> mPrimaryCellIndices;
    private final int mScrimNotEnrolledDefaultColor;
    private final int mScrimNotEnrolledPrimaryColor;
    private final int mScrimNotEnrolledSecondaryColor;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private int mEnrolledCount = 0;

    public void onBoundsChange(Rect rect) {}

    public GridController(
            Context context, FaceEnrollAnimationMultiAngleDrawable.BucketListener bucketListener) {
        mBucketListener = bucketListener;
        mScrimNotEnrolledDefaultColor = context.getColor(R.color.face_enroll_cell_not_enrolled);
        mScrimNotEnrolledPrimaryColor =
                context.getColor(R.color.face_enroll_cell_not_enrolled_primary);
        mScrimNotEnrolledSecondaryColor =
                context.getColor(R.color.face_enroll_cell_not_enrolled_secondary);
        int[] intArray = context.getResources().getIntArray(R.array.face_enroll_primary_buckets);
        mPrimaryCellIndices = new ArraySet<>(intArray.length);
        for (int i : intArray) {
            mPrimaryCellIndices.add(i);
        }
        mNoActivityScrimState =
                new ScrimState(
                        context.getColor(R.color.face_enroll_no_activity_gone),
                        context.getColor(R.color.face_enroll_no_activity_showing));
        mGridState = new GridState(context, mHandler);
        mCellStates = new CellState[25];
        for (int i2 = 0; i2 < mCellStates.length; i2++) {
            mCellStates[i2] =
                    new CellState(context, i2, mBucketListener, getScrimNotEnrolledColor(0, i2));
        }
    }

    public void stopPulseForNoActivity() {
        for (int i = 0; i < mCellStates.length; i++) {
            mCellStates[i].stopPulseForNoActivity();
        }
    }

    public void pulseForNoActivity(int i, int i2) {
        mCellStates[i].pulseForNoActivity(i2);
    }

    public void onUserLeaveGood() {
        for (int i = 0; i < mCellStates.length; i++) {
            mCellStates[i].fadeScrimOut(2);
            mCellStates[i].fadeCursorNow();
        }
        mGridState.fadeOut(null);
    }

    public void onUserEnterGood() {
        mGridState.fadeIn();
        for (int i = 0; i < mCellStates.length; i++) {
            mCellStates[i].fadeScrimIn();
        }
    }

    public void onAcquired(int i) {
        boolean isDone = mCellStates[i].isDone();
        if (mNoActivityScrimState.isShowing() && !isDone) {
            mNoActivityScrimState.fadeOut();
        }
        mCellStates[i].onAcquired();
        if (isDone) {
            return;
        }
        mEnrolledCount++;
        updateColor(true);
    }

    public void restoreState(int i, boolean z) {
        if (z) {
            mCellStates[i].setEarlyDone();
            mEnrolledCount++;
        }
    }

    private void updateColor(boolean z) {
        for (int i = 0; i < mCellStates.length; i++) {
            CellState cellState = mCellStates[i];
            if (!cellState.isDone()) {
                cellState.updateScrimNotEnrolledColor(
                        getScrimNotEnrolledColor(mEnrolledCount, i), z);
            }
        }
    }

    public void setEarlyDone(int i) {
        if (mCellStates[i].isDone()) {
            return;
        }
        mCellStates[i].setEarlyDone();
        mEnrolledCount++;
        updateColor(false);
    }

    public void draw(Canvas canvas) {
        canvas.save();
        if (mCellConfigs == null) {
            initializeCells(canvas.getWidth(), canvas.getHeight());
            for (int i2 = 0; i2 < mCellStates.length; i2++) {
                mCellStates[i2].updateConfig(mCellConfigs[i2]);
            }
        }
        canvas.translate(canvas.getWidth() / 2, canvas.getHeight() / 2);
        mNoActivityScrimState.draw(canvas);
        for (int i3 = 0; i3 < mCellStates.length; i3++) {
            mCellStates[i3].draw(canvas);
        }
        mGridState.draw(canvas);
        for (int i = 0; i < mCellStates.length; i++) {
            mCellStates[i].drawCursor(canvas);
        }
        canvas.restore();
    }

    private void initializeCells(int i, int i2) {
        float f = i * 0.32f;
        float f2 = i2 * 0.78f;
        float f3 = (-f) / 2.0f;
        float f4 = (-i2) / 2;
        float f5 = f / 2.0f;
        float f6 = i2 / 2;
        RectF rectF = new RectF(f3, f4, f5, f6);
        float f7 = (-i) / 2;
        float f8 = i / 2;
        RectF rectF2 = new RectF(f7, f3, f8, f5);
        float f9 = (-f2) / 2.0f;
        float f10 = f2 / 2.0f;
        RectF rectF3 = new RectF(f9, f4, f10, f6);
        RectF rectF4 = new RectF(f7, f9, f8, f10);
        RectF rectF5 = new RectF(f7, f4, f8, f6);
        float[][] fArr = {
            {72.26f, 165.41f, 252.3f, 342.3f},
            {78.0f, 131.2f, 107.5f, 17.8f},
            {52.0f, 48.8f, 72.0f, 12.0f},
            {50.0f, 102.05f},
            {0.0f, 38.0f, 40.0f},
            {0.0f, 90.0f, 52.0f}
        };
        float[][] fArr2 = {
            {35.45f, 30.07f, 35.0f, 35.4f},
            {24.0f, 31.0f, -35.0f, 31.6f},
            {26.0f, -31.0f, -31.0f, 26.0f},
            {81.0f, -23.85f},
            {52.0f, -26.0f, -40.0f},
            {90.0f, -52.0f, -52.0f}
        };
        Path path = new Path();
        path.arcTo(rectF2, fArr[0][0], fArr2[0][0]);
        path.arcTo(rectF, fArr[0][1], fArr2[0][1]);
        path.arcTo(rectF2, fArr[0][2], fArr2[0][2]);
        path.arcTo(rectF, fArr[0][3], fArr2[0][3]);
        Path path2 = new Path();
        path2.arcTo(rectF4, fArr[1][0], fArr2[1][0]);
        path2.arcTo(rectF, fArr[1][1], fArr2[1][1]);
        path2.arcTo(rectF2, fArr[1][2], fArr2[1][2]);
        path2.arcTo(rectF, fArr[1][3], fArr2[1][3]);
        Path path3 = new Path();
        path3.arcTo(rectF4, fArr[2][0], fArr2[2][0]);
        path3.arcTo(rectF, fArr[2][1], fArr2[2][1]);
        path3.arcTo(rectF2, fArr[2][2], fArr2[2][2]);
        path3.arcTo(rectF3, fArr[2][3], fArr2[2][3]);
        Path path4 = new Path();
        path4.arcTo(rectF, fArr[3][0], fArr2[3][0]);
        path4.arcTo(rectF4, fArr[3][1], fArr2[3][1]);
        Path path5 = new Path();
        path5.arcTo(rectF4, fArr[4][0], fArr2[4][0]);
        path5.arcTo(rectF3, fArr[4][1], fArr2[4][1]);
        path5.arcTo(rectF2, fArr[4][2], fArr2[4][2]);
        Path path6 = new Path();
        path6.arcTo(rectF5, fArr[5][0], fArr2[5][0]);
        path6.arcTo(rectF3, fArr[5][1], fArr2[5][1]);
        path6.arcTo(rectF4, fArr[5][2], fArr2[5][2]);
        mCellConfigs =
                new CellConfig[] {
                    new CellConfig(path6, 180),
                    new CellConfig(path5, 90, true),
                    new CellConfig(path4, 180),
                    new CellConfig(path5, 270),
                    new CellConfig(path6, 270),
                    new CellConfig(path5, 180),
                    new CellConfig(path3, 180),
                    new CellConfig(path2, 180),
                    new CellConfig(path3, 270),
                    new CellConfig(path5, 0, true),
                    new CellConfig(path4, 90),
                    new CellConfig(path2, 90),
                    new CellConfig(path, 0),
                    new CellConfig(path2, 270),
                    new CellConfig(path4, 270),
                    new CellConfig(path5, 180, true),
                    new CellConfig(path3, 90),
                    new CellConfig(path2, 0),
                    new CellConfig(path3, 0),
                    new CellConfig(path5, 0),
                    new CellConfig(path6, 90),
                    new CellConfig(path5, 90),
                    new CellConfig(path4, 0),
                    new CellConfig(path5, 270, true),
                    new CellConfig(path6, 0)
                };
    }

    private int getScrimNotEnrolledColor(int i, int i2) {
        if (mPrimaryCellIndices.isEmpty()) {
            return getScrimNotEnrolledColorWithoutPrimaryCells(i);
        }
        return getScrimNotEnrolledColorWithPrimaryCells(i2);
    }

    private int getScrimNotEnrolledColorWithoutPrimaryCells(int i) {
        return ColorUtils.setAlphaComponent(
                mScrimNotEnrolledDefaultColor, Math.round(getScrimNotEnrolledOpacity(i) * 255.0f));
    }

    private int getScrimNotEnrolledColorWithPrimaryCells(int i) {
        if (mPrimaryCellIndices.contains(i)) {
            return mScrimNotEnrolledPrimaryColor;
        }
        return mScrimNotEnrolledSecondaryColor;
    }

    private static float getScrimNotEnrolledOpacity(int i) {
        for (int length = SCRIM_OPACITY_THRESHOLDS.length - 1; length >= 0; length--) {
            if (i >= SCRIM_OPACITY_THRESHOLDS[length]) {
                return SCRIM_OPACITY_VALUES[length];
            }
        }
        return SCRIM_OPACITY_VALUES[0];
    }
}
