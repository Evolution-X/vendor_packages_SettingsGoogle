package com.google.android.settings.biometrics.fingerprint.model

class SpHal(val hex: UInt) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is SpHal && hex == other.hex
    }

    override fun hashCode(): Int {
        return hex.hashCode()
    }

    override fun toString(): String {
        return hex.hashCode().toString()
    }

    companion object {
        fun getInstance(s: String): SpHal = SpHal(s.toUInt(16))
    }
}
