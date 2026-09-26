package com.google.android.settings;

import com.android.settings.display.DisplayScreen;
import com.android.settings.fuelgauge.batterysaver.BatterySaverSchedulePreferenceController;
import com.android.settings.fuelgauge.batterysaver.BatterySaverScreen;
import com.android.settingslib.metadata.FixedArrayMap;
import com.android.settingslib.metadata.PreferenceScreenMetadataFactory;

import com.google.android.settings.display.DisplayGoogleScreen;
import com.google.android.settings.fuelgauge.batterysaver.AdaptiveBatteryScreen;
import com.google.android.settings.fuelgauge.batterysaver.BatterySaverGoogleApiScreen;
import com.google.android.settings.fuelgauge.batterysaver.BatterySaverGoogleScreen;
import com.google.android.settings.fuelgauge.batterysaver.BatterySaverScheduleScreen;
import com.google.android.settings.update.SoftwareUpdateScreen;

public abstract class SettingsGoogleScreenCollector {

    public static FixedArrayMap<String, PreferenceScreenMetadataFactory> get() {
        return new FixedArrayMap<>(6, SettingsGoogleScreenCollector::init);
    }

    private static void init(
            FixedArrayMap.OrderedInitializer<String, PreferenceScreenMetadataFactory>
                    orderedInitializer) {
        orderedInitializer.put(AdaptiveBatteryScreen.KEY, AdaptiveBatteryScreen::new);
        orderedInitializer.put(
                BatterySaverGoogleApiScreen.KEY, context -> new BatterySaverGoogleApiScreen());
        orderedInitializer.put(
                BatterySaverSchedulePreferenceController.KEY_BATTERY_SAVER_SCHEDULE,
                context -> new BatterySaverScheduleScreen());
        orderedInitializer.put(BatterySaverScreen.KEY, context -> new BatterySaverGoogleScreen());
        orderedInitializer.put(DisplayScreen.KEY, context -> new DisplayGoogleScreen());
        orderedInitializer.put(SoftwareUpdateScreen.KEY, context -> new SoftwareUpdateScreen());
    }
}
