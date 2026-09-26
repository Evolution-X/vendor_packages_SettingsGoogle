package com.google.android.settings.fuelgauge.batterysaver;

import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;
import android.util.Pair;
import android.view.View;

import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;
import com.android.settingslib.widget.SelectorWithWidgetPreference;

public class BatterySaverModePreferenceController extends BasePreferenceController
        implements SelectorWithWidgetPreference.OnClickListener,
                LifecycleObserver,
                OnResume,
                OnPause {
    private static final String TAG = "BatterySaverModePreferenceController";
    private static final String KEY_BASIC_SAVER = "basic_battery_saver";
    private static final String KEY_EXTREME_SAVER = "extreme_battery_saver";

    SelectorWithWidgetPreference mBasicPreference;
    SelectorWithWidgetPreference mExtremePreference;
    private final ContentObserver mContentObserver;
    boolean mCurrentBatterySaverMode;
    private HandlerThread mHandlerThread;
    boolean mIsFlipendoAggressiveMode;
    boolean mIsFlipendoEnabled;
    private final Boolean mIsFlipendoInstalled;

    public BatterySaverModePreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mIsFlipendoInstalled = FlipendoUtils.isFlipendoInstalled(mContext);
        mContentObserver =
                new ContentObserver(new Handler(Looper.getMainLooper())) {
                    @Override
                    public void onChange(boolean selfChange) {
                        refreshFlipendoStates();
                        if (mIsFlipendoAggressiveMode) {
                            return;
                        }
                        updateSaverModeSelection(!mIsFlipendoEnabled);
                    }
                };
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        PreferenceCategory preferenceCategory = screen.findPreference(getPreferenceKey());
        if (!mIsFlipendoInstalled) {
            if (preferenceCategory != null) { // does not exist?
                preferenceCategory.setVisible(false);
            }
        } else if (preferenceCategory != null) {
            refreshFlipendoStates();
            initRadioButton(preferenceCategory);
        }
    }

    @Override
    public void onRadioButtonClicked(SelectorWithWidgetPreference preference) {
        String key = preference.getKey();
        if (KEY_EXTREME_SAVER.equals(key)) {
            updateSaverModeSelection(false);
        } else if (KEY_BASIC_SAVER.equals(key)) {
            updateSaverModeSelection(true);
        }
        if (mIsFlipendoEnabled && mExtremePreference != null) { // non-existent check?
            mCurrentBatterySaverMode = mExtremePreference.isChecked();
        }
    }

    @Override
    public void onResume() {
        if (mIsFlipendoInstalled) {
            try {
                mContext.getContentResolver()
                        .registerContentObserver(
                                FlipendoUtils.FLIPENDO_ENABLED_OBSERVABLE_URI,
                                false,
                                mContentObserver);
                if (mBasicPreference != null) {
                    mCurrentBatterySaverMode = mBasicPreference.isChecked();
                }
                refreshFlipendoStates();
                updateSaverModeSelection(!mIsFlipendoEnabled && !mIsFlipendoAggressiveMode);
            } catch (Exception e) {
                Log.e(TAG, "onResume() failed", e);
            }
        }
    }

    @Override
    public void onPause() {
        if (mIsFlipendoInstalled) {
            mContext.getContentResolver().unregisterContentObserver(mContentObserver);
            if (mBasicPreference == null
                    || mCurrentBatterySaverMode == mBasicPreference.isChecked()) {
                return;
            }
            if (!mIsFlipendoAggressiveMode
                    && mIsFlipendoEnabled
                    && mExtremePreference != null // ?
                    && mExtremePreference.isChecked()) {
                return;
            }
            mHandlerThread = new HandlerThread(TAG);
            mHandlerThread.start();
            new Handler(mHandlerThread.getLooper())
                    .post(
                            () ->
                                    updateBatterySaverMode(
                                            mContext, !mBasicPreference.isChecked() ? 1 : 0));
        }
    }

    private void initRadioButton(PreferenceCategory preferenceCategory) {
        mBasicPreference = preferenceCategory.findPreference(KEY_BASIC_SAVER);
        if (mBasicPreference != null) {
            mBasicPreference.setExtraWidgetOnClickListener(null);
            mBasicPreference.setOnClickListener(this);
            mBasicPreference.setChecked(!mIsFlipendoAggressiveMode);
        }
        mExtremePreference = preferenceCategory.findPreference(KEY_EXTREME_SAVER);
        if (mExtremePreference != null) {
            mExtremePreference.setExtraWidgetOnClickListener(this::launchFlipendo);
            mExtremePreference.setOnClickListener(this);
            mExtremePreference.setChecked(mIsFlipendoAggressiveMode);
        }
    }

    private void updateSaverModeSelection(boolean isBasicChecked) {
        if (mBasicPreference == null || mExtremePreference == null) {
            return;
        }
        mBasicPreference.setChecked(isBasicChecked);
        mExtremePreference.setChecked(!isBasicChecked);
    }

    private void refreshFlipendoStates() {
        Pair<Boolean, Boolean> flipendoState = FlipendoUtils.getFlipendoState(mContext);
        mIsFlipendoAggressiveMode = flipendoState.first;
        mIsFlipendoEnabled = flipendoState.second;
    }

    private void updateBatterySaverMode(Context context, int mode) {
        Bundle bundle = new Bundle();
        bundle.putInt("update_flipendo_mode", mode);
        try {
            context.getContentResolver()
                    .call(
                            FlipendoUtils.FLIPENDO_STATE_AUTHORITY,
                            "update_flipendo_mode_method",
                            null,
                            bundle);
        } catch (Exception e) {
            Log.e(TAG, "updateBatterySaverMode() failed", e);
        }
        if (mHandlerThread != null) {
            mHandlerThread.quitSafely();
            mHandlerThread = null;
        }
    }

    private void launchFlipendo(View view) {
        try {
            mContext.startActivity(
                    new Intent("android.settings.batterysaver.flipendo")
                            .setPackage("com.google.android.flipendo"));
        } catch (Exception e) {
            Log.e(TAG, "launchFlipendo() failed", e);
        }
    }
}
