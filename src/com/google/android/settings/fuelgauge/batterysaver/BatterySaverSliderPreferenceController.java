package com.google.android.settings.fuelgauge.batterysaver;

import android.content.Context;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;

import com.android.settings.R;
import com.android.settingslib.Utils;
import com.android.settingslib.widget.SliderPreference;

class BatterySaverSliderPreferenceController implements Preference.OnPreferenceChangeListener {
    private final Context mContext;
    int mPercentage;
    SliderPreference mSliderPreference;

    BatterySaverSliderPreferenceController(Context context) {
        mContext = context;
        SliderPreference sliderPreference = new SliderPreference(context);
        mSliderPreference = sliderPreference;
        sliderPreference.setOrder(50);
        mSliderPreference.setMax(15);
        mSliderPreference.setMin(4);
        mSliderPreference.setKey("battery_saver_seek_bar");
        mSliderPreference.setSliderIncrement(1);
        mSliderPreference.setTickVisible(false);
        mSliderPreference.setHapticFeedbackMode(SliderPreference.HAPTIC_FEEDBACK_MODE_ON_TICKS);
        mSliderPreference.setUpdatesContinuously(true);
        mSliderPreference.setOnPreferenceChangeListener(this);
    }

    void updateSliderPreference(
            PreferenceCategory preferenceCategory, String scheduleKey, int threshold) {
        if ("key_battery_saver_percentage".equals(scheduleKey)) {
            mSliderPreference.setValue(Math.max(threshold / 5, 4));
            mPercentage = mSliderPreference.getValue() * 5;
            mSliderPreference.setTitle(formatStateDescription());
            preferenceCategory.addPreference(mSliderPreference);
            return;
        }
        preferenceCategory.removePreference(mSliderPreference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        int percentage = (Integer) newValue * 5;
        if (percentage <= 0 || percentage == mPercentage) {
            return true;
        }
        mPercentage = percentage;
        Settings.Global.putInt(
                mContext.getContentResolver(),
                Settings.Global.LOW_POWER_MODE_TRIGGER_LEVEL,
                mPercentage);
        mSliderPreference.setTitle(formatStateDescription());
        return true;
    }

    private CharSequence formatStateDescription() {
        return mContext.getString(
                R.string.battery_saver_seekbar_title, Utils.formatPercentage(mPercentage));
    }
}
