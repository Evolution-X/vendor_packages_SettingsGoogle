package com.google.android.settings.gestures.columbus;

import android.app.ActivityManager;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.content.pm.ShortcutInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.widget.SelectorWithWidgetPreference;

import com.google.android.settings.R;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ColumbusAppListPreferenceController extends BasePreferenceController
        implements SelectorWithWidgetPreference.OnClickListener, LifecycleObserver, OnStart {
    private static final String TAG = "ColumbusAppListPrefCtrl";
    static final String COLUMBUS_LAUNCH_APP_SECURE_KEY = "columbus_launch_app";
    static final String COLUMBUS_APP_SHORTCUTS_KEY = "columbus_app_shortcuts";

    private final LauncherApps mLauncherApps;
    private final MetricsFeatureProvider mMetricsFeatureProvider;
    private final String mOpenAppValue;
    private int mCurrentUser;
    private PreferenceCategory mPreferenceCategory;

    public ColumbusAppListPreferenceController(Context context, String key) {
        super(context, key);
        mLauncherApps = mContext.getSystemService(LauncherApps.class);
        mOpenAppValue = mContext.getString(R.string.columbus_setting_action_launch_value);
        mMetricsFeatureProvider = FeatureFactory.getFeatureFactory().getMetricsFeatureProvider();
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
            updateAppList();
        }
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        int count = mPreferenceCategory.getPreferenceCount();
        if (count == 0) {
            return;
        }
        String launchApp =
                Settings.Secure.getStringForUser(
                        mContext.getContentResolver(),
                        COLUMBUS_LAUNCH_APP_SECURE_KEY,
                        mCurrentUser);
        for (int i = 0; i < count; i++) {
            Preference pref = mPreferenceCategory.getPreference(i);
            if (pref instanceof ColumbusRadioButtonPreference radioPref) {
                radioPref.setChecked(TextUtils.equals(launchApp, radioPref.getKey()));
            }
        }
    }

    @Override
    public void onStart() {
        updateAppList();
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
                COLUMBUS_LAUNCH_APP_SECURE_KEY,
                radioPref.getKey(),
                mCurrentUser);
        Settings.Secure.putStringForUser(
                mContext.getContentResolver(),
                "columbus_launch_app_shortcut",
                radioPref.getKey(),
                mCurrentUser);
        mMetricsFeatureProvider.action(
                mContext, SettingsEnums.ACTION_COLUMBUS_SELECT_APP, radioPref.getKey());
        updateState(mPreferenceCategory);
    }

    private void updateAppList() {
        if (mPreferenceCategory == null) {
            return;
        }
        mPreferenceCategory.removeAll();
        List<LauncherActivityInfo> apps =
                mLauncherApps.getActivityList(null, UserHandle.of(mCurrentUser));
        apps.sort(Comparator.comparing(app -> app.getLabel().toString()));
        List<ShortcutInfo> shortcuts = queryForShortcuts();
        for (LauncherActivityInfo app : apps) {
            ArrayList<ShortcutInfo> appShortcuts =
                    shortcuts.stream()
                            .filter(
                                    s ->
                                            s.getPackage()
                                                    .equals(
                                                            app.getComponentName()
                                                                    .getPackageName()))
                            .collect(Collectors.toCollection(ArrayList::new));
            Bundle extras = new Bundle();
            extras.putParcelable(COLUMBUS_LAUNCH_APP_SECURE_KEY, app.getComponentName());
            extras.putParcelableArrayList(COLUMBUS_APP_SHORTCUTS_KEY, appShortcuts);
            makeRadioPreference(
                    app.getComponentName().flattenToString(),
                    app.getLabel(),
                    app.getIcon(DisplayMetrics.DENSITY_DEVICE_STABLE),
                    appShortcuts.isEmpty() ? null : v -> launchShortcutSelection(extras));
        }
    }

    private void launchShortcutSelection(Bundle extras) {
        new SubSettingLauncher(mContext)
                .setDestination(ColumbusGestureLaunchAppShortcutSettingsFragment.class.getName())
                .setSourceMetricsCategory(SettingsEnums.SETTINGS_COLUMBUS_APP_SELECT)
                .setExtras(extras)
                .launch();
    }

    private List<ShortcutInfo> queryForShortcuts() {
        LauncherApps.ShortcutQuery query = new LauncherApps.ShortcutQuery();
        query.setQueryFlags(
                LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC
                        | LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST);
        List<ShortcutInfo> shortcuts = null;
        try {
            shortcuts = mLauncherApps.getShortcuts(query, UserHandle.of(mCurrentUser));
        } catch (IllegalStateException | SecurityException e) {
            Log.e(TAG, "Failed to query for shortcuts", e);
        }
        return shortcuts == null ? new ArrayList<>() : shortcuts;
    }

    private void makeRadioPreference(
            String key, CharSequence title, Drawable icon, View.OnClickListener extraOnClick) {
        ColumbusRadioButtonPreference pref =
                new ColumbusRadioButtonPreference(mPreferenceCategory.getContext());
        pref.setKey(key);
        pref.setTitle(title);
        pref.setIcon(icon);
        pref.setOnClickListener(this);
        pref.setExtraWidgetOnClickListener(extraOnClick);
        mPreferenceCategory.addPreference(pref);
    }
}
