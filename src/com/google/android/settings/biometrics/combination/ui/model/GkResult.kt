package com.google.android.settings.biometrics.combination.ui.model

import com.android.settings.biometrics.BiometricUtils
import java.util.Arrays

sealed class GkResult {

    class Ok(val challenge: Long, val token: ByteArray) : GkResult() {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (Ok::class != other?.let { it::class }) return false
            other as Ok
            return challenge == other.challenge && Arrays.equals(token, other.token)
        }

        override fun hashCode(): Int {
            return (challenge.hashCode() * 31) + Arrays.hashCode(token)
        }

        override fun toString(): String {
            return "Ok(challenge=$challenge, token=${Arrays.toString(token)})"
        }
    }

    data class Failed(val e: BiometricUtils.GatekeeperCredentialNotMatchException) : GkResult()
}
