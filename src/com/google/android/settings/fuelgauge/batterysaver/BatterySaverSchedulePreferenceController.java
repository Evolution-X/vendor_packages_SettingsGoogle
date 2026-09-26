package com.google.android.settings.fuelgauge.batterysaver;

import android.content.Context;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import androidx.preference.TwoStatePreference;

import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.fuelgauge.BatterySaverUtils;

public class BatterySaverSchedulePreferenceController extends BasePreferenceController
        implements Preference.OnPreferenceChangeListener {
    static final int DEFAULT_MIN_SCHEDULE_THRESHOLD = 20;
    static final int DEFAULT_THRESHOLD = 0;
    public static final String KEY_BATTERY_SAVER_SCHEDULE = "battery_saver_base_on_percentage";

    private PreferenceCategory mPreferenceCategory;
    BatterySaverSliderPreferenceController mSliderPreferenceController;
    private final int mThreshold;

    public BatterySaverSchedulePreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mSliderPreferenceController = new BatterySaverSliderPreferenceController(context);
        mThreshold =
                Settings.Global.getInt(
                        context.getContentResolver(),
                        Settings.Global.LOW_POWER_MODE_TRIGGER_LEVEL,
                        0);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreferenceCategory = screen.findPreference(getPreferenceKey());
        if (mPreferenceCategory != null) {
            initPreferences();
        }
    }

    private void initPreferences() {
        TwoStatePreference preference =
                mPreferenceCategory.findPreference(KEY_BATTERY_SAVER_SCHEDULE);
        if (preference != null) {
            preference.setOnPreferenceChangeListener(this);
            preference.setChecked(
                    BatterySaverUtils.getBatterySaverScheduleKey(mContext)
                            .equals("key_battery_saver_percentage"));
            mSliderPreferenceController.updateSliderPreference(
                    mPreferenceCategory,
                    BatterySaverUtils.getBatterySaverScheduleKey(mContext),
                    mThreshold);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (!KEY_BATTERY_SAVER_SCHEDULE.equals(preference.getKey())) {
            return true;
        }
        boolean isChecked = (Boolean) newValue;
        BatterySaverUtils.setBatterySaverScheduleMode(
                mContext,
                "key_battery_saver_percentage",
                isChecked ? DEFAULT_MIN_SCHEDULE_THRESHOLD : DEFAULT_THRESHOLD);
        mSliderPreferenceController.updateSliderPreference(
                mPreferenceCategory,
                isChecked ? "key_battery_saver_percentage" : "key_battery_saver_no_schedule",
                isChecked ? DEFAULT_MIN_SCHEDULE_THRESHOLD : DEFAULT_THRESHOLD);
        return true;
    }
}
