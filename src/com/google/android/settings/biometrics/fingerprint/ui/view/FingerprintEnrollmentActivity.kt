package com.google.android.settings.biometrics.fingerprint.ui.view

import android.app.Activity
import android.content.Intent
import android.content.res.Resources
import android.hardware.fingerprint.FingerprintSensorProperties
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.android.settings.R as AR
import com.android.settings.biometrics.BiometricUtils
import com.android.settings.biometrics.BiometricsOnboardingProto.OnboardingAction
import com.android.settings.biometrics.fingerprint.FingerprintEnrollEnrolling
import com.android.settings.biometrics.fingerprint.FingerprintEnrollFindSensor
import com.android.settings.biometrics.fingerprint.SetupFingerprintEnrollEnrolling
import com.android.settings.biometrics.fingerprint.SetupFingerprintEnrollFindSensor
import com.android.settings.biometrics.metrics.OnboardingEvent
import com.android.settings.password.ChooseLockSettingsHelper
import com.google.android.settings.R
import com.google.android.settings.biometrics.fingerprint.factory.FingerprintViewModelFactory
import com.google.android.settings.biometrics.fingerprint.ui.model.CredentialModelImpl
import com.google.android.settings.biometrics.fingerprint.ui.model.EnrollmentRequestImpl
import com.google.android.settings.biometrics.fingerprint.ui.viewmodel.FingerprintEnrollResult
import com.google.android.settings.biometrics.fingerprint.ui.viewmodel.FingerprintEnrollmentCredentialAction
import com.google.android.settings.biometrics.fingerprint.ui.viewmodel.FingerprintEnrollmentViewModel
import com.google.android.settings.biometrics.fingerprint.ui.viewmodel.FingerprintMetricsViewModel
import com.google.android.settings.biometrics.fingerprint.ui.viewmodel.SetEnrollResultViewModel
import kotlinx.coroutines.launch

open class FingerprintEnrollmentActivity : FragmentActivity() {

    private val navController: NavController by lazy {
        val fragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
        (fragment as NavHostFragment).navController
    }

    private val enrollRequest: EnrollmentRequestImpl by lazy {
        val request = EnrollmentRequestImpl(intent, this is SetupActivity, this is AddAnother)
        Log.d(TAG, "Request: $request")
        request
    }

    private val credentialModel: CredentialModelImpl by lazy {
        CredentialModelImpl(intent.extras, SystemClock.elapsedRealtimeClock())
    }

    private val viewModel: FingerprintEnrollmentViewModel by viewModels()
    private val setEnrollResultViewModel: SetEnrollResultViewModel by viewModels()
    private val metricsViewModel: FingerprintMetricsViewModel by viewModels()

    private val nextActivityResultCallback =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (!viewModel.isWaitingActivityResult.compareAndSet(true, false)) {
                Log.w(TAG, "fail to reset isWaiting flag for enrollment")
            }
            val data = result.data
            val hasEnrolledFingerprint =
                data?.getBooleanExtra("finished_enrolling_fingerprint", false) ?: false
            Log.d(
                TAG,
                "get result $result, isSuw: ${viewModel.request.isSuw}, " +
                    "hasEnrolledFingerprint: $hasEnrolledFingerprint",
            )
            if (
                viewModel.request.isSuw &&
                    result.resultCode == RESULT_CANCELED &&
                    hasEnrolledFingerprint
            ) {
                setResult(RESULT_CANCELED, result.data)
                finish()
            }
            if (result.resultCode in FINISH_ON_RESULT_CODES) {
                setResult(result.resultCode, result.data)
                finish()
            }
        }

    private val chooseLockLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            onChooseOrConfirmLockResult(true, result)
        }

    private val onBackPressedCallback =
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                remove()
                metricsViewModel.appendAction(OnboardingAction.ACTION_CANCEL)
                if (navController.currentBackStack.value.size == 2) {
                    Log.d(TAG, "finalize result when backStack size is 2 and back event triggered")
                    val intent = Intent()
                    intent.putExtra(
                        "biometrics_onboarding_event",
                        metricsViewModel.sendMetricsToLogger(Activity.RESULT_CANCELED),
                    )
                    setResult(RESULT_CANCELED, intent)
                }
                onBackPressedDispatcher.onBackPressed()
                lifecycleScope.launch { addOnBackPressedCallback() }
            }
        }

    override val defaultViewModelProviderFactory: ViewModelProvider.Factory =
        FingerprintViewModelFactory()

    fun addOnBackPressedCallback() {
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeUseCase(this).applyTheme()
        setContentView(R.layout.fingerprint_enrollment_activity)
        Log.d(TAG, "onCreate() savedInstance:${savedInstanceState != null}")

        lifecycleScope.launch {
            checkCredential()
            initNavigation(getGraphId(viewModel.getSensorType()))
        }

        savedInstanceState?.let { bundle ->
            if (bundle.containsKey("challenge")) {
                credentialModel.challenge = bundle.getLong("challenge")
            }
            if (bundle.containsKey("hw_auth_token")) {
                credentialModel.token = bundle.getByteArray("hw_auth_token")
            }
        }

        addOnBackPressedCallback()

        lifecycleScope.launch {
            viewModel.generateChallengeFailedFlow.collect {
                onEnrollResult(FingerprintEnrollResult.GENERATE_CHALLENGE_FAILED)
            }
        }

        lifecycleScope.launch {
            setEnrollResultViewModel.resultFlow.collect { result -> onEnrollResult(result) }
        }

        lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onStart(owner: LifecycleOwner) {
                    val intent = Intent(ACTION_FINGERPRINT_ENROLL_START)
                    intent.setPackage(SYSTEM_UI_PACKAGE)
                    sendBroadcast(intent, "android.permission.USE_BIOMETRIC_INTERNAL")
                }

                override fun onPause(owner: LifecycleOwner) {
                    if (
                        isFinishing ||
                            !viewModel.shallFinishActivityDuringOnPause(isChangingConfigurations)
                    ) {
                        return
                    }
                    onEnrollResult(FingerprintEnrollResult.ACTIVITY_ON_PAUSE_UNEXPECTED)
                }

                override fun onStop(owner: LifecycleOwner) {
                    if (isFinishing) {
                        val intent = Intent(ACTION_FINGERPRINT_ENROLL_STOP)
                        intent.setPackage(SYSTEM_UI_PACKAGE)
                        sendBroadcast(intent, "android.permission.USE_BIOMETRIC_INTERNAL")
                    }
                }
            }
        )
    }

    private fun getGraphId(sensorType: Int): Int =
        when (sensorType) {
            FingerprintSensorProperties.TYPE_UDFPS_ULTRASONIC -> getUltrasonicGraphId()
            FingerprintSensorProperties.TYPE_UDFPS_OPTICAL -> R.navigation.udfps_enroll
            FingerprintSensorProperties.TYPE_POWER_BUTTON -> R.navigation.sfps_enroll
            else -> R.navigation.intro_page
        }

    fun getUltrasonicGraphId(): Int {
        if (viewModel.shouldUseSpEnroll) {
            Log.d(TAG, "sp enroll")
            return R.navigation.usudfps_sp_enroll
        }
        Log.d(TAG, "normal enroll")
        return R.navigation.usudfps_enroll
    }

    fun initNavigation(graphResId: Int) {
        val navGraph = navController.navInflater.inflate(graphResId)
        if (this is AddAnother) {
            Log.d(TAG, "Fast enrollment launched")
            if (credentialModel.isValidGkPwHandle || credentialModel.isValidToken) {
                navGraph.setStartDestination(R.id.enroll)
            }
        } else if (viewModel.request.isSkipIntro) {
            Log.d(TAG, "Skip intro launched")
            navGraph.setStartDestination(R.id.find_sensor)
        }
        navController.graph = navGraph
    }

    fun onEnrollResult(result: FingerprintEnrollResult) {
        val isSuw = viewModel.request.isSuw
        when (result) {
            FingerprintEnrollResult.INTRO_FRAGMENT_SKIP_OR_CANCEL_BUTTON,
            FingerprintEnrollResult.SPLIT_DIALOG_DISMISS,
            FingerprintEnrollResult.FIND_SENSOR_SKIP_BUTTON,
            FingerprintEnrollResult.ENROLL_SKIP_BUTTON -> {
                metricsViewModel.appendAction(OnboardingAction.ACTION_SKIP)
                Log.d(TAG, "onEnrollResult($result), set result ${RESULT_SKIPPED}")
                setActivityResultAndFinish(ActivityResult(RESULT_SKIPPED, null))
            }
            FingerprintEnrollResult.INTRO_FRAGMENT_DONE_AND_FINISH_BUTTON -> {
                metricsViewModel.appendAction(OnboardingAction.ACTION_NEXT)
                Log.d(TAG, "onEnrollResult($result), set result ${RESULT_FINISHED}")
                setActivityResultAndFinish(ActivityResult(RESULT_FINISHED, null))
            }
            FingerprintEnrollResult.FIND_SENSOR_ERROR_FINISH,
            FingerprintEnrollResult.ENROLL_ERROR_DIALOG_OK_BUTTON_FINISH -> {
                Log.d(TAG, "onEnrollResult($result), set result ${RESULT_FINISHED}")
                setActivityResultAndFinish(ActivityResult(RESULT_FINISHED, null))
            }
            FingerprintEnrollResult.FIND_SENSOR_ERROR_TIMEOUT,
            FingerprintEnrollResult.ACTIVITY_ON_PAUSE_UNEXPECTED,
            FingerprintEnrollResult.ENROLL_ERROR_DIALOG_OK_BUTTON_TIMEOUT -> {
                Log.d(TAG, "onEnrollResult($result), set result ${RESULT_TIMEOUT}")
                setActivityResultAndFinish(ActivityResult(RESULT_TIMEOUT, null))
            }
            FingerprintEnrollResult.GENERATE_CHALLENGE_FAILED -> {
                Log.d(TAG, "onEnrollResult($result), set result ${RESULT_CANCELED_CODE}")
                setActivityResultAndFinish(ActivityResult(RESULT_CANCELED_CODE, null))
            }
            FingerprintEnrollResult.CONFIRMATION_NEXT_BUTTON -> {
                metricsViewModel.appendAction(OnboardingAction.ACTION_NEXT)
                viewModel.revokeChallengeIfSuw()
                Log.d(TAG, "onEnrollResult($result), set result ${RESULT_FINISHED}")
                setActivityResultAndFinish(ActivityResult(RESULT_FINISHED, null))
            }
            FingerprintEnrollResult.INTRO_FRAGMENT_CONTINUE_ENROLL -> {
                startNextActivity(
                    "startNext",
                    if (isSuw) SetupFingerprintEnrollFindSensor::class.java
                    else FingerprintEnrollFindSensor::class.java,
                )
            }
            FingerprintEnrollResult.FIND_SENSOR_NEXT_SCREEN -> {
                startNextActivity(
                    "FIND_SENSOR_NEXT_SCREEN startNext",
                    if (isSuw) SetupFingerprintEnrollEnrolling::class.java
                    else FingerprintEnrollEnrolling::class.java,
                )
            }
        }
    }

    private fun startNextActivity(logPrefix: String, activityClass: Class<*>) {
        if (!viewModel.isWaitingActivityResult.compareAndSet(false, true)) {
            Log.w(TAG, "$logPrefix, isSuw:${viewModel.request.isSuw}, fail to set isWaiting flag")
        }
        val intent = Intent(this, activityClass)
        intent.putExtras(viewModel.getCredentialIntentExtrasForNextActivity())
        intent.putExtras(viewModel.request.nextIntentExtra)
        nextActivityResultCallback.launch(intent)
    }

    private fun setActivityResultAndFinish(activityResult: ActivityResult) {
        var data = activityResult.data
        val generatingChallengeExtras = viewModel.createGeneratingChallengeExtras()
        if (data == null) {
            data = Intent()
        }
        val event: OnboardingEvent = metricsViewModel.sendMetricsToLogger(activityResult.resultCode)
        data.putExtra("biometrics_onboarding_event", event)
        if (activityResult.resultCode == RESULT_FINISHED && generatingChallengeExtras != null) {
            data.putExtras(generatingChallengeExtras)
        }
        Log.d(
            TAG,
            "setActivityResultAndFinish($activityResult), override:$data, " +
                "challengeExtrasLen:${generatingChallengeExtras?.size() ?: 0}",
        )
        setResult(activityResult.resultCode, data)
        finish()
    }

    fun checkCredential() {
        when (viewModel.checkCredential(lifecycleScope)) {
            FingerprintEnrollmentCredentialAction.FAIL_NEED_TO_CHOOSE_LOCK -> {
                if (!viewModel.isWaitingActivityResult.compareAndSet(false, true)) {
                    Log.w(TAG, "chooseLock, fail to set isWaiting flag to true")
                }
                chooseLockLauncher.launch(chooseLockIntent)
            }
            FingerprintEnrollmentCredentialAction.FAIL_NEED_TO_CONFIRM_LOCK -> {
                if (!confirmLockLauncher.launch()) {
                    Log.e(TAG, "confirmLock, launched is true")
                    finish()
                } else if (!viewModel.isWaitingActivityResult.compareAndSet(false, true)) {
                    Log.w(TAG, "confirmLock, fail to set isWaiting flag to true")
                }
            }
            else -> {}
        }
    }

    fun onChooseOrConfirmLockResult(isChooseLock: Boolean, activityResult: ActivityResult) {
        if (!viewModel.isWaitingActivityResult.compareAndSet(true, false)) {
            Log.e(TAG, "isChooseLock:$isChooseLock, fail to unset waiting flag")
        }
        if (
            viewModel.generateChallengeAsCredentialActivityResult(
                isChooseLock,
                activityResult,
                lifecycleScope,
            )
        ) {
            return
        }
        setActivityResultAndFinish(activityResult)
    }

    private val chooseLockIntent: Intent
        get() {
            val chooseLockIntent =
                BiometricUtils.getChooseLockIntent(
                    this,
                    viewModel.request.isSuw,
                    viewModel.request.suwExtras,
                )
            chooseLockIntent.putExtra("hide_insecure_options", true)
            chooseLockIntent.putExtra("request_gk_pw_handle", true)
            chooseLockIntent.putExtra("for_fingerprint", true)
            viewModel.getValidUserId()?.let {
                chooseLockIntent.putExtra("android.intent.extra.USER_ID", it)
            }
            return chooseLockIntent
        }

    private val confirmLockLauncher: ChooseLockSettingsHelper
        get() {
            val builder = ChooseLockSettingsHelper.Builder(this)
            builder
                .setRequestCode(REQUEST_CONFIRM_OR_CHOOSE_LOCK)
                .setTitle(getString(R.string.security_settings_fingerprint_preference_title))
                .setRequestGatekeeperPasswordHandle(true)
                .setForegroundOnly(true)
                .setReturnCredentials(true)
            viewModel.getValidUserId()?.let { builder.setUserId(it) }
            return builder.build()
        }

    override fun onApplyThemeResource(theme: Resources.Theme, resid: Int, first: Boolean) {
        theme.applyStyle(AR.style.SetupWizardPartnerResource, true)
        super.onApplyThemeResource(theme, resid, first)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CONFIRM_OR_CHOOSE_LOCK) {
            onChooseOrConfirmLockResult(false, ActivityResult(resultCode, data))
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong("challenge", credentialModel.challenge)
        if (credentialModel.isValidToken) {
            outState.putByteArray("hw_auth_token", credentialModel.token)
        }
    }

    override val defaultViewModelCreationExtras: CreationExtras
        get() {
            val creationExtras = MutableCreationExtras(super.defaultViewModelCreationExtras)
            creationExtras[FingerprintViewModelFactory.ENROLLMENT_REQUEST_KEY] = enrollRequest
            creationExtras[FingerprintViewModelFactory.CREDENTIAL_MODEL_KEY] = credentialModel
            return creationExtras
        }

    class SetupActivity : FingerprintEnrollmentActivity()

    class InternalActivity : FingerprintEnrollmentActivity()

    class AddAnother : FingerprintEnrollmentActivity()

    companion object {
        private const val TAG = "FingerprintEnrollmentActivity"
        private const val REQUEST_CONFIRM_OR_CHOOSE_LOCK = 1
        private const val ACTION_FINGERPRINT_ENROLL_START =
            "com.google.android.biometric.fingerprint.enroll.start"
        private const val ACTION_FINGERPRINT_ENROLL_STOP =
            "com.google.android.biometric.fingerprint.enroll.stop"
        private const val SYSTEM_UI_PACKAGE = "com.android.systemui"
        private val FINISH_ON_RESULT_CODES = intArrayOf(1, 2, 11, 3)

        const val RESULT_SKIPPED = 2
        const val RESULT_TIMEOUT = 3
        const val RESULT_FINISHED = 1
        const val RESULT_CANCELED_CODE = 0
    }
}
