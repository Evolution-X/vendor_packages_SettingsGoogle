package com.google.android.settings.biometrics.fingerprint.data.repository

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

interface AccessibilityRepository {
    fun observeAnyAccessibilityServiceEnabled(): Flow<Boolean>
}

class AccessibilityRepositoryImpl(private val context: Context) : AccessibilityRepository {

    private val accessibilityEnabledUri: Uri =
        Settings.Secure.getUriFor(Settings.Secure.ACCESSIBILITY_ENABLED)

    override fun observeAnyAccessibilityServiceEnabled(): Flow<Boolean> =
        callbackFlow {
                fun sendCurrentValue() {
                    val enabled =
                        try {
                            Settings.Secure.getInt(
                                context.contentResolver,
                                Settings.Secure.ACCESSIBILITY_ENABLED,
                            )
                        } catch (e: Settings.SettingNotFoundException) {
                            Log.e(
                                TAG,
                                "Error finding setting, accessibility was not found: ${e.message}",
                            )
                            0
                        }
                    trySend(enabled == 1)
                }

                val observer =
                    object : ContentObserver(Handler(Looper.getMainLooper())) {
                        override fun onChange(selfChange: Boolean, uri: Uri?) {
                            sendCurrentValue()
                        }
                    }

                context.contentResolver.registerContentObserver(
                    accessibilityEnabledUri,
                    false,
                    observer,
                )
                sendCurrentValue()

                awaitClose { context.contentResolver.unregisterContentObserver(observer) }
            }
            .distinctUntilChanged()

    private companion object {
        const val TAG = "AccessibilityRepository"
    }
}
