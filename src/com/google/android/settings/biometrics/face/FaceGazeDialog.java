package com.google.android.settings.biometrics.face;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.fragment.app.DialogFragment;

import com.google.android.settings.R;

public class FaceGazeDialog extends DialogFragment {

    private DialogInterface.OnClickListener mButtonListener;

    static FaceGazeDialog newInstance() {
        return new FaceGazeDialog();
    }

    public void setButtonListener(DialogInterface.OnClickListener listener) {
        mButtonListener = listener;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return FaceEnrollDialogFactory.newBuilder(getActivity())
                .setTitle(R.string.face_enrolling_gaze_dialog_title)
                .setMessage(R.string.face_enrolling_gaze_dialog_message)
                .setPositiveButton(
                        R.string.face_enrolling_gaze_dialog_continue,
                        (dialog, which) -> {
                            if (mButtonListener != null) {
                                mButtonListener.onClick(dialog, which);
                            }
                        })
                .setNegativeButton(
                        R.string.face_enrolling_gaze_dialog_cancel,
                        (dialog, which) -> {
                            if (mButtonListener != null) {
                                mButtonListener.onClick(dialog, which);
                            }
                        })
                .build();
    }
}
