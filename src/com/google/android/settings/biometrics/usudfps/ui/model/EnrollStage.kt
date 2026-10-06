package com.google.android.settings.biometrics.usudfps.ui.model

enum class EnrollStage(val value: Int) {
    UNKNOWN(-1),
    CENTER(0),
    GUIDED(1),
    FINGERTIP(2),
    LEFT_EDGE(3),
    RIGHT_EDGE(4);

    companion object {
        val POSITIVE_STAGES = arrayOf(CENTER, GUIDED, FINGERTIP, LEFT_EDGE, RIGHT_EDGE)
        val INIT_STAGE = UNKNOWN
        val LAST_STAGE = RIGHT_EDGE
    }
}
