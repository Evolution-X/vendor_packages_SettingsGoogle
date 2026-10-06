package com.google.android.settings.biometrics.face;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;

import com.google.android.settings.R;
import com.google.android.setupdesign.R.anim;

public class FaceErrorDialog extends DialogFragment {

    static FaceErrorDialog newInstance(
            CharSequence error, int errMsgId, boolean requireDiversity, boolean fromSetupWizard) {
        Bundle args = new Bundle();
        args.putCharSequence("error_msg", error);
        args.putInt("error_id", errMsgId);
        args.putBoolean("require_diversity", requireDiversity);
        args.putBoolean("from_suw", fromSetupWizard);
        FaceErrorDialog dialog = new FaceErrorDialog();
        dialog.setArguments(args);
        return dialog;
    }

    private void finishWithResult(DialogInterface dialog, int resultCode) {
        dialog.dismiss();
        FragmentActivity activity = getActivity();
        activity.setResult(resultCode);
        activity.finish();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        CharSequence error = getArguments().getCharSequence("error_msg");
        final int errMsgId = getArguments().getInt("error_id");
        final boolean isTimeoutWithDiversity =
                errMsgId == 3 && getArguments().getBoolean("require_diversity");
        boolean fromSetupWizard = getArguments().getBoolean("from_suw");
        DialogInterface.OnClickListener listener =
                (dialog, which) ->
                        onErrorDialogClick(isTimeoutWithDiversity, errMsgId, dialog, which);
        FaceEnrollDialogFactory.DialogBuilder builder =
                FaceEnrollDialogFactory.newBuilder(getActivity());
        if (isTimeoutWithDiversity) {
            builder.setTitle(R.string.security_settings_face_enroll_timeout_title);
            builder.setMessage(R.string.security_settings_face_enroll_timeout_message);
            builder.setPositiveButton(
                    R.string.security_settings_face_enroll_timeout_use_fast_setup, listener);
            builder.setNegativeButton(
                    R.string.security_settings_face_enroll_timeout_try_again, listener);
        } else if (errMsgId == 1003) {
            builder.setTitle(R.string.security_settings_face_enroll_too_hot_title);
            builder.setMessage(R.string.security_settings_face_enroll_too_hot_message);
            if (fromSetupWizard) {
                builder.setPositiveButton(
                        R.string.security_settings_face_enroll_too_hot_skip_face_unlock, listener);
            } else {
                builder.setPositiveButton(
                        R.string.security_settings_face_enroll_too_hot_exit_setup, listener);
            }
        } else {
            builder.setTitle(
                    com.android.settings.R.string.security_settings_face_enroll_error_dialog_title);
            builder.setMessage(error);
            builder.setPositiveButton(
                    com.android.settings.R.string.security_settings_face_enroll_dialog_ok,
                    listener);
        }
        return builder.setOnBackKeyListener(
                        (dialog, event) -> {
                            finishWithResult(dialog, 2 /* cancelled */);
                        })
                .build();
    }

    private void onErrorDialogClick(
            boolean isTimeoutWithDiversity, int errMsgId, DialogInterface dialog, int which) {
        if (isTimeoutWithDiversity) {
            if (which == DialogInterface.BUTTON_POSITIVE) {
                getActivity()
                        .overridePendingTransition(anim.sud_slide_next_in, anim.sud_slide_next_out);
                finishWithResult(dialog, 4 /* single capture from multi timeout */);
                return;
            } else if (which == DialogInterface.BUTTON_NEGATIVE) {
                finishWithResult(dialog, 5 /* retry enrollment */);
                return;
            }
            return;
        }
        final int resultCode;
        if (errMsgId != 3 && errMsgId != 1003) {
            resultCode = 1 /* error */;
        } else {
            resultCode = 2 /* cancelled: timeout or too hot */;
        }
        if (which == DialogInterface.BUTTON_POSITIVE) {
            finishWithResult(dialog, resultCode);
        }
    }
}
