package com.google.android.settings.biometrics.face

import android.content.Context
import android.os.UserHandle
import android.provider.Settings
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.CompoundButton
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.settings.R

class FaceEnrollLockScreenBypassToggle
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    LinearLayout(context, attrs, defStyleAttr) {

    private val userHandle: Int by lazy { UserHandle.of(UserHandle.USER_CURRENT).identifier }

    private val defaultValue: Int by lazy {
        if (resources.getBoolean(com.android.internal.R.bool.config_faceAuthDismissesKeyguard)) 1
        else 0
    }

    val switch: MaterialSwitch by lazy { requireNotNull(findViewById(R.id.toggle)) }

    private var _isEnabled = true

    var innerCompoundButtonCheckedChangeListener: CompoundButton.OnCheckedChangeListener? = null

    init {
        LayoutInflater.from(context)
            .inflate(R.layout.face_enroll_lock_screen_bypass_toggle, this, true)
        switch.isEnabled = _isEnabled
        switch.isClickable = false
        switch.isFocusable = false
        switch.isChecked = isChecked()
        if (!_isEnabled) {
            findViewById<TextView>(R.id.subtitle)
                ?.setText(com.android.settingslib.widget.restricted.R.string.disabled_by_admin)
        }
    }

    fun setIsEnabled(enabled: Boolean) {
        _isEnabled = enabled
        switch.isEnabled = _isEnabled
    }

    val switchButton: MaterialSwitch
        get() = switch

    fun isChecked(): Boolean =
        Settings.Secure.getIntForUser(
            context.contentResolver,
            Settings.Secure.FACE_UNLOCK_DISMISSES_KEYGUARD,
            defaultValue,
            userHandle,
        ) != 0

    fun setChecked(checked: Boolean) {
        Settings.Secure.putIntForUser(
            context.contentResolver,
            Settings.Secure.FACE_UNLOCK_DISMISSES_KEYGUARD,
            if (checked) 1 else 0,
            userHandle,
        )
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        switch.setOnCheckedChangeListener { compoundButton, isChecked ->
            setChecked(isChecked)
            innerCompoundButtonCheckedChangeListener?.onCheckedChanged(compoundButton, isChecked)
        }
        if (_isEnabled) {
            setOnClickListener { switch.toggle() }
        }
    }

    override fun onDetachedFromWindow() {
        switch.setOnCheckedChangeListener(null)
        super.onDetachedFromWindow()
    }
}
