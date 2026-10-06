package com.google.android.settings.biometrics.usudfps.factory

import android.os.UserManager
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.android.internal.widget.LockPatternUtils
import com.android.settings.SettingsApplication
import com.android.settings.biometrics.GatekeeperPasswordProvider
import com.google.android.settings.biometrics.combination.data.repository.AccessRepositoryImpl
import com.google.android.settings.biometrics.fingerprint.data.repository.FingerprintsRepositoryImpl
import com.google.android.settings.biometrics.fingerprint.data.repository.FrrRepositoryImpl
import com.google.android.settings.biometrics.fingerprint.data.repository.HareRepositoryImpl
import com.google.android.settings.biometrics.fingerprint.data.repository.Sp001AllowListRepositoryImpl
import com.google.android.settings.biometrics.fingerprint.factory.FingerprintDataSourceFactory
import com.google.android.settings.biometrics.fingerprint.factory.FingerprintViewModelFactory
import com.google.android.settings.biometrics.fingerprint.factory.UdfpsFingerprintExtSupplier
import com.google.android.settings.biometrics.fingerprint.interactor.FingerprintEnrollStageThresholdInteractorImpl
import com.google.android.settings.biometrics.fingerprint.interactor.HareModeGetterImpl
import com.google.android.settings.biometrics.fingerprint.interactor.SafetySourceUpdaterImpl
import com.google.android.settings.biometrics.fingerprint.interactor.ScreenProtectorInteractorImpl
import com.google.android.settings.biometrics.fingerprint.interactor.Sp001AllowListInteractorImpl
import com.google.android.settings.biometrics.fingerprint.interactor.SpAccessPolicyImpl
import com.google.android.settings.biometrics.fingerprint.ui.model.SpQrCode
import com.google.android.settings.biometrics.fingerprint.ui.viewmodel.LockPatternInteractorImpl
import com.google.android.settings.biometrics.usudfps.ui.viewmodel.ConfirmUsUdfpsViewModel
import com.google.android.settings.biometrics.usudfps.ui.viewmodel.ConfirmUsUdfpsViewModelImpl
import com.google.android.settings.biometrics.usudfps.ui.viewmodel.EnrollUsUdfpsViewModel
import com.google.android.settings.biometrics.usudfps.ui.viewmodel.EnrollUsUdfpsViewModelImpl
import com.google.android.settings.biometrics.usudfps.ui.viewmodel.FindUsUdfpsViewModel
import com.google.android.settings.biometrics.usudfps.ui.viewmodel.FindUsUdfpsWithSpViewModel
import com.google.android.settings.biometrics.usudfps.ui.viewmodel.FindUsUdfpsWithSpViewModelImpl
import com.google.android.settings.biometrics.usudfps.ui.viewmodel.SpApplyViewModel
import com.google.android.settings.biometrics.usudfps.ui.viewmodel.SpApplyViewModelImpl
import com.google.android.settings.biometrics.usudfps.ui.viewmodel.SpEduViewModel
import com.google.android.settings.biometrics.usudfps.ui.viewmodel.SpEduViewModelImpl
import com.google.android.settings.biometrics.usudfps.ui.viewmodel.SpQrCodeScannerViewModel
import com.google.android.settings.biometrics.usudfps.ui.viewmodel.SpQrCodeScannerViewModelImpl
import com.google.android.settings.biometrics.usudfps.ui.viewmodel.SpSetupResultViewModel
import com.google.android.settings.biometrics.usudfps.ui.viewmodel.SpSetupResultViewModelImpl
import com.google.android.settings.biometrics.usudfps.ui.viewmodel.SpSetupViewModel
import com.google.android.settings.biometrics.usudfps.ui.viewmodel.SpSetupViewModelImpl
import kotlinx.coroutines.Dispatchers

open class UsudfpsViewModelFactory : FingerprintViewModelFactory() {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val application = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
        if (application == null) {
            Log.w(TAG, "create(), null application")
            return super.create(modelClass, extras)
        }
        when {
            modelClass.isAssignableFrom(FindUsUdfpsViewModel::class.java) -> {
                val enrollmentRequest = extras[ENROLLMENT_REQUEST_KEY]
                val biometricsEnvironment =
                    (application as SettingsApplication).biometricEnvironment
                if (enrollmentRequest != null && biometricsEnvironment != null) {
                    return FindUsUdfpsViewModel(enrollmentRequest.isSuw) as T
                }
            }
            modelClass.isAssignableFrom(EnrollUsUdfpsViewModel::class.java) -> {
                val enrollmentRequest = extras[ENROLLMENT_REQUEST_KEY]
                val credentialModel = extras[CREDENTIAL_MODEL_KEY]
                val settingsApplication = application as SettingsApplication
                val biometricsEnvironment = settingsApplication.biometricEnvironment
                val fingerprintManager = getFingerprintManager(application)
                if (
                    enrollmentRequest != null &&
                        credentialModel != null &&
                        biometricsEnvironment != null &&
                        fingerprintManager != null
                ) {
                    val dataSourceFactory = FingerprintDataSourceFactory
                    return EnrollUsUdfpsViewModelImpl(
                        SafetySourceUpdaterImpl(application),
                        enrollmentRequest.isSuw,
                        enrollmentRequest.isFastEnroll,
                        enrollmentRequest.enrollReason,
                        credentialModel,
                        biometricsEnvironment.createUserInteractor(),
                        biometricsEnvironment.sensorInteractor,
                        FingerprintEnrollStageThresholdInteractorImpl(
                            FingerprintsRepositoryImpl.getInstance(
                                dataSourceFactory.getFingerprintManagerDataSource(
                                    fingerprintManager
                                ),
                                dataSourceFactory.getResourcesDataSource(
                                    settingsApplication.resources
                                ),
                            )
                        ),
                        biometricsEnvironment.orientationInteractor,
                        biometricsEnvironment.createAccessibilityInteractor(),
                        biometricsEnvironment.createFingerprintEnrollInteractor(),
                        biometricsEnvironment.createFingerprintsEnrolledInteractor(),
                        biometricsEnvironment.createCanEnrollFingerprintsInteractor(),
                        HareModeGetterImpl(
                            HareRepositoryImpl.getInstance(
                                UdfpsFingerprintExtSupplier,
                                appScope,
                                Dispatchers.IO,
                            )
                        ),
                    )
                        as T
                }
            }
            modelClass.isAssignableFrom(ConfirmUsUdfpsViewModel::class.java) -> {
                val enrollmentRequest = extras[ENROLLMENT_REQUEST_KEY]
                val credentialModel = extras[CREDENTIAL_MODEL_KEY]
                val biometricsEnvironment =
                    (application as SettingsApplication).biometricEnvironment
                if (
                    enrollmentRequest != null &&
                        credentialModel != null &&
                        biometricsEnvironment != null
                ) {
                    return ConfirmUsUdfpsViewModelImpl(
                        enrollmentRequest.isSuw,
                        credentialModel.userId,
                        biometricsEnvironment.createUserInteractor(),
                        biometricsEnvironment.createFingerprintsEnrolledInteractor(),
                        biometricsEnvironment.createCanEnrollFingerprintsInteractor(),
                        HareModeGetterImpl(
                            HareRepositoryImpl.getInstance(
                                UdfpsFingerprintExtSupplier,
                                appScope,
                                Dispatchers.IO,
                            )
                        ),
                    )
                        as T
                }
            }
            modelClass.isAssignableFrom(SpSetupResultViewModel::class.java) -> {
                return SpSetupResultViewModelImpl() as T
            }
            modelClass.isAssignableFrom(SpQrCodeScannerViewModel::class.java) -> {
                val isEnrolling = extras[ENROLLMENT_REQUEST_KEY] != null
                val userManager = application.getSystemService(UserManager::class.java)
                if (userManager != null) {
                    return SpQrCodeScannerViewModelImpl(
                        isEnrolling,
                        ScreenProtectorInteractorImpl(
                            FrrRepositoryImpl.getInstance(UdfpsFingerprintExtSupplier),
                            AccessRepositoryImpl.getInstance(userManager),
                        ),
                        Sp001AllowListInteractorImpl(
                            Sp001AllowListRepositoryImpl.getInstance(application.resources)
                        ),
                    )
                        as T
                }
            }
            modelClass.isAssignableFrom(SpSetupViewModel::class.java) -> {
                val credentialModel = extras[CREDENTIAL_MODEL_KEY]
                val userManager = application.getSystemService(UserManager::class.java)
                if (credentialModel != null && userManager != null) {
                    val lockPatternUtils = LockPatternUtils(application)
                    val accessRepository = AccessRepositoryImpl.getInstance(userManager)
                    return SpSetupViewModelImpl(
                        credentialModel,
                        LockPatternInteractorImpl(
                            credentialModel.userId,
                            lockPatternUtils,
                            GatekeeperPasswordProvider(lockPatternUtils),
                        ),
                        SpAccessPolicyImpl(accessRepository),
                        ScreenProtectorInteractorImpl(
                            FrrRepositoryImpl.getInstance(UdfpsFingerprintExtSupplier),
                            accessRepository,
                        ),
                        Sp001AllowListInteractorImpl(
                            Sp001AllowListRepositoryImpl.getInstance(application.resources)
                        ),
                    )
                        as T
                }
            }
            modelClass.isAssignableFrom(SpApplyViewModel::class.java) -> {
                val spQrCode = extras[SP_QR_CODE_KEY]
                val userManager = application.getSystemService(UserManager::class.java)
                if (spQrCode != null && userManager != null) {
                    return SpApplyViewModelImpl(
                        spQrCode,
                        SpAccessPolicyImpl(AccessRepositoryImpl.getInstance(userManager)),
                        Sp001AllowListInteractorImpl(
                            Sp001AllowListRepositoryImpl.getInstance(application.resources)
                        ),
                    )
                        as T
                }
            }
            modelClass.isAssignableFrom(SpEduViewModel::class.java) -> {
                val isEnrolling = extras[ENROLLMENT_REQUEST_KEY] != null
                val userManager = application.getSystemService(UserManager::class.java)
                if (userManager != null) {
                    return SpEduViewModelImpl(
                        isEnrolling,
                        ScreenProtectorInteractorImpl(
                            FrrRepositoryImpl.getInstance(UdfpsFingerprintExtSupplier),
                            AccessRepositoryImpl.getInstance(userManager),
                        ),
                        Sp001AllowListInteractorImpl(
                            Sp001AllowListRepositoryImpl.getInstance(application.resources)
                        ),
                    )
                        as T
                }
            }
            modelClass.isAssignableFrom(FindUsUdfpsWithSpViewModel::class.java) -> {
                val isSuw = extras[ENROLLMENT_REQUEST_KEY]?.isSuw ?: false
                val userManager = application.getSystemService(UserManager::class.java)
                val settingsApplication = application as SettingsApplication
                val biometricsEnvironment = settingsApplication.biometricEnvironment
                if (userManager != null && biometricsEnvironment != null) {
                    val accessRepository = AccessRepositoryImpl.getInstance(userManager)
                    return FindUsUdfpsWithSpViewModelImpl(
                        isSuw,
                        SpAccessPolicyImpl(accessRepository),
                        ScreenProtectorInteractorImpl(
                            FrrRepositoryImpl.getInstance(UdfpsFingerprintExtSupplier),
                            accessRepository,
                        ),
                        Sp001AllowListInteractorImpl(
                            Sp001AllowListRepositoryImpl.getInstance(settingsApplication.resources)
                        ),
                        HareModeGetterImpl(
                            HareRepositoryImpl.getInstance(
                                UdfpsFingerprintExtSupplier,
                                appScope,
                                Dispatchers.IO,
                            )
                        ),
                    )
                        as T
                }
            }
        }
        Log.e(TAG, "create(), missing factory method for $modelClass")
        return super.create(modelClass, extras)
    }

    companion object {
        private const val TAG = "UsudfpsViewModelFactory"

        val SP_QR_CODE_KEY = object : CreationExtras.Key<SpQrCode> {}
    }
}
