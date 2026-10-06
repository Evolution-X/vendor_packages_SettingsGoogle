package com.google.android.settings.gestures.columbus;

import android.app.settings.SettingsEnums;
import android.content.Context;

import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

import com.google.android.settings.R;

@SearchIndexable
public class ColumbusSettings extends DashboardFragment {
    private static final String TAG = "ColumbusSettings";

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.columbus_settings) {
                @Override
                protected boolean isPageSearchEnabled(Context context) {
                    return ColumbusPreferenceController.isColumbusSupported(context);
                }
            };

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SETTINGS_COLUMBUS;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.columbus_settings;
    }
}
