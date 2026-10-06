package com.google.android.settings.gestures.columbus;

import android.app.ActivityManager;
import android.content.Context;
import android.provider.Settings;

import com.android.settings.core.BasePreferenceController;

import com.google.android.settings.R;

public class ColumbusPreferenceController extends BasePreferenceController {
    static final String FEATURE_QUICK_TAP = "com.google.android.feature.QUICK_TAP";

    public ColumbusPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        return isColumbusSupported(mContext) ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public CharSequence getSummary() {
        if (isColumbusEnabled(mContext)) {
            return mContext.getString(
                    R.string.columbus_summary,
                    mContext.getText(com.android.settings.R.string.gesture_setting_on),
                    ColumbusActionsPreferenceController.getColumbusAction(mContext));
        }
        return mContext.getText(com.android.settings.R.string.gesture_setting_off);
    }

    static boolean isColumbusSupported(Context context) {
        return context.getPackageManager().hasSystemFeature(FEATURE_QUICK_TAP);
    }

    static boolean isColumbusEnabled(Context context) {
        return Settings.Secure.getIntForUser(
                        context.getContentResolver(),
                        "columbus_enabled",
                        0,
                        ActivityManager.getCurrentUser())
                != 0;
    }
}
