package com.google.android.settings.biometrics.usudfps.feature

import android.content.Context
import com.android.settings.biometrics.fingerprint.feature.FingerprintExtPreferencesProvider
import com.android.settingslib.RestrictedPreference

class UsudfpsExtPreferencesProvider(context: Context) : FingerprintExtPreferencesProvider(context) {

    private val privateProvider: FingerprintExtPreferencesProvider by lazy {
        FrrExtPreferencesProvider(context)
    }

    override val size: Int = privateProvider.size

    override fun newPreference(index: Int, inflater: PreferenceInflater): RestrictedPreference? =
        privateProvider.newPreference(index, inflater)
}
