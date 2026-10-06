package com.google.android.settings.biometrics.combination.ui.view

import android.content.Intent
import android.os.Bundle
import android.text.Html
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.android.settings.biometrics.BiometricUtils
import com.android.settings.biometrics.GatekeeperPasswordProvider
import com.android.settings.password.ChooseLockSettingsHelper
import com.google.android.settings.R
import com.google.android.settings.biometrics.combination.ui.model.GkResult
import com.google.android.settings.biometrics.combination.ui.viewmodel.FrrCommonNotificationViewModel
import com.google.android.settings.biometrics.fingerprint.factory.FingerprintViewModelFactory
import com.google.android.settings.biometrics.fingerprint.model.FingerprintRemoval
import com.google.android.settings.biometrics.fingerprint.ui.view.GlifLayoutUseCase
import com.google.android.settings.biometrics.fingerprint.ui.view.ThemeUseCase
import com.google.android.setupcompat.template.FooterBarMixin
import com.google.android.setupcompat.template.FooterButton
import com.google.android.setupdesign.GlifLayout
import com.google.android.setupdesign.util.ThemeHelper
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class FrrCommonNotificationActivity : FragmentActivity() {

    private val viewModel: FrrCommonNotificationViewModel by viewModels()

    private val faceActivityLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { activityResult
            ->
            Log.d(TAG, "Face enrollment finished. Result:" + activityResult.resultCode)
            viewModel.isLaunchingActivity = false
            setClickable(true)
        }

    private val fingerprintActivityLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { activityResult
            ->
            Log.d(TAG, "Fingerprint enrollment finish. Result:" + activityResult.resultCode)
            viewModel.revokeChallenge()
            viewModel.isLaunchingActivity = false
            setClickable(true)
        }

    private val onFaceClickListener = View.OnClickListener {
        Log.d(TAG, "onFaceClickListener")
        viewModel.isLaunchingActivity = true
        val faceIntroIntent = BiometricUtils.getFaceIntroIntent(this, Intent())
        faceActivityLauncher.launch(faceIntroIntent)
    }

    private val onFingerprintClickListener = View.OnClickListener {
        Log.d(TAG, "onFingerprintClickListener")
        FrrDeleteAllFpsDialogFragment()
            .show(supportFragmentManager, "FrrDeleteAllFpsDialogFragment")
    }

    private val onPrimaryClickListener = View.OnClickListener {
        if (isClickable()) {
            Log.d(TAG, "Primary button to dismiss")
            finishAndRemoveTask()
        }
    }

    private var mIsClickable = true

    private val useExpressiveUi: Boolean by lazy {
        ThemeHelper.shouldApplyGlifExpressiveStyle(this)
    }

    fun isClickable(): Boolean = mIsClickable

    fun setClickable(clickable: Boolean) {
        Log.d(TAG, "setIsClickable($clickable)")
        requireViewById<TextView>(R.id.button_fingerprint)
            .setOnClickListener(if (clickable) onFingerprintClickListener else null)
        val facesGroup = requireViewById<View>(R.id.faces) as android.view.ViewGroup
        if (facesGroup.visibility == View.VISIBLE) {
            facesGroup
                .requireViewById<TextView>(R.id.button_face)
                .setOnClickListener(if (clickable) onFaceClickListener else null)
        }
        mIsClickable = clickable
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(
            if (useExpressiveUi) {
                R.layout.frr_common_notification_expressive
            } else {
                R.layout.frr_common_notification
            }
        )
        ThemeUseCase(this).applyTheme()
        val glifLayout = requireViewById<GlifLayout>(R.id.setup_wizard_layout)
        val glifLayoutUseCase = GlifLayoutUseCase(glifLayout)
        glifLayoutUseCase.setHeaderText(this, R.string.frr_notification_title)
        glifLayoutUseCase.setDescriptionText(getString(R.string.frr_notification_description))
        val footerBarMixin = glifLayout.getMixin(FooterBarMixin::class.java) as FooterBarMixin
        val footerButton =
            FooterButton.Builder(this)
                .setText(R.string.frr_notification_button_got_it)
                .setButtonType(FooterButton.ButtonType.OPT_IN)
                .setTheme(com.google.android.setupdesign.R.style.SudGlifButton_Primary)
                .build()
        footerButton.setOnClickListener(onPrimaryClickListener)
        footerBarMixin.primaryButton = footerButton
        if (savedInstanceState != null) {
            initViewModelAsSavedInstanceState(savedInstanceState)
            recoverySavedInstanceActions()
        }
        supportFragmentManager.setFragmentResultListener("FrrDeleteAllFpsDialogFragment", this) {
            _,
            bundle ->
            if (bundle.getBoolean("result_confirmed_delete_all")) {
                viewModel.isLaunchingActivity = true
                if (!getConfirmLockLauncher().launch()) {
                    Log.e(TAG, "confirmLock, launched is true")
                    finishAndRemoveTask()
                }
            }
        }
        lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onStart(owner: LifecycleOwner) {
                    if (!viewModel.isAllowFrrActivity()) {
                        Log.d(TAG, "unspecified password, close activity")
                        finishAndRemoveTask()
                        return
                    }
                    updateFaceViews()
                    updateSpViews()
                    updateFingerprintViews()
                    if (useExpressiveUi) {
                        val frrEntries = requireViewById<LinearLayout>(R.id.frr_entries)
                        FrrActivityUseCase().updateExpressViews(frrEntries)
                    }
                    Log.d(
                        TAG,
                        "onStart() - LA:" +
                            viewModel.isLaunchingActivity +
                            ", RAF:" +
                            viewModel.isRemovingAllFingerprints +
                            ", PwIsNot0L:" +
                            (viewModel.gkPwHandle != 0L) +
                            ", ResultIsNotNull:" +
                            (viewModel.gkOkResult != null),
                    )
                    setClickable(viewModel.isButtonsClickableWhenActivityForeground())
                }

                override fun onStop(owner: LifecycleOwner) {
                    setClickable(false)
                }
            }
        )
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("isLaunchingActivity", viewModel.isLaunchingActivity)
        outState.putBoolean("isRemovingFingerprints", viewModel.isRemovingAllFingerprints)
        outState.putLong("gkPwHandle", viewModel.gkPwHandle)
        viewModel.gkOkResult?.let { gkOkResult ->
            outState.putByteArray("token", gkOkResult.token)
            outState.putLong("challenge", gkOkResult.challenge)
        }
    }

    private fun initViewModelAsSavedInstanceState(bundle: Bundle) {
        viewModel.isLaunchingActivity =
            bundle.getBoolean("isLaunchingActivity", viewModel.isLaunchingActivity)
        viewModel.isRemovingAllFingerprints =
            bundle.getBoolean("isRemovingFingerprints", viewModel.isRemovingAllFingerprints)
        viewModel.gkPwHandle = bundle.getLong("gkPwHandle", viewModel.gkPwHandle)
        if (bundle.containsKey("challenge") && bundle.containsKey("token")) {
            val challenge = bundle.getLong("challenge", 0L)
            val token = bundle.getByteArray("token") ?: ByteArray(0)
            viewModel.gkOkResult = GkResult.Ok(challenge, token)
        }
    }

    private fun recoverySavedInstanceActions() {
        lifecycleScope.launch {
            if (viewModel.isRemovingAllFingerprints) {
                Log.d(TAG, "RemovingAllFingerprints - Restart removing all fingerprints")
                removeAllFingerprints()
            } else if (viewModel.gkPwHandle != 0L) {
                Log.d(TAG, "RemovingAllFingerprints - Restart generating challenge and token")
                generateChallengeAndToken()
            }
        }
    }

    private fun updateFaceViews() {
        val facesGroup = requireViewById<View>(R.id.faces) as android.view.ViewGroup
        if (!viewModel.shouldShowFaceUnlockViews) {
            facesGroup.visibility = View.GONE
            return
        }
        facesGroup.visibility = View.VISIBLE
        facesGroup
            .requireViewById<TextView>(R.id.button_face)
            .setOnClickListener(onFaceClickListener)
    }

    private fun updateSpViews() {
        val spsGroup = requireViewById<View>(R.id.sps) as android.view.ViewGroup
        if (viewModel.shouldShowSpViews) {
            spsGroup.visibility = View.VISIBLE
            val spTitle = spsGroup.requireViewById<TextView>(R.id.title_sp)
            spTitle.setText(
                if (viewModel.shouldShowNonMfgSpText) {
                    R.string.frr_notification_non_mfg_sp_title
                } else {
                    R.string.frr_notification_screen_protector_title
                }
            )
            val spMsg = spsGroup.requireViewById<TextView>(R.id.msg_sp)
            spMsg.setText(
                if (viewModel.shouldShowNonMfgSpText) {
                    R.string.frr_notification_non_mfg_sp_description
                } else {
                    R.string.frr_notification_screen_protector_description
                }
            )
            spsGroup.requireViewById<TextView>(R.id.button_sp).text =
                Html.fromHtml(getString(R.string.frr_notification_learn_more), 0)
            return
        }
        spsGroup.visibility = View.GONE
    }

    private fun updateFingerprintViews() {
        requireViewById<TextView>(R.id.button_fingerprint)
            .setOnClickListener(onFingerprintClickListener)
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
        caller: android.app.ComponentCaller,
    ) {
        if (requestCode == REQUEST_CONFIRM_LOCK) {
            if (resultCode == RESULT_OK && containsGkPwHandle(data)) {
                Log.d(TAG, "RemovingAllFingerprints - Got credential")
                lifecycleScope.launch {
                    viewModel.gkPwHandle = getGkPwHandle(data)
                    generateChallengeAndToken()
                }
                return
            }
            setClickable(true)
            return
        }
        super.onActivityResult(requestCode, resultCode, data, caller)
    }

    private suspend fun generateChallengeAndToken() {
        viewModel.fetchGkHat().collect { gkResult ->
            when (gkResult) {
                is GkResult.Failed -> setClickable(true)
                is GkResult.Ok -> {
                    viewModel.gkOkResult = gkResult
                    Log.d(TAG, "RemovingAllFingerprints - Got token")
                    removeAllFingerprints()
                }
            }
            viewModel.gkPwHandle = 0L
        }
    }

    private suspend fun removeAllFingerprints() {
        if (!viewModel.hasFingerprints) {
            startFingerprint()
            return
        }
        viewModel.isRemovingAllFingerprints = true
        viewModel.removeAllFingerprints().collect { fingerprintRemoval ->
            when (fingerprintRemoval) {
                is FingerprintRemoval.Error -> {
                    Log.d(
                        TAG,
                        "RemovingAllFingerprints - Remove failed: " + fingerprintRemoval.errString,
                    )
                    setClickable(true)
                    viewModel.isRemovingAllFingerprints = false
                }
                is FingerprintRemoval.Succeeded -> {
                    Log.d(
                        TAG,
                        "RemovingAllFingerprints - Remove " +
                            fingerprintRemoval.fp +
                            ", remaining: " +
                            fingerprintRemoval.remaining,
                    )
                    if (fingerprintRemoval.remaining == 0) {
                        viewModel.isRemovingAllFingerprints = false
                        startFingerprint()
                    }
                }
            }
        }
    }

    private fun startFingerprint() {
        val fingerprintIntent =
            FrrActivityUseCase().getFingerprintIntent(viewModel.gkOkResult, this)
        Log.d(TAG, "Start enrollment - hasGkOkResult:" + (viewModel.gkOkResult != null))
        fingerprintActivityLauncher.launch(fingerprintIntent)
        viewModel.isLaunchingActivity = true
    }

    private fun getConfirmLockLauncher(): ChooseLockSettingsHelper =
        FrrActivityUseCase().getConfirmLockLauncher(viewModel.userId, this, REQUEST_CONFIRM_LOCK)

    private fun containsGkPwHandle(intent: Intent?): Boolean =
        GatekeeperPasswordProvider.containsGatekeeperPasswordHandle(intent ?: Intent())

    private fun getGkPwHandle(intent: Intent?): Long =
        GatekeeperPasswordProvider.getGatekeeperPasswordHandle(intent ?: Intent())

    override val defaultViewModelProviderFactory: androidx.lifecycle.ViewModelProvider.Factory
        get() = FingerprintViewModelFactory()

    companion object {
        private const val TAG = "FrrCommonNotificationActivity"
        private const val REQUEST_CONFIRM_LOCK = 1
    }
}
