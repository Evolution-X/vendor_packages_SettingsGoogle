package com.google.android.settings.gestures.columbus;

import android.app.ActivityManager;
import android.app.IActivityManager;
import android.app.SynchronousUserSwitchObserver;
import android.app.UserSwitchObserver;
import android.app.settings.SettingsEnums;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.provider.Settings;
import android.util.Log;
import android.view.View;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.settings.SettingsActivity;
import com.android.settings.SubSettings;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;
import com.android.settingslib.widget.SelectorWithWidgetPreference;

import com.google.android.settings.R;

import java.util.HashMap;
import java.util.Map;

public class ColumbusActionsPreferenceController extends BasePreferenceController
        implements SelectorWithWidgetPreference.OnClickListener,
                LifecycleObserver,
                OnStart,
                OnStop {
    private static final String TAG = "ColumbusActionsPreference";
    static final String SECURE_KEY_COLUMBUS_ACTION = "columbus_action";
    static final String SECURE_KEY_COLUMBUS_LAUNCH_APP = "columbus_launch_app";
    private static final Uri COLUMBUS_ENABLED_URI = Settings.Secure.getUriFor("columbus_enabled");
    private static final Uri COLUMBUS_LAUNCH_APP_URI =
            Settings.Secure.getUriFor(SECURE_KEY_COLUMBUS_LAUNCH_APP);

    static final int[] ACTION_VALUE_RES_IDS = {
        R.string.columbus_setting_action_screenshot_value,
        R.string.columbus_setting_action_assistant_value,
        R.string.columbus_setting_action_play_pause_value,
        R.string.columbus_setting_action_overview_value,
        R.string.columbus_setting_action_notification_value,
        R.string.columbus_setting_action_flashlight_value,
        R.string.columbus_setting_action_launch_value,
    };
    static final int[] ACTION_TITLE_RES_IDS = {
        R.string.columbus_setting_action_screenshot_title,
        R.string.columbus_setting_action_assistant_title,
        R.string.columbus_setting_action_play_pause_title,
        R.string.columbus_setting_action_overview_title,
        R.string.columbus_setting_action_notification_title,
        R.string.columbus_setting_action_flashlight_title,
        R.string.columbus_setting_action_launch_title,
    };
    static final int[] ACTION_METRICS = {
        SettingsEnums.ACTION_COLUMBUS_ACTION_SCREENSHOT,
        SettingsEnums.ACTION_COLUMBUS_ACTION_ASSISTANT,
        SettingsEnums.ACTION_COLUMBUS_ACTION_PLAY_PAUSE,
        SettingsEnums.ACTION_COLUMBUS_ACTION_OVERVIEW,
        SettingsEnums.ACTION_COLUMBUS_ACTION_NOTIFICATION_SHADE,
        SettingsEnums.ACTION_COLUMBUS_ACTION_FLASHLIGHT,
        SettingsEnums.ACTION_COLUMBUS_ACTION_OPEN_APP,
    };
    static final ColumbusRadioButtonPreference.ContextualSummaryProvider[] ACTION_SUMMARIES = {
        null,
        null,
        null,
        null,
        null,
        null,
        ColumbusActionsPreferenceController::getLaunchAppSummary,
    };

    private static final Map<String, String> VALUE_TO_TITLE_MAP = new HashMap<>();
    private static String sDefaultAction;

    private final View.OnClickListener[] mActionExtraOnClick;
    private final Map<String, ColumbusRadioButtonPreference> mActionPreferences = new HashMap<>();
    private final IActivityManager mActivityManager;
    private final MetricsFeatureProvider mMetricsFeatureProvider;
    private PreferenceCategory mPreferenceCategory;
    private SettingObserver mSettingObserver;

    private final UserSwitchObserver mUserSwitchObserver =
            new SynchronousUserSwitchObserver() {
                @Override
                public void onUserSwitching(int newUserId) {
                    if (mSettingObserver != null) {
                        mSettingObserver.unregister(mContext.getContentResolver());
                        mSettingObserver.register(mContext.getContentResolver());
                    }
                }
            };

    public ColumbusActionsPreferenceController(Context context, String key) {
        super(context, key);
        mActivityManager = ActivityManager.getService();
        mMetricsFeatureProvider = FeatureFactory.getFeatureFactory().getMetricsFeatureProvider();
        mActionExtraOnClick =
                new View.OnClickListener[] {
                    null, null, null, null, null, null, v -> launchAppSelection(),
                };
    }

    private static CharSequence getLaunchAppSummary(Context context) {
        String app =
                Settings.Secure.getStringForUser(
                        context.getContentResolver(),
                        SECURE_KEY_COLUMBUS_LAUNCH_APP,
                        ActivityManager.getCurrentUser());
        if (app == null || app.isEmpty()) {
            return context.getString(R.string.columbus_setting_action_launch_summary_no_selection);
        }
        ComponentName component = ComponentName.unflattenFromString(app);
        if (component == null) {
            return context.getString(R.string.columbus_setting_action_launch_summary_no_selection);
        }
        PackageManager pm = context.getPackageManager();
        try {
            return pm.getApplicationLabel(pm.getActivityInfo(component, 0).applicationInfo);
        } catch (PackageManager.NameNotFoundException e) {
            return context.getString(R.string.columbus_setting_action_launch_summary_not_installed);
        }
    }

    private void launchAppSelection() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setClass(mContext, SubSettings.class);
        intent.putExtra(
                SettingsActivity.EXTRA_SHOW_FRAGMENT,
                ColumbusGestureLaunchSettingsFragment.class.getName());
        intent.putExtra(MetricsFeatureProvider.EXTRA_SOURCE_METRICS_CATEGORY, getMetricsCategory());
        mContext.startActivity(intent);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        if (!isAvailable()) {
            return;
        }
        mPreferenceCategory = screen.findPreference(getPreferenceKey());
        if (mPreferenceCategory != null) {
            mSettingObserver = new SettingObserver(mPreferenceCategory);
        }
        mActionPreferences.clear();
        VALUE_TO_TITLE_MAP.clear();
        for (int i = 0; i < ACTION_VALUE_RES_IDS.length; i++) {
            String value = mContext.getString(ACTION_VALUE_RES_IDS[i]);
            String title = mContext.getString(ACTION_TITLE_RES_IDS[i]);
            mActionPreferences.put(
                    value,
                    makeRadioPreference(
                            value,
                            title,
                            ACTION_SUMMARIES[i],
                            ACTION_METRICS[i],
                            mActionExtraOnClick[i]));
            VALUE_TO_TITLE_MAP.put(value, title);
            if (i == 0) {
                sDefaultAction = value;
            }
        }
    }

    private ColumbusRadioButtonPreference makeRadioPreference(
            String key,
            String title,
            ColumbusRadioButtonPreference.ContextualSummaryProvider summaryProvider,
            int metric,
            View.OnClickListener extraWidgetOnClickListener) {
        ColumbusRadioButtonPreference pref =
                new ColumbusRadioButtonPreference(mPreferenceCategory.getContext());
        pref.setKey(key);
        pref.setTitle(title);
        pref.setContextualSummaryProvider(summaryProvider);
        pref.updateSummary(mContext);
        pref.setMetric(metric);
        pref.setOnClickListener(this);
        pref.setExtraWidgetOnClickListener(extraWidgetOnClickListener);
        mPreferenceCategory.addPreference(pref);
        return pref;
    }

    @Override
    public int getAvailabilityStatus() {
        return ColumbusPreferenceController.isColumbusSupported(mContext)
                ? AVAILABLE
                : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public void onRadioButtonClicked(SelectorWithWidgetPreference emitter) {
        String key = emitter.getKey();
        if (key.equals(
                Settings.Secure.getStringForUser(
                        mContext.getContentResolver(),
                        SECURE_KEY_COLUMBUS_ACTION,
                        ActivityManager.getCurrentUser()))) {
            return;
        }
        Settings.Secure.putStringForUser(
                mContext.getContentResolver(),
                SECURE_KEY_COLUMBUS_ACTION,
                key,
                ActivityManager.getCurrentUser());
        updateState(mPreferenceCategory);
        if (emitter instanceof ColumbusRadioButtonPreference) {
            mMetricsFeatureProvider.action(
                    mContext, ((ColumbusRadioButtonPreference) emitter).getMetric());
        }
    }

    @Override
    public void updateState(Preference preference) {
        if (mActionPreferences.isEmpty()) {
            return;
        }
        String action =
                Settings.Secure.getStringForUser(
                        mContext.getContentResolver(),
                        SECURE_KEY_COLUMBUS_ACTION,
                        ActivityManager.getCurrentUser());
        if (action == null || !mActionPreferences.containsKey(action)) {
            action = sDefaultAction;
        }
        boolean columbusEnabled = ColumbusPreferenceController.isColumbusEnabled(mContext);
        for (ColumbusRadioButtonPreference pref : mActionPreferences.values()) {
            boolean checked = pref.getKey().equals(action);
            if (pref.isChecked() != checked) {
                pref.setChecked(checked);
            }
            pref.setEnabled(columbusEnabled);
            pref.updateSummary(mContext);
        }
    }

    @Override
    public void onStart() {
        try {
            mActivityManager.registerUserSwitchObserver(mUserSwitchObserver, TAG);
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to register user switch observer", e);
        }
        if (mSettingObserver != null) {
            mSettingObserver.register(mContext.getContentResolver());
            mSettingObserver.onChange(false);
        }
    }

    @Override
    public void onStop() {
        try {
            mActivityManager.unregisterUserSwitchObserver(mUserSwitchObserver);
        } catch (RemoteException e) {
            Log.e(TAG, "Failed  to unregister user switch observer", e);
        }
        if (mSettingObserver != null) {
            mSettingObserver.unregister(mContext.getContentResolver());
        }
    }

    static String getColumbusAction(Context context) {
        return columbusActionValueToTitle(
                Settings.Secure.getStringForUser(
                        context.getContentResolver(),
                        SECURE_KEY_COLUMBUS_ACTION,
                        ActivityManager.getCurrentUser()),
                context);
    }

    static String columbusActionValueToTitle(String value, Context context) {
        populateValueToTitleMapIfEmpty(context);
        if (value == null) {
            value = sDefaultAction;
        }
        return VALUE_TO_TITLE_MAP.getOrDefault(value, "");
    }

    private static void populateValueToTitleMapIfEmpty(Context context) {
        if (!VALUE_TO_TITLE_MAP.isEmpty()) {
            return;
        }
        for (int i = 0; i < ACTION_VALUE_RES_IDS.length; i++) {
            String value = context.getString(ACTION_VALUE_RES_IDS[i]);
            VALUE_TO_TITLE_MAP.put(value, context.getString(ACTION_TITLE_RES_IDS[i]));
            if (i == 0) {
                sDefaultAction = value;
            }
        }
    }

    private class SettingObserver extends ContentObserver {
        private final Preference mPreference;

        SettingObserver(Preference preference) {
            super(new Handler(Looper.myLooper()));
            mPreference = preference;
        }

        void register(ContentResolver contentResolver) {
            contentResolver.registerContentObserver(
                    COLUMBUS_ENABLED_URI, false, this, ActivityManager.getCurrentUser());
            contentResolver.registerContentObserver(
                    COLUMBUS_LAUNCH_APP_URI, false, this, ActivityManager.getCurrentUser());
        }

        void unregister(ContentResolver contentResolver) {
            contentResolver.unregisterContentObserver(this);
        }

        @Override
        public void onChange(boolean selfChange) {
            updateState(mPreference);
        }
    }
}
