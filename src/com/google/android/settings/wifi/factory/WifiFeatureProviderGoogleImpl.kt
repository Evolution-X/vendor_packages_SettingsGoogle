package com.google.android.settings.wifi.factory

import android.content.Context
import com.android.settings.R
import com.android.settings.wifi.dpp.WifiDppQrCodeGeneratorFragment
import com.android.settings.wifi.factory.WifiFeatureProvider
import com.google.android.settings.experiments.PhenotypeProxy
import com.google.android.settings.wifi.dpp.WifiDppQrCodeGeneratorFragmentGoogleImpl

class WifiFeatureProviderGoogleImpl(private val appContext: Context) :
    WifiFeatureProvider(appContext) {

    override fun getWifiDppQrCodeGeneratorFragment(): WifiDppQrCodeGeneratorFragment {
        return if (isPhFlagPrismQrEnabled()) {
            WifiDppQrCodeGeneratorFragmentGoogleImpl()
        } else {
            super.getWifiDppQrCodeGeneratorFragment()
        }
    }

    private fun isPhFlagPrismQrEnabled(): Boolean {
        return PhenotypeProxy.getBooleanFlagByPackageAndKey(
            appContext,
            appContext.getString(R.string.config_settingsintelligence_package_name),
            "PrismQrConfig__is_enabled_for_wifi_qr",
            true,
        )
    }
}
