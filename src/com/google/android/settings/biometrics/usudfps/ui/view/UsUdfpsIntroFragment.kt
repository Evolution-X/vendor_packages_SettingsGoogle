package com.google.android.settings.biometrics.usudfps.ui.view

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import com.google.android.settings.biometrics.fingerprint.ui.view.IntroFragment
import com.google.android.settings.biometrics.fingerprint.ui.viewmodel.UsUdfpsCalibratorViewModel
import com.google.android.settings.biometrics.usudfps.factory.UsudfpsViewModelFactory

open class UsUdfpsIntroFragment : IntroFragment() {

    private val calibratorInitViewModel: UsUdfpsCalibratorViewModel by
        activityViewModels(extrasProducer = { requireActivity().defaultViewModelCreationExtras }) {
            defaultViewModelProviderFactory
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate(), calibrator: ${calibratorInitViewModel.calibratorUuid}")
    }

    override val defaultViewModelProviderFactory: ViewModelProvider.Factory
        get() = UsudfpsViewModelFactory()

    companion object {
        private const val TAG = "UsudfpsIntroFragment"
    }
}
