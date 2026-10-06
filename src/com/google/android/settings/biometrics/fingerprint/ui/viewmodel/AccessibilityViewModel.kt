package com.google.android.settings.biometrics.fingerprint.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.settings.biometrics.fingerprint.interactor.CheckAccessibilityStatusInteractor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AccessibilityViewModel(
    private val checkAccessibilityStatusInteractor: CheckAccessibilityStatusInteractor
) : ViewModel() {

    private val _isAnyAccessibilityServiceEnabled = MutableStateFlow(false)
    val isAnyAccessibilityServiceEnabled: StateFlow<Boolean> =
        _isAnyAccessibilityServiceEnabled.asStateFlow()

    init {
        observeAnyAccessibilityServiceStatus()
    }

    private fun observeAnyAccessibilityServiceStatus() {
        viewModelScope.launch {
            checkAccessibilityStatusInteractor.observeAnyAccessibilityServiceEnabled().collect {
                _isAnyAccessibilityServiceEnabled.value = it
            }
        }
    }
}
