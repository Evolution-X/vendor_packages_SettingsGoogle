package com.google.android.settings.biometrics.fingerprint.ui.viewmodel

import com.android.internal.widget.LockPatternUtils
import com.android.settings.biometrics.GatekeeperPasswordProvider

interface LockPatternInteractor {
    fun getGkHat(gkPwHandle: Long, challenge: Long): ByteArray

    fun isUnspecifiedPassword(): Boolean

    fun removeGkPwHandle(gkPwHandle: Long)
}

class LockPatternInteractorImpl(
    private val userId: Int,
    private val lockPatternUtils: LockPatternUtils,
    private val gatekeeperPasswordProvider: GatekeeperPasswordProvider,
) : LockPatternInteractor {

    private val isUnspecifiedPassword: Boolean =
        lockPatternUtils.getActivePasswordQuality(userId) == PASSWORD_QUALITY_UNSPECIFIED

    override fun isUnspecifiedPassword(): Boolean = isUnspecifiedPassword

    override fun getGkHat(gkPwHandle: Long, challenge: Long): ByteArray {
        return gatekeeperPasswordProvider.requestGatekeeperHat(gkPwHandle, challenge, userId)
    }

    override fun removeGkPwHandle(gkPwHandle: Long) {
        gatekeeperPasswordProvider.removeGatekeeperPasswordHandle(gkPwHandle)
    }

    private companion object {
        const val PASSWORD_QUALITY_UNSPECIFIED = 0
    }
}
