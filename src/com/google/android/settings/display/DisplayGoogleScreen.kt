package com.google.android.settings.display

// import com.android.settingslib.metadata.PreferenceCategory
// import com.google.android.settings.R
import android.content.Context
import com.android.settings.display.DisplayScreen
import com.android.settings.dream.ScreensaverScreen
import com.android.settingslib.metadata.PreferenceHierarchy
import kotlinx.coroutines.CoroutineScope

class DisplayGoogleScreen : DisplayScreen() {

    override fun getPreferenceHierarchy(
        context: Context,
        coroutineScope: CoroutineScope,
    ): PreferenceHierarchy =
        super.getPreferenceHierarchy(context, coroutineScope).apply {
            -ScreensaverScreen.KEY
            onGroup("category_key_appearance") { +ScreensaverScreen.KEY }
            /*
            +PreferenceCategory(
                "category_touch",
                R.string.category_touch_purpose,
                R.string.touch_category_title,
            ) order -160 += {
                +"touch_sensitivity_settings_page"
            }
            */
        }
}
