package com.google.android.settings.biometrics.usudfps.preference

import android.content.Context
import android.util.Log
import androidx.preference.Preference
import androidx.preference.PreferenceDataStore
import com.android.settings.R as AR
import com.google.android.settings.R
import com.google.android.settings.biometrics.fingerprint.interactor.ScreenProtectorInteractor
import com.google.android.settings.biometrics.fingerprint.interactor.Sp001AllowListInteractor
import com.google.android.settings.biometrics.fingerprint.model.SpHal
import com.google.android.settings.biometrics.fingerprint.ui.model.SpUiData

abstract class ScreenProtectorDataStore :
    PreferenceDataStore(), Preference.SummaryProvider<Preference> {
    abstract fun putFirstPartySpHal(spHal: SpHal): Boolean
}

class ScreenProtectorDataStoreImpl(
    private val context: Context,
    private val sp001AllowListInteractor: Sp001AllowListInteractor,
    private val spInteractor: ScreenProtectorInteractor,
) : ScreenProtectorDataStore() {

    private val spUiData: SpUiData
        get() = sp001AllowListInteractor.getSpUiData(spInteractor.screenProtector)

    override fun getBoolean(key: String, defValue: Boolean): Boolean =
        when (spUiData) {
            is SpUiData.FirstParty,
            SpUiData.ThirdParty -> true
            SpUiData.None -> false
            SpUiData.Unset -> defValue
        }

    override fun putBoolean(key: String, value: Boolean) {
        spInteractor.setScreenProtector(
            if (value) sp001AllowListInteractor.spThirdParty else sp001AllowListInteractor.spNone
        )
    }

    override fun provideSummary(preference: Preference): CharSequence {
        val uiData = spUiData
        if (uiData is SpUiData.FirstParty) {
            return context.getString(
                R.string.security_settings_udfps_screen_protector_description_product_combined,
                uiData.productInfo.modelName,
            )
        }
        if (uiData is SpUiData.ThirdParty) {
            return context.getString(AR.string.switch_on_text)
        }
        return context.getString(R.string.security_settings_udfps_screen_protector_description_none)
    }

    override fun putFirstPartySpHal(spHal: SpHal): Boolean {
        try {
            if (sp001AllowListInteractor.getFirstPartySpProductInfo(spHal) != null) {
                spInteractor.setScreenProtector(spHal)
                return true
            }
            Log.d(TAG, "SpProductInfo not found for $spHal")
            return false
        } catch (e: Exception) {
            Log.e(TAG, "Fail to to set $spHal", e)
            return false
        }
    }

    private companion object {
        const val TAG = "SpDataStore"
    }
}
