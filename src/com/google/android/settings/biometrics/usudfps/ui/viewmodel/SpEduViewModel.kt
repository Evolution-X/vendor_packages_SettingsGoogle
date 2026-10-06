package com.google.android.settings.biometrics.usudfps.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.google.android.settings.biometrics.fingerprint.interactor.ScreenProtectorInteractor
import com.google.android.settings.biometrics.fingerprint.interactor.Sp001AllowListInteractor

abstract class SpEduViewModel : ViewModel() {
    abstract val isEnrolling: Boolean

    abstract fun setThirdPartyScreenProtector()
}

class SpEduViewModelImpl(
    override val isEnrolling: Boolean,
    private val spInteractor: ScreenProtectorInteractor,
    private val sp001AllowListInteractor: Sp001AllowListInteractor,
) : SpEduViewModel() {
    override fun setThirdPartyScreenProtector() {
        spInteractor.setScreenProtector(sp001AllowListInteractor.spThirdParty)
    }
}
