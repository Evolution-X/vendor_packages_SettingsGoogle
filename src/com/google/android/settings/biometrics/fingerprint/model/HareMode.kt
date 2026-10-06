package com.google.android.settings.biometrics.fingerprint.model

sealed class HareMode {
    object Disabled : HareMode()

    data class Enabled(val param1: Int, val param2: Int) : HareMode()

    companion object {
        fun newInstance(enabled: Boolean, param1: Int, param2: Int): HareMode =
            if (enabled) Enabled(param1, param2) else Disabled
    }
}
