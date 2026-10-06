package com.google.android.settings.biometrics.fingerprint.modules.sfps.widget

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.android.settings.R as AR
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.setupdesign.util.ThemeHelper

class FingerprintTouchDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder =
            if (ThemeHelper.shouldApplyGlifExpressiveStyle(requireContext())) {
                MaterialAlertDialogBuilder(requireActivity())
            } else {
                AlertDialog.Builder(requireActivity())
            }
        builder
            .setTitle(AR.string.security_settings_fingerprint_enroll_touch_dialog_title)
            .setMessage(AR.string.security_settings_fingerprint_enroll_touch_dialog_message)
            .setPositiveButton(AR.string.security_settings_fingerprint_enroll_dialog_ok) { dialog, _
                ->
                dialog.dismiss()
            }
        return builder.create()
    }
}
