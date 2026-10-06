package com.google.android.settings.biometrics.fingerprint.interactor

import android.hardware.biometrics.SensorLocationInternal
import com.google.android.settings.biometrics.fingerprint.data.repository.FingerprintsRepository
import kotlinx.coroutines.flow.first

interface FingerprintSensorTypeInteractor {
    suspend fun getType(): Int

    suspend fun getSensorLocation(): SensorLocationInternal
}

class FingerprintSensorTypeInteractorImpl(
    private val fingerprintsRepository: FingerprintsRepository
) : FingerprintSensorTypeInteractor {

    override suspend fun getType(): Int {
        return fingerprintsRepository.getFingerprintSensor().first().sensorType
    }

    override suspend fun getSensorLocation(): SensorLocationInternal {
        return fingerprintsRepository.getFingerprintSensor().first().location
    }
}
