package com.google.android.settings.biometrics.face;

import android.content.Context;

public abstract class Utils {
    public static float dpToPx(Context context, int dp) {
        return dp * (context.getResources().getDisplayMetrics().densityDpi / 160.0f);
    }
}
