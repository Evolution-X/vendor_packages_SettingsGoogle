package com.google.android.settings.biometrics.fingerprint.interactor

import com.google.android.settings.biometrics.combination.data.repository.AccessRepository
import com.google.android.settings.biometrics.fingerprint.data.repository.FrrRepository
import com.google.android.settings.biometrics.fingerprint.model.SpHal

interface ScreenProtectorInteractor {
    val screenProtector: SpHal?

    fun setScreenProtector(spHal: SpHal)
}

class ScreenProtectorInteractorImpl(
    private val frrRepository: FrrRepository,
    private val accessRepository: AccessRepository,
) : ScreenProtectorInteractor {
    override val screenProtector: SpHal?
        get() = frrRepository.screenProtector

    override fun setScreenProtector(spHal: SpHal) {
        if (accessRepository.isMainUser() || accessRepository.systemUserHandle != null) {
            frrRepository.setScreenProtector(spHal)
            return
        }
        throw SecurityException("Fail to setup sp")
    }
}
