package com.google.android.settings.biometrics.fingerprint.factory

import android.content.Context
import android.util.Log
import com.android.settings.biometrics.fingerprint.feature.ChallengeGeneratedInvoker
import com.android.settings.biometrics.fingerprint.feature.FingerprintExtPreferencesProvider
import java.lang.reflect.InvocationTargetException

object DynamicClassLoader {
    private const val TAG = "DynamicClassLoader"

    fun newFingerprintExtPreferencesProvider(
        className: String,
        context: Context,
    ): FingerprintExtPreferencesProvider? {
        return try {
            Class.forName(className).getConstructor(Context::class.java).newInstance(context)
                as FingerprintExtPreferencesProvider
        } catch (e: ClassCastException) {
            Log.e(TAG, "Fail to init class $className", e)
            null
        } catch (e: ClassNotFoundException) {
            Log.d(TAG, "Fail to find class $className")
            null
        } catch (e: IllegalAccessException) {
            Log.e(TAG, "Fail to init class $className", e)
            null
        } catch (e: InstantiationException) {
            Log.e(TAG, "Fail to init class $className", e)
            null
        } catch (e: NoSuchMethodException) {
            Log.e(TAG, "Fail to init class $className", e)
            null
        } catch (e: InvocationTargetException) {
            Log.e(TAG, "Fail to init class $className", e)
            null
        }
    }

    fun newChallengeGeneratedInvoker(className: String): ChallengeGeneratedInvoker? {
        return try {
            Class.forName(className).getConstructor().newInstance() as ChallengeGeneratedInvoker
        } catch (e: ClassCastException) {
            Log.e(TAG, "Fail to init class $className", e)
            null
        } catch (e: ClassNotFoundException) {
            Log.d(TAG, "Fail to find class $className")
            null
        } catch (e: IllegalAccessException) {
            Log.e(TAG, "Fail to init class $className", e)
            null
        } catch (e: InstantiationException) {
            Log.e(TAG, "Fail to init class $className", e)
            null
        } catch (e: NoSuchMethodException) {
            Log.e(TAG, "Fail to init class $className", e)
            null
        } catch (e: InvocationTargetException) {
            Log.e(TAG, "Fail to init class $className", e)
            null
        }
    }
}
