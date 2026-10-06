package com.google.android.settings.biometrics.usudfps.feature

import android.os.Bundle
import android.util.Log
import androidx.preference.PreferenceFragmentCompat
import com.android.settings.biometrics.fingerprint.feature.ChallengeGeneratedInvoker
import com.google.android.settings.R
import com.google.android.settings.biometrics.fingerprint.model.SpHal
import com.google.android.settings.biometrics.usudfps.preference.ScreenProtectorPreference

class ScreenProtectorInvoker : ChallengeGeneratedInvoker {

    override val intentKeyForBundle: String = "screen_protector_apply_bundle"

    override fun invoke(fragment: PreferenceFragmentCompat, bundle: Bundle): Boolean {
        Log.d(TAG, "start invoking")
        val context = fragment.context
        if (context == null) {
            Log.d(TAG, "Missing context")
            return false
        }
        val preference =
            fragment.findPreference<ScreenProtectorPreference>(
                context.resources.getString(R.string.security_settings_udfps_screen_protector_key)
            )
        if (preference == null) {
            Log.d(TAG, "Preference not found")
            return false
        }
        val hexString = bundle.getString(KEY_HEX)
        if (hexString == null) {
            Log.d(TAG, "No string for hex")
            return false
        }
        val hex = hexString.toUIntOrNull(16)
        if (hex == null) {
            Log.d(TAG, "Fail to convert to UInt as $hexString")
            return false
        }
        val result = preference.putFirstPartySpHal(SpHal(hex))
        if (result) {
            preference.forceUpdate()
        }
        return result
    }

    fun newIntentBundle(spHal: SpHal): Bundle =
        Bundle().apply { putString(KEY_HEX, spHal.hex.toString(16)) }

    private companion object {
        const val TAG = "SpPreferenceInvoker"
        const val KEY_HEX = "hex"
    }
}
