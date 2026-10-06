package com.google.android.settings.biometrics.fingerprint.interactor

import com.google.android.settings.biometrics.fingerprint.data.repository.AccessibilityRepository
import kotlinx.coroutines.flow.Flow

interface CheckAccessibilityStatusInteractor {
    fun observeAnyAccessibilityServiceEnabled(): Flow<Boolean>
}

class CheckAccessibilityStatusInteractorImpl(
    private val accessibilityRepository: AccessibilityRepository
) : CheckAccessibilityStatusInteractor {

    override fun observeAnyAccessibilityServiceEnabled(): Flow<Boolean> {
        return accessibilityRepository.observeAnyAccessibilityServiceEnabled()
    }
}
