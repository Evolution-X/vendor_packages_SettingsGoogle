package com.google.android.settings.biometrics.fingerprint.model

class SpProductInfo(val modelName: String) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is SpProductInfo && modelName == other.modelName
    }

    override fun hashCode(): Int {
        return modelName.hashCode()
    }

    override fun toString(): String {
        return "ModelStr:$modelName"
    }
}
