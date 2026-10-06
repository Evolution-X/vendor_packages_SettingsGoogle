package com.google.android.settings.biometrics.usudfps.preference

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.annotation.VisibleForTesting
import androidx.preference.PreferenceViewHolder
import com.android.settings.biometrics.fingerprint.feature.PrimarySwitchIntentPreference
import com.android.settings.password.ChooseLockSettingsHelper
import com.android.settingslib.core.instrumentation.SettingsJankMonitor
import com.android.settingslib.spaprivileged.framework.common.userManager
import com.google.android.settings.biometrics.combination.data.repository.AccessRepositoryImpl
import com.google.android.settings.biometrics.fingerprint.interactor.SpAccessPolicy
import com.google.android.settings.biometrics.fingerprint.interactor.SpAccessPolicyImpl
import com.google.android.settings.biometrics.fingerprint.model.SpHal
import com.google.android.settings.biometrics.usudfps.ui.view.SpSetupActivity

open class ScreenProtectorPreference : PrimarySwitchIntentPreference {

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int,
    ) : super(context, attrs, defStyleAttr, defStyleRes)

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
    ) : super(context, attrs, defStyleAttr)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context) : super(context)

    private val spAccessPolicy: SpAccessPolicy by lazy {
        SpAccessPolicyImpl(AccessRepositoryImpl.getInstance(context.userManager))
    }

    private var isPrefEnabled = true

    override fun forceUpdate() {
        notifyChanged()
    }

    override fun getLaunchedIntent(token: ByteArray): Intent =
        Intent(context, SpSetupActivity::class.java)
            .putExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN, token)

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        setSwitchEnabled(isPrefEnabled)
        isChecked = preferenceDataStore?.getBoolean(key, false) ?: false
        super.onBindViewHolder(holder)
        val switchWidget = holder.findViewById(androidx.preference.R.id.switchWidget)
        switchWidget?.setOnClickListener { view: View? ->
            if (view == null || !view.isEnabled) {
                return@setOnClickListener
            }
            val newChecked = !isChecked
            if (callChangeListener(newChecked)) {
                SettingsJankMonitor.detectToggleJank(key, view)
                isChecked = newChecked
                persistBoolean(newChecked)
                notifyChanged()
            }
        }
        switchWidget?.isEnabled = isEnabled
        updateSummaryIfDifferent()
    }

    override fun getConfirmationDialogBeforeStateChange(
        newValue: Any?
    ): ConfirmationDialogDetails? {
        if (newValue !is Boolean || newValue) {
            return null
        }
        return ConfirmationDialogDetails(
            ConfirmSpOffDialogFragment(),
            ConfirmSpOffDialogFragment.REQUEST_KEY,
        )
    }

    override val confirmDialogFragmentResultKey: String
        get() = ConfirmSpOffDialogFragment.REQUEST_KEY

    override fun processConfirmationDialogResult(resultBundle: Bundle) {
        val switchView = switch
        if (resultBundle.getBoolean(ConfirmSpOffDialogFragment.KEY_RESULT_CONFIRMED_OFF)) {
            Log.d(TAG, "Confirm change, update UI and datastore")
            if (switchView != null && switchView.isChecked) {
                SettingsJankMonitor.detectToggleJank(key, switchView)
            }
            isChecked = false
            persistBoolean(false)
            notifyChanged()
            return
        }
        Log.d(TAG, "Rollback change, update UI")
        if (switchView != null && !switchView.isChecked) {
            SettingsJankMonitor.detectToggleJank(key, switchView)
        }
        isChecked = true
        notifyChanged()
    }

    override fun setEnabled(enabled: Boolean) {
        isPrefEnabled = enabled
        super.setEnabled(enabled && spAccessPolicy.canEdit)
    }

    override fun isEnabled(): Boolean = isPrefEnabled && spAccessPolicy.canEdit

    private fun updateSummaryIfDifferent() {
        val newSummary = summaryProvider?.provideSummary(this) ?: ""
        if ((summary ?: "") == newSummary) {
            return
        }
        summary = newSummary
    }

    fun putFirstPartySpHal(spHal: SpHal): Boolean =
        (preferenceDataStore as ScreenProtectorDataStore).putFirstPartySpHal(spHal)

    override fun onAttached() {
        super.onAttached()
        context.sendBroadcast(
            Intent(ATTACH_INTENT).setPackage(SYSTEMUI_PACKAGE),
            Manifest.permission.USE_BIOMETRIC_INTERNAL,
        )
    }

    override fun onDetached() {
        context.sendBroadcast(
            Intent(DETACH_INTENT).setPackage(SYSTEMUI_PACKAGE),
            Manifest.permission.USE_BIOMETRIC_INTERNAL,
        )
        super.onDetached()
    }

    companion object {
        private const val TAG = "ScreenProtectorPref"
        private const val SYSTEMUI_PACKAGE = "com.android.systemui"

        @VisibleForTesting
        const val ATTACH_INTENT = "com.google.android.biometric.screenprotector.preference.attach"

        @VisibleForTesting
        const val DETACH_INTENT = "com.google.android.biometric.screenprotector.preference.detach"
    }
}
