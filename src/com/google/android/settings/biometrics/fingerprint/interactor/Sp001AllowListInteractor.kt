package com.google.android.settings.biometrics.fingerprint.interactor

import android.os.Build
import android.util.Log
import com.google.android.settings.biometrics.fingerprint.data.repository.Sp001AllowListRepository
import com.google.android.settings.biometrics.fingerprint.model.CapybaraMetricsStatus
import com.google.android.settings.biometrics.fingerprint.model.SpData
import com.google.android.settings.biometrics.fingerprint.model.SpHal
import com.google.android.settings.biometrics.fingerprint.model.SpProductInfo
import com.google.android.settings.biometrics.fingerprint.ui.model.SpQrCode
import com.google.android.settings.biometrics.fingerprint.ui.model.SpUiData

interface Sp001AllowListInteractor {
    val spNone: SpHal
    val spThirdParty: SpHal

    fun isEnabled(): Boolean

    fun getFirstPartySpHal(spQrCode: SpQrCode): SpHal?

    fun getFirstPartySpProductInfo(spQrCode: SpQrCode): SpProductInfo?

    fun getFirstPartySpProductInfo(spHal: SpHal?): SpProductInfo?

    fun getSpUiData(spHal: SpHal?): SpUiData

    fun getCapybaraMetricsStatus(spHal: SpHal?): CapybaraMetricsStatus

    fun isValidFormat(spQrCode: SpQrCode): Boolean
}

class Sp001AllowListInteractorImpl(private val sp001AllowListRepository: Sp001AllowListRepository) :
    Sp001AllowListInteractor {

    override val spNone: SpHal by lazy { sp001AllowListRepository.spNone }

    override val spThirdParty: SpHal by lazy { sp001AllowListRepository.spThirdParty }

    private val spFirstPartyList: List<SpData> by lazy { sp001AllowListRepository.allowList }

    override fun isEnabled(): Boolean {
        return spFirstPartyList.isNotEmpty()
    }

    private fun firstSpDataOrNull(list: List<SpData>, spQrCode: SpQrCode): SpData? {
        val uid = spQrCode.uri.getQueryParameter(KEY_UID)
        val hex = runCatching { uid?.toUInt(16) }.getOrNull() ?: return null
        return list.firstOrNull { it.hal.hex == hex }
    }

    override fun getFirstPartySpHal(spQrCode: SpQrCode): SpHal? =
        firstSpDataOrNull(spFirstPartyList, spQrCode)?.hal

    override fun getFirstPartySpProductInfo(spQrCode: SpQrCode): SpProductInfo? =
        firstSpDataOrNull(spFirstPartyList, spQrCode)?.detail

    override fun getFirstPartySpProductInfo(spHal: SpHal?): SpProductInfo? {
        if (spHal == null) return null
        return spFirstPartyList.firstOrNull { it.hal == spHal }?.detail
    }

    override fun getSpUiData(spHal: SpHal?): SpUiData {
        val firstPartySpProductInfo = getFirstPartySpProductInfo(spHal)
        if (firstPartySpProductInfo != null) {
            return SpUiData.FirstParty(firstPartySpProductInfo)
        }
        return if (spHal == spNone) {
            SpUiData.None
        } else if (spHal == spThirdParty) {
            SpUiData.ThirdParty
        } else {
            SpUiData.Unset
        }
    }

    override fun getCapybaraMetricsStatus(spHal: SpHal?): CapybaraMetricsStatus {
        if (!isEnabled()) {
            return CapybaraMetricsStatus.NOT_SUPPORTED
        }
        return when (getSpUiData(spHal)) {
            is SpUiData.Unset -> CapybaraMetricsStatus.UNSET
            is SpUiData.None -> CapybaraMetricsStatus.NO_SCREEN_PROTECTOR
            else -> CapybaraMetricsStatus.HAS_SCREEN_PROTECTOR
        }
    }

    override fun isValidFormat(spQrCode: SpQrCode): Boolean {
        val uri = spQrCode.uri
        val isSchemeValid = uri.scheme == SCHEME
        val isAuthorityValid = uri.authority == AUTHORITY
        val isPortValid = uri.port == -1
        val isPathValid = uri.path.isNullOrEmpty()
        val isFormatValid = uri.getQueryParameter(KEY_FORMAT) == FORMAT_VERSION
        val isQuerySizeValid = uri.queryParameterNames.size == 2
        if (Build.IS_DEBUGGABLE) {
            Log.d(
                TAG,
                "isValidFormat S:$isSchemeValid, A:$isAuthorityValid, PO:$isPortValid," +
                    " PA:$isPathValid, F:$isFormatValid, Q:$isQuerySizeValid",
            )
        }
        return isSchemeValid &&
            isAuthorityValid &&
            isPathValid &&
            isFormatValid &&
            isPortValid &&
            isQuerySizeValid
    }

    companion object {
        private const val TAG = "Sp001AllowListInteractor"
        private const val SCHEME = "mfg-sp"
        private const val AUTHORITY = "pixel"
        private const val KEY_FORMAT = "format"
        private const val KEY_UID = "uid"
        private const val FORMAT_VERSION = "0.01"
    }
}
