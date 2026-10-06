package com.google.android.settings.biometrics.combination.ui.view

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import com.android.settings.biometrics.BiometricUtils
import com.android.settings.password.ChooseLockSettingsHelper
import com.google.android.settings.R
import com.google.android.settings.biometrics.combination.ui.model.GkResult

class FrrActivityUseCase {

    fun getConfirmLockLauncher(
        userId: Int,
        activity: Activity,
        requestCode: Int,
    ): ChooseLockSettingsHelper {
        checkNotNull(activity)
        val builder = ChooseLockSettingsHelper.Builder(activity)
        builder
            .setRequestCode(requestCode)
            .setTitle(activity.getString(R.string.security_settings_fingerprint_preference_title))
            .setRequestGatekeeperPasswordHandle(true)
            .setForegroundOnly(true)
            .setReturnCredentials(true)
            .setUserId(userId)
        val chooseLockSettingsHelper = builder.build()
        checkNotNull(chooseLockSettingsHelper)
        return chooseLockSettingsHelper
    }

    fun getFingerprintIntent(ok: GkResult.Ok?, context: Context): Intent {
        checkNotNull(context)
        val fingerprintIntroIntent = BiometricUtils.getFingerprintIntroIntent(context, Intent())
        if (ok != null) {
            fingerprintIntroIntent.putExtra("challenge", ok.challenge)
            fingerprintIntroIntent.putExtra("hw_auth_token", ok.token)
        }
        fingerprintIntroIntent.putExtra("isFromFrr", true)
        return fingerprintIntroIntent
    }

    fun updateExpressViews(linearLayout: LinearLayout) {
        checkNotNull(linearLayout)
        val visibleChildren =
            (0 until linearLayout.childCount)
                .map { linearLayout.getChildAt(it) }
                .filter { it.visibility == View.VISIBLE }
        val count = visibleChildren.size
        val context = linearLayout.context
        visibleChildren.forEachIndexed { index, view ->
            val backgroundRes =
                when {
                    index == 0 && count == 1 -> R.drawable.frr_entry_expressive_single_bg
                    index == 0 -> R.drawable.frr_entry_expressive_top_bg
                    index > 0 && index == count - 1 -> R.drawable.frr_entry_expressive_bottom_bg
                    else -> R.drawable.frr_entry_expressive_inner_bg
                }
            view.background = ContextCompat.getDrawable(context, backgroundRes)
            val layoutParams = checkNotNull(view.layoutParams) as LinearLayout.LayoutParams
            layoutParams.bottomMargin =
                if (index < count - 1) {
                    context.resources.getDimensionPixelSize(R.dimen.frr_divider_height_expressive)
                } else {
                    0
                }
            view.layoutParams = layoutParams
        }
    }
}
