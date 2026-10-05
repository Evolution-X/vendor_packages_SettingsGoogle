package com.google.android.settings.overlay

import com.google.android.settings.fuelgauge.BatterySettingsFeatureProviderGoogleImpl
import com.google.android.settings.fuelgauge.BatteryStatusFeatureProviderGoogleImpl
import com.google.android.settings.fuelgauge.PowerUsageFeatureProviderGoogleImpl
import com.google.android.settings.privatespace.PrivateSpaceLoginFeatureProviderGoogleImpl
import com.google.android.settings.vpn2.AdvancedVpnFeatureProviderGoogleImpl
import com.google.android.settings.wifi.factory.WifiFeatureProviderGoogleImpl

abstract class FeatureFactoryImpl : com.android.settings.overlay.FeatureFactoryImpl() {

    override val powerUsageFeatureProvider by lazy { PowerUsageFeatureProviderGoogleImpl(appContext) }

    override val batteryStatusFeatureProvider by lazy { BatteryStatusFeatureProviderGoogleImpl(appContext) }

    override val batterySettingsFeatureProvider by lazy { BatterySettingsFeatureProviderGoogleImpl() }

    override val advancedVpnFeatureProvider by lazy { AdvancedVpnFeatureProviderGoogleImpl() }

    override val wifiFeatureProvider by lazy { WifiFeatureProviderGoogleImpl(appContext) }

    override val privateSpaceLoginFeatureProvider by lazy { PrivateSpaceLoginFeatureProviderGoogleImpl() }
}
