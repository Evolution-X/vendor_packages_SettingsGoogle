package com.google.android.settings.biometrics.combination.data.repository

import android.os.UserHandle
import android.os.UserManager
import android.util.Log

interface AccessRepository {
    val systemUserHandle: UserHandle?

    fun isMainUser(): Boolean
}

class AccessRepositoryImpl private constructor(private val userManager: UserManager) :
    AccessRepository {

    private val isMainUserValue: Boolean by lazy {
        try {
            userManager.isMainUser
        } catch (e: Exception) {
            Log.e(TAG, "Fail to call UserManager API for current user")
            false
        }
    }

    override val systemUserHandle: UserHandle? by lazy {
        try {
            userManager.userProfiles.firstOrNull { it.isSystem }
        } catch (e: Exception) {
            Log.e(TAG, "Fail to call UserManager API for current user")
            null
        }
    }

    override fun isMainUser(): Boolean {
        return isMainUserValue
    }

    companion object {
        private const val TAG = "AccessRepositoryImpl"

        @Volatile private var instance: AccessRepository? = null

        @JvmStatic
        @Synchronized
        fun getInstance(userManager: UserManager): AccessRepository {
            checkNotNull(userManager)
            return instance ?: AccessRepositoryImpl(userManager).also { instance = it }
        }
    }
}
