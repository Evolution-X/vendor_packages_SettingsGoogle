package com.google.android.settings.biometrics.fingerprint.model

class SpData(val detail: SpProductInfo, val hal: SpHal) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SpData) return false
        return detail == other.detail && hal == other.hal
    }

    override fun hashCode(): Int {
        return (detail.hashCode() * 31) + hal.hashCode()
    }

    override fun toString(): String {
        return "detail:[$detail], hal:$hal"
    }
}
