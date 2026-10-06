package com.google.android.settings.biometrics.usudfps.ui.model

import com.android.settings.R as AR
import com.google.android.settings.R

enum class HareStage(
    val value: Int,
    val lottieResId: Int? = null,
    val titleResId: Int,
    val subtitleResId: Int? = null,
    val vibratorDisabledSubtitleResId: Int? = null,
    val setFocusOnEnrollingView: Boolean = true,
    val adjustScrollableHeaderIfNeeded: Boolean = true,
    val updateActivityTitle: Boolean = false,
) {
    INIT(
        value = -1,
        titleResId = AR.string.security_settings_fingerprint_enroll_udfps_title,
        subtitleResId = R.string.security_settings_udfps_enroll_start_message_new,
        vibratorDisabledSubtitleResId =
            R.string.security_settings_udfps_enroll_start_message_without_haptic,
        setFocusOnEnrollingView = false,
        adjustScrollableHeaderIfNeeded = false,
        updateActivityTitle = true,
    ),
    PAD1(
        value = 0,
        lottieResId = R.raw.udfps_center_hint_lottie_expressive,
        titleResId = AR.string.security_settings_fingerprint_enroll_repeat_title,
        subtitleResId = R.string.security_settings_udfps_enroll_start_message_new,
        vibratorDisabledSubtitleResId =
            R.string.security_settings_udfps_enroll_start_message_without_haptic,
    ),
    FINGERTIP(
        value = 1,
        lottieResId = R.raw.udfps_tip_hint_lottie_expressive,
        titleResId = R.string.enroll_usudfps_fingertip_title_simple,
    ),
    LEFT_EDGE1(
        value = 2,
        lottieResId = R.raw.udfps_left_edge_hint_lottie_expressive,
        titleResId = R.string.enroll_usudfps_left_edge_title_simple,
        subtitleResId = AR.string.security_settings_udfps_enroll_edge_message,
        vibratorDisabledSubtitleResId = AR.string.security_settings_udfps_enroll_edge_message,
    ),
    LEFT_EDGE2(
        value = 3,
        lottieResId = R.raw.udfps_left_edge_hint_lottie_expressive,
        titleResId = R.string.enroll_usudfps_left_edge_title_simple,
        subtitleResId = AR.string.security_settings_fingerprint_enroll_repeat_message,
        vibratorDisabledSubtitleResId =
            AR.string.security_settings_fingerprint_enroll_repeat_message,
    ),
    RIGHT_EDGE1(
        value = 4,
        lottieResId = R.raw.udfps_right_edge_hint_lottie_expressive,
        titleResId = R.string.enroll_usudfps_right_edge_title_simple,
        subtitleResId = AR.string.security_settings_udfps_enroll_edge_message,
        vibratorDisabledSubtitleResId = AR.string.security_settings_udfps_enroll_edge_message,
    ),
    RIGHT_EDGE2(
        value = 5,
        lottieResId = R.raw.udfps_right_edge_hint_lottie_expressive,
        titleResId = R.string.enroll_usudfps_right_edge_title_simple,
        subtitleResId = AR.string.security_settings_fingerprint_enroll_repeat_message,
        vibratorDisabledSubtitleResId =
            AR.string.security_settings_fingerprint_enroll_repeat_message,
    ),
    PAD2(
        value = 6,
        lottieResId = R.raw.udfps_center_hint_lottie_expressive,
        titleResId = AR.string.security_settings_fingerprint_enroll_repeat_title,
        subtitleResId = AR.string.security_settings_udfps_enroll_repeat_a11y_message,
        vibratorDisabledSubtitleResId = AR.string.security_settings_udfps_enroll_repeat_a11y_message,
    );

    companion object {
        val TAP_TO_STAGE =
            arrayOf(
                INIT,
                PAD1,
                PAD1,
                PAD1,
                FINGERTIP,
                FINGERTIP,
                FINGERTIP,
                LEFT_EDGE1,
                LEFT_EDGE2,
                RIGHT_EDGE1,
                RIGHT_EDGE2,
                PAD2,
            )
    }
}
