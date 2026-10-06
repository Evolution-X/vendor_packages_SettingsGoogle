package com.google.android.settings.biometrics.combination.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.settings.biometrics.combination.interactor.VibratorInteractor
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

abstract class VibratorViewModel : ViewModel() {
    abstract fun isVibratorEnabled(): StateFlow<Boolean>
}

class VibratorViewModelImpl(vibratorInteractor: VibratorInteractor) : VibratorViewModel() {

    private val isVibratorEnabledFlow =
        vibratorInteractor().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), false)

    override fun isVibratorEnabled(): StateFlow<Boolean> = isVibratorEnabledFlow
}
