package com.google.android.settings.biometrics.usudfps.ui.viewmodel

import android.os.UserHandle
import androidx.lifecycle.ViewModel
import com.google.android.settings.biometrics.fingerprint.interactor.Sp001AllowListInteractor
import com.google.android.settings.biometrics.fingerprint.interactor.SpAccessPolicy
import com.google.android.settings.biometrics.fingerprint.model.SpHal
import com.google.android.settings.biometrics.fingerprint.model.SpProductInfo
import com.google.android.settings.biometrics.fingerprint.ui.model.SpQrCode

abstract class SpApplyViewModel : ViewModel() {
    abstract val canEdit: Boolean
    abstract val switchToMainUserForEditing: Boolean
    abstract val systemUserHandleForEditing: UserHandle?
    abstract val isValidQrCode: Boolean
    abstract val firstPartySpProductInfo: SpProductInfo?
    abstract val spHal: SpHal?

    abstract fun isSpEnabled(): Boolean
}

class SpApplyViewModelImpl(
    private val spQrCode: SpQrCode,
    private val spAccessPolicy: SpAccessPolicy,
    private val sp001AllowListInteractor: Sp001AllowListInteractor,
) : SpApplyViewModel() {

    override fun isSpEnabled(): Boolean = sp001AllowListInteractor.isEnabled()

    override val canEdit: Boolean
        get() = spAccessPolicy.canEdit

    override val switchToMainUserForEditing: Boolean
        get() = spAccessPolicy.canEditAsProfileUser

    override val systemUserHandleForEditing: UserHandle?
        get() = spAccessPolicy.systemUserHandleForEditing

    override val isValidQrCode: Boolean = sp001AllowListInteractor.isValidFormat(spQrCode)

    override val firstPartySpProductInfo: SpProductInfo? =
        sp001AllowListInteractor.getFirstPartySpProductInfo(spQrCode)

    override val spHal: SpHal? by lazy { sp001AllowListInteractor.getFirstPartySpHal(spQrCode) }
}
