package com.google.android.settings.biometrics.udfps.ui.view

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.airbnb.lottie.LottieAnimationView
import com.android.settings.R as AR
import com.android.settings.biometrics.BiometricsOnboardingProto.OnboardingAction
import com.android.settings.biometrics.BiometricsOnboardingProto.OnboardingScreen
import com.google.android.settings.R
import com.google.android.settings.biometrics.fingerprint.ui.view.GlifLayoutUseCase
import com.google.android.settings.biometrics.fingerprint.ui.view.NavOptionsUseCase
import com.google.android.settings.biometrics.fingerprint.ui.view.SkipFindFpsDialog
import com.google.android.settings.biometrics.fingerprint.ui.viewmodel.FingerprintEnrollResult
import com.google.android.settings.biometrics.fingerprint.ui.viewmodel.FingerprintMetricsViewModel
import com.google.android.settings.biometrics.fingerprint.ui.viewmodel.SetEnrollResultViewModel
import com.google.android.settings.biometrics.udfps.factory.UdfpsViewModelFactory
import com.google.android.settings.biometrics.udfps.ui.viewmodel.FindUdfpsViewModel
import com.google.android.setupcompat.template.FooterBarMixin
import com.google.android.setupcompat.template.FooterButton
import com.google.android.setupdesign.GlifLayout
import com.google.android.setupdesign.util.LottieAnimationHelper
import com.google.android.setupdesign.util.ThemeHelper
import kotlinx.coroutines.launch

class FindUdfpsFragment : Fragment(R.layout.find_udfps) {

    private val viewModel: FindUdfpsViewModel by
        viewModels(extrasProducer = { requireActivity().defaultViewModelCreationExtras }) {
            defaultViewModelProviderFactory
        }
    private val setEnrollResultViewModel: SetEnrollResultViewModel by activityViewModels()
    private val metricsViewModel: FingerprintMetricsViewModel by activityViewModels()

    private val useExpressStyle: Boolean by lazy {
        ThemeHelper.shouldApplyGlifExpressiveStyle(requireContext())
    }

    private val onSkipClickListener = View.OnClickListener {
        if (!viewModel.isSuw) {
            lifecycleScope.launch {
                setEnrollResultViewModel.emit(FingerprintEnrollResult.FIND_SENSOR_SKIP_BUTTON)
            }
        } else {
            SkipFindFpsDialog.showDialog(childFragmentManager)
        }
    }

    private val onNextClickListener = View.OnClickListener {
        metricsViewModel.appendAction(OnboardingAction.ACTION_NEXT)
        findNavController()
            .navigate(R.id.action_find_sensor_to_enroll, null, NavOptionsUseCase.newNavOptions())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (useExpressStyle) {
            val lottieView = view.requireViewById<LottieAnimationView>(R.id.illustration_lottie)
            lottieView.setAnimation(R.raw.fingerprint_udfps_edu_lottie_expressive)
            val colors =
                requireContext()
                    .resources
                    .getStringArray(R.array.fingerprint_udfps_education_illustration)
                    .toList()
            LottieAnimationHelper.get().applyColor(requireContext(), lottieView, colors)
        }
        bindView(requireActivity(), view as GlifLayout, onNextClickListener, onSkipClickListener)
        viewLifecycleOwner.lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onStart(owner: LifecycleOwner) {
                    metricsViewModel.setScreen(OnboardingScreen.SCREEN_EDUCATION)
                }
            }
        )
    }

    override val defaultViewModelProviderFactory: ViewModelProvider.Factory
        get() = UdfpsViewModelFactory()

    companion object {
        fun bindView(
            activity: FragmentActivity,
            glifLayout: GlifLayout,
            onNextClickListener: View.OnClickListener,
            onSkipClickListener: View.OnClickListener,
        ) {
            val glifLayoutUseCase = GlifLayoutUseCase(glifLayout)
            glifLayoutUseCase.setHeaderText(
                activity,
                AR.string.security_settings_udfps_enroll_find_sensor_title,
            )
            glifLayoutUseCase.setDescriptionText(
                glifLayout.context.getText(
                    R.string.fingerprint_udfps_enroll_find_sensor_description
                )
            )
            val footerBarMixin = glifLayout.getMixin(FooterBarMixin::class.java)
            footerBarMixin.secondaryButton =
                FooterButton.Builder(activity)
                    .setText(AR.string.security_settings_fingerprint_enroll_enrolling_skip)
                    .setButtonType(FooterButton.ButtonType.SKIP)
                    .setTheme(com.google.android.setupdesign.R.style.SudGlifButton_Secondary)
                    .build()
            footerBarMixin.secondaryButton.setOnClickListener(onSkipClickListener)
            footerBarMixin.primaryButton =
                FooterButton.Builder(activity)
                    .setText(AR.string.security_settings_udfps_enroll_find_sensor_start_button)
                    .setButtonType(FooterButton.ButtonType.NEXT)
                    .setTheme(com.google.android.setupdesign.R.style.SudGlifButton_Primary)
                    .build()
            footerBarMixin.primaryButton.setOnClickListener(onNextClickListener)
            val lottieView =
                glifLayout.requireViewById<LottieAnimationView>(R.id.illustration_lottie)
            lottieView.setOnClickListener {
                if (lottieView.isAnimating) {
                    lottieView.pauseAnimation()
                } else {
                    lottieView.playAnimation()
                }
            }
            lottieView.playAnimation()
        }
    }
}
