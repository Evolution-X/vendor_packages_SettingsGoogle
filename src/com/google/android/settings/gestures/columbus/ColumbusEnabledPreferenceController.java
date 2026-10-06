package com.google.android.settings.gestures.columbus;

import android.app.ActivityManager;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.widget.Toast;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;
import com.android.settingslib.widget.MainSwitchPreference;

import com.google.android.settings.R;

public class ColumbusEnabledPreferenceController extends BasePreferenceController
        implements Preference.OnPreferenceChangeListener,
                ColumbusGestureHelper.GestureListener,
                LifecycleObserver,
                OnPause,
                OnResume {
    static final String SECURE_KEY_COLUMBUS_ENABLED = "columbus_enabled";

    private final ColumbusGestureHelper mColumbusGestureHelper;
    private final MetricsFeatureProvider mMetricsFeatureProvider;
    private Handler mHandler;
    private MainSwitchPreference mSwitchBar;

    public ColumbusEnabledPreferenceController(Context context, String key) {
        super(context, key);
        mMetricsFeatureProvider = FeatureFactory.getFeatureFactory().getMetricsFeatureProvider();
        mColumbusGestureHelper = new ColumbusGestureHelper(context);
        if (Looper.myLooper() != null) {
            mHandler = new Handler(Looper.myLooper());
        }
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        if (isAvailable()) {
            mSwitchBar = screen.findPreference(getPreferenceKey());
        }
    }

    @Override
    public void updateState(Preference preference) {
        if (mSwitchBar != null) {
            mSwitchBar.setChecked(ColumbusPreferenceController.isColumbusEnabled(mContext));
        }
    }

    @Override
    public int getAvailabilityStatus() {
        return ColumbusPreferenceController.isColumbusSupported(mContext)
                ? AVAILABLE
                : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean enabled = (Boolean) newValue;
        Settings.Secure.putIntForUser(
                mContext.getContentResolver(),
                SECURE_KEY_COLUMBUS_ENABLED,
                enabled ? 1 : 0,
                ActivityManager.getCurrentUser());
        mMetricsFeatureProvider.action(
                mContext,
                enabled
                        ? SettingsEnums.ACTION_COLUMBUS_ENABLED
                        : SettingsEnums.ACTION_COLUMBUS_DISABLED);
        return true;
    }

    @Override
    public void onResume() {
        mColumbusGestureHelper.bindToColumbusServiceProxy();
        mColumbusGestureHelper.setListener(this);
    }

    @Override
    public void onPause() {
        mColumbusGestureHelper.setListener(null);
        mColumbusGestureHelper.unbindFromColumbusServiceProxy();
    }

    @Override
    public void onTrigger() {
        if (mHandler == null) {
            return;
        }
        mHandler.post(
                () ->
                        Toast.makeText(
                                        mSwitchBar.getContext(),
                                        R.string.columbus_gesture_detected,
                                        Toast.LENGTH_SHORT)
                                .show());
    }
}
