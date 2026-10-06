package com.google.android.settings.biometrics.fingerprint.ui.view

import androidx.navigation.NavOptions
import com.google.android.settings.R

object NavOptionsUseCase {
    fun newNavOptions(): NavOptions {
        return defaultNavOptionsBuilder.build()
    }

    fun newSkipEnrollNavOptions(): NavOptions {
        return defaultNavOptionsBuilder.setPopUpTo(R.id.enroll, true).build()
    }

    fun newPopAllScreensNavOptions(): NavOptions {
        return defaultNavOptionsBuilder.setPopUpTo(R.id.intro, true).build()
    }

    fun newBackToEnrollNavOptions(): NavOptions {
        return defaultNavOptionsBuilder.setPopUpTo(R.id.finish, true).build()
    }

    fun newBackToFindSensorNavOptions(): NavOptions {
        return defaultNavOptionsBuilder.setPopUpTo(R.id.find_sensor, false).build()
    }

    private val defaultNavOptionsBuilder: NavOptions.Builder
        get() =
            NavOptions.Builder()
                .setEnterAnim(
                    com.google.android.setupdesign.R.anim
                        .shared_x_axis_activity_open_enter_dynamic_color
                )
                .setExitAnim(com.google.android.setupdesign.R.anim.shared_x_axis_activity_open_exit)
                .setPopEnterAnim(
                    com.google.android.setupdesign.R.anim
                        .shared_x_axis_activity_close_enter_dynamic_color
                )
                .setPopExitAnim(
                    com.google.android.setupdesign.R.anim.shared_x_axis_activity_close_exit
                )
}
