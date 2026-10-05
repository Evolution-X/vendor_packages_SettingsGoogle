package com.google.android.settings.fuelgauge.adaptivecharging;

import android.content.Intent;

import com.android.settings.SettingsActivity;
import com.android.settings.fuelgauge.SmartBatterySettings;

public class AdaptiveChargingSettingsActivity extends SettingsActivity {
    @Override
    public Intent getIntent() {
        Intent intent = super.getIntent();
        intent.putExtra(EXTRA_SHOW_FRAGMENT, SmartBatterySettings.class.getName());
        return intent;
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return SmartBatterySettings.class.getName().equals(fragmentName);
    }
}
