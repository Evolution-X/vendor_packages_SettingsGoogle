package com.google.android.settings.biometrics.sfps.view

import android.hardware.biometrics.BiometricFingerprintConstants.FINGERPRINT_ERROR_CANCELED
import android.hardware.biometrics.BiometricFingerprintConstants.FINGERPRINT_ERROR_TIMEOUT
import android.hardware.biometrics.fingerprint.SensorLocationData
import android.os.Bundle
import android.view.Surface
import android.view.View
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import com.android.settings.R as AR
import com.android.settings.biometrics.BiometricsOnboardingProto.OnboardingAction
import com.android.settings.biometrics.BiometricsOnboardingProto.OnboardingScreen
import com.android.settings.biometrics.fingerprint2.lib.model.FingerEnrollState
import com.android.settingslib.widget.LottieColorUtils
import com.google.android.settings.R
import com.google.android.settings.biometrics.fingerprint.ui.view.GlifLayoutUseCase
import com.google.android.settings.biometrics.fingerprint.ui.view.NavOptionsUseCase
import com.google.android.settings.biometrics.fingerprint.ui.view.SkipFindFpsDialog
import com.google.android.settings.biometrics.fingerprint.ui.viewmodel.FingerprintEnrollResult
import com.google.android.settings.biometrics.fingerprint.ui.viewmodel.FingerprintMetricsViewModel
import com.google.android.settings.biometrics.fingerprint.ui.viewmodel.SetEnrollResultViewModel
import com.google.android.settings.biometrics.sfps.factory.SfpsViewModelFactory
import com.google.android.settings.biometrics.sfps.model.EnrollOperationModel
import com.google.android.settings.biometrics.sfps.viewmodel.FindSfpsViewModel
import com.google.android.settings.biometrics.sfps.widget.FingerprintErrorDialog
import com.google.android.setupcompat.template.FooterBarMixin
import com.google.android.setupcompat.template.FooterButton
import com.google.android.setupdesign.GlifLayout
import com.google.android.setupdesign.util.LottieAnimationHelper
import com.google.android.setupdesign.util.ThemeHelper
import kotlinx.coroutines.launch

class FindSfpsFragment : Fragment(R.layout.sfps_education) {

    private val viewModel: FindSfpsViewModel by
        viewModels(extrasProducer = { requireActivity().defaultViewModelCreationExtras }) {
            defaultViewModelProviderFactory
        }

    private val useExpressStyle: Boolean by lazy {
        ThemeHelper.shouldApplyGlifExpressiveStyle(requireContext())
    }

    private val setEnrollResultViewModel: SetEnrollResultViewModel by activityViewModels()
    private val metricsViewModel: FingerprintMetricsViewModel by activityViewModels()

    private val skipClickListener = View.OnClickListener {
        lifecycleScope.launch {
            if (!viewModel.isSuw) {
                viewModel.finishEnrollment()
                setEnrollResultViewModel.emit(FingerprintEnrollResult.FIND_SENSOR_SKIP_BUTTON)
            } else {
                SkipFindFpsDialog.showDialog(childFragmentManager)
            }
        }
    }

    private val pauseDelegate = actionDelegate(AR.string.pause_animation)

    private val resumeDelegate = actionDelegate(AR.string.resume_animation)

    private fun actionDelegate(labelResId: Int) =
        object : AccessibilityDelegateCompat() {
            override fun onInitializeAccessibilityNodeInfo(
                host: View,
                info: AccessibilityNodeInfoCompat,
            ) {
                super.onInitializeAccessibilityNodeInfo(host, info)
                info.className = null
                info.addAction(
                    AccessibilityActionCompat(
                        AccessibilityNodeInfoCompat.ACTION_CLICK,
                        view?.context?.getString(labelResId),
                    )
                )
            }
        }

    // Stock gates the sensor-location based illustrations behind a flag that is compiled out
    // (always false) in the reference build.
    private fun isDesktopFingerprintSensorLocationEnabled(): Boolean = false

    override val defaultViewModelProviderFactory: ViewModelProvider.Factory
        get() = SfpsViewModelFactory()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.onConfigurationChanged(resources.configuration)
        viewModel.onUpdateOrientation(requireContext().display.rotation)
        val glifLayout = view.requireViewById<GlifLayout>(R.id.setup_wizard_layout)
        val glifLayoutUseCase = GlifLayoutUseCase(glifLayout)
        glifLayoutUseCase.setHeaderText(
            requireActivity(),
            AR.string.security_settings_sfps_enroll_find_sensor_title,
        )
        glifLayoutUseCase.setDescriptionText(
            getString(R.string.fingerprint_sfps_enroll_find_sensor_description)
        )
        setupSecondaryButton(glifLayout.getMixin(FooterBarMixin::class.java))
        viewModel.readyForEnrollment()
        viewLifecycleOwner.lifecycleScope.launch {
            setFragmentResultListener(FingerprintErrorDialog.RESULT_LISTENER) { _, bundle ->
                val errorMsgId = bundle.getInt(FingerprintErrorDialog.KEY_MESSAGE_ID)
                if (!bundle.getBoolean(FingerprintErrorDialog.KEY_WAS_BACK_PRESSED)) {
                    metricsViewModel.setErrorCode(errorMsgId)
                    val result =
                        if (errorMsgId == FINGERPRINT_ERROR_TIMEOUT) {
                            FingerprintEnrollResult.FIND_SENSOR_ERROR_TIMEOUT
                        } else {
                            FingerprintEnrollResult.FIND_SENSOR_ERROR_FINISH
                        }
                    viewLifecycleOwner.lifecycleScope.launch {
                        setEnrollResultViewModel.emit(result)
                    }
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            launch {
                viewModel.enrollState.collect { state ->
                    if (state is FingerEnrollState.EnrollProgress) {
                        proceedToEnrolling()
                    } else if (state is FingerEnrollState.EnrollError) {
                        val operationState = viewModel.enrollOperationState.value
                        if (
                            state.errorId == FINGERPRINT_ERROR_CANCELED &&
                                lifecycle.currentState == Lifecycle.State.RESUMED &&
                                operationState == EnrollOperationModel.Running
                        ) {
                            view.post {
                                viewModel.finishEnrollment()
                                viewModel.readyForEnrollment()
                            }
                        } else {
                            showErrorDialog(state)
                        }
                    }
                }
            }
            launch {
                viewModel.sfpsLottieInfo.collect { (isFolded, rotation) ->
                    setupLottie(view, getSfpsIllustrationLottieAnimation(view, isFolded, rotation))
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        metricsViewModel.setScreen(OnboardingScreen.SCREEN_EDUCATION)
    }

    override fun onResume() {
        super.onResume()
        if (requireActivity().isFinishing) {
            return
        }
        viewModel.readyForEnrollment()
    }

    override fun onStop() {
        super.onStop()
        if (requireActivity().isChangingConfigurations) {
            return
        }
        viewModel.finishEnrollment()
    }

    private fun proceedToEnrolling() {
        viewModel.finishEnrollment()
        metricsViewModel.appendAction(OnboardingAction.ACTION_NEXT)
        findNavController()
            .navigate(R.id.action_find_sensor_to_enroll, null, NavOptionsUseCase.newNavOptions())
    }

    private fun setupSecondaryButton(footerBarMixin: FooterBarMixin) {
        footerBarMixin.secondaryButton =
            FooterButton.Builder(requireActivity())
                .setText(AR.string.security_settings_fingerprint_enroll_enrolling_skip)
                .setButtonType(FooterButton.ButtonType.SKIP)
                .setTheme(com.google.android.setupdesign.R.style.SudGlifButton_Secondary)
                .build()
                .apply { setOnClickListener(skipClickListener) }
    }

    private suspend fun getSfpsIllustrationLottieAnimation(
        view: View,
        isFolded: Boolean,
        rotation: Int,
    ): Int {
        if (isDesktopFingerprintSensorLocationEnabled()) {
            val sensorLocationData =
                viewModel.getSensorLocation().sensorLocationData
                    ?: return AR.raw.fingerprint_edu_lottie
            return when (sensorLocationData.tag) {
                SensorLocationData.Tag.powerButtonDisplayLocation -> {
                    val location = sensorLocationData.powerButtonDisplayLocation
                    if (location.sensorLocationXPixels == 0) {
                        AR.raw.fingerprint_edu_left_side_lottie
                    } else if (location.sensorLocationYPixels == 0) {
                        AR.raw.fingerprint_edu_power_button_top_left_lottie
                    } else {
                        AR.raw.fingerprint_edu_lottie
                    }
                }
                SensorLocationData.Tag.powerButtonPhysicalLocation ->
                    AR.raw.fingerprint_edu_keyboard_top_right_lottie
                else -> AR.raw.fingerprint_edu_lottie
            }
        }
        if (useExpressStyle) {
            val colors =
                requireContext()
                    .resources
                    .getStringArray(R.array.sfps_education_illustration)
                    .toList()
            LottieAnimationHelper.get()
                .applyColor(
                    requireContext(),
                    view.requireViewById<LottieAnimationView>(R.id.illustration_lottie),
                    colors,
                )
            return R.raw.sfps_edu_lottie_expression
        }
        return when (rotation) {
            Surface.ROTATION_90 ->
                if (isFolded) AR.raw.fingerprint_edu_lottie_folded_top_left
                else AR.raw.fingerprint_edu_lottie_portrait_top_left
            Surface.ROTATION_180 ->
                if (isFolded) AR.raw.fingerprint_edu_lottie_folded_bottom_left
                else AR.raw.fingerprint_edu_lottie_landscape_bottom_left
            Surface.ROTATION_270 ->
                if (isFolded) AR.raw.fingerprint_edu_lottie_folded_bottom_right
                else AR.raw.fingerprint_edu_lottie_portrait_bottom_right
            else ->
                if (isFolded) AR.raw.fingerprint_edu_lottie_folded_top_right
                else AR.raw.fingerprint_edu_lottie_landscape_top_right
        }
    }

    private fun setupLottie(view: View, lottieResId: Int) {
        val lottieView = view.requireViewById<LottieAnimationView>(R.id.illustration_lottie)
        lottieView.setAnimation(lottieResId)
        lottieView.playAnimation()
        lottieView.visibility = View.VISIBLE
        lottieView.setOnClickListener { viewModel.didTouchLottie() }
        lottieView.repeatCount = LottieDrawable.INFINITE
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.shouldAnimateLottie.collect { shouldAnimate ->
                if (shouldAnimate) {
                    lottieView.resumeAnimation()
                    ViewCompat.setAccessibilityDelegate(lottieView, pauseDelegate)
                } else {
                    lottieView.pauseAnimation()
                    ViewCompat.setAccessibilityDelegate(lottieView, resumeDelegate)
                }
            }
        }
        if (isDesktopFingerprintSensorLocationEnabled()) {
            LottieColorUtils.applyIlloColors(requireContext(), lottieView)
        } else {
            LottieColorUtils.applyDynamicColors(requireContext(), lottieView)
        }
    }

    private fun showErrorDialog(state: FingerEnrollState.EnrollError) {
        viewLifecycleOwner.lifecycleScope.launch {
            val dialog = FingerprintErrorDialog.newInstance(state.errorId, viewModel.isSuw)
            if (parentFragmentManager.findFragmentByTag(ERROR_DIALOG_TAG) == null) {
                dialog.show(parentFragmentManager, ERROR_DIALOG_TAG)
            }
        }
    }

    companion object {
        private const val ERROR_DIALOG_TAG = "SfpsEduErrorDialog"
    }
}
