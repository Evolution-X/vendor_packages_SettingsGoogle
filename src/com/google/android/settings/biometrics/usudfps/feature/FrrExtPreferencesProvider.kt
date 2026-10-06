package com.google.android.settings.biometrics.usudfps.feature

import android.content.Context
import com.android.settings.biometrics.fingerprint.feature.FingerprintExtPreferencesProvider
import com.android.settingslib.RestrictedPreference
import com.android.settingslib.spaprivileged.framework.common.userManager
import com.google.android.settings.R
import com.google.android.settings.biometrics.combination.data.repository.AccessRepositoryImpl
import com.google.android.settings.biometrics.fingerprint.data.repository.FrrRepositoryImpl
import com.google.android.settings.biometrics.fingerprint.data.repository.Sp001AllowListRepositoryImpl
import com.google.android.settings.biometrics.fingerprint.factory.UdfpsFingerprintExtSupplier
import com.google.android.settings.biometrics.fingerprint.interactor.ScreenProtectorInteractorImpl
import com.google.android.settings.biometrics.fingerprint.interactor.Sp001AllowListInteractor
import com.google.android.settings.biometrics.fingerprint.interactor.Sp001AllowListInteractorImpl
import com.google.android.settings.biometrics.usudfps.preference.ScreenProtectorDataStoreImpl

class FrrExtPreferencesProvider(context: Context) : FingerprintExtPreferencesProvider(context) {

    private val sp001AllowListInteractor: Sp001AllowListInteractor by lazy {
        Sp001AllowListInteractorImpl(Sp001AllowListRepositoryImpl.getInstance(context.resources))
    }

    override val size: Int
        get() = if (sp001AllowListInteractor.isEnabled()) 1 else 0

    override fun newPreference(index: Int, inflater: PreferenceInflater): RestrictedPreference? {
        if (index != 0 || !sp001AllowListInteractor.isEnabled()) {
            return null
        }
        val preference =
            inflater
                .inflateFromResource(R.xml.security_settings_udfps_screen_protector)
                .getPreference(0) as RestrictedPreference
        preference.parent?.removePreference(preference)
        val dataStore =
            ScreenProtectorDataStoreImpl(
                context,
                sp001AllowListInteractor,
                ScreenProtectorInteractorImpl(
                    FrrRepositoryImpl.getInstance(UdfpsFingerprintExtSupplier),
                    AccessRepositoryImpl.getInstance(context.userManager),
                ),
            )
        preference.preferenceDataStore = dataStore
        preference.summaryProvider = dataStore
        return preference
    }
}
