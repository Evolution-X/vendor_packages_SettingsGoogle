package com.google.android.settings.biometrics.sfps.widget

import android.app.Dialog
import android.os.Bundle
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import com.android.settings.R as AR
import com.android.settings.biometrics.fingerprint.FingerprintErrorDialog as AospFingerprintErrorDialog

class FingerprintErrorDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val errorMsgId = requireArguments().getInt(KEY_MESSAGE_ID)
        val shouldShowTryAgain = requireArguments().getBoolean(KEY_SHOULD_SHOW_TRY_AGAIN)
        val isSuw = requireArguments().getBoolean(KEY_IS_SUW)
        val errorTitle = AospFingerprintErrorDialog.getErrorTitle(errorMsgId)
        val errorMessage =
            if (isSuw) {
                AospFingerprintErrorDialog.getSetupErrorMessage(errorMsgId)
            } else {
                AospFingerprintErrorDialog.getErrorMessage(errorMsgId)
            }
        val builder = AlertDialog.Builder(requireContext())
        builder.setMessage(errorMessage).setTitle(errorTitle).setPositiveButton(
            AR.string.security_settings_fingerprint_enroll_dialog_ok
        ) { dialog, _ ->
            setFragmentResult(
                RESULT_LISTENER,
                bundleOf(KEY_MESSAGE_ID to errorMsgId, KEY_WAS_BACK_PRESSED to false),
            )
            dialog.dismiss()
        }
        if (shouldShowTryAgain) {
            builder.setPositiveButton(
                AR.string.security_settings_fingerprint_enroll_dialog_try_again
            ) { dialog, _ ->
                dialog.dismiss()
                setFragmentResult(TRY_AGAIN_LISTENER, bundleOf(TRY_AGAIN_LISTENER to true))
            }
            builder.setNegativeButton(AR.string.security_settings_fingerprint_enroll_dialog_ok) {
                dialog,
                _ ->
                setFragmentResult(
                    RESULT_LISTENER,
                    bundleOf(KEY_MESSAGE_ID to errorMsgId, KEY_WAS_BACK_PRESSED to false),
                )
                dialog.dismiss()
            }
        }
        val dialog = builder.create()
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)
        dialog.onBackPressedDispatcher.addCallback {
            dismiss()
            setFragmentResult(
                RESULT_LISTENER,
                bundleOf(KEY_MESSAGE_ID to errorMsgId, KEY_WAS_BACK_PRESSED to true),
            )
        }
        return dialog
    }

    companion object {
        const val RESULT_LISTENER = "sfps_result_listener"
        const val TRY_AGAIN_LISTENER = "sfps_try_again_listener"
        const val KEY_MESSAGE_ID = "fingerprint_message_id"
        const val KEY_WAS_BACK_PRESSED = "was_back_pressed"
        private const val KEY_IS_SUW = "is_suw"
        private const val KEY_SHOULD_SHOW_TRY_AGAIN = "should_show_try_again"

        fun newInstance(
            errorMsgId: Int,
            isSuw: Boolean,
            shouldShowTryAgain: Boolean = false,
        ): FingerprintErrorDialog {
            val args =
                Bundle().apply {
                    putInt(KEY_MESSAGE_ID, errorMsgId)
                    putBoolean(KEY_IS_SUW, isSuw)
                    putBoolean(KEY_SHOULD_SHOW_TRY_AGAIN, shouldShowTryAgain)
                }
            return FingerprintErrorDialog().apply { arguments = args }
        }
    }
}
