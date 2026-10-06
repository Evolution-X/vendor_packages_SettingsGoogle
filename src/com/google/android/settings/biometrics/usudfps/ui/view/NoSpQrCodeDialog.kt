package com.google.android.settings.biometrics.usudfps.ui.view

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.google.android.settings.R

class NoSpQrCodeDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.screen_protector_no_qr_code_title)
            .setMessage(R.string.screen_protector_no_qr_code_msg)
            .setCancelable(false)
            .setPositiveButton(R.string.screen_protector_no_qr_code_ok) { _, _ -> dismiss() }
            .create()
            .apply {
                setCancelable(false)
                setCanceledOnTouchOutside(false)
            }

    companion object {
        fun showDialog(fragmentManager: FragmentManager) {
            NoSpQrCodeDialog().show(fragmentManager, NoSpQrCodeDialog::class.java.name)
        }
    }
}
