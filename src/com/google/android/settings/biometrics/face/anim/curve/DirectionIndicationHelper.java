package com.google.android.settings.biometrics.face.anim.curve;

public class DirectionIndicationHelper {
    private static final int[][] BUCKET_PRIORITY = {
        {12, 0},
        {2, 0},
        {7, 0},
        {3, 23},
        {4, 45},
        {8, 45},
        {9, 68},
        {14, 90},
        {13, 90},
        {19, 113},
        {24, 135},
        {18, 135},
        {23, 158},
        {22, 180},
        {17, 180},
        {21, 203},
        {20, 225},
        {16, 225},
        {15, 248},
        {10, 270},
        {11, 270},
        {5, 293},
        {0, 315},
        {6, 315},
        {1, 338}
    };

    public int getNoProgressPulseAngle(boolean[] zArr) {
        for (int i = 1; i < BUCKET_PRIORITY.length; i++) {
            int[] iArr2 = BUCKET_PRIORITY[i];
            if (!zArr[iArr2[0]]) {
                return iArr2[1];
            }
        }
        return 0;
    }

    public int getNoProgressBucket(boolean[] zArr) {
        for (int i = 1; i < BUCKET_PRIORITY.length; i++) {
            int i2 = BUCKET_PRIORITY[i][0];
            if (!zArr[i2]) {
                return i2;
            }
        }
        return 0;
    }
}
