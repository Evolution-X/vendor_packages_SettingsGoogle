package com.google.android.settings.biometrics.face.data.repository

import android.hardware.face.FaceManager

interface FaceManagerRepository {
    val hasEnrolled: Boolean

    fun isSupport(): Boolean
}

class FaceManagerRepositoryImpl
private constructor(private val userId: Int, private val faceManager: FaceManager?) :
    FaceManagerRepository {

    override val hasEnrolled: Boolean
        get() = faceManager?.hasEnrolledTemplates(userId) ?: false

    override fun isSupport(): Boolean = faceManager != null

    companion object {
        @Volatile private var instances: MutableMap<Int, FaceManagerRepository>? = null

        @Synchronized
        @JvmStatic
        fun getInstance(userId: Int, faceManager: FaceManager?): FaceManagerRepository {
            val map =
                instances ?: LinkedHashMap<Int, FaceManagerRepository>().also { instances = it }
            return map.getOrPut(userId) { FaceManagerRepositoryImpl(userId, faceManager) }
        }
    }
}
