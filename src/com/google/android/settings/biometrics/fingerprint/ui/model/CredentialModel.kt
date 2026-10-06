package com.google.android.settings.biometrics.fingerprint.ui.model

import android.os.Bundle
import android.os.UserHandle
import java.time.Clock

interface CredentialModel {
    val userId: Int
    var gkPwHandle: Long
    var challenge: Long
    var token: ByteArray?
    val isValidGkPwHandle: Boolean
    val isValidChallenge: Boolean
    val isValidToken: Boolean
    val isValidUserId: Boolean

    fun clearGkPwHandle()
}

class CredentialModelImpl(extras: Bundle?, private val clock: Clock) : CredentialModel {

    private val mInitMillis: Long = clock.millis()
    private var clearGkPwHandleMillis: Long? = null
    private var updateChallengeMillis: Long? = null
    private var updateTokenMillis: Long? = null

    private val bundle: Bundle = extras ?: Bundle()

    override val userId: Int = bundle.getInt("android.intent.extra.USER_ID", UserHandle.myUserId())
    override var gkPwHandle: Long = bundle.getLong("gk_pw_handle", 0L)

    override var challenge: Long = bundle.getLong("challenge", -1L)
        set(value) {
            updateChallengeMillis = clock.millis()
            field = value
        }

    override var token: ByteArray? = bundle.getByteArray("hw_auth_token")
        set(value) {
            updateTokenMillis = clock.millis()
            field = value
        }

    override val isValidGkPwHandle: Boolean
        get() = gkPwHandle != 0L

    override val isValidChallenge: Boolean
        get() = challenge != -1L

    override val isValidToken: Boolean
        get() = token != null

    override val isValidUserId: Boolean
        get() = userId != USER_NULL

    override fun clearGkPwHandle() {
        clearGkPwHandleMillis = clock.millis()
        gkPwHandle = 0L
    }

    override fun toString(): String {
        val tokenLength = token?.size ?: 0
        return "${javaClass.simpleName}:{initMillis:$mInitMillis, userId:$userId, " +
            "challenge:{len:${challenge.toString().length}, updateMillis:$updateChallengeMillis}, " +
            "token:{len:$tokenLength, isValid:$isValidToken, updateMillis:$updateTokenMillis}, " +
            "gkPwHandle:{len:${gkPwHandle.toString().length}, isValid:$isValidGkPwHandle, " +
            "clearMillis:$clearGkPwHandleMillis} }"
    }

    private companion object {
        const val USER_NULL = -10000
    }
}
