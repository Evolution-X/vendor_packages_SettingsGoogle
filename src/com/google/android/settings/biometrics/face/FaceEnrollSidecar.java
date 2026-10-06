package com.google.android.settings.biometrics.face;

import android.app.Activity;
import android.content.Intent;
import android.hardware.biometrics.BiometricFaceConstants;
import android.hardware.face.FaceManager;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;
import android.view.Surface;

import androidx.fragment.app.Fragment;

import com.android.settings.biometrics.face.FaceUpdater;

import java.util.ArrayList;
import java.util.Arrays;

public class FaceEnrollSidecar extends Fragment {
    private static final String TAG = "FaceEnrollSidecar";

    private boolean mDebugConsent;
    private int[] mDisabledFeatures;
    private boolean mDone;
    private boolean mEnrolling;
    private CancellationSignal mEnrollmentCancel;
    private FaceUpdater mFaceUpdater;
    private final Intent mIntent;
    private Listener mListener;
    private PreviewSurfaceProvider mPreviewSurfaceProvider;
    private boolean mShouldManagePreview;
    private boolean mSingleFromMulti;
    private boolean mTalkbackEnabled;
    private byte[] mToken;
    private int mUserId;

    private int mEnrollmentSteps = -1;
    private int mEnrollmentRemaining = 0;
    private Handler mHandler = new Handler();
    private int mEnrollmentTypeVendorCode = 0;

    private final Runnable mTimeoutRunnable = () -> cancelEnrollment();

    private final Runnable mStartEnrollRunnable =
            new Runnable() {
                @Override
                public void run() {
                    if (mEnrolling) {
                        return;
                    }
                    startEnrollment();
                }
            };

    private final FaceManager.EnrollmentCallback mEnrollmentCallback =
            new FaceManager.EnrollmentCallback() {
                @Override
                public void onEnrollmentProgress(int remaining) {
                    FaceEnrollSidecar.this.onEnrollmentProgress(remaining);
                }

                @Override
                public void onEnrollmentHelp(int helpMsgId, CharSequence helpString) {
                    FaceEnrollSidecar.this.onEnrollmentHelp(helpMsgId, helpString);
                }

                @Override
                public void onEnrollmentError(int errMsgId, CharSequence errString) {
                    FaceEnrollSidecar.this.onEnrollmentError(errMsgId, errString);
                }
            };

    private ArrayList<QueuedEvent> mQueuedEvents = new ArrayList<>();

    public interface Listener {
        void onEnrollmentError(int errMsgId, CharSequence errString);

        void onEnrollmentHelp(int helpMsgId, CharSequence helpString);

        void onEnrollmentProgressChange(int steps, int remaining);
    }

    interface PreviewSurfaceProvider {
        Surface getPreviewSurface();
    }

    private abstract static class QueuedEvent {
        public abstract void send(Listener listener);
    }

    private static final class QueuedEnrollmentProgress extends QueuedEvent {
        int enrollmentSteps;
        int remaining;

        QueuedEnrollmentProgress(int steps, int remaining) {
            this.enrollmentSteps = steps;
            this.remaining = remaining;
        }

        @Override
        public void send(Listener listener) {
            listener.onEnrollmentProgressChange(enrollmentSteps, remaining);
        }
    }

    private static final class QueuedEnrollmentHelp extends QueuedEvent {
        int helpMsgId;
        CharSequence helpString;

        QueuedEnrollmentHelp(int helpMsgId, CharSequence helpString) {
            this.helpMsgId = helpMsgId;
            this.helpString = helpString;
        }

        @Override
        public void send(Listener listener) {
            listener.onEnrollmentHelp(helpMsgId, helpString);
        }
    }

    private static final class QueuedEnrollmentError extends QueuedEvent {
        int errMsgId;
        CharSequence errString;

        QueuedEnrollmentError(int errMsgId, CharSequence errString) {
            this.errMsgId = errMsgId;
            this.errString = errString;
        }

        @Override
        public void send(Listener listener) {
            listener.onEnrollmentError(errMsgId, errString);
        }
    }

    public FaceEnrollSidecar(Intent intent) {
        mIntent = intent;
    }

    void init(
            int[] disabledFeatures,
            boolean singleFromMulti,
            boolean talkbackEnabled,
            boolean shouldManagePreview,
            boolean debugConsent) {
        mDisabledFeatures = Arrays.copyOf(disabledFeatures, disabledFeatures.length);
        mSingleFromMulti = singleFromMulti;
        mTalkbackEnabled = talkbackEnabled;
        mShouldManagePreview = shouldManagePreview;
        mDebugConsent = debugConsent;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mToken = activity.getIntent().getByteArrayExtra("hw_auth_token");
        mUserId = activity.getIntent().getIntExtra(Intent.EXTRA_USER_ID, UserHandle.USER_NULL);
        mFaceUpdater = new FaceUpdater(activity);
    }

    @Override
    public void onStart() {
        super.onStart();
        mHandler.postDelayed(mStartEnrollRunnable, 750L);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (getActivity().isChangingConfigurations()) {
            return;
        }
        mHandler.removeCallbacks(mStartEnrollRunnable);
        cancelEnrollment();
    }

    protected void startEnrollment() {
        Surface previewSurface;
        mHandler.removeCallbacks(mTimeoutRunnable);
        mEnrollmentSteps = -1;
        mEnrollmentCancel = new CancellationSignal();
        mEnrolling = true;
        boolean requireDiversity = true;
        for (int feature : mDisabledFeatures) {
            if (feature == BiometricFaceConstants.FEATURE_REQUIRE_REQUIRE_DIVERSITY) {
                requireDiversity = false;
            }
        }
        if (mSingleFromMulti) {
            mEnrollmentTypeVendorCode = TYPE_SINGLE_FROM_MULTI;
        } else if (requireDiversity) {
            mEnrollmentTypeVendorCode = TYPE_MULTI_ANGLE;
        } else {
            mEnrollmentTypeVendorCode = TYPE_SINGLE_ANGLE_ACCESSIBILITY;
        }
        updateSettingsCache();
        if (mShouldManagePreview) {
            previewSurface = null;
        } else if (mPreviewSurfaceProvider == null) {
            Log.e(TAG, "Preview surface provider is null");
            previewSurface = null;
        } else {
            previewSurface = mPreviewSurfaceProvider.getPreviewSurface();
            if (previewSurface == null) {
                Log.e(TAG, "Preview surface is null");
            }
        }
        mFaceUpdater.enroll(
                mUserId,
                mToken,
                mEnrollmentCancel,
                mEnrollmentCallback,
                mDisabledFeatures,
                previewSurface,
                mDebugConsent,
                mIntent);
    }

    private void updateSettingsCache() {
        int attentionRequired = 1;
        int diversityRequired = 1;
        for (int feature : mDisabledFeatures) {
            if (feature == BiometricFaceConstants.FEATURE_REQUIRE_ATTENTION) {
                attentionRequired = 0;
            } else if (feature == BiometricFaceConstants.FEATURE_REQUIRE_REQUIRE_DIVERSITY) {
                diversityRequired = 0;
            }
        }
        Settings.Secure.putIntForUser(
                getActivity().getContentResolver(),
                Settings.Secure.FACE_UNLOCK_ATTENTION_REQUIRED,
                attentionRequired,
                mUserId);
        Settings.Secure.putIntForUser(
                getActivity().getContentResolver(),
                Settings.Secure.FACE_UNLOCK_DIVERSITY_REQUIRED,
                diversityRequired,
                mUserId);
    }

    public boolean cancelEnrollment() {
        mHandler.removeCallbacks(mTimeoutRunnable);
        if (!mEnrolling) {
            return false;
        }
        mEnrollmentCancel.cancel();
        mEnrolling = false;
        mEnrollmentSteps = -1;
        return true;
    }

    protected void onEnrollmentProgress(int remaining) {
        if (mEnrollmentSteps == -1) {
            mEnrollmentSteps = remaining;
        }
        mEnrollmentRemaining = remaining;
        mDone = remaining == 0;
        Listener listener = mListener;
        if (listener != null) {
            listener.onEnrollmentProgressChange(mEnrollmentSteps, remaining);
        } else {
            mQueuedEvents.add(new QueuedEnrollmentProgress(mEnrollmentSteps, remaining));
        }
    }

    protected void onEnrollmentHelp(int helpMsgId, CharSequence helpString) {
        Listener listener = mListener;
        if (listener != null) {
            listener.onEnrollmentHelp(helpMsgId, helpString);
        } else {
            mQueuedEvents.add(new QueuedEnrollmentHelp(helpMsgId, helpString));
        }
    }

    protected void onEnrollmentError(int errMsgId, CharSequence errString) {
        Listener listener = mListener;
        if (listener != null) {
            listener.onEnrollmentError(errMsgId, errString);
        } else {
            mQueuedEvents.add(new QueuedEnrollmentError(errMsgId, errString));
        }
        mEnrolling = false;
    }

    public void setListener(Listener listener) {
        mListener = listener;
        if (listener == null) {
            return;
        }
        for (int i = 0; i < mQueuedEvents.size(); i++) {
            mQueuedEvents.get(i).send(mListener);
        }
        mQueuedEvents.clear();
    }

    void setPreviewSurfaceProvider(PreviewSurfaceProvider previewSurfaceProvider) {
        mPreviewSurfaceProvider = previewSurfaceProvider;
    }

    public boolean isEnrolling() {
        return mEnrolling;
    }

    public void logEnrollmentEnded(int result, boolean completedWithoutPartialCommit) {
        switch (result) {
            case RESULT_TIMEOUT:
                logEnrollmentTimeout();
                break;
            case RESULT_SUCCESS:
                logEnrollmentSuccess(completedWithoutPartialCommit);
                break;
            case RESULT_ERROR:
                logEnrollmentError();
                break;
            default:
                break;
        }
    }

    private void logEnrollmentTimeout() {
        switch (mEnrollmentTypeVendorCode) {
            case TYPE_MULTI_ANGLE:
                if (mTalkbackEnabled) {
                    FaceUtils.writeVendorLog(mUserId, VENDOR_TIMEOUT_MULTI_TALKBACK);
                } else {
                    FaceUtils.writeVendorLog(mUserId, VENDOR_TIMEOUT_MULTI);
                }
                break;
            case TYPE_SINGLE_ANGLE_ACCESSIBILITY:
                if (mTalkbackEnabled) {
                    FaceUtils.writeVendorLog(mUserId, VENDOR_TIMEOUT_SINGLE_TALKBACK);
                } else {
                    FaceUtils.writeVendorLog(mUserId, VENDOR_TIMEOUT_SINGLE);
                }
                break;
            case TYPE_SINGLE_FROM_MULTI:
                if (mTalkbackEnabled) {
                    FaceUtils.writeVendorLog(mUserId, VENDOR_TIMEOUT_FROM_MULTI_TALKBACK);
                } else {
                    FaceUtils.writeVendorLog(mUserId, VENDOR_TIMEOUT_FROM_MULTI);
                }
                break;
            default:
                break;
        }
    }

    private void logEnrollmentSuccess(boolean completedWithoutPartialCommit) {
        switch (mEnrollmentTypeVendorCode) {
            case TYPE_MULTI_ANGLE:
                if (completedWithoutPartialCommit) {
                    FaceUtils.writeVendorLog(
                            mUserId,
                            mTalkbackEnabled
                                    ? VENDOR_SUCCESS_MULTI_FAST_TALKBACK
                                    : VENDOR_SUCCESS_MULTI_FAST);
                } else {
                    FaceUtils.writeVendorLog(
                            mUserId,
                            mTalkbackEnabled
                                    ? VENDOR_SUCCESS_MULTI_PARTIAL_TALKBACK
                                    : VENDOR_SUCCESS_MULTI_PARTIAL);
                }
                break;
            case TYPE_SINGLE_ANGLE_ACCESSIBILITY:
                if (mTalkbackEnabled) {
                    FaceUtils.writeVendorLog(mUserId, VENDOR_SUCCESS_SINGLE_TALKBACK);
                } else {
                    FaceUtils.writeVendorLog(mUserId, VENDOR_SUCCESS_SINGLE);
                }
                break;
            case TYPE_SINGLE_FROM_MULTI:
                if (mTalkbackEnabled) {
                    FaceUtils.writeVendorLog(mUserId, VENDOR_SUCCESS_FROM_MULTI_TALKBACK);
                } else {
                    FaceUtils.writeVendorLog(mUserId, VENDOR_SUCCESS_FROM_MULTI);
                }
                break;
            default:
                break;
        }
    }

    private void logEnrollmentError() {
        switch (mEnrollmentTypeVendorCode) {
            case TYPE_MULTI_ANGLE:
                if (mTalkbackEnabled) {
                    FaceUtils.writeVendorLog(mUserId, VENDOR_ERROR_MULTI_TALKBACK);
                } else {
                    FaceUtils.writeVendorLog(mUserId, VENDOR_ERROR_MULTI);
                }
                break;
            case TYPE_SINGLE_ANGLE_ACCESSIBILITY:
                if (mTalkbackEnabled) {
                    FaceUtils.writeVendorLog(mUserId, VENDOR_ERROR_SINGLE_TALKBACK);
                } else {
                    FaceUtils.writeVendorLog(mUserId, VENDOR_ERROR_SINGLE);
                }
                break;
            case TYPE_SINGLE_FROM_MULTI:
                if (mTalkbackEnabled) {
                    FaceUtils.writeVendorLog(mUserId, VENDOR_ERROR_FROM_MULTI_TALKBACK);
                } else {
                    FaceUtils.writeVendorLog(mUserId, VENDOR_ERROR_FROM_MULTI);
                }
                break;
            default:
                break;
        }
    }

    static final int RESULT_TIMEOUT = 0;
    static final int RESULT_SUCCESS = 1;
    static final int RESULT_ERROR = 2;

    private static final int TYPE_MULTI_ANGLE = 0;
    private static final int TYPE_SINGLE_ANGLE_ACCESSIBILITY = 1;
    private static final int TYPE_SINGLE_FROM_MULTI = 2;

    private static final int VENDOR_TIMEOUT_MULTI = 1142;
    private static final int VENDOR_TIMEOUT_SINGLE = 1146;
    private static final int VENDOR_TIMEOUT_FROM_MULTI = 1156;
    private static final int VENDOR_TIMEOUT_MULTI_TALKBACK = 1149;
    private static final int VENDOR_TIMEOUT_SINGLE_TALKBACK = 1153;
    private static final int VENDOR_TIMEOUT_FROM_MULTI_TALKBACK = 1159;
    private static final int VENDOR_SUCCESS_MULTI_FAST = 1143;
    private static final int VENDOR_SUCCESS_MULTI_PARTIAL = 1144;
    private static final int VENDOR_SUCCESS_SINGLE = 1147;
    private static final int VENDOR_SUCCESS_FROM_MULTI = 1157;
    private static final int VENDOR_SUCCESS_MULTI_FAST_TALKBACK = 1150;
    private static final int VENDOR_SUCCESS_MULTI_PARTIAL_TALKBACK = 1151;
    private static final int VENDOR_SUCCESS_SINGLE_TALKBACK = 1154;
    private static final int VENDOR_SUCCESS_FROM_MULTI_TALKBACK = 1160;
    private static final int VENDOR_ERROR_MULTI = 1145;
    private static final int VENDOR_ERROR_SINGLE = 1148;
    private static final int VENDOR_ERROR_FROM_MULTI = 1158;
    private static final int VENDOR_ERROR_MULTI_TALKBACK = 1152;
    private static final int VENDOR_ERROR_SINGLE_TALKBACK = 1155;
    private static final int VENDOR_ERROR_FROM_MULTI_TALKBACK = 1161;
}
