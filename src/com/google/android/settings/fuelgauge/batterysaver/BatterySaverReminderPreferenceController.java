package com.google.android.settings.fuelgauge.batterysaver;

import android.content.Context;
import android.provider.Settings;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;

public class BatterySaverReminderPreferenceController extends TogglePreferenceController {
    public BatterySaverReminderPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public boolean isChecked() {
        return Settings.Global.getInt(
                        mContext.getContentResolver(),
                        Settings.Global.LOW_POWER_MODE_REMINDER_ENABLED,
                        1)
                == 1;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        Settings.Global.putInt(
                mContext.getContentResolver(),
                Settings.Global.LOW_POWER_MODE_REMINDER_ENABLED,
                isChecked ? 1 : 0);
        return true;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_battery;
    }
}
