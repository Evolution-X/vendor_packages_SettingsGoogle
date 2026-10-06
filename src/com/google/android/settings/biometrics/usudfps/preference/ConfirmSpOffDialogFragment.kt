package com.google.android.settings.biometrics.usudfps.preference

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import com.google.android.settings.R

class ConfirmSpOffDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        AlertDialog.Builder(requireActivity())
            .setTitle(R.string.security_settings_udfps_screen_protector_remove_title)
            .setMessage(R.string.security_settings_udfps_screen_protector_remove_description)
            .setPositiveButton(R.string.security_settings_udfps_screen_protector_confirm_button) {
                _,
                _ ->
                passResult(true)
            }
            .setNegativeButton(android.R.string.cancel) { _, _ -> passResult(false) }
            .create()

    private fun passResult(confirmed: Boolean) {
        setFragmentResult(REQUEST_KEY, bundleOf(KEY_RESULT_CONFIRMED_OFF to confirmed))
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        passResult(false)
    }

    companion object {
        const val REQUEST_KEY = "ConfirmSpOffDialogFragment"
        const val KEY_RESULT_CONFIRMED_OFF = "result_confirmed_off"
    }
}
