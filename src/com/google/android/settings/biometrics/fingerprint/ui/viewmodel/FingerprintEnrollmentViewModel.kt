package com.google.android.settings.biometrics.fingerprint.ui.viewmodel

import android.app.Activity
import android.hardware.fingerprint.FingerprintManager
import android.os.Bundle
import android.util.Log
import androidx.activity.result.ActivityResult
import androidx.lifecycle.ViewModel
import com.android.settings.biometrics.BiometricUtils
import com.google.android.settings.biometrics.fingerprint.interactor.FingerprintChallengeGenerator
import com.google.android.settings.biometrics.fingerprint.interactor.FingerprintSensorTypeInteractor
import com.google.android.settings.biometrics.fingerprint.interactor.Sp001AllowListInteractor
import com.google.android.settings.biometrics.fingerprint.ui.model.CredentialModel
import com.google.android.settings.biometrics.fingerprint.ui.model.EnrollmentRequest
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class FingerprintEnrollmentViewModel(
    val request: EnrollmentRequest,
    private val lockPatternInteractor: LockPatternInteractor,
    private val fingerprintChallengeGenerator: FingerprintChallengeGenerator,
    private val fingerprintSensorTypeInteractor: FingerprintSensorTypeInteractor,
    private val credentialModel: CredentialModel,
    private val sp001AllowListInteractor: Sp001AllowListInteractor,
) : ViewModel() {

    val isWaitingActivityResult = AtomicBoolean(false)
    val shouldUseSpEnroll: Boolean by lazy { sp001AllowListInteractor.isEnabled() }

    private val _generateChallengeFailedFlow = MutableSharedFlow<Boolean>()
    val generateChallengeFailedFlow: SharedFlow<Boolean> =
        _generateChallengeFailedFlow.asSharedFlow()
    private var isGeneratingChallengeDuringCheckingCredential = false

    suspend fun getSensorType(): Int = fingerprintSensorTypeInteractor.getType()

    fun shallFinishActivityDuringOnPause(isChangingConfigurations: Boolean): Boolean {
        return !isChangingConfigurations && !request.isSuw && !isWaitingActivityResult.get()
    }

    fun createGeneratingChallengeExtras(): Bundle? {
        if (
            !isGeneratingChallengeDuringCheckingCredential ||
                !credentialModel.isValidToken ||
                !credentialModel.isValidChallenge
        ) {
            return null
        }
        return Bundle().apply {
            putByteArray("hw_auth_token", credentialModel.token)
            putLong("challenge", credentialModel.challenge)
        }
    }

    fun checkCredential(scope: CoroutineScope): FingerprintEnrollmentCredentialAction {
        if (isValidCredential()) {
            return FingerprintEnrollmentCredentialAction.CREDENTIAL_VALID
        }
        if (isUnspecifiedPassword()) {
            return FingerprintEnrollmentCredentialAction.FAIL_NEED_TO_CHOOSE_LOCK
        }
        if (credentialModel.isValidGkPwHandle) {
            val gkPwHandle = credentialModel.gkPwHandle
            credentialModel.clearGkPwHandle()
            generateChallenge(gkPwHandle, false, scope)
            isGeneratingChallengeDuringCheckingCredential = true
            return FingerprintEnrollmentCredentialAction.IS_GENERATING_CHALLENGE
        }
        return FingerprintEnrollmentCredentialAction.FAIL_NEED_TO_CONFIRM_LOCK
    }

    private fun generateChallenge(
        gkPwHandle: Long,
        revokeGkPwHandle: Boolean,
        scope: CoroutineScope,
    ) {
        fingerprintChallengeGenerator.generateChallenge(
            object : FingerprintManager.GenerateChallengeCallback {
                override fun onChallengeGenerated(sensorId: Int, userId: Int, challenge: Long) {
                    try {
                        try {
                            val gkHat = lockPatternInteractor.getGkHat(gkPwHandle, challenge)
                            credentialModel.challenge = challenge
                            credentialModel.token = gkHat
                            if (revokeGkPwHandle) {
                                lockPatternInteractor.removeGkPwHandle(gkPwHandle)
                            }
                            Log.d(
                                TAG,
                                "generateChallenge(), model:$credentialModel, " +
                                    "revokeGkPwHandle:$revokeGkPwHandle",
                            )
                            if (isValidCredential()) {
                                return
                            }
                            Log.w(
                                TAG,
                                "generateChallenge, invalid Credential or IllegalStateException",
                            )
                            emitGenerateChallengeFailed(scope)
                        } catch (e: BiometricUtils.GatekeeperCredentialNotMatchException) {
                            Log.e(
                                TAG,
                                "generateChallenge, GatekeeperCredentialNotMatchException",
                                e,
                            )
                            if (revokeGkPwHandle) {
                                lockPatternInteractor.removeGkPwHandle(gkPwHandle)
                            }
                            Log.d(
                                TAG,
                                "generateChallenge(), model:$credentialModel, " +
                                    "revokeGkPwHandle:$revokeGkPwHandle",
                            )
                            isValidCredential()
                            Log.w(
                                TAG,
                                "generateChallenge, invalid Credential or IllegalStateException",
                            )
                            emitGenerateChallengeFailed(scope)
                        }
                    } catch (th: Throwable) {
                        if (revokeGkPwHandle) {
                            lockPatternInteractor.removeGkPwHandle(gkPwHandle)
                        }
                        Log.d(
                            TAG,
                            "generateChallenge(), model:$credentialModel, " +
                                "revokeGkPwHandle:$revokeGkPwHandle",
                        )
                        if (isValidCredential()) {
                            throw th
                        }
                        Log.w(TAG, "generateChallenge, invalid Credential or IllegalStateException")
                        emitGenerateChallengeFailed(scope)
                        throw th
                    }
                }
            }
        )
    }

    private fun emitGenerateChallengeFailed(scope: CoroutineScope) {
        scope.launch { _generateChallengeFailedFlow.emit(true) }
    }

    private fun isValidCredential(): Boolean =
        !isUnspecifiedPassword() && credentialModel.isValidToken

    private fun isUnspecifiedPassword(): Boolean = lockPatternInteractor.isUnspecifiedPassword()

    fun generateChallengeAsCredentialActivityResult(
        shouldRevokeGkPwHandle: Boolean,
        activityResult: ActivityResult,
        scope: CoroutineScope,
    ): Boolean {
        val data = activityResult.data
        if (
            (!(shouldRevokeGkPwHandle && activityResult.resultCode == Activity.RESULT_FIRST_USER) &&
                (shouldRevokeGkPwHandle || activityResult.resultCode != Activity.RESULT_OK)) ||
                data == null
        ) {
            return false
        }
        generateChallenge(data.getLongExtra("gk_pw_handle", 0L), true, scope)
        return true
    }

    fun revokeChallengeIfSuw() {
        if (request.isSuw) {
            fingerprintChallengeGenerator.revokeChallenge(credentialModel.challenge)
            credentialModel.challenge = -1L
        }
    }

    fun getValidUserId(): Int? = if (credentialModel.isValidUserId) credentialModel.userId else null

    fun getCredentialIntentExtrasForNextActivity(): Bundle {
        return Bundle().apply {
            if (credentialModel.isValidGkPwHandle) {
                putLong("gk_pw_handle", credentialModel.gkPwHandle)
            }
            if (credentialModel.isValidToken) {
                putByteArray("hw_auth_token", credentialModel.token)
            }
            if (credentialModel.isValidUserId) {
                putInt("android.intent.extra.USER_ID", credentialModel.userId)
            }
            putLong("challenge", credentialModel.challenge)
        }
    }

    private companion object {
        const val TAG = "FingerprintEnrollmentViewModel"
    }
}

enum class FingerprintEnrollmentCredentialAction {
    CREDENTIAL_VALID,
    IS_GENERATING_CHALLENGE,
    FAIL_NEED_TO_CHOOSE_LOCK,
    FAIL_NEED_TO_CONFIRM_LOCK,
}
