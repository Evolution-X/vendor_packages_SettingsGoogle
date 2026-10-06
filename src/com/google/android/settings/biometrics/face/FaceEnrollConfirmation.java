package com.google.android.settings.biometrics.face;

import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.hardware.biometrics.BiometricManager;
import android.hardware.display.AmbientDisplayConfiguration;
import android.hardware.face.FaceManager;
import android.hardware.face.FaceSensorProperties;
import android.os.Bundle;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;

import androidx.fragment.app.FragmentActivity;

import com.android.settings.SetupWizardUtils;
import com.android.settings.Utils;
import com.android.settings.biometrics.BiometricsOnboardingProto.OnboardingAction;
import com.android.settings.biometrics.BiometricsOnboardingProto.OnboardingScreen;
import com.android.settings.biometrics.face.FaceAttentionController;
import com.android.settings.biometrics.metrics.BiometricsLogger;
import com.android.settings.biometrics.metrics.OnboardingEvent;
import com.android.settings.biometrics.metrics.OnboardingScreenInfoEvent;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.widget.LottieColorUtils;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.settings.R;
import com.google.android.setupcompat.template.FooterBarMixin;
import com.google.android.setupcompat.template.FooterButton;
import com.google.android.setupcompat.util.WizardManagerHelper;
import com.google.android.setupdesign.GlifLayout;
import com.google.android.setupdesign.template.FloatingBackButtonMixin;
import com.google.android.setupdesign.transition.TransitionHelper;
import com.google.android.setupdesign.util.DynamicColorPalette;
import com.google.android.setupdesign.util.LottieAnimationHelper;
import com.google.android.setupdesign.util.ThemeHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FaceEnrollConfirmation extends FragmentActivity implements OnBackInvokedCallback {
    private static final String TAG = "FaceEnrollConfirmation";

    static final int REQUEST_FACE_SETTINGS = 1;

    protected ArrayList<Integer> mActions = new ArrayList<>();
    private AmbientDisplayConfiguration mAmbientDisplayConfig;
    protected BiometricsLogger mBiometricsLogger;
    private FaceAttentionController mFaceAttentionController;
    private FaceEnrollLockScreenBypassToggle mFaceEnrollLockScreenBypassToggle;
    private FooterBarMixin mFooterBarMixin;
    private boolean mGazeEnabled;
    private PorterDuffColorFilter mIconColorFilter;
    private boolean mIsUsingExpressStyle;
    private LottieAnimationView mLockScreenBypassLottie;
    protected OnboardingEvent mOnboardingEvent;
    private boolean mShowIllustration;
    private boolean mShowLockScreenBypass;
    protected long mStartTimeMillis;
    protected byte[] mToken;
    protected int mUserId;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (ThemeHelper.shouldApplyGlifExpressiveStyle(getApplicationContext())) {
            if (!ThemeHelper.trySetSuwTheme(this)) {
                setTheme(ThemeHelper.getSuwDefaultTheme(getApplicationContext()));
                ThemeHelper.trySetDynamicColor(this);
            }
        } else {
            setTheme(SetupWizardUtils.getTheme(this, getIntent()));
            ThemeHelper.trySetDynamicColor(this);
        }
        mIsUsingExpressStyle = ThemeHelper.shouldApplyGlifExpressiveStyle(getApplicationContext());
        super.onCreate(savedInstanceState);
        mToken = getIntent().getByteArrayExtra("hw_auth_token");
        mUserId = getIntent().getIntExtra(Intent.EXTRA_USER_ID, UserHandle.myUserId());
        getIntent().putExtra("finished_enrolling_face", true);
        if (savedInstanceState != null) {
            if (mToken == null) {
                mToken = savedInstanceState.getByteArray("hw_auth_token");
            }
            mUserId = savedInstanceState.getInt(Intent.EXTRA_USER_ID, mUserId);
            mStartTimeMillis = savedInstanceState.getLong("start_time", 0L);
            mActions = savedInstanceState.getIntegerArrayList("onboarding_actions");
            mOnboardingEvent =
                    savedInstanceState.getParcelable("onboarding_event", OnboardingEvent.class);
        }
        boolean hasEnrolledTemplates = false;
        int enrolledCount = 0;
        try {
            FaceManager faceManagerOrNull = Utils.getFaceManagerOrNull(this);
            if (faceManagerOrNull != null) {
                hasEnrolledTemplates = faceManagerOrNull.hasEnrolledTemplates(mUserId);
                if (hasEnrolledTemplates) {
                    enrolledCount = faceManagerOrNull.getEnrolledFaces().size();
                }
            } else {
                hasEnrolledTemplates = getIntent().getBooleanExtra("finished_enrolling_face", true);
                Log.w(TAG, "FaceManager is null, respect EXTRA_FINISHED_ENROLL_FACE instead");
            }
        } catch (NullPointerException e) {
            Log.e(TAG, "Failed to check enrolled templates", e);
        }
        if (mStartTimeMillis <= 0) {
            mStartTimeMillis = SystemClock.elapsedRealtime();
        }
        mBiometricsLogger =
                FeatureFactory.getFeatureFactory()
                        .getBiometricsFeatureProvider()
                        .getBiometricsLogger();
        if (mOnboardingEvent == null && mBiometricsLogger != null) {
            OnboardingEvent onboardingEventFromIntent = getOnboardingEventFromIntent(getIntent());
            mOnboardingEvent = onboardingEventFromIntent;
            if (onboardingEventFromIntent != null) {
                onboardingEventFromIntent.setEnrolledCount(enrolledCount);
            }
        }
        if (BiometricsLogger.LOGGABLE) {
            Log.d(
                    BiometricsLogger.TAG,
                    getClass().getSimpleName() + ".onCreate event=" + mOnboardingEvent);
        }
        mShowIllustration =
                getApplicationContext()
                        .getResources()
                        .getBoolean(R.bool.config_face_enroll_confirmation_show_illustration);
        mShowLockScreenBypass =
                !isManagedProfile(this) && !isAnySetupWizard() && hasEnrolledTemplates;
        int layoutResId;
        if (mShowLockScreenBypass) {
            layoutResId = R.layout.face_enroll_confirmation_show_lock_screen_bypass;
        } else if (mShowIllustration) {
            layoutResId = R.layout.face_enroll_confirmation_show_illustration;
        } else {
            layoutResId = R.layout.face_enroll_confirmation_hide_illustration;
        }
        setContentView(layoutResId);
        setHeaderText(R.string.security_settings_face_enroll_finish_title);
        if (mShowLockScreenBypass) {
            GlifLayout layout = getLayout();
            String descriptionText;
            if (isFaceStrengthClass3()) {
                descriptionText = getUseClass3BiometricDescription();
            } else {
                descriptionText =
                        getUseClass1BiometricDescription(isBiometricClass1FoldableDevice());
            }
            layout.setDescriptionText(descriptionText);
            mFaceEnrollLockScreenBypassToggle = findViewById(R.id.lock_screen_bypass_toggle);
            mFaceEnrollLockScreenBypassToggle.setIsEnabled(mShowLockScreenBypass);
            setScreenLockBypassLottie(mFaceEnrollLockScreenBypassToggle.isChecked());
            mFaceEnrollLockScreenBypassToggle.setInnerCompoundButtonCheckedChangeListener(
                    (CompoundButton.OnCheckedChangeListener)
                            (buttonView, isChecked) -> {
                                updateOnboardingScreenInfoActions(
                                        OnboardingAction.ACTION_FACE_SKIP_LOCK_SCREEN_ON_VALUE);
                                setScreenLockBypassLottie(isChecked);
                            });
            if (mIsUsingExpressStyle) {
                MaterialSwitch switchButton = mFaceEnrollLockScreenBypassToggle.getSwitchButton();
                switchButton.setThumbIconDrawable(
                        switchButton
                                .getContext()
                                .getDrawable(
                                        com.android.settingslib.widget.theme.R.drawable
                                                .settingslib_expressive_switch_thumb_icon));
            }
        } else if (!mShowIllustration) {
            getLayout()
                    .setDescriptionText(R.string.face_confirmation_hide_illustration_description);
            setNonIllustrationThemeColor();
        } else {
            setScreenLockBypassLottie(false);
            getLayout().setDescriptionText(R.string.face_enroll_finish_subtitle);
        }
        mGazeEnabled = getResources().getBoolean(com.android.settings.R.bool.config_gazeEnabled);
        if (mGazeEnabled) {
            boolean gazeEnabledFromIntent = getIntent().getBooleanExtra("gaze_enabled", false);
            FaceAttentionController controller =
                    new FaceAttentionController(getApplicationContext());
            mFaceAttentionController = controller;
            controller.setToken(mToken);
            mFaceAttentionController.setAttentionStatus(
                    mUserId,
                    gazeEnabledFromIntent,
                    success -> {
                        if (!success) {
                            Log.w(TAG, "Failed to set attention status");
                        }
                    });
        }
        mAmbientDisplayConfig = new AmbientDisplayConfiguration(this);
        FooterBarMixin footerBarMixin = (FooterBarMixin) getLayout().getMixin(FooterBarMixin.class);
        mFooterBarMixin = footerBarMixin;
        FooterButton.Builder builder = new FooterButton.Builder(this);
        int buttonTextResource;
        if (WizardManagerHelper.isAnySetupWizard(getIntent())) {
            buttonTextResource = R.string.security_settings_face_enroll_next;
        } else {
            buttonTextResource = com.android.settings.R.string.security_settings_face_enroll_done;
        }
        footerBarMixin.setPrimaryButton(
                builder.setText(buttonTextResource)
                        .setListener(this::onButtonPositive)
                        .setButtonType(FooterButton.ButtonType.NEXT)
                        .setTheme(com.google.android.setupdesign.R.style.SudGlifButton_Primary)
                        .build());
        enablePickupGesture();
        enableTapScreenGesture();
        getOnBackInvokedDispatcher()
                .registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT, this);
        ((FloatingBackButtonMixin) getLayout().getMixin(FloatingBackButtonMixin.class))
                .setOnBackPressedCallback(this::onBackInvoked);
    }

    private void setScreenLockBypassLottie(boolean checked) {
        LottieAnimationView lottieAnimationView = findViewById(R.id.lock_screen_bypass_lottie);
        mLockScreenBypassLottie = lottieAnimationView;
        if (lottieAnimationView != null) {
            boolean wasAnimating = lottieAnimationView.isAnimating();
            mLockScreenBypassLottie.setOnClickListener(
                    view -> {
                        if (mLockScreenBypassLottie.isAnimating()) {
                            mLockScreenBypassLottie.pauseAnimation();
                        } else {
                            mLockScreenBypassLottie.resumeAnimation();
                        }
                    });
            mLockScreenBypassLottie.setAnimation(
                    getConfirmationLockBypassIllustration(mIsUsingExpressStyle, checked));
            LottieColorUtils.applyDynamicColors(getApplicationContext(), mLockScreenBypassLottie);
            if (wasAnimating) {
                mLockScreenBypassLottie.playAnimation();
            }
            if (mIsUsingExpressStyle) {
                applyIllustrationLottieThemeColor();
            }
        }
    }

    private int getConfirmationLockBypassIllustration(boolean expressStyle, boolean enabled) {
        if (expressStyle) {
            return enabled
                    ? R.raw.face_confirmation_lockscreen_bypass_enable_lottie_expressive
                    : R.raw.face_confirmation_lockscreen_bypass_disable_lottie_expressive;
        }
        return enabled
                ? R.raw.face_confirmation_lockscreen_bypass_enable_lottie
                : R.raw.face_confirmation_lockscreen_bypass_disable_lottie;
    }

    private void applyIllustrationLottieThemeColor() {
        ArrayList<String> colorPalette = new ArrayList<>();
        Collections.addAll(colorPalette, getResources().getStringArray(R.array.face_unlock_bypass));
        LottieAnimationHelper.get()
                .applyColor(getApplicationContext(), mLockScreenBypassLottie, colorPalette);
    }

    private boolean isManagedProfile(Context context) {
        return context.getSystemService(UserManager.class).isManagedProfile(mUserId);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mLockScreenBypassLottie != null) {
            mLockScreenBypassLottie.playAnimation();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mLockScreenBypassLottie != null) {
            mLockScreenBypassLottie.cancelAnimation();
        }
        if (isChangingConfigurations()
                || WizardManagerHelper.isAnySetupWizard(getIntent())
                || isFinishing()) {
            return;
        }
        setResult(RESULT_TIMEOUT_OR_TOO_HOT, newResultIntent());
        finish();
    }

    @Override
    protected void onDestroy() {
        if (mFaceEnrollLockScreenBypassToggle != null) {
            mFaceEnrollLockScreenBypassToggle.setInnerCompoundButtonCheckedChangeListener(null);
        }
        getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback(this);
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putByteArray("hw_auth_token", mToken);
        outState.putInt(Intent.EXTRA_USER_ID, mUserId);
        if (mOnboardingEvent != null) {
            outState.putLong("start_time", mStartTimeMillis);
            outState.putIntegerArrayList("onboarding_actions", mActions);
            outState.putParcelable("onboarding_event", mOnboardingEvent);
        }
    }

    private GlifLayout getLayout() {
        return (GlifLayout) findViewById(R.id.face_enroll_confirmation);
    }

    void onButtonPositive(View view) {
        updateOnboardingScreenInfoActions(OnboardingAction.ACTION_NEXT_VALUE);
        if (WizardManagerHelper.isAnySetupWizard(getIntent())) {
            revokeChallenge();
        }
        TransitionHelper.applyForwardTransition(this, 6);
        Intent resultIntent = newResultIntent();
        if (resultIntent == null) {
            resultIntent = new Intent();
        }
        resultIntent.putExtra("hw_auth_token", mToken);
        resultIntent.putExtra("challenge", getIntent().getLongExtra("challenge", 0L));
        resultIntent.putExtra("sensor_id", getIntent().getIntExtra("sensor_id", 0));
        setResult(RESULT_FINISHED, resultIntent);
        finish();
    }

    private void setHeaderText(int titleResId) {
        TextView headerTextView = getLayout().getHeaderTextView();
        CharSequence currentText = headerTextView.getText();
        CharSequence newText = getText(titleResId);
        if (currentText != newText) {
            if (!TextUtils.isEmpty(newText)) {
                headerTextView.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
            }
            getLayout().setHeaderText(newText);
            setTitle(newText);
        }
    }

    private void enablePickupGesture() {
        if (mAmbientDisplayConfig.pickupGestureEnabled(mUserId)) {
            return;
        }
        Log.d(TAG, "Reset DOZE_PICK_UP_GESTURE null, pickupGestureEnabled is default ON.");
        Settings.Secure.putStringForUser(
                getContentResolver(), "doze_pulse_on_pick_up", null, mUserId);
    }

    private void enableTapScreenGesture() {
        if (mAmbientDisplayConfig.tapGestureEnabled(mUserId)) {
            return;
        }
        Log.d(TAG, "Reset DOZE_TAP_SCREEN_GESTURE null, tapGestureEnabled is default ON.");
        Settings.Secure.putStringForUser(getContentResolver(), "doze_tap_gesture", null, mUserId);
    }

    private void revokeChallenge() {
        FaceManager faceManager = getSystemService(FaceManager.class);
        if (faceManager != null) {
            faceManager.revokeChallenge(
                    getIntent().getIntExtra("sensor_id", -1),
                    mUserId,
                    getIntent().getLongExtra("challenge", 0L));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        OnboardingEvent onboardingEventFromIntent = getOnboardingEventFromIntent(data);
        Boolean loggable = BiometricsLogger.LOGGABLE;
        if (loggable) {
            Log.d(
                    BiometricsLogger.TAG,
                    getClass().getSimpleName()
                            + ": current event="
                            + mOnboardingEvent
                            + ", eventFromData="
                            + onboardingEventFromIntent);
        }
        if (onboardingEventFromIntent != null) {
            mOnboardingEvent = onboardingEventFromIntent;
        }
        mStartTimeMillis = SystemClock.elapsedRealtime();
        if (loggable) {
            Log.d(
                    BiometricsLogger.TAG,
                    getClass().getSimpleName() + ": received event=" + mOnboardingEvent);
        }
        if (requestCode == REQUEST_FACE_SETTINGS) {
            setResult(
                    resultCode == RESULT_TIMEOUT_OR_TOO_HOT
                            ? RESULT_TIMEOUT_OR_TOO_HOT
                            : RESULT_FINISHED,
                    data);
            finish();
        } else if (requestCode == REQUEST_NEXT_BIOMETRIC) {
            Log.d(TAG, "Next biometric's result: " + resultCode);
            setResult(RESULT_FINISHED, data);
            finish();
        }
    }

    protected PorterDuffColorFilter getIconColorFilter() {
        if (mIconColorFilter == null) {
            mIconColorFilter =
                    new PorterDuffColorFilter(
                            DynamicColorPalette.getColor(
                                    this, DynamicColorPalette.ColorType.ACCENT),
                            PorterDuff.Mode.SRC_IN);
        }
        return mIconColorFilter;
    }

    private void setNonIllustrationThemeColor() {
        ImageView iconFold = findViewById(R.id.icon_fold);
        ImageView iconLight = findViewById(R.id.icon_light);
        if (iconFold == null || iconLight == null) {
            return;
        }
        iconFold.getBackground().setColorFilter(getIconColorFilter());
        iconLight.getBackground().setColorFilter(getIconColorFilter());
    }

    @Override
    public void onBackInvoked() {
        updateOnboardingScreenInfoActions(OnboardingAction.ACTION_CANCEL_VALUE);
        Intent resultIntent = newResultIntent();
        if (resultIntent == null) {
            resultIntent = new Intent();
        }
        resultIntent.putExtra("finished_enrolling_face", true);
        setResult(RESULT_CANCELLED, resultIntent);
        finish();
        TransitionHelper.applyBackwardTransition(this, 6);
    }

    boolean isAnySetupWizard() {
        return WizardManagerHelper.isAnySetupWizard(getIntent());
    }

    boolean isFaceStrengthClass3() {
        List<FaceSensorProperties> faceSensorProperties = hasFaceSensorProperties();
        if (faceSensorProperties == null) {
            return false;
        }
        boolean isClass3 = false;
        for (FaceSensorProperties properties : faceSensorProperties) {
            if (properties.getSensorStrength() == FaceSensorProperties.STRENGTH_STRONG) {
                isClass3 = true;
                break;
            }
        }
        BiometricManager biometricManager = getSystemService(BiometricManager.class);
        return isClass3
                && biometricManager != null
                && biometricManager.canAuthenticate(
                                BiometricManager.Authenticators.BIOMETRIC_STRONG)
                        == BiometricManager.BIOMETRIC_SUCCESS;
    }

    private List<FaceSensorProperties> hasFaceSensorProperties() {
        FaceManager faceManager = getSystemService(FaceManager.class);
        if (faceManager == null) {
            Log.w(TAG, "Unable to get face manager...");
            return null;
        }
        List<FaceSensorProperties> sensorProperties = faceManager.getSensorProperties();
        if (sensorProperties != null && !sensorProperties.isEmpty()) {
            return sensorProperties;
        }
        Log.d(TAG, "FaceSensorProperties were empty");
        return null;
    }

    boolean isBiometricClass1FoldableDevice() {
        List<FaceSensorProperties> faceSensorProperties = hasFaceSensorProperties();
        if (faceSensorProperties == null) {
            return false;
        }
        for (FaceSensorProperties properties : faceSensorProperties) {
            if (properties.getSensorStrength() == FaceSensorProperties.STRENGTH_CONVENIENCE
                    && FaceUtils.isFoldable(getApplicationContext())) {
                return true;
            }
        }
        return false;
    }

    String getUseClass1BiometricDescription(boolean isFolded) {
        int stringResId =
                isFolded
                        ? R.string
                                .security_settings_face_enroll_finish_description_without_bp_folded
                        : R.string.security_settings_face_enroll_finish_description_without_bp;
        return getString(stringResId);
    }

    String getUseClass3BiometricDescription() {
        return getString(R.string.face_enroll_finish_subtitle);
    }

    private void updateOnboardingScreenInfoActions(int action) {
        if (mBiometricsLogger == null || mOnboardingEvent == null) {
            return;
        }
        mActions.add(action);
    }

    private Intent newResultIntent() {
        if (mBiometricsLogger == null || mOnboardingEvent == null) {
            return null;
        }
        addScreenInfoToEvent();
        Intent intent = new Intent();
        intent.putExtra(
                "biometrics_onboarding_event_bytes",
                mBiometricsLogger.eventToMessageByteArray(mOnboardingEvent));
        return intent;
    }

    private OnboardingEvent getOnboardingEventFromIntent(Intent intent) {
        if (mBiometricsLogger == null
                || intent == null
                || !intent.hasExtra("biometrics_onboarding_event_bytes")) {
            return null;
        }
        return mBiometricsLogger.messageByteArrayToEvent(
                intent.getByteArrayExtra("biometrics_onboarding_event_bytes"));
    }

    private void addScreenInfoToEvent() {
        if (mBiometricsLogger == null || mOnboardingEvent == null) {
            return;
        }
        mOnboardingEvent.addScreenInfo(
                new OnboardingScreenInfoEvent(
                        OnboardingScreen.SCREEN_CONFIRMATION_VALUE,
                        SystemClock.elapsedRealtime() - mStartTimeMillis,
                        mActions.stream().mapToInt(Integer::intValue).toArray()));
        if (BiometricsLogger.LOGGABLE) {
            Log.d(
                    BiometricsLogger.TAG,
                    getClass().getSimpleName()
                            + ": add screen info="
                            + mOnboardingEvent.getScreenInfos());
        }
        mActions.clear();
    }

    static final int REQUEST_NEXT_BIOMETRIC = 2;

    static final int RESULT_FINISHED = 1;
    static final int RESULT_CANCELLED = 0;
    static final int RESULT_TIMEOUT_OR_TOO_HOT = 3;
}
