package com.google.android.settings.biometrics.combination.ui.view

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import com.google.android.settings.R

class FrrDeleteAllFpsDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog =
            AlertDialog.Builder(requireActivity())
                .setTitle(R.string.delete_all_fps_dialog_title)
                .setMessage(R.string.delete_all_fps_dialog_msg)
                .setPositiveButton(R.string.delete_all_fps_dialog_delete_button) { _, _ ->
                    sendFragmentResult(true)
                }
                .setNegativeButton(R.string.delete_all_fps_dialog_cancel_button) { _, _ ->
                    sendFragmentResult(false)
                }
                .setCancelable(true)
                .create()
        checkNotNull(dialog)
        dialog.setCancelable(true)
        dialog.setCanceledOnTouchOutside(true)
        dialog.setOnCancelListener { sendFragmentResult(false) }
        return dialog
    }

    private fun sendFragmentResult(confirmed: Boolean) {
        setFragmentResult(
            "FrrDeleteAllFpsDialogFragment",
            bundleOf("result_confirmed_delete_all" to confirmed),
        )
    }
}
