package com.google.android.settings.biometrics.fingerprint.data.datasource

import android.content.res.Resources
import com.android.settings.R as AR

interface ResourcesDataSource {
    fun getSuwMaxFingerprintsEnrollable(): Int
}

class ResourcesDataSourceImpl private constructor(resources: Resources) : ResourcesDataSource {

    private val suwMaxFingerprintsEnrollable: Int =
        resources.getInteger(AR.integer.suw_max_fingerprints_enrollable)

    override fun getSuwMaxFingerprintsEnrollable(): Int = suwMaxFingerprintsEnrollable

    companion object {
        @Volatile private var instance: ResourcesDataSource? = null

        @Synchronized
        @JvmStatic
        fun getInstance(resources: Resources): ResourcesDataSource {
            return instance ?: ResourcesDataSourceImpl(resources).also { instance = it }
        }
    }
}
