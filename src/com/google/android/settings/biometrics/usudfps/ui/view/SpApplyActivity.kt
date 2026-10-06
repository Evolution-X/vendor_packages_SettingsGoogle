package com.google.android.settings.biometrics.usudfps.ui.view

import android.content.DialogInterface
import android.content.Intent
import android.content.res.Resources
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import com.airbnb.lottie.LottieAnimationView
import com.android.settings.R as AR
import com.android.settings.biometrics.fingerprint.FingerprintSettings
import com.android.settingslib.widget.LottieColorUtils
import com.google.android.settings.R
import com.google.android.settings.biometrics.fingerprint.ui.model.SpQrCode
import com.google.android.settings.biometrics.fingerprint.ui.view.GlifLayoutUseCase
import com.google.android.settings.biometrics.fingerprint.ui.view.ThemeUseCase
import com.google.android.settings.biometrics.ui.view.LottiePlayPauseUseCase
import com.google.android.settings.biometrics.usudfps.factory.UsudfpsViewModelFactory
import com.google.android.settings.biometrics.usudfps.feature.ScreenProtectorInvoker
import com.google.android.settings.biometrics.usudfps.ui.viewmodel.SpApplyViewModel
import com.google.android.setupcompat.template.FooterBarMixin
import com.google.android.setupcompat.template.FooterButton
import com.google.android.setupdesign.GlifLayout
import com.google.android.setupdesign.util.LottieAnimationHelper
import com.google.android.setupdesign.util.ThemeHelper

class SpApplyActivity : ComponentActivity(R.layout.sp_apply) {

    private val viewModel: SpApplyViewModel by viewModels()

    private var lottiePlayPauseUseCase: LottiePlayPauseUseCase? = null

    private val useExpressive: Boolean by lazy {
        ThemeHelper.shouldApplyGlifExpressiveStyle(applicationContext)
    }

    private val onApplyClickListener: View.OnClickListener by lazy {
        object : View.OnClickListener {
            override fun onClick(v: View?) {
                val spHal = viewModel.spHal ?: return
                val invoker = ScreenProtectorInvoker()
                val intent =
                    Intent()
                        .setPackage(SETTINGS_PACKAGE)
                        .setClass(this@SpApplyActivity, FingerprintSettings::class.java)
                        .putExtra(invoker.intentKeyForBundle, invoker.newIntentBundle(spHal))
                if (viewModel.switchToMainUserForEditing) {
                    val systemUserHandle = viewModel.systemUserHandleForEditing
                    if (systemUserHandle != null) {
                        Log.d(TAG, "Launch Fingerprint Settings as system user profile")
                        startActivityAsUser(intent, systemUserHandle)
                    } else {
                        Log.e(TAG, "System user profile not found")
                    }
                } else {
                    startActivity(intent)
                }
                finish()
            }
        }
    }

    private val onCancelClickListener: View.OnClickListener by lazy {
        object : View.OnClickListener {
            override fun onClick(v: View?) {
                finish()
            }
        }
    }

    private val onDialogLearnMoreListener: DialogInterface.OnClickListener by lazy {
        object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface?, which: Int) {
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(getString(R.string.sp_apply_dialog_button_url)),
                    )
                )
            }
        }
    }

    private val _defaultViewModelProviderFactory: ViewModelProvider.Factory by lazy {
        UsudfpsViewModelFactory()
    }

    override val defaultViewModelProviderFactory: ViewModelProvider.Factory =
        _defaultViewModelProviderFactory

    private fun showInfoDialog(messageResId: Int) {
        AlertDialog.Builder(this)
            .setMessage(messageResId)
            .setPositiveButton(R.string.sp_apply_dialog_button_got_it, null)
            .setOnDismissListener { finish() }
            .create()
            .apply {
                setCanceledOnTouchOutside(false)
                show()
            }
    }

    private fun showLearnMoreDialog() {
        AlertDialog.Builder(this)
            .setMessage(R.string.sp_apply_dialog_msg_incompatible)
            .setNegativeButton(AR.string.learn_more, onDialogLearnMoreListener)
            .setPositiveButton(R.string.sp_apply_dialog_button_got_it, null)
            .setOnDismissListener { finish() }
            .create()
            .apply {
                setCanceledOnTouchOutside(false)
                show()
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeUseCase(this).applyTheme()
        val productInfo = viewModel.firstPartySpProductInfo
        if (!viewModel.isSpEnabled()) {
            showLearnMoreDialog()
            return
        }
        if (!viewModel.canEdit) {
            showInfoDialog(R.string.sp_apply_dialog_msg_main_user_only)
            return
        }
        if (!viewModel.isValidQrCode) {
            showInfoDialog(R.string.sp_apply_dialog_msg_invalid_qr_code)
            return
        }
        if (productInfo == null) {
            showLearnMoreDialog()
            return
        }
        val glifLayout = requireViewById<GlifLayout>(R.id.setup_wizard_layout)
        val lottieView = glifLayout.requireViewById<LottieAnimationView>(R.id.illustration_lottie)
        if (useExpressive) {
            lottieView.setAnimation(R.raw.fingerprint_sp_expressive)
            val colors =
                applicationContext.resources
                    .getStringArray(R.array.touch_sensor_screen_protector)
                    .toList()
            LottieAnimationHelper.get().applyColor(applicationContext, lottieView, colors)
        } else {
            LottieColorUtils.applyDynamicColors(applicationContext, lottieView)
        }
        lottieView.visibility = View.VISIBLE
        glifLayout.requireViewById<View>(R.id.icon_apply).visibility = View.VISIBLE
        glifLayout.requireViewById<View>(R.id.msg_apply).visibility = View.VISIBLE
        val glifLayoutUseCase = GlifLayoutUseCase(glifLayout)
        glifLayoutUseCase.setHeaderText(
            this,
            getString(R.string.sp_apply_title_product_combined, productInfo.modelName),
        )
        glifLayoutUseCase.setDescriptionText(getString(R.string.protector_edu_subtitle))
        val footerBarMixin = glifLayout.getMixin(FooterBarMixin::class.java)
        footerBarMixin.secondaryButton =
            FooterButton.Builder(this)
                .setText(android.R.string.cancel)
                .setButtonType(FooterButton.ButtonType.SKIP)
                .setTheme(com.google.android.setupdesign.R.style.SudGlifButton_Secondary)
                .build()
                .apply { setOnClickListener(onCancelClickListener) }
        footerBarMixin.primaryButton =
            FooterButton.Builder(this)
                .setText(R.string.sp_apply_yes_button)
                .setButtonType(FooterButton.ButtonType.OPT_IN)
                .setTheme(com.google.android.setupdesign.R.style.SudGlifButton_Primary)
                .build()
                .apply { setOnClickListener(onApplyClickListener) }
        lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onStart(owner: LifecycleOwner) {
                    lottiePlayPauseUseCase =
                        LottiePlayPauseUseCase(
                                requireViewById<LottieAnimationView>(R.id.illustration_lottie)
                            )
                            .apply { startAnimationAndSetupAccessibility() }
                }

                override fun onStop(owner: LifecycleOwner) {
                    lottiePlayPauseUseCase?.clearResources()
                    lottiePlayPauseUseCase = null
                }
            }
        )
    }

    override fun onApplyThemeResource(theme: Resources.Theme, resid: Int, first: Boolean) {
        theme.applyStyle(AR.style.SetupWizardPartnerResource, true)
        super.onApplyThemeResource(theme, resid, first)
    }

    override val defaultViewModelCreationExtras: CreationExtras
        get() =
            MutableCreationExtras(super.defaultViewModelCreationExtras).apply {
                set(UsudfpsViewModelFactory.SP_QR_CODE_KEY, SpQrCode(intent.data.toString()))
            }

    companion object {
        private const val TAG = "SpApplyActivity"
        private const val SETTINGS_PACKAGE = "com.android.settings"
    }
}
