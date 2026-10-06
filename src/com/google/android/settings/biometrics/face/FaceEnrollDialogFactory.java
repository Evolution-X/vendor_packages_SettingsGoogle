package com.google.android.settings.biometrics.face;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.KeyEvent;

public abstract class FaceEnrollDialogFactory {

    public interface OnBackKeyListener {
        void onBackKeyUp(DialogInterface dialog, KeyEvent event);
    }

    public static DialogBuilder newBuilder(Context context) {
        return new DialogBuilder(context);
    }

    private FaceEnrollDialogFactory() {}

    public static class DialogBuilder {
        private final AlertDialog.Builder mBuilder;
        private OnBackKeyListener mOnBackKeyListener;

        private DialogBuilder(Context context) {
            mBuilder = new AlertDialog.Builder(context);
        }

        public DialogBuilder setTitle(int titleResId) {
            mBuilder.setTitle(titleResId);
            return this;
        }

        public DialogBuilder setMessage(int messageResId) {
            mBuilder.setMessage(messageResId);
            return this;
        }

        public DialogBuilder setMessage(CharSequence message) {
            mBuilder.setMessage(message);
            return this;
        }

        public DialogBuilder setPositiveButton(
                int textResId, DialogInterface.OnClickListener listener) {
            mBuilder.setPositiveButton(textResId, listener);
            return this;
        }

        public DialogBuilder setNegativeButton(
                int textResId, DialogInterface.OnClickListener listener) {
            mBuilder.setNegativeButton(textResId, listener);
            return this;
        }

        public DialogBuilder setOnBackKeyListener(OnBackKeyListener onBackKeyListener) {
            mOnBackKeyListener = onBackKeyListener;
            return this;
        }

        public Dialog build() {
            AlertDialog alertDialog = mBuilder.setCancelable(false).create();
            alertDialog.setCanceledOnTouchOutside(false);
            if (mOnBackKeyListener != null) {
                alertDialog.setOnKeyListener(
                        new DialogInterface.OnKeyListener() {
                            private boolean mCanceled = false;

                            @Override
                            public boolean onKey(
                                    DialogInterface dialog, int keyCode, KeyEvent event) {
                                if (keyCode != KeyEvent.KEYCODE_BACK) {
                                    return false;
                                }
                                if (event.getAction() == KeyEvent.ACTION_UP && !mCanceled) {
                                    mCanceled = true;
                                    mOnBackKeyListener.onBackKeyUp(dialog, event);
                                }
                                return true;
                            }
                        });
            }
            return alertDialog;
        }
    }
}
