package com.google.android.settings.biometrics.face;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Log;

import androidx.fragment.app.FragmentActivity;

import com.google.android.settings.R;

public class FaceEnrollTrampoline extends FragmentActivity {
    private static final String TAG = "FaceEnrollTrampoline";

    private static final int REQUEST_ENROLL = 1;
    private static final int REQUEST_PARTICIPATION = 2;

    private Intent mExtras;
    private boolean mNextLaunched;
    private int mUserId;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!getPackageName().equals(getLaunchedFromPackage())) {
            Log.w(TAG, "Invalid caller: " + getLaunchedFromPackage());
            finish();
            mNextLaunched = true;
            return;
        }
        mUserId = getIntent().getIntExtra(Intent.EXTRA_USER_ID, UserHandle.myUserId());
        if (savedInstanceState != null) {
            mNextLaunched = savedInstanceState.getBoolean("next_launched");
        }
        mExtras = getIntent();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("next_launched", mNextLaunched);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == 4 /* single capture from multi timeout */) {
            Intent intent = new Intent(mExtras);
            intent.putExtra("accessibility_diversity", false);
            intent.putExtra("from_multi_timeout", true);
            startEnrollActivity(intent);
            return;
        }
        if (resultCode == 5 /* retry enrollment */) {
            startEnrollActivity(mExtras);
            return;
        }
        if (resultCode == 11 /* vendor retry */) {
            setResult(RESULT_CANCELED, data);
            finish();
        } else if (resultCode == 13 /* overlay missing pending intent */) {
            Log.d(TAG, "Overlay reported no pending intent... launching enrollment anyway");
            startEnrollActivity(mExtras);
        } else {
            setResult(resultCode, data);
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mNextLaunched) {
            return;
        }
        mNextLaunched = true;
        if (Build.IS_ENG || Build.IS_USERDEBUG) {
            if (new FaceEnrollOverlayLauncher(this).maybeLaunchOverlayParticipationFlow(mExtras)) {
                return;
            }
            Log.e(TAG, "Using FaceEnrollParticipation");
            Intent intent = new Intent(this, FaceEnrollParticipation.class);
            intent.putExtras(mExtras);
            startActivityForResult(intent, REQUEST_PARTICIPATION);
            return;
        }
        startEnrollActivity(mExtras);
    }

    private void startEnrollActivity(Intent intent) {
        final Intent target;
        boolean useTrafficLight =
                getResources().getBoolean(R.bool.config_face_enroll_use_traffic_light);
        if (!useTrafficLight) {
            target = new Intent(this, FaceEnrollEnrolling.class);
        } else {
            target =
                    new Intent(
                            "com.google.android.settings.future.biometrics.faceenroll.action.ENROLL");
        }
        if (useTrafficLight) {
            String packageName = getString(R.string.config_face_enroll_traffic_light_package);
            if (TextUtils.isEmpty(packageName)) {
                throw new IllegalStateException("Package name must not be empty");
            }
            target.setPackage(packageName);
        }
        target.putExtras(intent);
        target.putExtra(Intent.EXTRA_USER_ID, mUserId);
        startActivityForResult(target, REQUEST_ENROLL);
    }
}
