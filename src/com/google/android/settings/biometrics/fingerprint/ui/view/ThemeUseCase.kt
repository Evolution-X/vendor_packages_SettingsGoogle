package com.google.android.settings.biometrics.fingerprint.ui.view

import androidx.activity.ComponentActivity
import com.android.settings.SetupWizardUtils
import com.google.android.setupdesign.util.ThemeHelper

class ThemeUseCase(private val activity: ComponentActivity) {

    fun applyTheme() {
        val applyGlifExpressiveStyle = ThemeHelper.shouldApplyGlifExpressiveStyle(activity)
        if (applyGlifExpressiveStyle) {
            if (ThemeHelper.trySetSuwTheme(activity)) {
                return
            }
            activity.setTheme(ThemeHelper.getSuwDefaultTheme(activity))
            ThemeHelper.trySetDynamicColor(activity)
        } else {
            activity.setTheme(SetupWizardUtils.getTheme(activity, activity.intent))
            ThemeHelper.trySetDynamicColor(activity)
        }
    }
}
