package com.google.android.settings.gestures.columbus;

import com.android.settings.SettingsActivity;

public class ColumbusSettingsActivity extends SettingsActivity {
    @Override
    protected boolean isValidFragment(String fragmentName) {
        return ColumbusSettings.class.getName().equals(fragmentName);
    }
}
