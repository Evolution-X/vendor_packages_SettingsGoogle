package com.google.android.settings.biometrics.face

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.UserHandle
import android.os.UserManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.settings.R.string.config_face_enroll_traffic_light_package

private const val TAG = "FaceEnrollTrampoline"

class FaceEnrollOverlayLauncher(private val activity: Activity) {

    fun maybeLaunchOverlayParticipationFlow(intent: Intent): Boolean {
        val overlayIntent =
            Intent()
                .setComponent(
                    ComponentName(
                        "com.google.android.apps.overlay",
                        "com.google.android.apps.vision.overlay.internal.setupwizard." +
                            "SetupWizardFaceEnrollActivity",
                    )
                )
                .putExtra(
                    "face_action",
                    "com.google.android.settings.future.biometrics.faceenroll.action.ENROLL",
                )
                .putExtra(
                    "face_package",
                    activity.getString(config_face_enroll_traffic_light_package),
                )
                .putExtras(intent)
        val preferredUserHandle = getPreferredUserHandle(activity.packageManager, overlayIntent)
        if (preferredUserHandle == null) {
            Log.i(TAG, "No matches; can't use Overlay for new participation flow")
            return false
        }
        Log.i(TAG, "Found match; launching Overlay participation flow as user $preferredUserHandle")
        return try {
            activity.startActivityForResultAsUser(
                overlayIntent,
                REQUEST_CODE_PARTICIPATION,
                preferredUserHandle,
            )
            Log.i(TAG, "Overlay launched successfully")
            true
        } catch (e: Exception) {
            Log.w(TAG, "Error launching Overlay", e)
            false
        }
    }

    private fun getPreferredUserHandle(
        packageManager: PackageManager,
        intent: Intent,
    ): UserHandle? {
        for (userHandle in getSortedUserHandles()) {
            Log.d(TAG, "Checking if Overlay participation flow can be launched in $userHandle")
            val matched = doesProfileResolveIntent(activity, packageManager, intent, userHandle)
            Log.d(TAG, "Profile with handle $userHandle matched = $matched")
            if (matched) {
                return userHandle
            }
        }
        return null
    }

    private fun getSortedUserHandles(): List<UserHandle> {
        val userManager = ContextCompat.getSystemService(activity, UserManager::class.java)
        if (userManager != null) {
            val sortedProfiles = userProfilesSortedByPreferredProfile(userManager)
            Log.d(TAG, "All user profiles (sorted): $sortedProfiles")
            return sortedProfiles
        }
        Log.w(TAG, "Unable to get UserManager; will just check the current user")
        return listOf(UserHandle.CURRENT)
    }

    companion object {
        private const val REQUEST_CODE_PARTICIPATION = 2
    }
}

private fun userProfilesSortedByPreferredProfile(userManager: UserManager): List<UserHandle> =
    userManager.userProfiles.sortedBy { userHandle ->
        when {
            userManager.isManagedProfile(userHandle.identifier) -> 1
            isMainUser(userManager, userHandle) -> 2
            else -> 3
        }
    }

private fun isMainUser(userManager: UserManager, userHandle: UserHandle): Boolean =
    userManager.getUserInfo(userHandle.identifier)?.isMain == true

private fun doesProfileResolveIntent(
    context: Context,
    packageManager: PackageManager,
    intent: Intent,
    userHandle: UserHandle,
): Boolean {
    val userPackageManager = context.createContextAsUser(userHandle, 0).packageManager
    Log.d(TAG, "Ensuring Overlay is enabled for userId: ${userHandle.identifier}")
    try {
        userPackageManager.setApplicationEnabledSetting(
            "com.google.android.apps.overlay",
            PackageManager.COMPONENT_ENABLED_STATE_DEFAULT,
            0,
        )
    } catch (e: IllegalArgumentException) {
        Log.w(
            TAG,
            "Unable to enable Overlay in handle $userHandle" +
                ". Will try to resolve the intent anyway.",
            e,
        )
    }
    Log.d(TAG, "Checking if Overlay Intent resolves in $userHandle")
    val resolveInfo =
        packageManager.resolveActivityAsUser(
            intent,
            PackageManager.MATCH_DEFAULT_ONLY,
            userHandle.identifier,
        )
    Log.d(TAG, "ResolveInfo is: $resolveInfo")
    return resolveInfo != null
}
