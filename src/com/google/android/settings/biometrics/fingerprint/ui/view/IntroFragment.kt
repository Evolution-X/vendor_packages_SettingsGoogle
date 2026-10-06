package com.google.android.settings.biometrics.fingerprint.ui.view

import android.app.admin.DevicePolicyManager
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.hardware.fingerprint.FingerprintSensorProperties
import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.android.settings.R as AR
import com.android.settings.biometrics.BiometricsOnboardingProto.OnboardingAction
import com.android.settings.biometrics.BiometricsOnboardingProto.OnboardingScreen
import com.google.android.settings.R
import com.google.android.settings.biometrics.fingerprint.ui.viewmodel.FingerprintEnrollIntroUiState
import com.google.android.settings.biometrics.fingerprint.ui.viewmodel.FingerprintEnrollResult
import com.google.android.settings.biometrics.fingerprint.ui.viewmodel.FingerprintMetricsViewModel
import com.google.android.settings.biometrics.fingerprint.ui.viewmodel.IntroViewModel
import com.google.android.settings.biometrics.fingerprint.ui.viewmodel.SetEnrollResultViewModel
import com.google.android.setupcompat.template.FooterBarMixin
import com.google.android.setupcompat.template.FooterButton
import com.google.android.setupdesign.GlifLayout
import com.google.android.setupdesign.template.RequireScrollMixin
import com.google.android.setupdesign.util.DeviceHelper
import com.google.android.setupdesign.util.ThemeHelper
import java.util.function.Supplier
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

open class IntroFragment : Fragment(AR.layout.fingerprint_enroll_introduction) {

    private val viewModel: IntroViewModel by activityViewModels()
    private val setEnrollResultViewModel: SetEnrollResultViewModel by activityViewModels()
    private val metricsViewModel: FingerprintMetricsViewModel by activityViewModels()

    private val onNextClickListener = View.OnClickListener {
        lifecycleScope.launch {
            if (viewModel.uiState.first().enrollable) {
                if (showSplitScreenDialogIfNeed()) {
                    return@launch
                }
                when (viewModel.getSensorType()) {
                    FingerprintSensorProperties.TYPE_UDFPS_ULTRASONIC,
                    FingerprintSensorProperties.TYPE_UDFPS_OPTICAL,
                    FingerprintSensorProperties.TYPE_POWER_BUTTON -> {
                        metricsViewModel.appendAction(OnboardingAction.ACTION_NEXT)
                        findNavController()
                            .navigate(
                                R.id.action_intro_to_find_sensor,
                                null,
                                NavOptionsUseCase.newNavOptions(),
                            )
                    }
                    else ->
                        setEnrollResultViewModel.emit(
                            FingerprintEnrollResult.INTRO_FRAGMENT_CONTINUE_ENROLL
                        )
                }
            } else {
                setEnrollResultViewModel.emit(
                    FingerprintEnrollResult.INTRO_FRAGMENT_DONE_AND_FINISH_BUTTON
                )
            }
        }
    }

    private val onSkipOrCancelClickListener = View.OnClickListener {
        lifecycleScope.launch {
            setEnrollResultViewModel.emit(
                FingerprintEnrollResult.INTRO_FRAGMENT_SKIP_OR_CANCEL_BUTTON
            )
        }
    }

    private val footerBarMixin: FooterBarMixin
        get() = (requireView() as GlifLayout).getMixin(FooterBarMixin::class.java)

    private val requireScrollMixin: RequireScrollMixin
        get() = (requireView() as GlifLayout).getMixin(RequireScrollMixin::class.java)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fingerprint_enroll_introduction_2, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val glifLayout = view as GlifLayout

        lifecycleScope.launch {
            val learnMoreText =
                requireContext()
                    .getString(
                        R.string
                            .security_settings_fingerprint_v2_enroll_introduction_message_learn_more_2,
                        0,
                    )
            val colorFilter =
                PorterDuffColorFilter(
                    requireContext()
                        .getColor(
                            com.android.settingslib.widget.theme.R.color
                                .settingslib_materialColorOnSurfaceVariant
                        ),
                    PorterDuff.Mode.SRC_IN,
                )
            val isUdfps =
                viewModel.getSensorType() == FingerprintSensorProperties.TYPE_UDFPS_OPTICAL ||
                    viewModel.getSensorType() == FingerprintSensorProperties.TYPE_UDFPS_ULTRASONIC

            bindView(
                requireActivity(),
                glifLayout,
                learnMoreText,
                colorFilter,
                isUdfps,
                viewModel.isFingerprintUnlockDisabledByAdmin,
                viewModel.isParentalConsentRequired,
                {
                    requireContext()
                        .getSystemService(DevicePolicyManager::class.java)!!
                        .resources
                        .getString("Settings.FINGERPRINT_UNLOCK_DISABLED") {
                            requireContext()
                                .getString(
                                    AR.string
                                        .security_settings_fingerprint_enroll_introduction_message_unlock_disabled
                                )
                        }
                },
                { DeviceHelper.getDeviceName(requireContext()) },
            )

            if (ThemeHelper.shouldApplyGlifExpressiveStyle(requireContext())) {
                view
                    .findViewById<ImageView>(AR.id.illustrationImage)!!
                    .setImageResource(R.drawable.fingerprint_enroll_introduction_expressive)
            }
        }

        viewLifecycleOwner.lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onStart(owner: LifecycleOwner) {
                    metricsViewModel.setScreen(OnboardingScreen.SCREEN_INTRO)
                    initPrimaryFooterButton()
                    initSecondaryFooterButton()
                    collectPageStatusFlowIfNeed()
                    showSplitScreenDialogIfNeed()
                }
            }
        )
    }

    private fun initPrimaryFooterButton() {
        if (footerBarMixin.primaryButton != null) return
        val button =
            FooterButton.Builder(requireContext())
                .setText(AR.string.security_settings_fingerprint_enroll_introduction_agree)
                .setButtonType(FooterButton.ButtonType.OPT_IN)
                .setTheme(com.google.android.setupdesign.R.style.SudGlifButton_Primary)
                .build()
        button.setOnClickListener(onNextClickListener)
        footerBarMixin.primaryButton = button
    }

    private fun initSecondaryFooterButton() {
        if (footerBarMixin.secondaryButton != null) return
        val button =
            FooterButton.Builder(requireContext())
                .setText(AR.string.security_settings_fingerprint_enroll_introduction_no_thanks)
                .setButtonType(FooterButton.ButtonType.NEXT)
                .setTheme(com.google.android.setupdesign.R.style.SudGlifButton_Primary)
                .build()
        button.setOnClickListener(onSkipOrCancelClickListener)
        footerBarMixin.setSecondaryButton(button, true)
    }

    private fun collectPageStatusFlowIfNeed() {
        lifecycleScope.launch {
            var hasRequireScrollWithButton = requireScrollMixin.isScrollingRequired
            viewModel.uiState.collect { uiState ->
                val expressive = ThemeHelper.shouldApplyGlifExpressiveStyle(requireContext())
                Log.d(TAG, "collectPageStatusFlowIfNeed uiState:$uiState")
                if (!hasRequireScrollWithButton && !uiState.hasScrolledToBottom) {
                    requireScrollMixin.requireScrollWithButton(
                        requireActivity(),
                        footerBarMixin.primaryButton!!,
                        footerBarMixin.secondaryButton!!,
                        getMoreButtonTextRes(),
                        onNextClickListener,
                    )
                    if (!expressive) {
                        requireScrollMixin.setOnRequireScrollStateChangedListener { scrollNeeded ->
                            if (!scrollNeeded) {
                                lifecycleScope.launch { viewModel.onScrollToBottom() }
                            }
                            updateFooterButtons(uiState, false)
                        }
                    }
                    hasRequireScrollWithButton = true
                }
                updateFooterButtons(uiState, expressive)
            }
        }
    }

    private fun updateFooterButtons(uiState: FingerprintEnrollIntroUiState, isExpressive: Boolean) {
        val showSecondary =
            (uiState.enrollable && uiState.hasScrolledToBottom) ||
                !requireScrollMixin.isScrollingRequired
        Log.d(
            TAG,
            "updateFooterButtons($uiState), showSecondaryBtn:$showSecondary, isExpressive:$isExpressive",
        )
        val errorText = requireView().requireViewById<TextView>(AR.id.error_text)
        if (uiState.enrollable) {
            errorText.text = null
            errorText.visibility = View.GONE
        } else {
            errorText.setText(AR.string.fingerprint_intro_error_max)
            errorText.visibility = View.VISIBLE
        }
        if (isExpressive) return

        footerBarMixin.primaryButton?.let { primary ->
            val textRes =
                when {
                    !uiState.enrollable -> AR.string.done
                    showSecondary ->
                        AR.string.security_settings_fingerprint_enroll_introduction_agree
                    else -> getMoreButtonTextRes()
                }
            primary.setText(context, textRes)
        }
        footerBarMixin.secondaryButton?.visibility =
            if (showSecondary) View.VISIBLE else View.INVISIBLE
    }

    private fun getMoreButtonTextRes(): Int {
        return AR.string.security_settings_face_enroll_introduction_more
    }

    private fun showSplitScreenDialogIfNeed(): Boolean {
        val activity = activity ?: return false
        val companion = SplitScreenDialog.Companion
        val childFm = childFragmentManager
        companion.dismissExistingDialog(childFm)
        if (!companion.shouldShowDialog(activity)) {
            return false
        }
        companion.showDialog(childFm)
        if (viewModel.request.isSuw) {
            return true
        }
        childFragmentManager.registerFragmentLifecycleCallbacks(
            object : FragmentManager.FragmentLifecycleCallbacks() {
                override fun onFragmentDetached(fm: FragmentManager, f: Fragment) {
                    if (f is SplitScreenDialog) {
                        lifecycleScope.launch {
                            setEnrollResultViewModel.emit(
                                FingerprintEnrollResult.SPLIT_DIALOG_DISMISS
                            )
                        }
                    }
                }
            },
            false,
        )
        return true
    }

    companion object {
        private const val TAG = "IntroFragment"

        fun bindView(
            activity: FragmentActivity,
            glifLayout: GlifLayout,
            learnMoreText: String,
            colorFilter: PorterDuffColorFilter,
            isUdfps: Boolean,
            disabledByAdmin: Boolean,
            parentalConsentRequired: Boolean,
            unlockDisabledMessage: Supplier<String?>,
            deviceName: Supplier<CharSequence>,
        ) {
            val learnMoreView = glifLayout.requireViewById<TextView>(AR.id.footer_learn_more)
            learnMoreView.movementMethod = LinkMovementMethod.getInstance()
            learnMoreView.text = Html.fromHtml(learnMoreText, 0)

            glifLayout.requireViewById<ImageView>(AR.id.icon_fingerprint).colorFilter = colorFilter
            glifLayout.requireViewById<ImageView>(AR.id.icon_device_locked).colorFilter =
                colorFilter
            glifLayout.requireViewById<ImageView>(AR.id.icon_trash_can).colorFilter = colorFilter
            glifLayout.requireViewById<ImageView>(AR.id.icon_info).colorFilter = colorFilter
            glifLayout.requireViewById<ImageView>(AR.id.icon_link).colorFilter = colorFilter
            glifLayout
                .requireViewById<View>(com.google.android.setupdesign.R.id.sud_scroll_view)
                .importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
            glifLayout.requireViewById<ImageView>(R.id.icon_security_privacy_safe).colorFilter =
                colorFilter
            glifLayout.requireViewById<ImageView>(R.id.icon_privacy_tip).colorFilter = colorFilter

            val footer6 = glifLayout.requireViewById<TextView>(AR.id.footer_message_6)
            val iconShield = glifLayout.requireViewById<ImageView>(AR.id.icon_shield)
            iconShield.colorFilter = colorFilter
            footer6.text =
                activity.getString(
                    R.string.security_settings_fingerprint_v2_enroll_introduction_footer_message_6_2
                )
            if (isUdfps) {
                footer6.visibility = View.VISIBLE
                iconShield.visibility = View.VISIBLE
            } else {
                footer6.visibility = View.GONE
                iconShield.visibility = View.GONE
            }

            val glifLayoutUseCase = GlifLayoutUseCase(glifLayout)
            if (disabledByAdmin && !parentalConsentRequired) {
                glifLayoutUseCase.setHeaderText(
                    activity,
                    AR.string
                        .security_settings_fingerprint_enroll_introduction_title_unlock_disabled,
                )
                glifLayoutUseCase.setDescriptionText(unlockDisabledMessage.get())
            } else {
                glifLayoutUseCase.setHeaderText(
                    activity,
                    AR.string.security_settings_fingerprint_enroll_introduction_title,
                )
                glifLayoutUseCase.setDescriptionText(
                    activity.getString(
                        R.string.security_settings_fingerprint_enroll_introduction_v3_message_2,
                        deviceName.get(),
                    )
                )
            }
        }
    }
}
