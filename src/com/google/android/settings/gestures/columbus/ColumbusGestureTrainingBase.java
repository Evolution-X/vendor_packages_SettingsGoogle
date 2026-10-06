package com.google.android.settings.gestures.columbus;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;

import com.android.settings.SettingsActivity;
import com.android.settings.SetupWizardUtils;
import com.android.settings.SubSettings;
import com.android.settings.core.InstrumentedActivity;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

/** Base for the Quick Tap training flow, which keeps Quick Tap on while it is showing. */
public abstract class ColumbusGestureTrainingBase extends InstrumentedActivity
        implements ColumbusGestureHelper.GestureListener {
    static final String EXTRA_LAUNCHED_FROM = "launched_from";
    static final String FLOW_SETUP = "setup";
    static final String FLOW_DEFERRED_SETUP = "deferred_setup";
    static final String FLOW_ACCIDENTAL_TRIGGER = "accidental_trigger";

    // ??? Not a framework or setupcompat constant; the setup flow treats it as a skip.
    static final int RESULT_SKIP = 101;

    protected ColumbusGestureHelper mColumbusGestureHelper;
    private boolean mColumbusWasEnabled;
    private boolean mEnableColumbusOnPause;
    private String mLaunchedFrom;

    @Override
    public void onTrigger() {}

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mColumbusGestureHelper = new ColumbusGestureHelper(getApplicationContext());
        mColumbusGestureHelper.setListener(this);
        mLaunchedFrom = getIntent().getStringExtra(EXTRA_LAUNCHED_FROM);
    }

    @Override
    public void onResume() {
        super.onResume();
        mColumbusWasEnabled =
                ColumbusPreferenceController.isColumbusEnabled(getApplicationContext());
        if (!mColumbusWasEnabled) {
            Settings.Secure.putIntForUser(
                    getContentResolver(), "columbus_enabled", 1, ActivityManager.getCurrentUser());
        }
        mColumbusGestureHelper.bindToColumbusServiceProxy();
        mColumbusGestureHelper.setListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        mColumbusGestureHelper.setListener(null);
        mColumbusGestureHelper.unbindFromColumbusServiceProxy();
        if (!mEnableColumbusOnPause && !mColumbusWasEnabled) {
            Settings.Secure.putIntForUser(
                    getContentResolver(), "columbus_enabled", 0, ActivityManager.getCurrentUser());
        }
    }

    protected void launchColumbusGestureSettings(int sourceMetricsCategory) {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setClass(this, SubSettings.class);
        intent.putExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT, ColumbusSettings.class.getName());
        intent.putExtra(
                MetricsFeatureProvider.EXTRA_SOURCE_METRICS_CATEGORY, sourceMetricsCategory);
        startActivity(intent);
    }

    /** Starts the next step of the flow, forwarding the flow type and setup extras. */
    protected void startNextStep(Class<? extends Activity> step, int requestCode) {
        Intent intent = new Intent(this, step);
        intent.putExtra(EXTRA_LAUNCHED_FROM, getIntent().getStringExtra(EXTRA_LAUNCHED_FROM));
        SetupWizardUtils.copySetupExtras(getIntent(), intent);
        startActivityForResult(intent, requestCode);
    }

    protected boolean flowTypeSetup() {
        return FLOW_SETUP.contentEquals(mLaunchedFrom);
    }

    protected boolean flowTypeDeferredSetup() {
        return FLOW_DEFERRED_SETUP.contentEquals(mLaunchedFrom);
    }

    protected boolean flowTypeAccidentalTrigger() {
        return FLOW_ACCIDENTAL_TRIGGER.contentEquals(mLaunchedFrom);
    }

    protected void handleDone() {
        setResult(RESULT_OK);
        mColumbusGestureHelper.setListener(null);
        finishAndRemoveTask();
    }

    protected void setEnableColumbusOnPause() {
        mEnableColumbusOnPause = true;
    }
}
