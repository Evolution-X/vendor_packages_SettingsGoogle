package com.google.android.settings;

import com.android.settings.display.DisplayScreen;
import com.android.settingslib.metadata.FixedArrayMap;
import com.android.settingslib.metadata.PreferenceScreenMetadataFactory;

import com.google.android.settings.display.DisplayGoogleScreen;

public abstract class SettingsGoogleScreenCollector {

    public static FixedArrayMap<String, PreferenceScreenMetadataFactory> get() {
        return new FixedArrayMap<>(1, SettingsGoogleScreenCollector::init);
    }

    private static void init(
            FixedArrayMap.OrderedInitializer<String, PreferenceScreenMetadataFactory>
                    orderedInitializer) {
        orderedInitializer.put(DisplayScreen.KEY, context -> new DisplayGoogleScreen());
    }
}
