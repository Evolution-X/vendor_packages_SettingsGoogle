package com.google.android.settings.gestures.columbus;

import android.app.ActivityManager;
import android.app.settings.SettingsEnums;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.LauncherApps;
import android.content.pm.ShortcutInfo;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.DisplayMetrics;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.widget.SelectorWithWidgetPreference;

import com.google.android.settings.R;

import java.util.List;

public class ColumbusAppShortcutListPreferenceController extends BasePreferenceController
        implements SelectorWithWidgetPreference.OnClickListener {
    static final String COLUMBUS_LAUNCH_APP_SHORTCUT_SECURE_KEY = "columbus_launch_app_shortcut";

    private final LauncherApps mLauncherApps;
    private final MetricsFeatureProvider mMetricsFeatureProvider;
    private final String mOpenAppValue;
    private ComponentName mApplication;
    private int mCurrentUser;
    private PreferenceCategory mPreferenceCategory;
    private List<ShortcutInfo> mShortcutInfos;

    public ColumbusAppShortcutListPreferenceController(Context context, String key) {
        super(context, key);
        mLauncherApps = mContext.getSystemService(LauncherApps.class);
        mMetricsFeatureProvider = FeatureFactory.getFeatureFactory().getMetricsFeatureProvider();
        mOpenAppValue = mContext.getString(R.string.columbus_setting_action_launch_value);
    }

    @Override
    public int getAvailabilityStatus() {
        return ColumbusPreferenceController.isColumbusSupported(mContext)
                ? AVAILABLE
                : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        if (isAvailable()) {
            mCurrentUser = ActivityManager.getCurrentUser();
            mPreferenceCategory = screen.findPreference(getPreferenceKey());
            createShortcutList();
        }
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        int count = mPreferenceCategory.getPreferenceCount();
        if (count == 0) {
            return;
        }
        String shortcut =
                Settings.Secure.getStringForUser(
                        mContext.getContentResolver(),
                        COLUMBUS_LAUNCH_APP_SHORTCUT_SECURE_KEY,
                        mCurrentUser);
        for (int i = 0; i < count; i++) {
            Preference pref = mPreferenceCategory.getPreference(i);
            if (pref instanceof ColumbusRadioButtonPreference radioPref) {
                radioPref.setChecked(TextUtils.equals(shortcut, radioPref.getKey()));
            }
        }
    }

    @Override
    public void onRadioButtonClicked(SelectorWithWidgetPreference emitter) {
        if (!(emitter instanceof ColumbusRadioButtonPreference radioPref)) {
            return;
        }
        Settings.Secure.putStringForUser(
                mContext.getContentResolver(), "columbus_action", mOpenAppValue, mCurrentUser);
        Settings.Secure.putStringForUser(
                mContext.getContentResolver(),
                "columbus_launch_app",
                mApplication.flattenToString(),
                mCurrentUser);
        Settings.Secure.putStringForUser(
                mContext.getContentResolver(),
                COLUMBUS_LAUNCH_APP_SHORTCUT_SECURE_KEY,
                radioPref.getKey(),
                mCurrentUser);
        mMetricsFeatureProvider.action(
                mContext, SettingsEnums.ACTION_COLUMBUS_SELECT_APP_SHORTCUT, radioPref.getKey());
        updateState(mPreferenceCategory);
    }

    void setApplicationPackageAndShortcuts(
            ComponentName application, List<ShortcutInfo> shortcuts) {
        mApplication = application;
        mShortcutInfos = shortcuts;
        createShortcutList();
    }

    private void createShortcutList() {
        if (mApplication == null || mShortcutInfos == null || mPreferenceCategory == null) {
            return;
        }
        mPreferenceCategory.removeAll();
        Drawable appIcon =
                mLauncherApps
                        .getActivityList(mApplication.getPackageName(), UserHandle.of(mCurrentUser))
                        .stream()
                        .filter(info -> info.getComponentName().equals(mApplication))
                        .findFirst()
                        .map(info -> info.getIcon(DisplayMetrics.DENSITY_DEVICE_STABLE))
                        .orElse(null);
        makeRadioPreference(
                mApplication.flattenToString(),
                mContext.getString(R.string.columbus_setting_action_open_app_title),
                appIcon);
        for (ShortcutInfo shortcut : mShortcutInfos) {
            makeRadioPreference(
                    shortcut.getId(),
                    shortcut.getLabel(),
                    mLauncherApps.getShortcutIconDrawable(
                            shortcut, DisplayMetrics.DENSITY_DEVICE_STABLE));
        }
    }

    private void makeRadioPreference(String key, CharSequence title, Drawable icon) {
        ColumbusRadioButtonPreference pref = new ColumbusRadioButtonPreference(mContext);
        pref.setKey(key);
        pref.setTitle(title);
        pref.setIcon(icon);
        pref.setOnClickListener(this);
        mPreferenceCategory.addPreference(pref);
    }
}
