package com.google.android.settings.biometrics.usudfps.ui.view

import android.app.Dialog
import android.content.DialogInterface
import android.hardware.biometrics.BiometricFingerprintConstants.FINGERPRINT_ERROR_TIMEOUT
import android.hardware.biometrics.BiometricFingerprintConstants.FINGERPRINT_ERROR_UNABLE_TO_PROCESS
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.FloatingWindow
import androidx.navigation.fragment.findNavController
import com.android.settings.R as AR
import com.android.settings.biometrics.fingerprint.FingerprintErrorDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.settings.biometrics.fingerprint.ui.viewmodel.FingerprintEnrollResult
import com.google.android.settings.biometrics.fingerprint.ui.viewmodel.FingerprintMetricsViewModel
import com.google.android.settings.biometrics.fingerprint.ui.viewmodel.SetEnrollResultViewModel
import com.google.android.setupdesign.util.ThemeHelper
import kotlinx.coroutines.launch

class EnrollUsUdfpsErrorDialog : DialogFragment(), FloatingWindow {

    private val setEnrollResultViewModel: SetEnrollResultViewModel by activityViewModels()
    private val metricsViewModel: FingerprintMetricsViewModel by activityViewModels()
    private var onDialogBtnClicked = false

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val errorId = requireArguments().getInt(KEY_FINGERPRINT_MESSAGE_ID)
        val isSuw = requireArguments().getBoolean(KEY_IS_SUW)

        val errorTitle = FingerprintErrorDialog.getErrorTitle(errorId)
        val errorMessage =
            if (isSuw) {
                FingerprintErrorDialog.getSetupErrorMessage(errorId)
            } else {
                FingerprintErrorDialog.getErrorMessage(errorId)
            }

        val builder: AlertDialog.Builder =
            if (ThemeHelper.shouldApplyGlifExpressiveStyle(requireContext())) {
                MaterialAlertDialogBuilder(requireActivity())
            } else {
                AlertDialog.Builder(requireActivity())
            }

        builder.setTitle(errorTitle).setMessage(errorMessage)

        val okClickListener = DialogInterface.OnClickListener { _, _ ->
            onDialogBtnClicked = true
            lifecycleScope.launch {
                val result =
                    if (errorId == FINGERPRINT_ERROR_TIMEOUT) {
                        FingerprintEnrollResult.ENROLL_ERROR_DIALOG_OK_BUTTON_TIMEOUT
                    } else {
                        FingerprintEnrollResult.ENROLL_ERROR_DIALOG_OK_BUTTON_FINISH
                    }
                metricsViewModel.setErrorCode(errorId)
                setEnrollResultViewModel.emit(result)
            }
        }

        if (errorId == FINGERPRINT_ERROR_UNABLE_TO_PROCESS) {
            builder.setPositiveButton(
                AR.string.security_settings_fingerprint_enroll_dialog_try_again
            ) { _, _ ->
                onDialogBtnClicked = true
                dismiss()
            }
            builder.setNegativeButton(
                AR.string.security_settings_fingerprint_enroll_dialog_ok,
                okClickListener,
            )
        } else {
            builder.setPositiveButton(
                AR.string.security_settings_fingerprint_enroll_dialog_ok,
                okClickListener,
            )
        }

        return builder.create().apply {
            setCancelable(false)
            setCanceledOnTouchOutside(false)
        }
    }

    override fun onCancel(dialog: DialogInterface) {
        Log.d(TAG, "Dialog was canceled, onDialogBtnClicked:$onDialogBtnClicked")
        if (!onDialogBtnClicked) {
            findNavController().popBackStack()
        }
        super.onCancel(dialog)
    }

    companion object {
        const val TAG = "EnrollUsUdfpsErrorDialog"
        private const val KEY_FINGERPRINT_MESSAGE_ID = "fingerprint_message_id"
        private const val KEY_IS_SUW = "is_suw"

        fun newInstance(errorId: Int, isSuw: Boolean): EnrollUsUdfpsErrorDialog {
            return EnrollUsUdfpsErrorDialog().apply {
                arguments =
                    Bundle().apply {
                        putInt(KEY_FINGERPRINT_MESSAGE_ID, errorId)
                        putBoolean(KEY_IS_SUW, isSuw)
                    }
            }
        }
    }
}
