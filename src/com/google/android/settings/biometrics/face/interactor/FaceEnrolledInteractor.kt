package com.google.android.settings.biometrics.face.interactor

import com.google.android.settings.biometrics.face.data.repository.FaceManagerRepository

interface FaceEnrolledInteractor {
    val hasEnrolled: Boolean

    fun isSupportFaceUnlock(): Boolean
}

class FaceEnrolledInteractorImpl(private val faceManagerRepository: FaceManagerRepository) :
    FaceEnrolledInteractor {

    override fun isSupportFaceUnlock(): Boolean = faceManagerRepository.isSupport()

    override val hasEnrolled: Boolean
        get() = faceManagerRepository.hasEnrolled
}
