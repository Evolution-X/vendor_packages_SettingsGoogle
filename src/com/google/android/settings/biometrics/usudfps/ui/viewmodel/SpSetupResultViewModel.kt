package com.google.android.settings.biometrics.usudfps.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.google.android.settings.biometrics.usudfps.ui.model.SpSetupResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

abstract class SpSetupResultViewModel : ViewModel() {
    abstract val resultFlow: SharedFlow<SpSetupResult>

    abstract suspend fun emit(result: SpSetupResult)
}

class SpSetupResultViewModelImpl : SpSetupResultViewModel() {
    private val _resultFlow = MutableSharedFlow<SpSetupResult>()

    override val resultFlow: SharedFlow<SpSetupResult>
        get() = _resultFlow.asSharedFlow()

    override suspend fun emit(result: SpSetupResult) {
        _resultFlow.emit(result)
    }
}
