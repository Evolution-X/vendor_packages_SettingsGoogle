package com.google.android.settings.gestures.columbus;

import android.app.ActivityManager;
import android.app.IActivityManager;
import android.app.SynchronousUserSwitchObserver;
import android.app.UserSwitchObserver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.provider.Settings;
import android.util.Log;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.TwoStatePreference;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

/** Toggle backed by a Secure setting that is only enabled while Quick Tap is on. */
public abstract class ColumbusTogglePreferenceController extends TogglePreferenceController
        implements LifecycleObserver, OnStart, OnStop {
    private static final String TAG = "ColumbusTogglePreference";
    private static final Uri COLUMBUS_ENABLED_URI = Settings.Secure.getUriFor("columbus_enabled");

    private final IActivityManager mActivityManager;
    private final MetricsFeatureProvider mMetricsFeatureProvider;
    private SettingObserver mSettingObserver;
    private TwoStatePreference mSwitchPreference;

    private final UserSwitchObserver mUserSwitchObserver =
            new SynchronousUserSwitchObserver() {
                @Override
                public void onUserSwitching(int newUserId) {
                    if (mSettingObserver != null) {
                        mSettingObserver.unregister();
                        mSettingObserver.register();
                    }
                }
            };

    public ColumbusTogglePreferenceController(Context context, String key, int metricsCategory) {
        super(context, key);
        mActivityManager = ActivityManager.getService();
        mMetricsFeatureProvider = FeatureFactory.getFeatureFactory().getMetricsFeatureProvider();
        setMetricsCategory(metricsCategory);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mSwitchPreference = screen.findPreference(getPreferenceKey());
        if (mSwitchPreference == null) {
            return;
        }
        mSettingObserver = new SettingObserver(mSwitchPreference);
    }

    @Override
    public boolean isChecked() {
        return Settings.Secure.getIntForUser(
                        mContext.getContentResolver(),
                        getPreferenceKey(),
                        0,
                        ActivityManager.getCurrentUser())
                != 0;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        mMetricsFeatureProvider.action(mContext, getMetricsCategory(), isChecked);
        return Settings.Secure.putIntForUser(
                mContext.getContentResolver(),
                getPreferenceKey(),
                isChecked ? 1 : 0,
                ActivityManager.getCurrentUser());
    }

    @Override
    public int getAvailabilityStatus() {
        return ColumbusPreferenceController.isColumbusSupported(mContext)
                ? AVAILABLE
                : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public void onStart() {
        try {
            mActivityManager.registerUserSwitchObserver(mUserSwitchObserver, TAG);
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to register user switch observer", e);
        }
        if (mSettingObserver != null) {
            mSettingObserver.register();
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
            mSettingObserver.unregister();
        }
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        if (mSwitchPreference != null) {
            mSwitchPreference.setEnabled(ColumbusPreferenceController.isColumbusEnabled(mContext));
        }
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_system;
    }

    private class SettingObserver extends ContentObserver {
        private final Preference mPreference;

        SettingObserver(Preference preference) {
            super(new Handler(Looper.myLooper()));
            mPreference = preference;
        }

        void register() {
            mContext.getContentResolver()
                    .registerContentObserver(
                            COLUMBUS_ENABLED_URI, false, this, ActivityManager.getCurrentUser());
        }

        void unregister() {
            mContext.getContentResolver().unregisterContentObserver(this);
        }

        @Override
        public void onChange(boolean selfChange) {
            updateState(mPreference);
        }
    }
}
