package com.google.android.settings.biometrics.fingerprint.interactor

import android.os.UserHandle
import com.google.android.settings.biometrics.combination.data.repository.AccessRepository

interface SpAccessPolicy {
    val canEdit: Boolean
    val canEditAsProfileUser: Boolean
    val systemUserHandleForEditing: UserHandle?
}

class SpAccessPolicyImpl(private val accessRepository: AccessRepository) : SpAccessPolicy {

    override val systemUserHandleForEditing: UserHandle? by lazy {
        accessRepository.systemUserHandle
    }

    val canEditAsMainUser: Boolean
        get() = accessRepository.isMainUser()

    override val canEditAsProfileUser: Boolean
        get() = !canEditAsMainUser && systemUserHandleForEditing != null

    override val canEdit: Boolean
        get() = canEditAsMainUser || canEditAsProfileUser
}
