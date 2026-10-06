package com.google.android.settings.biometrics.fingerprint.ui.view

import android.app.Activity
import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.android.settings.R as AR
import com.android.settings.biometrics.BiometricUtils

class SplitScreenDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val alertDialog =
            AlertDialog.Builder(requireContext())
                .setTitle(AR.string.biometric_settings_add_fingerprint_in_split_mode_title)
                .setMessage(AR.string.biometric_settings_add_fingerprint_in_split_mode_message)
                .setCancelable(false)
                .setPositiveButton(AR.string.biometric_settings_add_biometrics_in_split_mode_ok) {
                    dialog,
                    _ ->
                    dismiss()
                }
                .create()
        alertDialog.setCancelable(false)
        alertDialog.setCanceledOnTouchOutside(false)
        return alertDialog
    }

    companion object {
        fun shouldShowDialog(activity: Activity): Boolean {
            return BiometricUtils.isSplitScreenEnrollmentDisabled(activity)
        }

        fun dismissExistingDialog(fragmentManager: FragmentManager) {
            val existing =
                fragmentManager.findFragmentByTag(SplitScreenDialog::class.java.name)
                    as? SplitScreenDialog
            existing?.dismiss()
        }

        fun showDialog(fragmentManager: FragmentManager) {
            SplitScreenDialog().show(fragmentManager, SplitScreenDialog::class.java.name)
        }
    }
}
