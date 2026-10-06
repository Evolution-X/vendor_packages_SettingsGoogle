package com.google.android.settings.biometrics.face;

import static android.hardware.biometrics.BiometricFaceConstants.FACE_ACQUIRED_GOOD;
import static android.hardware.biometrics.BiometricFaceConstants.FACE_ACQUIRED_NOT_DETECTED;
import static android.hardware.biometrics.BiometricFaceConstants.FACE_ACQUIRED_POOR_GAZE;
import static android.hardware.biometrics.BiometricFaceConstants.FACE_ACQUIRED_TOO_CLOSE;
import static android.hardware.biometrics.BiometricFaceConstants.FACE_ACQUIRED_TOO_FAR;
import static android.hardware.biometrics.BiometricFaceConstants.FACE_ACQUIRED_TOO_HIGH;
import static android.hardware.biometrics.BiometricFaceConstants.FACE_ACQUIRED_TOO_LEFT;
import static android.hardware.biometrics.BiometricFaceConstants.FACE_ACQUIRED_TOO_LOW;
import static android.hardware.biometrics.BiometricFaceConstants.FACE_ACQUIRED_TOO_RIGHT;
import static android.hardware.biometrics.BiometricFaceConstants.FACE_ERROR_CANCELED;
import static android.hardware.biometrics.BiometricFaceConstants.FACE_ERROR_NO_SPACE;
import static android.hardware.biometrics.BiometricFaceConstants.FACE_ERROR_TIMEOUT;
import static android.hardware.biometrics.BiometricFaceConstants.FACE_ERROR_VENDOR_BASE;
import static android.hardware.biometrics.BiometricFaceConstants.FEATURE_REQUIRE_ATTENTION;
import static android.hardware.biometrics.BiometricFaceConstants.FEATURE_REQUIRE_REQUIRE_DIVERSITY;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.hardware.face.Face;
import android.hardware.face.FaceManager;
import android.media.AudioAttributes;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.view.accessibility.AccessibilityManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;

import com.android.settings.biometrics.face.FaceUpdater;

import com.google.android.settings.R;
import com.google.android.settings.biometrics.face.anim.FaceEnrollAnimationBase;
import com.google.android.setupcompat.template.FooterBarMixin;
import com.google.android.setupcompat.template.FooterButton;
import com.google.android.setupcompat.util.WizardManagerHelper;
import com.google.android.setupdesign.GlifLayout;
import com.google.android.setupdesign.util.ThemeResolver;

import java.util.ArrayList;

public class FaceEnrollEnrolling extends FragmentActivity implements FaceEnrollSidecar.Listener {
    private static final String TAG = "FaceEnrollEnrolling";

    private static final int REQUEST_FACE_ERROR_DIALOG = 1;
    private static final int REQUEST_FACE_CONFIRMATION = 2;

    public static final int RESULT_CANCELLED = 2;
    public static final int RESULT_TIMEOUT = 3;
    public static final int RESULT_RETRY = 5;

    private static final long HELP_MESSAGE_TIMEOUT_MS = 3000L;
    private static final long ATTENUATE_THRESHOLD_MS = 3000L;
    private static final long NOT_CENTERED_HINT_DELAY_MS = 3000L;
    private static final long NO_PROGRESS_TIMEOUT_MS = 33000L;
    private static final int GAZE_DIALOG_FAIL_COUNT_THRESHOLD = 10;
    private static final long GAZE_DIALOG_MIN_ELAPSED_MS = 5000L;
    private static final int DEBOUNCE_WINDOW_SIZE = 10;
    private static final long ENROLL_COMPLETE_DELAY_MS = 500L;

    private static final AudioAttributes SONIFICATION_AUDIO_ATTRIBUTES =
            new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .build();

    private boolean mCenterAcquired;
    private boolean mDebugConsent;
    private boolean mDidCommitPartialEnrollment;
    private long mEnrollmentStartTime;
    private TextView mErrorText;
    private FooterBarMixin mFooterBarMixin;
    private boolean mFromSetupWizard;
    private int mGazeFailCount;
    private Handler mHandler;
    private HelpController mHelpController;
    private Interpolator mLinearOutSlowInInterpolator;
    private FaceEnrollPreviewFragment mPreviewFragment;
    private java.util.List<Face> mPreviouslyEnrolledFaces;
    private boolean mRequireAttention;
    private boolean mRequireDiversity;
    private boolean mShouldManagePreview;
    private boolean mShowingAnimationHelp;
    private FaceEnrollSidecar mSidecar;
    private boolean mSingleFromMulti;
    private boolean mTalkbackEnabled;
    protected byte[] mToken;
    protected int mUserId;
    private UserManager mUserManager;
    private VibrationEffect mVibrationEffect;
    private Vibrator mVibrator;

    private ArrayList<Integer> mDisabledFeatures = new ArrayList<>();
    private int mRemaining = -1;

    private Runnable mMultiAngleNotCenteredBeforeZeroZeroRunnable =
            () -> mHelpController.showHelp(getText(R.string.face_enrolling_center_head));

    private Runnable mNoProgressTimeoutRunnable =
            new Runnable() {
                @Override
                public void run() {
                    if (mRemaining == -1 || mRemaining == 25) {
                        if (mSidecar != null) {
                            mSidecar.cancelEnrollment();
                            mSidecar.logEnrollmentEnded(FaceEnrollSidecar.RESULT_TIMEOUT, false);
                        }
                        showErrorDialog(
                                getText(
                                        com.android.settings.R.string
                                                .security_settings_face_enroll_error_timeout_dialog_message),
                                FACE_ERROR_TIMEOUT);
                    }
                }
            };

    private FaceEnrollAnimationBase.AnimationListener mAnimationListener =
            new FaceEnrollAnimationBase.AnimationListener() {
                @Override
                public void onEnrollAnimationStarted() {
                    mFooterBarMixin.getSecondaryButton().setVisibility(View.INVISIBLE);
                }

                @Override
                public void onEnrollAnimationFinished() {
                    if (mRequireDiversity) {
                        return;
                    }
                    Intent intent =
                            new Intent(FaceEnrollEnrolling.this, FaceEnrollConfirmation.class);
                    intent.putExtras(getIntent());
                    startActivityForResult(intent, REQUEST_FACE_CONFIRMATION);
                }

                @Override
                public void showHelp(CharSequence help) {
                    mShowingAnimationHelp = true;
                    mHelpController.showHelp(help);
                }

                @Override
                public void clearHelp() {
                    if (mShowingAnimationHelp) {
                        mShowingAnimationHelp = false;
                        mHelpController.clearHelp();
                    }
                }
            };

    class HelpController {
        private Debouncer mDebouncer;
        private Runnable mHelpFinishedRunnable;
        private ViewPropertyAnimator mTextAnimation;
        private long mTextShownTime;

        private HelpController() {
            mDebouncer = new Debouncer(DEBOUNCE_WINDOW_SIZE);
            mHelpFinishedRunnable = () -> clearHelp();
        }

        void debounceAndMaybeShowHelp(int msgId, CharSequence help) {
            if (TextUtils.isEmpty(help)) {
                mDebouncer.reset();
                return;
            }
            mDebouncer.updateBuffer(msgId);
            if (mDebouncer.passesDebounce(msgId)) {
                FaceEnrollEnrolling.this.mShowingAnimationHelp = false;
                showHelp(help);
            }
        }

        void clearHelpIfOverAttenuateThreshold() {
            if (System.currentTimeMillis() - mTextShownTime >= ATTENUATE_THRESHOLD_MS) {
                clearHelp();
            }
        }

        void showHelp(CharSequence help) {
            mHandler.removeCallbacks(mHelpFinishedRunnable);
            mHandler.postDelayed(mHelpFinishedRunnable, HELP_MESSAGE_TIMEOUT_MS);
            if ((mErrorText.getVisibility() == View.VISIBLE
                            && TextUtils.equals(help, mErrorText.getText()))
                    || TextUtils.isEmpty(help)) {
                return;
            }
            mTextShownTime = System.currentTimeMillis();
            mErrorText.setText(help);
            float translationY =
                    getResources().getDimensionPixelSize(R.dimen.face_error_text_appear_distance);
            Animation animation = mErrorText.getAnimation();
            if (animation != null && !animation.hasEnded()) {
                mErrorText.getAnimation().cancel();
            }
            float alpha;
            if (mErrorText.getVisibility() == View.VISIBLE) {
                translationY = mErrorText.getTranslationY();
                alpha = mErrorText.getAlpha();
            } else {
                alpha = 0.0f;
            }
            mErrorText.setVisibility(View.VISIBLE);
            mErrorText.setTranslationY(translationY);
            mErrorText.setAlpha(alpha);
            mErrorText
                    .animate()
                    .alpha(1.0f)
                    .translationY(0.0f)
                    .setDuration(200L)
                    .setInterpolator(mLinearOutSlowInInterpolator)
                    .start();
        }

        void clearHelp() {
            mHandler.removeCallbacks(mHelpFinishedRunnable);
            if (mTextAnimation != null) {
                Log.w(TAG, "Already clearing help");
            } else if (mErrorText.getVisibility() == View.VISIBLE) {
                mTextAnimation =
                        mErrorText
                                .animate()
                                .alpha(0.0f)
                                .translationY(
                                        getResources()
                                                .getDimensionPixelSize(
                                                        R.dimen.face_error_text_appear_distance))
                                .setDuration(200L)
                                .setInterpolator(mLinearOutSlowInInterpolator)
                                .withEndAction(
                                        () -> {
                                            mErrorText.setVisibility(View.INVISIBLE);
                                            mTextAnimation = null;
                                        });
                mTextAnimation.start();
            }
        }
    }

    @Override
    protected void onApplyThemeResource(Resources.Theme theme, int resid, boolean first) {
        theme.applyStyle(com.android.settings.R.style.SetupWizardPartnerResource, true);
        super.onApplyThemeResource(theme, resid, first);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        ThemeResolver.getDefault().applyTheme(this);
        setContentView(R.layout.face_enrolling);
        ((SquareFrameLayout) findViewById(R.id.square_frame_layout))
                .setOuterRegion(R.id.indicator_view, 30);
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mToken = savedInstanceState.getByteArray("hw_auth_token");
            mUserId = savedInstanceState.getInt("user_id", UserHandle.myUserId());
            mFromSetupWizard = savedInstanceState.getBoolean("is_suw");
            mRequireDiversity = savedInstanceState.getBoolean("accessibility_diversity", true);
            mRequireAttention = savedInstanceState.getBoolean("accessibility_vision", true);
            mSingleFromMulti = savedInstanceState.getBoolean("from_multi_timeout", false);
            mDebugConsent = savedInstanceState.getBoolean("debug_consent", false);
        } else {
            mToken = getIntent().getByteArrayExtra("hw_auth_token");
            mUserId = getIntent().getIntExtra(Intent.EXTRA_USER_ID, UserHandle.myUserId());
            mFromSetupWizard = WizardManagerHelper.isAnySetupWizard(getIntent());
            mRequireDiversity = getIntent().getBooleanExtra("accessibility_diversity", true);
            mRequireAttention = getIntent().getBooleanExtra("accessibility_vision", true);
            mSingleFromMulti = getIntent().getBooleanExtra("from_multi_timeout", false);
            mDebugConsent = getIntent().getBooleanExtra("debug_consent", false);
        }
        mShouldManagePreview =
                getResources().getBoolean(R.bool.config_face_settings_should_manage_preview);
        mVibrator = getSystemService(Vibrator.class);
        mVibrationEffect = VibrationEffect.get(VibrationEffect.EFFECT_DOUBLE_CLICK);
        FooterBarMixin footerBarMixin = getLayout().getMixin(FooterBarMixin.class);
        mFooterBarMixin = footerBarMixin;
        footerBarMixin.setRemoveFooterBarWhenEmpty(false);
        if (mFromSetupWizard) {
            mFooterBarMixin.setSecondaryButton(
                    new FooterButton.Builder(this)
                            .setText(R.string.face_enrolling_do_it_later)
                            .setListener(this::onButtonNegative)
                            .setButtonType(FooterButton.ButtonType.SKIP)
                            .setTheme(
                                    com.google.android.setupdesign.R.style.SudGlifButton_Secondary)
                            .build());
        } else {
            mFooterBarMixin.setSecondaryButton(
                    new FooterButton.Builder(this)
                            .setText(R.string.face_enrolling_gaze_dialog_cancel)
                            .setListener(this::onButtonNegative)
                            .setButtonType(FooterButton.ButtonType.CANCEL)
                            .setTheme(
                                    com.google.android.setupdesign.R.style.SudGlifButton_Secondary)
                            .build());
        }
        mFooterBarMixin.getSecondaryButton().setVisibility(View.VISIBLE);
        mUserManager = getSystemService(UserManager.class);
        mHandler = new Handler();
        mErrorText = findViewById(R.id.error_text);
        mLinearOutSlowInInterpolator =
                AnimationUtils.loadInterpolator(this, android.R.interpolator.linear_out_slow_in);
        mHelpController = new HelpController();
        mTalkbackEnabled = false;
        AccessibilityManager accessibilityManager =
                getApplicationContext().getSystemService(AccessibilityManager.class);
        if (accessibilityManager != null) {
            mTalkbackEnabled =
                    accessibilityManager.isEnabled()
                            && accessibilityManager.isTouchExplorationEnabled();
        }
        if (!mRequireDiversity) {
            setHeaderText(R.string.face_enrolling_title_accessibility);
            getLayout().setDescriptionText(R.string.face_enrolling_center_head);
            addDisabledFeature(FEATURE_REQUIRE_REQUIRE_DIVERSITY);
        } else {
            setHeaderText(R.string.face_enrolling_title);
        }
        addDisabledFeature(FEATURE_REQUIRE_ATTENTION);
        if (mRequireDiversity) {
            mHandler.postDelayed(
                    mMultiAngleNotCenteredBeforeZeroZeroRunnable, NOT_CENTERED_HINT_DELAY_MS);
            mHandler.postDelayed(mNoProgressTimeoutRunnable, NO_PROGRESS_TIMEOUT_MS);
        }
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        startEnrollment();
    }

    private void addDisabledFeature(int feature) {
        if (!mDisabledFeatures.contains(feature)) {
            mDisabledFeatures.add(feature);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != REQUEST_FACE_ERROR_DIALOG) {
            if (requestCode == REQUEST_FACE_CONFIRMATION) {
                setResult(resultCode);
                finish();
            }
            return;
        }
        if (resultCode == RESULT_TIMEOUT || resultCode == RESULT_CANCELLED) {
            setResult(resultCode);
            finish();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mSidecar != null) {
            mSidecar.setListener(null);
        }
        if (isChangingConfigurations() || mRemaining == 0) {
            return;
        }
        if (mSidecar != null) {
            mSidecar.cancelEnrollment();
            getSupportFragmentManager()
                    .beginTransaction()
                    .remove(mSidecar)
                    .commitAllowingStateLoss();
            mSidecar = null;
        }
        if (!mFromSetupWizard) {
            setResult(RESULT_TIMEOUT);
        }
        finish();
    }

    @Override
    public void onBackPressed() {
        if (mSidecar != null) {
            mSidecar.setListener(null);
            mSidecar.cancelEnrollment();
            getSupportFragmentManager()
                    .beginTransaction()
                    .remove(mSidecar)
                    .commitAllowingStateLoss();
            mSidecar = null;
        }
        super.onBackPressed();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putByteArray("hw_auth_token", mToken);
        outState.putInt("user_id", mUserId);
        outState.putBoolean("is_suw", mFromSetupWizard);
        outState.putBoolean("accessibility_vision", mRequireAttention);
        outState.putBoolean("accessibility_diversity", mRequireDiversity);
        outState.putBoolean("from_multi_timeout", mSingleFromMulti);
        outState.putBoolean("debug_consent", mDebugConsent);
    }

    private void startEnrollment() {
        mEnrollmentStartTime = System.currentTimeMillis();
        mPreviouslyEnrolledFaces = getSystemService(FaceManager.class).getEnrolledFaces(mUserId);
        FaceEnrollPreviewFragment previewFragment =
                (FaceEnrollPreviewFragment)
                        getSupportFragmentManager().findFragmentByTag("tag_preview");
        mPreviewFragment = previewFragment;
        if (previewFragment == null) {
            previewFragment = new FaceEnrollPreviewFragment();
            mPreviewFragment = previewFragment;
            previewFragment.setAnimationListener(mAnimationListener);
            mPreviewFragment.setFromSetupWizard(mFromSetupWizard);
            mPreviewFragment.setShouldManagePreview(mShouldManagePreview);
            mPreviewFragment.setAnimationDrawableMode(mRequireDiversity);
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(mPreviewFragment, "tag_preview")
                    .commitAllowingStateLoss();
        } else {
            previewFragment.setAnimationListener(mAnimationListener);
        }
        FaceEnrollSidecar sidecar =
                (FaceEnrollSidecar) getSupportFragmentManager().findFragmentByTag("tag_sidecar");
        mSidecar = sidecar;
        if (sidecar == null) {
            int[] disabledFeatures = new int[mDisabledFeatures.size()];
            for (int i = 0; i < mDisabledFeatures.size(); i++) {
                disabledFeatures[i] = mDisabledFeatures.get(i);
            }
            sidecar = new FaceEnrollSidecar(getIntent());
            mSidecar = sidecar;
            sidecar.init(
                    disabledFeatures,
                    mSingleFromMulti,
                    mTalkbackEnabled,
                    mShouldManagePreview,
                    mDebugConsent);
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(mSidecar, "tag_sidecar")
                    .commitAllowingStateLoss();
        }
        mSidecar.setListener(this);
        if (!mShouldManagePreview) {
            mSidecar.setPreviewSurfaceProvider(mPreviewFragment);
        }
    }

    @Override
    public void onEnrollmentHelp(int msgId, CharSequence help) {
        if (msgId == 1140) {
            mDidCommitPartialEnrollment = true;
        }
        if (mTalkbackEnabled || !mRequireDiversity) {
            switch (msgId) {
                case FACE_ACQUIRED_TOO_CLOSE:
                    help = getText(R.string.face_enrolling_too_close);
                    break;
                case FACE_ACQUIRED_TOO_FAR:
                    help = getText(R.string.face_enrolling_too_far);
                    break;
                case FACE_ACQUIRED_TOO_HIGH:
                    help = getText(R.string.face_enrolling_too_high);
                    break;
                case FACE_ACQUIRED_TOO_LOW:
                    help = getText(R.string.face_enrolling_too_low);
                    break;
                case FACE_ACQUIRED_TOO_RIGHT:
                    help = getText(R.string.face_enrolling_too_right);
                    break;
                case FACE_ACQUIRED_TOO_LEFT:
                    help = getText(R.string.face_enrolling_too_left);
                    break;
                case FACE_ACQUIRED_NOT_DETECTED:
                    help = getText(R.string.face_enrolling_center_head);
                    break;
                default:
                    break;
            }
        } else if (msgId == FACE_ACQUIRED_NOT_DETECTED) {
            help = getText(R.string.face_enrolling_center_head);
        } else {
            switch (msgId) {
                case FACE_ACQUIRED_TOO_CLOSE:
                case FACE_ACQUIRED_TOO_HIGH:
                case FACE_ACQUIRED_TOO_LOW:
                case FACE_ACQUIRED_TOO_RIGHT:
                case FACE_ACQUIRED_TOO_LEFT:
                    help = getText(R.string.face_enrolling_center_head);
                    break;
                case FACE_ACQUIRED_TOO_FAR:
                    help = getText(R.string.face_enrolling_too_far);
                    break;
                default:
                    switch (msgId) {
                        case 1126:
                        case 1127:
                        case 1128:
                        case 1129:
                        case 1130:
                        case 1131:
                        case 1132:
                        case 1133:
                            help = getText(R.string.face_enrolling_turned_too_far);
                            break;
                        default:
                            break;
                    }
                    break;
            }
        }
        if (isFinishing()) {
            return;
        }
        if (!mCenterAcquired && msgId == FACE_ACQUIRED_POOR_GAZE && mSidecar.isEnrolling()) {
            mGazeFailCount++;
            if (mGazeFailCount >= GAZE_DIALOG_FAIL_COUNT_THRESHOLD
                    && System.currentTimeMillis() - mEnrollmentStartTime
                            >= GAZE_DIALOG_MIN_ELAPSED_MS) {
                showGazeDialog();
            }
        }
        if (!mRequireDiversity) {
            if (msgId != FACE_ACQUIRED_GOOD) {
                mHelpController.debounceAndMaybeShowHelp(msgId, help);
            } else {
                mHelpController.clearHelpIfOverAttenuateThreshold();
            }
        } else if (FaceUtils.isOneOfCenterBuckets(msgId) && !mCenterAcquired) {
            mHandler.removeCallbacks(mMultiAngleNotCenteredBeforeZeroZeroRunnable);
            mHelpController.clearHelp();
            mCenterAcquired = true;
        } else if (msgId != FACE_ACQUIRED_GOOD && mCenterAcquired) {
            mHelpController.debounceAndMaybeShowHelp(msgId, help);
        } else if (msgId != FACE_ACQUIRED_GOOD && !mCenterAcquired) {
            mHelpController.debounceAndMaybeShowHelp(msgId, help);
        } else if (msgId == FACE_ACQUIRED_GOOD) {
            mHelpController.clearHelpIfOverAttenuateThreshold();
        }
        mPreviewFragment.onEnrollmentHelp(msgId, help);
    }

    private void showGazeDialog() {
        mVibrator.vibrate(mVibrationEffect, SONIFICATION_AUDIO_ATTRIBUTES);
        mSidecar.cancelEnrollment();
        FaceGazeDialog gazeDialog = FaceGazeDialog.newInstance();
        gazeDialog.setButtonListener((dialog, which) -> onGazeDialogClick(dialog, which));
        gazeDialog.show(getSupportFragmentManager(), FaceGazeDialog.class.getName());
    }

    private void onGazeDialogClick(DialogInterface dialog, int which) {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            addDisabledFeature(FEATURE_REQUIRE_ATTENTION);
        }
        int[] disabledFeatures = new int[mDisabledFeatures.size()];
        for (int i = 0; i < mDisabledFeatures.size(); i++) {
            disabledFeatures[i] = mDisabledFeatures.get(i);
        }
        mSidecar.init(
                disabledFeatures,
                mSingleFromMulti,
                mTalkbackEnabled,
                mShouldManagePreview,
                mDebugConsent);
        mEnrollmentStartTime = System.currentTimeMillis();
        mGazeFailCount = 0;
        mSidecar.startEnrollment();
    }

    @Override
    public void onEnrollmentError(int errMsgId, CharSequence errString) {
        CharSequence message;
        mSidecar.logEnrollmentEnded(
                errMsgId == FACE_ERROR_TIMEOUT
                        ? FaceEnrollSidecar.RESULT_TIMEOUT
                        : FaceEnrollSidecar.RESULT_ERROR,
                false);
        if (errMsgId == FACE_ERROR_TIMEOUT) {
            message =
                    getText(
                            com.android.settings.R.string
                                    .security_settings_face_enroll_error_timeout_dialog_message);
        } else {
            message =
                    (errMsgId < FACE_ERROR_VENDOR_BASE && errMsgId != FACE_ERROR_NO_SPACE)
                            ? getText(
                                    com.android.settings.R.string
                                            .security_settings_face_enroll_error_generic_dialog_message)
                            : errString;
        }
        getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mPreviewFragment.onEnrollmentError(errMsgId, errString);
        if (errMsgId != FACE_ERROR_CANCELED) {
            showErrorDialog(message, errMsgId);
        }
    }

    @Override
    public void onEnrollmentProgressChange(int steps, int remaining) {
        Log.v(TAG, "Steps: " + steps + " Remaining: " + remaining);
        mRemaining = remaining;
        mPreviewFragment.onEnrollmentProgressChange(steps, remaining);
        if (remaining == 0) {
            if (mDidCommitPartialEnrollment) {
                mSidecar.logEnrollmentEnded(FaceEnrollSidecar.RESULT_SUCCESS, false);
                showPartialEnrollmentDialog();
            } else {
                mSidecar.logEnrollmentEnded(FaceEnrollSidecar.RESULT_SUCCESS, true);
                mHandler.postDelayed(this::onEnrollmentComplete, ENROLL_COMPLETE_DELAY_MS);
            }
        }
    }

    private void onEnrollmentComplete() {
        mHelpController.clearHelp();
        if (!mUserManager.getUserInfo(mUserId).isManagedProfile()) {
            Settings.Secure.putIntForUser(
                    getContentResolver(), Settings.Secure.FACE_UNLOCK_KEYGUARD_ENABLED, 1, mUserId);
        }
        if (mRequireDiversity) {
            Intent intent = new Intent(this, FaceEnrollConfirmation.class);
            intent.putExtras(getIntent());
            startActivityForResult(intent, REQUEST_FACE_CONFIRMATION);
        }
    }

    private void showPartialEnrollmentDialog() {
        FaceEnrollDialogFactory.DialogBuilder builder =
                FaceEnrollDialogFactory.newBuilder(this)
                        .setTitle(R.string.security_settings_face_enroll_partial_title)
                        .setMessage(R.string.security_settings_face_enroll_partial_message)
                        .setPositiveButton(
                                com.android.settings.R.string
                                        .security_settings_face_enroll_dialog_ok,
                                (dialog, which) -> {
                                    dialog.dismiss();
                                    onEnrollmentComplete();
                                })
                        .setNegativeButton(
                                R.string.security_settings_face_enroll_partial_start_over,
                                (dialog, which) -> restartEnrollmentFromDialog(dialog));
        builder.setOnBackKeyListener((dialog, event) -> onEnrollmentComplete());
        builder.build().show();
    }

    private void restartEnrollmentFromDialog(DialogInterface dialog) {
        FaceManager faceManager = getSystemService(FaceManager.class);
        if (faceManager == null) {
            Log.e(TAG, "Unable to remove face. Face manager was null!");
            return;
        }
        Face newlyEnrolledFace = findNewlyEnrolledFace();
        if (newlyEnrolledFace == null) {
            Log.e(TAG, "Unable to remove face. No newly enrolled face found.");
            return;
        }
        new FaceUpdater(this, faceManager)
                .remove(
                        newlyEnrolledFace,
                        mUserId,
                        new FaceManager.RemovalCallback() {
                            @Override
                            public void onRemovalError(
                                    Face face, int errMsgId, CharSequence errString) {
                                Log.e(
                                        TAG,
                                        "Unable to remove face: "
                                                + face.getBiometricId()
                                                + " error: "
                                                + errMsgId
                                                + " "
                                                + errString);
                                Toast.makeText(
                                                FaceEnrollEnrolling.this,
                                                errString,
                                                Toast.LENGTH_SHORT)
                                        .show();
                                finishFromDialog(dialog, RESULT_CANCELLED);
                            }

                            @Override
                            public void onRemovalSucceeded(Face face, int remaining) {
                                if (remaining == 0) {
                                    finishFromDialog(dialog, RESULT_RETRY);
                                }
                            }
                        });
    }

    private void finishFromDialog(DialogInterface dialog, int resultCode) {
        dialog.dismiss();
        setResult(resultCode);
        finish();
    }

    private GlifLayout getLayout() {
        return (GlifLayout) findViewById(R.id.setup_wizard_layout);
    }

    private void setHeaderText(int titleResId) {
        CharSequence headerText = getLayout().getHeaderTextView().getText();
        CharSequence newText = getText(titleResId);
        if (headerText != newText) {
            getLayout().setHeaderText(newText);
            setTitle(newText);
        }
    }

    private void onButtonNegative(View view) {
        setResult(RESULT_CANCELLED);
        finish();
    }

    private void showErrorDialog(CharSequence error, int errMsgId) {
        try {
            FaceErrorDialog.newInstance(error, errMsgId, mRequireDiversity, mFromSetupWizard)
                    .show(getSupportFragmentManager(), FaceErrorDialog.class.getName());
        } catch (IllegalStateException e) {
            Log.w(TAG, "Can't show error after onSaveInstanceState, " + errMsgId);
        }
    }

    private Face findNewlyEnrolledFace() {
        if (mPreviouslyEnrolledFaces == null) {
            Log.w(TAG, "Previously enrolled faces not set!");
        }
        java.util.List<Face> enrolledFaces =
                getSystemService(FaceManager.class).getEnrolledFaces(mUserId);
        if (enrolledFaces == null || enrolledFaces.isEmpty()) {
            Log.e(TAG, "Failed to find newly enrolled face. No faces enrolled.");
            return null;
        }
        Face newlyEnrolledFace = null;
        for (Face face : enrolledFaces) {
            if (mPreviouslyEnrolledFaces == null || !mPreviouslyEnrolledFaces.contains(face)) {
                if (newlyEnrolledFace != null) {
                    Log.e(TAG, "Found more than one newly enrolled face.");
                    return null;
                }
                newlyEnrolledFace = face;
            }
        }
        if (newlyEnrolledFace == null) {
            Log.e(TAG, "No newly enrolled face found.");
        }
        return newlyEnrolledFace;
    }
}
