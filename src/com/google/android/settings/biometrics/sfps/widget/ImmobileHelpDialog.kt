package com.google.android.settings.biometrics.sfps.widget

import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Rect
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.TouchDelegate
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.settings.R
import com.google.android.setupdesign.util.ThemeHelper

class ImmobileHelpDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder =
            if (ThemeHelper.shouldApplyGlifExpressiveStyle(requireContext())) {
                MaterialAlertDialogBuilder(requireContext())
            } else {
                AlertDialog.Builder(requireContext())
            }
        var continueText =
            requireActivity().getString(R.string.setup_fingerprint_enroll_immobile_help_continue)
        val message =
            builder
                .setTitle(R.string.setup_fingerprint_enroll_immobile_help_title)
                .setMessage(R.string.setup_fingerprint_enroll_immobile_help_subtitle)
        if (TextUtils.isEmpty(continueText)) {
            continueText =
                requireActivity()
                    .getString(R.string.setup_fingerprint_enroll_immobile_help_continue_fallback)
        }
        message.setPositiveButton(continueText) { dialog, which ->
            if (which != DialogInterface.BUTTON_POSITIVE) {
                return@setPositiveButton
            }
            FingerprintExtUtils.resumeEnroll()
            dialog.dismiss()
        }
        val dialog = builder.create()
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)
        dialog.onBackPressedDispatcher.addCallback(
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    FingerprintExtUtils.resumeEnroll()
                    dialog.dismiss()
                }
            }
        )
        return dialog
    }

    override fun onResume() {
        // Stock wraps the "No dialog created!" log and resumeEnroll() in a lambda that is
        // never invoked, so nothing happens when the dialog is missing.
        if (dialog != null) {
            setUpTouchDelegate((dialog as? AlertDialog)?.getButton(DialogInterface.BUTTON_POSITIVE))
            adjustDimensionsIfNeeded(dialog)
        }
        super.onResume()
    }

    private fun setUpTouchDelegate(button: View?) {
        if (button == null) {
            return
        }
        val parent = button.parent as View
        lateinit var listener: ViewTreeObserver.OnGlobalLayoutListener
        listener = ViewTreeObserver.OnGlobalLayoutListener {
            parent.touchDelegate =
                TouchDelegate(Rect(parent.left, button.top, parent.right, button.bottom), button)
            parent.viewTreeObserver.removeOnGlobalLayoutListener(listener)
        }
        parent.viewTreeObserver.addOnGlobalLayoutListener(listener)
    }

    private fun adjustDimensionsIfNeeded(dialog: Dialog?) {
        val displayWidth = resources.displayMetrics.widthPixels
        val dialogWidth = resources.getDimensionPixelSize(R.dimen.immobile_dialog_width)
        dialog
            ?.window
            ?.setLayout(
                if (dialogWidth > displayWidth) ViewGroup.LayoutParams.WRAP_CONTENT
                else dialogWidth,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        Log.d(TAG, "Immobile dialog: dialogWidth=$dialogWidth, displayWidth=$displayWidth")
    }

    private companion object {
        const val TAG = "ImmobileHelpDialog"
    }
}
