package com.google.android.settings.biometrics.udfps.factory

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.android.settings.SettingsApplication
import com.google.android.settings.biometrics.fingerprint.data.datasource.FingerprintManagerDataSource
import com.google.android.settings.biometrics.fingerprint.data.repository.FingerprintsRepositoryImpl
import com.google.android.settings.biometrics.fingerprint.factory.FingerprintDataSourceFactory
import com.google.android.settings.biometrics.fingerprint.factory.FingerprintViewModelFactory
import com.google.android.settings.biometrics.fingerprint.interactor.FingerprintEnrollStageThresholdInteractorImpl
import com.google.android.settings.biometrics.fingerprint.interactor.SafetySourceUpdaterImpl
import com.google.android.settings.biometrics.udfps.ui.viewmodel.ConfirmUdfpsViewModel
import com.google.android.settings.biometrics.udfps.ui.viewmodel.EnrollUdfpsViewModel
import com.google.android.settings.biometrics.udfps.ui.viewmodel.EnrollUdfpsViewModelImpl
import com.google.android.settings.biometrics.udfps.ui.viewmodel.FindUdfpsViewModel

class UdfpsViewModelFactory : FingerprintViewModelFactory() {

    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val application =
            extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as? Application
        if (application == null) {
            Log.w(TAG, "create(), null application")
            return super.create(modelClass, extras)
        }
        val biometricsEnvironment = (application as SettingsApplication).biometricEnvironment

        when {
            modelClass.isAssignableFrom(FindUdfpsViewModel::class.java) -> {
                val enrollmentRequest = extras[ENROLLMENT_REQUEST_KEY]
                if (enrollmentRequest != null && biometricsEnvironment != null) {
                    @Suppress("UNCHECKED_CAST")
                    return FindUdfpsViewModel(enrollmentRequest.isSuw) as T
                }
            }
            modelClass.isAssignableFrom(EnrollUdfpsViewModel::class.java) -> {
                val enrollmentRequest = extras[ENROLLMENT_REQUEST_KEY]
                val credentialModel = extras[CREDENTIAL_MODEL_KEY]
                val fingerprintManager = getFingerprintManager(application)
                if (
                    enrollmentRequest != null &&
                        credentialModel != null &&
                        biometricsEnvironment != null &&
                        fingerprintManager != null
                ) {
                    val dataSourceFactory = FingerprintDataSourceFactory
                    val fingerprintManagerDataSource: FingerprintManagerDataSource =
                        dataSourceFactory.getFingerprintManagerDataSource(fingerprintManager)
                    @Suppress("UNCHECKED_CAST")
                    return EnrollUdfpsViewModelImpl(
                        SafetySourceUpdaterImpl(application),
                        enrollmentRequest.isSuw,
                        enrollmentRequest.isFastEnroll,
                        enrollmentRequest.enrollReason,
                        credentialModel,
                        biometricsEnvironment.createUserInteractor(),
                        biometricsEnvironment.sensorInteractor,
                        FingerprintEnrollStageThresholdInteractorImpl(
                            FingerprintsRepositoryImpl.getInstance(
                                fingerprintManagerDataSource,
                                dataSourceFactory.getResourcesDataSource(application.resources),
                            )
                        ),
                        biometricsEnvironment.orientationInteractor,
                        biometricsEnvironment.createAccessibilityInteractor(),
                        biometricsEnvironment.createFingerprintEnrollInteractor(),
                        biometricsEnvironment.createFingerprintsEnrolledInteractor(),
                        biometricsEnvironment.createCanEnrollFingerprintsInteractor(),
                    )
                        as T
                }
            }
            modelClass.isAssignableFrom(ConfirmUdfpsViewModel::class.java) -> {
                val enrollmentRequest = extras[ENROLLMENT_REQUEST_KEY]
                val credentialModel = extras[CREDENTIAL_MODEL_KEY]
                if (
                    enrollmentRequest != null &&
                        credentialModel != null &&
                        biometricsEnvironment != null
                ) {
                    @Suppress("UNCHECKED_CAST")
                    return ConfirmUdfpsViewModel(
                        enrollmentRequest.isSuw,
                        credentialModel.userId,
                        biometricsEnvironment.createUserInteractor(),
                        biometricsEnvironment.createFingerprintsEnrolledInteractor(),
                        biometricsEnvironment.createCanEnrollFingerprintsInteractor(),
                    )
                        as T
                }
            }
        }
        Log.e(TAG, "create(), missing factory method for $modelClass")
        @Suppress("UNCHECKED_CAST")
        return create(modelClass)
    }

    companion object {
        private const val TAG = "UdfpsViewModelFactory"
    }
}
