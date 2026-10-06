package com.google.android.settings.biometrics.sfps.factory

import android.content.Context
import android.hardware.fingerprint.FingerprintEnrollOptions
import android.hardware.fingerprint.FingerprintManager
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.android.settings.SettingsApplication
import com.android.settings.Utils
import com.google.android.settings.R
import com.google.android.settings.biometrics.fingerprint.factory.FingerprintRepositoryFactory
import com.google.android.settings.biometrics.fingerprint.factory.FingerprintViewModelFactory.Companion.CREDENTIAL_MODEL_KEY
import com.google.android.settings.biometrics.fingerprint.factory.FingerprintViewModelFactory.Companion.ENROLLMENT_REQUEST_KEY
import com.google.android.settings.biometrics.fingerprint.interactor.FingerprintSensorTypeInteractorImpl
import com.google.android.settings.biometrics.fingerprint.interactor.PrivateProfileInteractorImpl
import com.google.android.settings.biometrics.fingerprint.modules.sfps.viewmodel.EnrollSfpsViewModel
import com.google.android.settings.biometrics.sfps.viewmodel.ConfirmSfpsViewModel
import com.google.android.settings.biometrics.sfps.viewmodel.FindSfpsViewModel

open class SfpsViewModelFactory : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val application = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
        if (application == null) {
            Log.w(TAG, "create(), null application")
            return super.create(modelClass)
        }
        if (modelClass.isAssignableFrom(FindSfpsViewModel::class.java)) {
            Log.d(TAG, "Creating FindSensorViewModel")
            val enrollmentRequest = extras[ENROLLMENT_REQUEST_KEY]!!
            val settingsApplication = application as SettingsApplication
            val biometricsEnvironment = settingsApplication.biometricEnvironment!!
            val options =
                FingerprintEnrollOptions.Builder()
                    .setEnrollReason(enrollmentRequest.enrollReason)
                    .build()
            val fingerprintManager = getFingerprintManager(application)
            if (fingerprintManager == null) {
                Log.e(TAG, "create(), null fingerprintManager")
                return super.create(modelClass)
            }
            val fingerprintsRepository =
                FingerprintRepositoryFactory.getFingerprintsRepository(
                    fingerprintManager,
                    settingsApplication.resources,
                )
            return FindSfpsViewModel(
                enrollmentRequest,
                biometricsEnvironment.foldStateInteractor,
                biometricsEnvironment.orientationInteractor,
                biometricsEnvironment.createFingerprintEnrollInteractor(),
                extras[CREDENTIAL_MODEL_KEY]!!,
                biometricsEnvironment.createUserInteractor(),
                biometricsEnvironment.createAccessibilityInteractor(),
                FingerprintSensorTypeInteractorImpl(fingerprintsRepository),
                options,
            )
                as T
        }
        if (modelClass.isAssignableFrom(EnrollSfpsViewModel::class.java)) {
            val settingsApplication = application as SettingsApplication
            val biometricsEnvironment = settingsApplication.biometricEnvironment!!
            val enrollmentRequest = extras[ENROLLMENT_REQUEST_KEY]!!
            val thresholds =
                settingsApplication.resources
                    .getStringArray(R.array.config_sfps_enroll_stage_thresholds)
                    .map { it.toFloat() }
                    .toFloatArray()
            return EnrollSfpsViewModel(
                enrollmentRequest.isFastEnroll,
                biometricsEnvironment.foldStateInteractor,
                biometricsEnvironment.orientationInteractor,
                biometricsEnvironment.createAccessibilityInteractor(),
                biometricsEnvironment.vibrationInteractor,
                biometricsEnvironment.createFingerprintEnrollInteractor(),
                enrollmentRequest,
                extras[CREDENTIAL_MODEL_KEY]!!,
                thresholds,
                biometricsEnvironment.createUserInteractor(),
                biometricsEnvironment.createFingerprintsEnrolledInteractor(),
                biometricsEnvironment.createCanEnrollFingerprintsInteractor(),
            )
                as T
        }
        if (modelClass.isAssignableFrom(ConfirmSfpsViewModel::class.java)) {
            val biometricsEnvironment = (application as SettingsApplication).biometricEnvironment!!
            val enrollmentRequest = extras[ENROLLMENT_REQUEST_KEY]!!
            val credentialModel = extras[CREDENTIAL_MODEL_KEY]!!
            return ConfirmSfpsViewModel(
                credentialModel.userId,
                biometricsEnvironment.createUserInteractor(),
                biometricsEnvironment.createFingerprintsEnrolledInteractor(),
                biometricsEnvironment.createCanEnrollFingerprintsInteractor(),
                PrivateProfileInteractorImpl(application),
                enrollmentRequest,
            )
                as T
        }
        Log.e(TAG, "create(), missing factory method for $modelClass")
        return create(modelClass)
    }

    protected open fun getFingerprintManager(context: Context): FingerprintManager? =
        Utils.getFingerprintManagerOrNull(context)

    companion object {
        private const val TAG = "FingerprintViewModelFactory"
    }
}
