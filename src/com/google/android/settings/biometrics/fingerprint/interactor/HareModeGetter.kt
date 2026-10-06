package com.google.android.settings.biometrics.fingerprint.interactor

import com.google.android.settings.biometrics.fingerprint.data.repository.HareRepository
import com.google.android.settings.biometrics.fingerprint.model.HareMode

interface HareModeGetter {
    suspend fun getHareMode(): HareMode
}

class HareModeGetterImpl(private val hareRepository: HareRepository) : HareModeGetter {
    override suspend fun getHareMode(): HareMode = hareRepository.getHareMode()
}
