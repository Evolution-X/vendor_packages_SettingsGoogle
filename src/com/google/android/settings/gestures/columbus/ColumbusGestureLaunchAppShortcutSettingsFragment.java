package com.google.android.settings.gestures.columbus;

import android.app.settings.SettingsEnums;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ShortcutInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.android.settings.dashboard.DashboardFragment;

import com.google.android.settings.R;

public class ColumbusGestureLaunchAppShortcutSettingsFragment extends DashboardFragment
        implements ColumbusGestureHelper.GestureListener {
    private static final String TAG = "ColumbusAppShortcutSettings";

    private ColumbusGestureHelper mColumbusGestureHelper;
    private Context mContext;
    private Handler mHandler;

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SETTINGS_COLUMBUS_APP_SHORTCUT_SELECT;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
        mColumbusGestureHelper = new ColumbusGestureHelper(context);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        use(ColumbusAppShortcutListPreferenceController.class)
                .setApplicationPackageAndShortcuts(
                        getIntent()
                                .getParcelableExtra(
                                        ColumbusAppListPreferenceController
                                                .COLUMBUS_LAUNCH_APP_SECURE_KEY,
                                        ComponentName.class),
                        getIntent()
                                .getParcelableArrayListExtra(
                                        ColumbusAppListPreferenceController
                                                .COLUMBUS_APP_SHORTCUTS_KEY,
                                        ShortcutInfo.class));
        mHandler = new Handler(Looper.myLooper());
    }

    @Override
    public void onResume() {
        super.onResume();
        mColumbusGestureHelper.bindToColumbusServiceProxy();
        mColumbusGestureHelper.setListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        mColumbusGestureHelper.setListener(null);
        mColumbusGestureHelper.unbindFromColumbusServiceProxy();
        finish();
    }

    @Override
    public void onTrigger() {
        mHandler.post(
                () ->
                        Toast.makeText(
                                        mContext,
                                        R.string.columbus_gesture_detected,
                                        Toast.LENGTH_SHORT)
                                .show());
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.columbus_launch_app_shortcut_settings;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }
}
