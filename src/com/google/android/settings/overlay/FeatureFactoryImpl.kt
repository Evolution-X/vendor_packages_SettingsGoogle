package com.google.android.settings.overlay

import com.google.android.settings.wifi.factory.WifiFeatureProviderGoogleImpl

abstract class FeatureFactoryImpl : com.android.settings.overlay.FeatureFactoryImpl() {

    override val wifiFeatureProvider by lazy { WifiFeatureProviderGoogleImpl(appContext) }
}
