package com.google.android.settings.biometrics.fingerprint.factory

import android.content.res.Resources
import android.hardware.fingerprint.FingerprintManager
import com.google.android.settings.biometrics.fingerprint.data.repository.FingerprintsRepository
import com.google.android.settings.biometrics.fingerprint.data.repository.FingerprintsRepositoryImpl

object FingerprintRepositoryFactory {

    fun getFingerprintsRepository(
        fingerprintManager: FingerprintManager,
        resources: Resources,
    ): FingerprintsRepository {
        val dataSourceFactory = FingerprintDataSourceFactory
        return FingerprintsRepositoryImpl.getInstance(
            dataSourceFactory.getFingerprintManagerDataSource(fingerprintManager),
            dataSourceFactory.getResourcesDataSource(resources),
        )
    }
}
