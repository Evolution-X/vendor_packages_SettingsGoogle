package com.google.android.settings.biometrics.fingerprint.data.repository

import android.content.res.Resources
import android.util.Log
import com.google.android.settings.R
import com.google.android.settings.biometrics.fingerprint.model.SpData
import com.google.android.settings.biometrics.fingerprint.model.SpHal
import com.google.android.settings.biometrics.fingerprint.model.SpProductInfo

interface Sp001AllowListRepository {
    val allowList: List<SpData>

    val spNone: SpHal

    val spThirdParty: SpHal
}

class Sp001AllowListRepositoryImpl private constructor(private val res: Resources) :
    Sp001AllowListRepository {

    override val allowList: List<SpData> by lazy {
        val hals = res.getStringArray(R.array.sp_config_001_first_party_hals)
        checkNotNull(hals)
        val modelNames = res.getStringArray(R.array.sp_config_001_ui_model_names)
        checkNotNull(modelNames)
        if (hals.isEmpty()) {
            Log.d(TAG, "No allow list found in resources!!")
            emptyList()
        } else if (hals.size != modelNames.size) {
            Log.d(TAG, "Allow list resource sizes do not match!!")
            emptyList()
        } else {
            val list = hals.mapIndexed { index, hal ->
                SpData(SpProductInfo(modelNames[index]), SpHal.getInstance(hal))
            }
            Log.d(TAG, "Allow list loaded with ${list.size} items")
            list
        }
    }

    override val spNone: SpHal by lazy {
        SpHal.getInstance(res.getString(R.string.sp_config_001_hal_none))
    }

    override val spThirdParty: SpHal by lazy {
        SpHal.getInstance(res.getString(R.string.sp_config_001_hal_third_party))
    }

    companion object {
        private const val TAG = "Sp001AllowListRepository"

        @Volatile private var instance: Sp001AllowListRepository? = null

        @JvmStatic
        @Synchronized
        fun getInstance(resources: Resources): Sp001AllowListRepository {
            checkNotNull(resources)
            return instance ?: Sp001AllowListRepositoryImpl(resources).also { instance = it }
        }
    }
}
