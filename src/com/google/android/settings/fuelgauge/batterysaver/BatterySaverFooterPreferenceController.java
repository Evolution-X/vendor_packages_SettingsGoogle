package com.google.android.settings.fuelgauge.batterysaver;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;

import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.HelpUtils;
import com.android.settingslib.widget.FooterPreference;

import com.google.android.settings.R;

public class BatterySaverFooterPreferenceController extends BasePreferenceController {
    private FooterPreference mPreference;

    public BatterySaverFooterPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
        setupFooter();
    }

    void setupFooter() {
        if (TextUtils.isEmpty(mContext.getString(R.string.help_url_battery_saver_settings))) {
            return;
        }
        addHelpLink();
    }

    void addHelpLink() {
        if (mPreference != null) {
            mPreference.setLearnMoreAction(this::onHelpLinkClicked);
            mPreference.setLearnMoreText(
                    mContext.getString(com.android.settings.R.string.battery_saver_link_a11y));
        }
    }

    private void onHelpLinkClicked(View view) {
        mContext.startActivity(
                HelpUtils.getHelpIntent(
                        mContext,
                        mContext.getString(R.string.help_url_battery_saver_settings),
                        ""));
    }
}
