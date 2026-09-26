package com.google.android.settings.fuelgauge.batterysaver

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Bundle
import android.util.Log
import com.android.settingslib.datastore.AbstractKeyedDataObservable
import com.android.settingslib.datastore.HandlerExecutor
import com.android.settingslib.datastore.KeyValueStore

class BatterySaverModeDataStore(private val context: Context) :
    AbstractKeyedDataObservable<String>(), KeyValueStore {

    private var contentObserver: ContentObserver? = null
    private var isFlipendoAggressiveMode = false
    private var isFlipendoEnabled = false
    private val isFlipendoInstalled = FlipendoUtils.isFlipendoInstalled(context)

    init {
        refreshFlipendoStates(false)
    }

    override fun contains(key: String): Boolean =
        key == BasicBatterySaverPreference.KEY || key == ExtremeBatterySaverPreference.KEY

    override fun <T : Any> getValue(key: String, valueType: Class<T>): T? {
        val result =
            when (key) {
                BasicBatterySaverPreference.KEY -> !isFlipendoAggressiveMode && !isFlipendoEnabled
                ExtremeBatterySaverPreference.KEY -> isFlipendoAggressiveMode || isFlipendoEnabled
                else -> null
            }
        @Suppress("UNCHECKED_CAST")
        return result as? T
    }

    override fun <T : Any> setValue(key: String, valueType: Class<T>, value: T?) {
        if (value is Boolean) {
            val mode =
                when (key) {
                    BasicBatterySaverPreference.KEY -> if (value) 0 else 1
                    ExtremeBatterySaverPreference.KEY -> if (value) 1 else 0
                    else -> return
                }
            Log.i(TAG, "setValue ($key, $value) with $mode")
            val bundle = Bundle().apply { putInt("update_flipendo_mode", mode) }
            try {
                context.contentResolver.call(
                    FlipendoUtils.FLIPENDO_STATE_AUTHORITY,
                    "update_flipendo_mode_method",
                    null,
                    bundle,
                )
            } catch (e: Exception) {
                Log.e(TAG, "setValue failed", e)
            }
        }
    }

    override fun onFirstObserverAdded() {
        if (isFlipendoInstalled) {
            contentObserver =
                object : ContentObserver(HandlerExecutor.main) {
                    override fun onChange(selfChange: Boolean, uri: Uri?) {
                        Log.i(TAG, "Flipendo state changed")
                        refreshFlipendoStates(true)
                    }
                }
            context.contentResolver.registerContentObserver(
                FlipendoUtils.FLIPENDO_ENABLED_OBSERVABLE_URI,
                false,
                contentObserver!!,
            )
        }
    }

    override fun onLastObserverRemoved() {
        contentObserver?.let { context.contentResolver.unregisterContentObserver(it) }
    }

    fun refreshFlipendoStates(notify: Boolean) {
        if (isFlipendoInstalled) {
            val flipendoState = FlipendoUtils.getFlipendoState(context)
            isFlipendoAggressiveMode = flipendoState.first
            isFlipendoEnabled = flipendoState.second
            Log.i(TAG, "Flipendo aggressive=$isFlipendoAggressiveMode, enabled=$isFlipendoEnabled")
            if (notify && !isFlipendoAggressiveMode) {
                notifyChange(1)
            }
        }
    }

    companion object {
        private const val TAG = "BatterySaverModeDataStore"
    }
}
