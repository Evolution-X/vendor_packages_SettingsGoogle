package com.google.android.settings.biometrics.fingerprint.interactor

import android.content.Context
import com.android.settings.Utils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

interface PrivateProfileInteractor {
    val isPrivateProfile: Flow<Boolean>
}

class PrivateProfileInteractorImpl(val context: Context) : PrivateProfileInteractor {
    override val isPrivateProfile: Flow<Boolean> = flow {
        emit(Utils.isPrivateProfile(context.userId, context))
    }
}
