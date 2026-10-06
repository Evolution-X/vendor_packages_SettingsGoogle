package com.google.android.settings.biometrics.fingerprint.feature

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.android.settings.R as AR
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.settings.R
import com.google.android.setupdesign.util.ThemeHelper

class UdfpsEnrollCalibrationDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog =
            if (ThemeHelper.shouldApplyGlifExpressiveStyle(requireContext())) {
                MaterialAlertDialogBuilder(requireActivity())
                    .setTitle(titleId)
                    .setMessage(messageId)
                    .setPositiveButton(positiveBtnTextId) { dialogInterface, _ ->
                        dialogInterface?.dismiss()
                    }
                    .create()
            } else {
                AlertDialog.Builder(requireActivity(), AR.style.Theme_AlertDialog)
                    .setTitle(titleId)
                    .setMessage(messageId)
                    .setPositiveButton(positiveBtnTextId) { dialogInterface, _ ->
                        dialogInterface?.dismiss()
                    }
                    .create()
            }
        setCancelable(false)
        checkNotNull(dialog)
        return dialog
    }

    private companion object {
        val titleId: Int = R.string.fingerprint_udfps_pre_enroll_runner_dialog_title
        val messageId: Int = R.string.fingerprint_udfps_pre_enroll_runner_dialog_message
        val positiveBtnTextId: Int = AR.string.done
    }
}
