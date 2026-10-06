package com.google.android.settings.gestures.columbus;

import android.app.settings.SettingsEnums;
import android.content.Context;

public class ColumbusLowSensitivityPreferenceController extends ColumbusTogglePreferenceController {
    public ColumbusLowSensitivityPreferenceController(Context context, String key) {
        super(context, key, SettingsEnums.ACTION_COLUMBUS_LOW_SENSITIVITY);
    }
}
