package com.google.android.settings.biometrics.combination.interactor

import com.google.android.settings.biometrics.combination.data.repository.VibratorRepository
import kotlinx.coroutines.flow.Flow

interface VibratorInteractor {
    operator fun invoke(): Flow<Boolean>
}

class VibratorInteractorImpl(private val vibratorRepository: VibratorRepository) :
    VibratorInteractor {
    override fun invoke(): Flow<Boolean> = vibratorRepository.getVibratorStatus()
}
