package com.google.android.settings.biometrics.fingerprint.factory

import android.content.res.Resources
import android.hardware.fingerprint.FingerprintManager
import com.google.android.settings.biometrics.fingerprint.data.datasource.FingerprintManagerDataSource
import com.google.android.settings.biometrics.fingerprint.data.datasource.FingerprintManagerDataSourceImpl
import com.google.android.settings.biometrics.fingerprint.data.datasource.ResourcesDataSource
import com.google.android.settings.biometrics.fingerprint.data.datasource.ResourcesDataSourceImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope

object FingerprintDataSourceFactory {

    fun getResourcesDataSource(resources: Resources): ResourcesDataSource {
        return ResourcesDataSourceImpl.getInstance(resources)
    }

    fun getFingerprintManagerDataSource(
        fingerprintManager: FingerprintManager
    ): FingerprintManagerDataSource {
        return FingerprintManagerDataSourceImpl.getInstance(
            fingerprintManager,
            MainScope(),
            Dispatchers.IO,
        )
    }
}
