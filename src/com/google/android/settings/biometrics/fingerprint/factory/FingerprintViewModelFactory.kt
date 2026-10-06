package com.google.android.settings.biometrics.fingerprint.factory

import android.app.Application
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.hardware.biometrics.BiometricAuthenticator
import android.hardware.fingerprint.FingerprintManager
import android.os.UserManager
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.android.internal.widget.LockPatternUtils
import com.android.settings.Utils
import com.android.settings.biometrics.GatekeeperPasswordProvider
import com.android.settings.biometrics.ParentalControlsUtils
import com.android.settings.overlay.FeatureFactory
import com.android.settingslib.RestrictedLockUtilsInternal
import com.google.android.settings.biometrics.combination.data.repository.AccessRepositoryImpl
import com.google.android.settings.biometrics.combination.data.repository.VibratorRepositoryImpl
import com.google.android.settings.biometrics.combination.interactor.VibratorInteractorImpl
import com.google.android.settings.biometrics.combination.ui.viewmodel.FrrCommonNotificationViewModel
import com.google.android.settings.biometrics.combination.ui.viewmodel.FrrCommonNotificationViewModelImpl
import com.google.android.settings.biometrics.combination.ui.viewmodel.VibratorViewModel
import com.google.android.settings.biometrics.combination.ui.viewmodel.VibratorViewModelImpl
import com.google.android.settings.biometrics.face.data.repository.FaceManagerRepositoryImpl
import com.google.android.settings.biometrics.face.interactor.FaceEnrolledInteractorImpl
import com.google.android.settings.biometrics.fingerprint.data.repository.AccessibilityRepositoryImpl
import com.google.android.settings.biometrics.fingerprint.data.repository.FingerprintsRepositoryImpl
import com.google.android.settings.biometrics.fingerprint.data.repository.FrrRepositoryImpl
import com.google.android.settings.biometrics.fingerprint.data.repository.Sp001AllowListRepositoryImpl
import com.google.android.settings.biometrics.fingerprint.interactor.CheckAccessibilityStatusInteractorImpl
import com.google.android.settings.biometrics.fingerprint.interactor.FingerprintChallengeGeneratorImpl
import com.google.android.settings.biometrics.fingerprint.interactor.FingerprintMaxTemplatesInteractorImpl
import com.google.android.settings.biometrics.fingerprint.interactor.FingerprintNumOfEnrolledInteractorImpl
import com.google.android.settings.biometrics.fingerprint.interactor.FingerprintRemovalInteractorImpl
import com.google.android.settings.biometrics.fingerprint.interactor.FingerprintSensorTypeInteractorImpl
import com.google.android.settings.biometrics.fingerprint.interactor.ScreenProtectorInteractorImpl
import com.google.android.settings.biometrics.fingerprint.interactor.Sp001AllowListInteractorImpl
import com.google.android.settings.biometrics.fingerprint.interactor.SpAccessPolicyImpl
import com.google.android.settings.biometrics.fingerprint.interactor.UsUdfpsCalibratorInteractor
import com.google.android.settings.biometrics.fingerprint.ui.model.CredentialModel
import com.google.android.settings.biometrics.fingerprint.ui.model.EnrollmentRequest
import com.google.android.settings.biometrics.fingerprint.ui.viewmodel.AccessibilityViewModel
import com.google.android.settings.biometrics.fingerprint.ui.viewmodel.FingerprintEnrollmentViewModel
import com.google.android.settings.biometrics.fingerprint.ui.viewmodel.FingerprintMetricsViewModel
import com.google.android.settings.biometrics.fingerprint.ui.viewmodel.FingerprintMetricsViewModelImpl
import com.google.android.settings.biometrics.fingerprint.ui.viewmodel.IntroViewModel
import com.google.android.settings.biometrics.fingerprint.ui.viewmodel.LockPatternInteractorImpl
import com.google.android.settings.biometrics.fingerprint.ui.viewmodel.SetEnrollResultViewModel
import com.google.android.settings.biometrics.fingerprint.ui.viewmodel.UsUdfpsCalibratorViewModel
import java.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

open class FingerprintViewModelFactory : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val application =
            extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as? Application
        if (application == null) {
            Log.w(TAG, "create(), null application")
            @Suppress("UNCHECKED_CAST")
            return super.create(modelClass) as T
        }

        when {
            modelClass.isAssignableFrom(FingerprintMetricsViewModel::class.java) -> {
                val fingerprintManager = getFingerprintManager(application)
                val enrollmentRequest = extras[ENROLLMENT_REQUEST_KEY] as? EnrollmentRequest
                val credentialModel = extras[CREDENTIAL_MODEL_KEY] as? CredentialModel
                val userManager = application.getSystemService(UserManager::class.java)
                if (
                    fingerprintManager != null &&
                        enrollmentRequest != null &&
                        credentialModel != null &&
                        userManager != null
                ) {
                    val fingerprintsRepository =
                        getFingerprintsRepository(application, fingerprintManager)
                    val accessRepository = AccessRepositoryImpl.getInstance(userManager)
                    val frrRepository = FrrRepositoryImpl.getInstance(UdfpsFingerprintExtSupplier)
                    return FingerprintMetricsViewModelImpl(
                        enrollmentRequest,
                        credentialModel.userId,
                        Clock.systemUTC(),
                        FingerprintNumOfEnrolledInteractorImpl(
                            credentialModel.userId,
                            fingerprintsRepository,
                        ),
                        ScreenProtectorInteractorImpl(frrRepository, accessRepository),
                        Sp001AllowListInteractorImpl(
                            Sp001AllowListRepositoryImpl.getInstance(application.resources)
                        ),
                        FeatureFactory.featureFactory.biometricsFeatureProvider.biometricsLogger,
                    )
                        as T
                }
                @Suppress("UNCHECKED_CAST")
                return FingerprintMetricsViewModel() as T
            }
            modelClass.isAssignableFrom(SetEnrollResultViewModel::class.java) -> {
                return SetEnrollResultViewModel() as T
            }
            modelClass.isAssignableFrom(IntroViewModel::class.java) -> {
                val fingerprintManager = getFingerprintManager(application)
                val enrollmentRequest = extras[ENROLLMENT_REQUEST_KEY] as? EnrollmentRequest
                val credentialModel = extras[CREDENTIAL_MODEL_KEY] as? CredentialModel
                val parentalControlsEnforcedAdmin =
                    ParentalControlsUtils.parentConsentRequired(
                        application,
                        BiometricAuthenticator.TYPE_FINGERPRINT,
                    )
                if (
                    fingerprintManager != null &&
                        enrollmentRequest != null &&
                        credentialModel != null
                ) {
                    val keyguardAdminEnforcedAdmin =
                        RestrictedLockUtilsInternal.checkIfKeyguardFeaturesDisabled(
                            application,
                            DevicePolicyManager.KEYGUARD_DISABLE_FINGERPRINT,
                            credentialModel.userId,
                        )
                    val fingerprintsRepository =
                        getFingerprintsRepository(application, fingerprintManager)
                    return IntroViewModel(
                        parentalControlsEnforcedAdmin,
                        keyguardAdminEnforcedAdmin,
                        FingerprintMaxTemplatesInteractorImpl(fingerprintsRepository),
                        FingerprintSensorTypeInteractorImpl(fingerprintsRepository),
                        FingerprintNumOfEnrolledInteractorImpl(
                            credentialModel.userId,
                            fingerprintsRepository,
                        ),
                        enrollmentRequest,
                    )
                        as T
                }
            }
            modelClass.isAssignableFrom(FingerprintEnrollmentViewModel::class.java) -> {
                val fingerprintManager = getFingerprintManager(application)
                val enrollmentRequest = extras[ENROLLMENT_REQUEST_KEY] as? EnrollmentRequest
                val credentialModel = extras[CREDENTIAL_MODEL_KEY] as? CredentialModel
                if (
                    fingerprintManager != null &&
                        enrollmentRequest != null &&
                        credentialModel != null
                ) {
                    val fingerprintsRepository =
                        getFingerprintsRepository(application, fingerprintManager)
                    val lockPatternUtils =
                        FeatureFactory.featureFactory.securityFeatureProvider.getLockPatternUtils(
                            application
                        )
                    return FingerprintEnrollmentViewModel(
                        enrollmentRequest,
                        LockPatternInteractorImpl(
                            credentialModel.userId,
                            lockPatternUtils,
                            GatekeeperPasswordProvider(lockPatternUtils),
                        ),
                        FingerprintChallengeGeneratorImpl(
                            credentialModel.userId,
                            fingerprintsRepository,
                        ),
                        FingerprintSensorTypeInteractorImpl(fingerprintsRepository),
                        credentialModel,
                        Sp001AllowListInteractorImpl(
                            Sp001AllowListRepositoryImpl.getInstance(application.resources)
                        ),
                    )
                        as T
                }
            }
            modelClass.isAssignableFrom(UsUdfpsCalibratorViewModel::class.java) -> {
                val enrollmentRequest = extras[ENROLLMENT_REQUEST_KEY] as? EnrollmentRequest
                if (enrollmentRequest != null) {
                    val mainThreadHandler = application.mainThreadHandler
                    return UsUdfpsCalibratorViewModel(
                        UsUdfpsCalibratorInteractor(
                            mainThreadHandler,
                            enrollmentRequest.calibratorUuid,
                        ),
                        enrollmentRequest,
                    )
                        as T
                }
            }
            modelClass.isAssignableFrom(AccessibilityViewModel::class.java) -> {
                return AccessibilityViewModel(
                    CheckAccessibilityStatusInteractorImpl(AccessibilityRepositoryImpl(application))
                )
                    as T
            }
            modelClass.isAssignableFrom(FrrCommonNotificationViewModel::class.java) -> {
                val userId = application.userId
                val fingerprintManager = getFingerprintManager(application)
                val userManager = application.getSystemService(UserManager::class.java)
                if (fingerprintManager != null && userManager != null) {
                    val fingerprintManagerDataSource =
                        FingerprintDataSourceFactory.getFingerprintManagerDataSource(
                            fingerprintManager
                        )
                    val resources = application.resources
                    val fingerprintsRepository =
                        FingerprintsRepositoryImpl.getInstance(
                            fingerprintManagerDataSource,
                            FingerprintDataSourceFactory.getResourcesDataSource(resources),
                        )
                    val faceManagerRepository =
                        FaceManagerRepositoryImpl.getInstance(
                            userId,
                            Utils.getFaceManagerOrNull(application),
                        )
                    val lockPatternUtils = LockPatternUtils(application)
                    val accessRepository = AccessRepositoryImpl.getInstance(userManager)
                    val frrRepository = FrrRepositoryImpl.getInstance(UdfpsFingerprintExtSupplier)
                    return FrrCommonNotificationViewModelImpl(
                        userId,
                        LockPatternInteractorImpl(
                            userId,
                            lockPatternUtils,
                            GatekeeperPasswordProvider(lockPatternUtils),
                        ),
                        FingerprintChallengeGeneratorImpl(userId, fingerprintsRepository),
                        FingerprintRemovalInteractorImpl(userId, fingerprintsRepository),
                        FingerprintNumOfEnrolledInteractorImpl(userId, fingerprintsRepository),
                        FaceEnrolledInteractorImpl(faceManagerRepository),
                        SpAccessPolicyImpl(accessRepository),
                        ScreenProtectorInteractorImpl(frrRepository, accessRepository),
                        Sp001AllowListInteractorImpl(
                            Sp001AllowListRepositoryImpl.getInstance(resources)
                        ),
                    )
                        as T
                }
            }
            modelClass.isAssignableFrom(VibratorViewModel::class.java) -> {
                return VibratorViewModelImpl(
                    VibratorInteractorImpl(VibratorRepositoryImpl(application))
                )
                    as T
            }
        }
        Log.e(TAG, "create(), missing factory method for $modelClass")
        @Suppress("UNCHECKED_CAST")
        return super.create(modelClass) as T
    }

    protected fun getFingerprintManager(context: Context): FingerprintManager? {
        return Utils.getFingerprintManagerOrNull(context)
    }

    private fun getFingerprintsRepository(
        context: Context,
        fingerprintManager: FingerprintManager,
    ) =
        FingerprintRepositoryFactory.getFingerprintsRepository(
            fingerprintManager,
            context.resources,
        )

    companion object {
        private const val TAG = "FingerprintViewModelFactory"

        val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        val ENROLLMENT_REQUEST_KEY = object : CreationExtras.Key<EnrollmentRequest> {}

        val CREDENTIAL_MODEL_KEY = object : CreationExtras.Key<CredentialModel> {}
    }
}
