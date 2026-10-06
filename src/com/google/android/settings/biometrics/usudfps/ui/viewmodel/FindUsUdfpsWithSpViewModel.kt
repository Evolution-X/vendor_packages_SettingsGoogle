package com.google.android.settings.biometrics.usudfps.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.android.settings.biometrics.fingerprint.interactor.HareModeGetter
import com.google.android.settings.biometrics.fingerprint.interactor.ScreenProtectorInteractor
import com.google.android.settings.biometrics.fingerprint.interactor.Sp001AllowListInteractor
import com.google.android.settings.biometrics.fingerprint.interactor.SpAccessPolicy
import com.google.android.settings.biometrics.fingerprint.model.HareMode
import com.google.android.settings.biometrics.fingerprint.ui.model.SpUiData

abstract class FindUsUdfpsWithSpViewModel : ViewModel() {
    abstract val isSuw: Boolean
    abstract val shouldShowSpButtons: Boolean

    abstract fun setNoScreenProtectorIfUnsetBefore()

    abstract suspend fun getHareMode(): HareMode
}

class FindUsUdfpsWithSpViewModelImpl(
    override val isSuw: Boolean,
    private val spAccessPolicy: SpAccessPolicy,
    private val spInteractor: ScreenProtectorInteractor,
    private val sp001AllowListInteractor: Sp001AllowListInteractor,
    private val hareModeGetter: HareModeGetter,
) : FindUsUdfpsWithSpViewModel() {

    private val isSpEnabled: Boolean by lazy { sp001AllowListInteractor.isEnabled() }

    private val isSpEditable: Boolean by lazy { spAccessPolicy.canEdit }

    override val shouldShowSpButtons: Boolean
        get() = isSpEnabled && isSpEditable && spUiData !is SpUiData.FirstParty

    private val spUiData: SpUiData
        get() = sp001AllowListInteractor.getSpUiData(spInteractor.screenProtector)

    override fun setNoScreenProtectorIfUnsetBefore() {
        if (isSpEnabled && isSpEditable && spUiData is SpUiData.Unset) {
            Log.d(TAG, "Set default value")
            spInteractor.setScreenProtector(sp001AllowListInteractor.spNone)
        } else {
            Log.d(TAG, "Set nothing")
        }
    }

    override suspend fun getHareMode(): HareMode = hareModeGetter.getHareMode()

    companion object {
        private const val TAG = "FindUsUdfpsWithSpViewModel"
    }
}
