package com.google.android.settings.biometrics.usudfps.ui.view

import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
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
import com.google.android.settings.biometrics.fingerprint.model.HareMode
import com.google.android.settings.biometrics.fingerprint.ui.view.GlifLayoutUseCase
import com.google.android.settings.biometrics.fingerprint.ui.view.NavOptionsUseCase
import com.google.android.settings.biometrics.fingerprint.ui.view.SkipFindFpsDialog
import com.google.android.settings.biometrics.fingerprint.ui.viewmodel.FingerprintEnrollResult
import com.google.android.settings.biometrics.fingerprint.ui.viewmodel.FingerprintMetricsViewModel
import com.google.android.settings.biometrics.fingerprint.ui.viewmodel.SetEnrollResultViewModel
import com.google.android.settings.biometrics.fingerprint.ui.viewmodel.UsUdfpsCalibratorViewModel
import com.google.android.settings.biometrics.ui.view.LottiePlayPauseUseCase
import com.google.android.settings.biometrics.usudfps.factory.UsudfpsViewModelFactory
import com.google.android.settings.biometrics.usudfps.ui.viewmodel.FindUsUdfpsWithSpViewModel
import com.google.android.setupcompat.template.FooterBarMixin
import com.google.android.setupcompat.template.FooterButton
import com.google.android.setupdesign.GlifLayout
import com.google.android.setupdesign.util.LottieAnimationHelper
import com.google.android.setupdesign.util.ThemeHelper
import kotlinx.coroutines.launch

class FindUsUdfpsWithSpFragment : Fragment() {

    private val viewModel: FindUsUdfpsWithSpViewModel by
        activityViewModels(extrasProducer = { requireActivity().defaultViewModelCreationExtras }) {
            defaultViewModelProviderFactory
        }
    private val setEnrollResultViewModel: SetEnrollResultViewModel by activityViewModels()
    private val metricsViewModel: FingerprintMetricsViewModel by activityViewModels()

    private var lottiePlayPauseUseCase: LottiePlayPauseUseCase? = null

    private val onSkipClickListener = View.OnClickListener {
        if (!viewModel.isSuw) {
            lifecycleScope.launch {
                setEnrollResultViewModel.emit(FingerprintEnrollResult.FIND_SENSOR_SKIP_BUTTON)
            }
            return@OnClickListener
        }
        SkipFindFpsDialog.showDialog(childFragmentManager)
    }

    private val onEnrollClickListener = View.OnClickListener {
        viewModel.setNoScreenProtectorIfUnsetBefore()
        metricsViewModel.appendAction(OnboardingAction.ACTION_NEXT)
        findNavController()
            .navigate(R.id.action_find_sensor_to_enroll, null, NavOptionsUseCase.newNavOptions())
    }

    private val onSpClickListener = View.OnClickListener {
        Log.d(TAG, "start to setup sp")
        metricsViewModel.appendAction(OnboardingAction.ACTION_SETUP_CAPYBARA)
        findNavController()
            .navigate(R.id.action_find_sensor_to_sp_edu, null, NavOptionsUseCase.newNavOptions())
    }

    private val useExpressiveUi: Boolean by lazy {
        ThemeHelper.shouldApplyGlifExpressiveStyle(requireContext())
    }

    private val calibratorInitViewModel: UsUdfpsCalibratorViewModel by
        activityViewModels(extrasProducer = { requireActivity().defaultViewModelCreationExtras }) {
            defaultViewModelProviderFactory
        }

    private val _defaultViewModelProviderFactory: ViewModelProvider.Factory by lazy {
        UsudfpsViewModelFactory()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        calibratorInitViewModel.calibrator?.onWaitingPage(lifecycle, childFragmentManager, null)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? =
        inflater.inflate(
            if (useExpressiveUi) {
                R.layout.find_usudfps_with_sp_expressive
            } else {
                R.layout.find_usudfps_with_sp
            },
            container,
            false,
        )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onStart(owner: LifecycleOwner) {
                    lifecycleScope.launch {
                        updateLayout(view as GlifLayout)
                        updateIllustrationTheme(view)
                        lottiePlayPauseUseCase =
                            LottiePlayPauseUseCase(
                                    view.requireViewById<LottieAnimationView>(
                                        R.id.illustration_lottie
                                    )
                                )
                                .apply { startAnimationAndSetupAccessibility() }
                    }
                    metricsViewModel.setScreen(OnboardingScreen.SCREEN_EDUCATION)
                }

                override fun onStop(owner: LifecycleOwner) {
                    lottiePlayPauseUseCase?.clearResources()
                    lottiePlayPauseUseCase = null
                }
            }
        )
    }

    private fun updateIllustrationTheme(glifLayout: GlifLayout) {
        val lottieView = glifLayout.requireViewById<LottieAnimationView>(R.id.illustration_lottie)
        val colors =
            requireContext()
                .resources
                .getStringArray(
                    if (useExpressiveUi) {
                        R.array.touch_sensor_education
                    } else {
                        R.array.touch_sensor_education_2024
                    }
                )
                .toList()
        LottieAnimationHelper.get().applyColor(requireContext(), lottieView, colors)
    }

    private suspend fun updateLayout(glifLayout: GlifLayout) {
        val glifLayoutUseCase = GlifLayoutUseCase(glifLayout)
        glifLayoutUseCase.setHeaderText(
            requireActivity(),
            AR.string.security_settings_udfps_enroll_find_sensor_title,
        )
        glifLayoutUseCase.setDescriptionText(
            requireContext()
                .getText(
                    if (viewModel.getHareMode() is HareMode.Enabled) {
                        R.string.find_usudfps_subtitle_simple
                    } else {
                        R.string.fingerprint_udfps_enroll_find_sensor_description
                    }
                )
        )
        val footerBarMixin = glifLayout.getMixin(FooterBarMixin::class.java)
        footerBarMixin.primaryButton =
            FooterButton.Builder(requireContext())
                .setText(AR.string.security_settings_udfps_enroll_find_sensor_start_button)
                .setButtonType(FooterButton.ButtonType.NEXT)
                .setTheme(com.google.android.setupdesign.R.style.SudGlifButton_Primary)
                .build()
                .apply { setOnClickListener(onEnrollClickListener) }
        footerBarMixin.secondaryButton =
            FooterButton.Builder(glifLayout.context)
                .setText(AR.string.security_settings_fingerprint_enroll_enrolling_skip)
                .setButtonType(FooterButton.ButtonType.SKIP)
                .setTheme(com.google.android.setupdesign.R.style.SudGlifButton_Secondary)
                .build()
                .apply { setOnClickListener(onSkipClickListener) }
        val layoutContainer = glifLayout.requireViewById<LinearLayout>(AR.id.layout_container)
        val spButton = layoutContainer.requireViewById<Button>(R.id.sp_button)
        spButton.setOnClickListener(onSpClickListener)
        if (viewModel.shouldShowSpButtons) {
            layoutContainer.gravity = Gravity.TOP
            spButton.visibility = View.VISIBLE
        } else {
            layoutContainer.gravity = Gravity.BOTTOM
            spButton.visibility = View.GONE
        }
    }

    override val defaultViewModelProviderFactory: ViewModelProvider.Factory
        get() =
            activity?.defaultViewModelProviderFactory as? UsudfpsViewModelFactory
                ?: _defaultViewModelProviderFactory

    private companion object {
        const val TAG = "FindUsUdfpsWithSp"
    }
}
