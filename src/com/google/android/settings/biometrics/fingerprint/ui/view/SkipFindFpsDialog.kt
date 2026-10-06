package com.google.android.settings.biometrics.fingerprint.ui.view

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.android.settings.R as AR
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.settings.biometrics.fingerprint.ui.viewmodel.FingerprintEnrollResult
import com.google.android.settings.biometrics.fingerprint.ui.viewmodel.FingerprintMetricsViewModel
import com.google.android.settings.biometrics.fingerprint.ui.viewmodel.SetEnrollResultViewModel
import com.google.android.setupdesign.util.ThemeHelper
import kotlinx.coroutines.launch

class SkipFindFpsDialog : DialogFragment() {

    private val setEnrollResultViewModel: SetEnrollResultViewModel by activityViewModels()
    private val metricsViewModel: FingerprintMetricsViewModel by activityViewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()
        val activity = requireActivity()
        return if (ThemeHelper.shouldApplyGlifExpressiveStyle(context)) {
            MaterialAlertDialogBuilder(activity)
                .setTitle(AR.string.setup_fingerprint_enroll_skip_title)
                .setMessage(AR.string.setup_fingerprint_enroll_skip_after_adding_lock_text)
                .setPositiveButton(AR.string.skip_anyway_button_label) { _, _ ->
                    lifecycleScope.launch {
                        setEnrollResultViewModel.emit(
                            FingerprintEnrollResult.FIND_SENSOR_SKIP_BUTTON
                        )
                    }
                }
                .setNegativeButton(AR.string.go_back_button_label, null)
                .create()
        } else {
            android.app.AlertDialog.Builder(activity)
                .setTitle(AR.string.setup_fingerprint_enroll_skip_title)
                .setMessage(AR.string.setup_fingerprint_enroll_skip_after_adding_lock_text)
                .setPositiveButton(AR.string.skip_anyway_button_label) { _, _ ->
                    lifecycleScope.launch {
                        setEnrollResultViewModel.emit(
                            FingerprintEnrollResult.FIND_SENSOR_SKIP_BUTTON
                        )
                    }
                }
                .setNegativeButton(AR.string.go_back_button_label, null)
                .create()
        }
    }

    companion object {
        fun showDialog(fragmentManager: FragmentManager) {
            SkipFindFpsDialog().show(fragmentManager, SkipFindFpsDialog::class.java.name)
        }
    }
}
