package com.google.android.settings.biometrics;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.UserInfo;
import android.hardware.face.FaceManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Process;
import android.os.UserManager;
import android.safetycenter.SafetySourceData;
import android.safetycenter.SafetySourceIssue;
import android.text.format.DateUtils;
import android.util.Log;

import com.android.settings.Utils;
import com.android.settings.biometrics.BiometricEnrollActivity;
import com.android.settings.biometrics.face.FaceEnroll;
import com.android.settings.biometrics.fingerprint.FingerprintEnroll;
import com.android.settings.security.ScreenLockPreferenceDetailsUtils;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedLockUtilsInternal;

import com.google.android.settings.R;

public class BiometricsSafetySourceIssueController {
    private static final String TAG = "BiometricsIssueCtr";
    private static final String ACTION_SAFETY_ISSUE_DISMISS =
            "biometric_safety_issue_dismiss_action";

    private Context mContext;
    private int mCurrentSafetyIssueActionLaunchCount;
    private long mSafetyIssueActionLastLaunchTime;
    private SharedPreferences mSharedPreferences;
    private BroadcastReceiver mReceiver =
            new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (ACTION_SAFETY_ISSUE_DISMISS.equals(intent.getAction())) {
                        Log.d(TAG, "SafetyIssue dismissed");
                        notifySafetyIssueActionLaunched();
                    }
                }
            };

    public BiometricsSafetySourceIssueController(Context context) {
        mContext = context;
        mSharedPreferences =
                context.getSharedPreferences("BiometricSafetyIssue", Context.MODE_PRIVATE);
        mSafetyIssueActionLastLaunchTime =
                mSharedPreferences.getLong("safety_issue_last_launch_time", 0L);
        mCurrentSafetyIssueActionLaunchCount =
                mSharedPreferences.getInt("safety_issue_launch_count", 0);
        mContext.registerReceiver(
                mReceiver,
                new IntentFilter(ACTION_SAFETY_ISSUE_DISMISS),
                Context.RECEIVER_EXPORTED);
    }

    private boolean shouldShowSafetyIssue(int userId) {
        long timePassedSinceLastLaunch = getTimePassedSinceLastLaunch();
        if (mCurrentSafetyIssueActionLaunchCount > 2) {
            Log.d(TAG, "Skip for {" + userId + "}: Exceeded maximum launch count");
            return false;
        }
        if (mCurrentSafetyIssueActionLaunchCount == 1 && timePassedSinceLastLaunch < 7) {
            Log.d(
                    TAG,
                    "Skip for {"
                            + userId
                            + "}: The block duration after first launch has not yet passed");
            return false;
        }
        if (mCurrentSafetyIssueActionLaunchCount == 2 && timePassedSinceLastLaunch < 14) {
            Log.d(
                    TAG,
                    "Skip for {"
                            + userId
                            + "}: The block duration after second launch has not yet passed");
            return false;
        }
        if (isLockscreenSet()) {
            return true;
        }
        Log.d(TAG, "Skip for {" + userId + "}: no lockscreen set");
        return false;
    }

    boolean isLockscreenSet() {
        int identifier = Process.myUserHandle().getIdentifier();
        RestrictedLockUtils.EnforcedAdmin enforcedAdmin =
                RestrictedLockUtilsInternal.checkIfPasswordQualityIsSet(mContext, identifier);
        ScreenLockPreferenceDetailsUtils screenLockPreferenceDetailsUtils =
                new ScreenLockPreferenceDetailsUtils(mContext);
        return screenLockPreferenceDetailsUtils.isPasswordQualityManaged(identifier, enforcedAdmin)
                || screenLockPreferenceDetailsUtils.isLockPatternSecure();
    }

    long getTimePassedSinceLastLaunch() {
        return (System.currentTimeMillis() - mSafetyIssueActionLastLaunchTime)
                / DateUtils.DAY_IN_MILLIS;
    }

    public void notifySafetyIssueActionLaunched() {
        mSafetyIssueActionLastLaunchTime = System.currentTimeMillis();
        mCurrentSafetyIssueActionLaunchCount++;
        Log.d(TAG, "Action is launched: count=" + mCurrentSafetyIssueActionLaunchCount);
        mSharedPreferences
                .edit()
                .putLong("safety_issue_last_launch_time", mSafetyIssueActionLastLaunchTime)
                .putInt("safety_issue_launch_count", mCurrentSafetyIssueActionLaunchCount)
                .apply();
    }

    public SafetySourceIssue getSafetySourceIssue(String sourceId) {
        int userId = Process.myUserHandle().getIdentifier();
        if (!shouldShowSafetyIssue(userId)) {
            return null;
        }
        UserInfo userInfo =
                ((UserManager) mContext.getSystemService(UserManager.class)).getUserInfo(userId);
        boolean isManagedProfile = userInfo != null && userInfo.isManagedProfile();
        boolean isFingerprintSource = "AndroidFingerprintUnlock".equals(sourceId);
        boolean isFaceSource = "AndroidFaceUnlock".equals(sourceId);
        FingerprintManager fingerprintManagerOrNull = Utils.getFingerprintManagerOrNull(mContext);
        FaceManager faceManagerOrNull = Utils.getFaceManagerOrNull(mContext);
        boolean hasFingerprintHardware = Utils.hasFingerprintHardware(mContext);
        boolean hasFaceHardware = Utils.hasFaceHardware(mContext);
        boolean isFpEnrollNeeded =
                hasFingerprintHardware && !fingerprintManagerOrNull.hasEnrolledFingerprints(userId);
        boolean isFaceEnrollNeeded =
                hasFaceHardware && !faceManagerOrNull.hasEnrolledTemplates(userId);
        int fpEnrolledCount =
                hasFingerprintHardware
                        ? fingerprintManagerOrNull.getEnrolledFingerprints(userId).size()
                        : 0;
        Log.d(
                TAG,
                "getSafetySourceIssue source="
                        + sourceId
                        + ", isFpEnrollNeeded="
                        + isFpEnrollNeeded
                        + ", isFaceEnrollNeeded="
                        + isFaceEnrollNeeded
                        + ", fpEnrolledCount="
                        + fpEnrolledCount
                        + ", userId="
                        + userId
                        + ", isWorkProfile="
                        + isManagedProfile);

        int titleResId;
        int subtitleResId;
        int actionTitleResId;
        Class<?> enrollActivityClass;
        if (isFpEnrollNeeded && isFaceEnrollNeeded && isFingerprintSource) {
            titleResId =
                    isManagedProfile
                            ? R.string
                                    .biometric_safety_issue_setup_fingerprint_face_unlock_title_for_work
                            : R.string.biometric_safety_issue_setup_fingerprint_face_unlock_title;
            subtitleResId = R.string.biometric_safety_issue_setup_fingerprint_face_unlock_subtitle;
            actionTitleResId = R.string.biometric_safety_issue_setup_action_title;
            enrollActivityClass = BiometricEnrollActivity.class;
        } else if (isFpEnrollNeeded && !isFaceEnrollNeeded && isFingerprintSource) {
            titleResId =
                    isManagedProfile
                            ? R.string
                                    .biometric_safety_issue_setup_fingerprint_unlock_title_for_work
                            : R.string.biometric_safety_issue_setup_fingerprint_unlock_title;
            subtitleResId = R.string.biometric_safety_issue_setup_fingerprint_unlock_subtitle;
            actionTitleResId = R.string.biometric_safety_issue_setup_action_title;
            enrollActivityClass = FingerprintEnroll.class;
        } else if (!isFpEnrollNeeded && isFaceEnrollNeeded && isFaceSource) {
            titleResId =
                    isManagedProfile
                            ? R.string.biometric_safety_issue_setup_face_unlock_title_for_work
                            : R.string.biometric_safety_issue_setup_face_unlock_title;
            subtitleResId = R.string.biometric_safety_issue_setup_face_unlock_subtitle;
            actionTitleResId = R.string.biometric_safety_issue_setup_action_title;
            enrollActivityClass = FaceEnroll.class;
        } else if (fpEnrolledCount == 1 && !isFaceEnrollNeeded && isFingerprintSource) {
            titleResId =
                    isManagedProfile
                            ? R.string.biometric_safety_issue_add_more_fingerprints_title_for_work
                            : R.string.biometric_safety_issue_add_more_fingerprints_title;
            subtitleResId = R.string.biometric_safety_issue_add_more_fingerprints_subtitle;
            actionTitleResId = R.string.biometric_safety_issue_add_more_action_title;
            enrollActivityClass = FingerprintEnroll.class;
        } else {
            return null;
        }

        return new SafetySourceIssue.Builder(
                        "BiometricUnlockIssue",
                        mContext.getString(titleResId),
                        mContext.getString(subtitleResId),
                        SafetySourceData.SEVERITY_LEVEL_INFORMATION,
                        "BiometricUnlockIssueType")
                .setIssueCategory(SafetySourceIssue.ISSUE_CATEGORY_DEVICE)
                .addAction(
                        new SafetySourceIssue.Action.Builder(
                                        "SetBiometricUnlockActionId",
                                        mContext.getString(actionTitleResId),
                                        createActionPendingIntent(enrollActivityClass, 0, userId))
                                .build())
                .setIssueActionability(SafetySourceIssue.ISSUE_ACTIONABILITY_MANUAL)
                .setOnDismissPendingIntent(createOnDismissPendingIntent())
                .build();
    }

    private PendingIntent createActionPendingIntent(Class<?> cls, int requestCode, int userId) {
        return PendingIntent.getActivity(
                mContext,
                requestCode,
                new Intent(mContext, cls)
                        .putExtra("launch_from_safety_source_issue", true)
                        .putExtra(Intent.EXTRA_USER_ID, userId),
                PendingIntent.FLAG_IMMUTABLE);
    }

    private PendingIntent createOnDismissPendingIntent() {
        return PendingIntent.getBroadcast(
                mContext, 0, new Intent(ACTION_SAFETY_ISSUE_DISMISS), PendingIntent.FLAG_IMMUTABLE);
    }
}
