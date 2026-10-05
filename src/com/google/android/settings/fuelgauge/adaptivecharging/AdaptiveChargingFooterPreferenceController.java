package com.google.android.settings.fuelgauge.adaptivecharging;

import android.content.Context;
import android.text.TextUtils;

import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.HelpUtils;
import com.android.settingslib.widget.FooterPreference;

import com.google.android.settings.R;

public class AdaptiveChargingFooterPreferenceController extends BasePreferenceController {
    private FooterPreference mPreference;

    public AdaptiveChargingFooterPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        mPreference = preferenceScreen.findPreference(getPreferenceKey());
        setupFooter();
    }

    void setupFooter() {
        if (TextUtils.isEmpty(mContext.getString(R.string.help_url_adaptive_charging_settings))) {
            return;
        }
        addHelpLink();
    }

    void addHelpLink() {
        if (mPreference != null) {
            mPreference.setLearnMoreAction(
                    view -> {
                        Context context = mContext;
                        context.startActivity(
                                HelpUtils.getHelpIntent(
                                        context,
                                        context.getString(
                                                R.string.help_url_adaptive_charging_settings),
                                        ""));
                    });
            mPreference.setLearnMoreText(mContext.getString(R.string.adaptive_charging_link_a11y));
        }
    }
}
