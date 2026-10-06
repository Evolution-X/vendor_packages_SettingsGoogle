package com.google.android.settings.biometrics.face;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;

import androidx.fragment.app.FragmentActivity;

import com.android.settings.biometrics.BiometricUtils;
import com.android.settings.overlay.FeatureFactory;
import com.android.systemui.unfold.compat.ScreenSizeFoldProvider;
import com.android.systemui.unfold.updates.FoldProvider.FoldCallback;

import com.google.android.settings.R;
import com.google.android.setupcompat.template.FooterBarMixin;
import com.google.android.setupcompat.template.FooterButton;
import com.google.android.setupcompat.util.WizardManagerHelper;
import com.google.android.setupdesign.GlifLayout;
import com.google.android.setupdesign.util.ThemeHelper;

public class FaceEnrollParticipation extends FragmentActivity {
    private static final String TAG = "FaceEnrollParticipation";

    public static final int RESULT_INTERRUPTED = 3;

    private boolean mDebugConsent;
    private int mDevicePostureState;
    private IBinder mFaceService;
    private boolean mLaunchedPostureGuidance;
    private boolean mNextLaunched;
    private FooterButton mPrimaryButton;
    private ScreenSizeFoldProvider mScreenSizeFoldProvider;
    private int mUserId;
    private FoldCallback mFoldCallback = null;
    private Intent mPostureGuidanceIntent = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
        ThemeHelper.trySetDynamicColor(this);
        super.onCreate(savedInstanceState);
        if (!getPackageName().equals(getLaunchedFromPackage())) {
            Log.w(TAG, "Invalid caller: " + getLaunchedFromPackage());
            finish();
            return;
        }
        mUserId = getIntent().getIntExtra(Intent.EXTRA_USER_ID, UserHandle.myUserId());
        if (savedInstanceState != null) {
            mLaunchedPostureGuidance = savedInstanceState.getBoolean("launched_posture_guidance");
            mNextLaunched = savedInstanceState.getBoolean("next_launched");
        }
        setContentView(R.layout.face_enroll_participation);
        FooterBarMixin footerBarMixin = getLayout().getMixin(FooterBarMixin.class);
        FooterButton primaryButton =
                new FooterButton.Builder(this)
                        .setText(R.string.face_enrolling_confirm_help_debug)
                        .setListener(this::onButtonPositive)
                        .setButtonType(FooterButton.ButtonType.NEXT)
                        .setTheme(com.google.android.setupdesign.R.style.SudGlifButton_Primary)
                        .build();
        mPrimaryButton = primaryButton;
        primaryButton.setEnabled(false);
        footerBarMixin.setPrimaryButton(mPrimaryButton);
        footerBarMixin.setSecondaryButton(
                new FooterButton.Builder(this)
                        .setText(R.string.face_enrolling_skip_help_debug)
                        .setListener(this::onButtonNegative)
                        .setButtonType(FooterButton.ButtonType.SKIP)
                        .setTheme(com.google.android.setupdesign.R.style.SudGlifButton_Secondary)
                        .build());
        findViewById(R.id.agree_to_participate)
                .setOnClickListener(
                        view -> {
                            mPrimaryButton.setEnabled(((CheckBox) view).isChecked());
                        });
        mDebugConsent = false;
        getApplicationContext();
        IBinder service = ServiceManager.getService("face");
        mFaceService = service;
        if (service == null) {
            Log.e(TAG, "Could not connect to face service");
        }
        mPostureGuidanceIntent =
                FeatureFactory.getFeatureFactory()
                        .getFaceFeatureProvider()
                        .getPostureGuidanceIntent(getApplicationContext());
    }

    @Override
    protected void onApplyThemeResource(Resources.Theme theme, int resid, boolean first) {
        theme.applyStyle(com.android.settings.R.style.SetupWizardPartnerResource, true);
        super.onApplyThemeResource(theme, resid, first);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mPostureGuidanceIntent == null) {
            Log.d(TAG, "Device do not support posture guidance");
            return;
        }
        BiometricUtils.setDevicePosturesAllowEnroll(
                getResources()
                        .getInteger(
                                com.android.settings.R.integer
                                        .config_face_enroll_supported_posture));
        if (mFoldCallback == null) {
            mFoldCallback = this::onFoldUpdated;
        }
        if (mScreenSizeFoldProvider == null) {
            ScreenSizeFoldProvider provider = new ScreenSizeFoldProvider(getApplicationContext());
            mScreenSizeFoldProvider = provider;
            provider.registerCallback(mFoldCallback, getMainExecutor());
        }
    }

    void onFoldUpdated(boolean folded) {
        int devicePostureState = folded ? 1 : 3;
        mDevicePostureState = devicePostureState;
        if (!BiometricUtils.shouldShowPostureGuidance(devicePostureState, mLaunchedPostureGuidance)
                || mNextLaunched) {
            return;
        }
        launchPostureGuidance();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("launched_posture_guidance", mLaunchedPostureGuidance);
        outState.putBoolean("next_launched", mNextLaunched);
    }

    @Override
    protected void onStop() {
        super.onStop();
        ScreenSizeFoldProvider screenSizeFoldProvider = mScreenSizeFoldProvider;
        if (screenSizeFoldProvider != null) {
            screenSizeFoldProvider.unregisterCallback(mFoldCallback);
            mScreenSizeFoldProvider = null;
            mFoldCallback = null;
        }
        if (isChangingConfigurations()
                || mNextLaunched
                || WizardManagerHelper.isAnySetupWizard(getIntent())
                || BiometricUtils.isPostureGuidanceShowing(
                        mDevicePostureState, mLaunchedPostureGuidance)) {
            return;
        }
        setResult(RESULT_INTERRUPTED);
        finish();
    }

    private GlifLayout getLayout() {
        return (GlifLayout) findViewById(R.id.face_enroll_participation);
    }

    void onButtonPositive(View view) {
        Log.d(TAG, "Participant agreed to data collection");
        sendDebugMessageToFaceService("--enable");
        mDebugConsent = true;
        Settings.Secure.putIntForUser(
                getContentResolver(), Settings.Secure.BIOMETRIC_DEBUG_ENABLED, 1, mUserId);
        startEnrolling();
    }

    void onButtonNegative(View view) {
        sendDebugMessageToFaceService("--disable");
        Settings.Secure.putIntForUser(
                getContentResolver(), Settings.Secure.BIOMETRIC_DEBUG_ENABLED, 0, mUserId);
        startEnrolling();
    }

    void sendDebugMessageToFaceService(String message) {
        if (mFaceService != null) {
            try (java.io.FileOutputStream output = new java.io.FileOutputStream("/dev/null")) {
                mFaceService.dump(output.getFD(), new String[] {"--hal", message});
            } catch (RemoteException e) {
                e.printStackTrace();
            } catch (java.io.IOException e) {
                e.printStackTrace();
                Log.e(TAG, "IOException", e);
            }
        }
    }

    private void startEnrolling() {
        final Intent intent;
        boolean useTrafficLight =
                getResources().getBoolean(R.bool.config_face_enroll_use_traffic_light);
        if (!useTrafficLight) {
            intent = new Intent(this, FaceEnrollEnrolling.class);
        } else {
            intent =
                    new Intent(
                            "com.google.android.settings.future.biometrics.faceenroll.action.ENROLL");
        }
        if (useTrafficLight) {
            String packageName = getString(R.string.config_face_enroll_traffic_light_package);
            if (TextUtils.isEmpty(packageName)) {
                throw new IllegalStateException("Package name must not be empty");
            }
            intent.setPackage(packageName);
        }
        intent.putExtras(getIntent());
        intent.putExtra("debug_consent", mDebugConsent);
        startActivityForResult(intent, 1 /* enroll */);
        mNextLaunched = true;
    }

    private void launchPostureGuidance() {
        if (mPostureGuidanceIntent == null || mLaunchedPostureGuidance) {
            return;
        }
        BiometricUtils.copyMultiBiometricExtras(getIntent(), mPostureGuidanceIntent);
        startActivityForResult(mPostureGuidanceIntent, 7 /* posture guidance */);
        mLaunchedPostureGuidance = true;
        int stayAnimation = com.google.android.setupdesign.R.anim.sud_stay;
        overridePendingTransition(stayAnimation, stayAnimation);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 7) {
            mLaunchedPostureGuidance = false;
            if (resultCode == RESULT_CANCELED || resultCode == 2 /* posture guidance skipped */) {
                setResult(resultCode);
                finish();
            }
            return;
        }
        if (requestCode == 1) {
            setResult(resultCode, data);
            finish();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
