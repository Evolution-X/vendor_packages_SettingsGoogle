package com.google.android.settings.biometrics.usudfps.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.android.settings.biometrics.fingerprint.interactor.ScreenProtectorInteractor
import com.google.android.settings.biometrics.fingerprint.interactor.Sp001AllowListInteractor
import com.google.android.settings.biometrics.fingerprint.model.SpProductInfo
import com.google.android.settings.biometrics.fingerprint.ui.model.SpQrCode

abstract class SpQrCodeScannerViewModel : ViewModel() {
    abstract val isEnrolling: Boolean
    abstract val qrCodeProductInfo: SpProductInfo?

    abstract fun setQrCodeContent(content: String)

    abstract fun isQrCodeValidFormat(): Boolean

    abstract fun isQrCodeAllowed(): Boolean

    abstract fun setQrCodeScreenProtector(): Boolean
}

class SpQrCodeScannerViewModelImpl(
    override val isEnrolling: Boolean,
    private val spInteractor: ScreenProtectorInteractor,
    private val sp001AllowListInteractor: Sp001AllowListInteractor,
) : SpQrCodeScannerViewModel() {

    private var spQrCode: SpQrCode? = null

    override fun setQrCodeContent(content: String) {
        spQrCode = SpQrCode(content)
    }

    override fun isQrCodeValidFormat(): Boolean {
        val qrCode = spQrCode ?: return false
        return sp001AllowListInteractor.isValidFormat(qrCode)
    }

    override fun isQrCodeAllowed(): Boolean {
        val qrCode = spQrCode ?: return false
        return sp001AllowListInteractor.getFirstPartySpHal(qrCode) != null
    }

    override val qrCodeProductInfo: SpProductInfo?
        get() = spQrCode?.let { sp001AllowListInteractor.getFirstPartySpProductInfo(it) }

    override fun setQrCodeScreenProtector(): Boolean {
        val spHal =
            spQrCode?.let { sp001AllowListInteractor.getFirstPartySpHal(it) } ?: return false
        try {
            spInteractor.setScreenProtector(spHal)
            return true
        } catch (e: SecurityException) {
            Log.e(TAG, "Fail to set sp", e)
        }
        return false
    }

    companion object {
        private const val TAG = "SpQrCodeScannerViewModel"
    }
}
