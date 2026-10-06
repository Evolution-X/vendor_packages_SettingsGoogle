package com.google.android.settings.gestures.columbus;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.android.settings.dashboard.DashboardFragment;

import com.google.android.settings.R;

public class ColumbusGestureLaunchSettingsFragment extends DashboardFragment
        implements ColumbusGestureHelper.GestureListener {
    private static final String TAG = "ColumbusLaunchSettings";

    private ColumbusGestureHelper mColumbusGestureHelper;
    private Context mContext;
    private Handler mHandler;

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SETTINGS_COLUMBUS_APP_SELECT;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mColumbusGestureHelper = new ColumbusGestureHelper(context);
        mContext = context;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        return R.xml.columbus_launch_settings;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }
}
