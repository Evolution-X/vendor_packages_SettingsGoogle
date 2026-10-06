package com.google.android.settings.biometrics.fingerprint.ui.model

import android.net.Uri

data class SpQrCode(val uri: Uri) {
    constructor(uriString: String) : this(Uri.parse(uriString))
}
