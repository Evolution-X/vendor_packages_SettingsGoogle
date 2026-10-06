package com.google.android.settings.biometrics.usudfps.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.google.android.settings.biometrics.fingerprint.interactor.ScreenProtectorInteractor
import com.google.android.settings.biometrics.fingerprint.interactor.Sp001AllowListInteractor
import com.google.android.settings.biometrics.fingerprint.interactor.SpAccessPolicy
import com.google.android.settings.biometrics.fingerprint.ui.model.CredentialModel
import com.google.android.settings.biometrics.fingerprint.ui.model.SpUiData
import com.google.android.settings.biometrics.fingerprint.ui.viewmodel.LockPatternInteractor

abstract class SpSetupViewModel : ViewModel() {
    abstract val shouldSkipIntroFragment: Boolean

    abstract fun isValidToEditSp(): Boolean
}

class SpSetupViewModelImpl(
    private val credentialModel: CredentialModel,
    private val lockPatternInteractor: LockPatternInteractor,
    private val spAccessPolicy: SpAccessPolicy,
    private val spInteractor: ScreenProtectorInteractor,
    private val sp001AllowListInteractor: Sp001AllowListInteractor,
) : SpSetupViewModel() {

    private val canEditSp: Boolean by lazy { spAccessPolicy.canEdit }

    private val hasValidTokenInCredentialModel: Boolean by lazy { credentialModel.isValidToken }

    override fun isValidToEditSp(): Boolean =
        canEditSp &&
            hasValidTokenInCredentialModel &&
            !lockPatternInteractor.isUnspecifiedPassword()

    override val shouldSkipIntroFragment: Boolean
        get() =
            when (sp001AllowListInteractor.getSpUiData(spInteractor.screenProtector)) {
                SpUiData.Unset,
                SpUiData.None -> false
                SpUiData.ThirdParty,
                is SpUiData.FirstParty -> true
            }
}
