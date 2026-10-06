package com.google.android.settings.biometrics.usudfps.ui.view

import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.airbnb.lottie.LottieAnimationView
import com.android.settings.biometrics.BiometricsOnboardingProto.OnboardingAction
import com.android.settings.biometrics.BiometricsOnboardingProto.OnboardingScreen
import com.android.settingslib.widget.LottieColorUtils
import com.google.android.settings.R
import com.google.android.settings.biometrics.fingerprint.ui.view.GlifLayoutUseCase
import com.google.android.settings.biometrics.fingerprint.ui.view.NavOptionsUseCase
import com.google.android.settings.biometrics.fingerprint.ui.viewmodel.FingerprintMetricsViewModel
import com.google.android.settings.biometrics.ui.view.LottiePlayPauseUseCase
import com.google.android.settings.biometrics.usudfps.factory.UsudfpsViewModelFactory
import com.google.android.settings.biometrics.usudfps.ui.model.SpSetupResult
import com.google.android.settings.biometrics.usudfps.ui.viewmodel.SpEduViewModel
import com.google.android.settings.biometrics.usudfps.ui.viewmodel.SpSetupResultViewModel
import com.google.android.setupcompat.template.FooterBarMixin
import com.google.android.setupdesign.GlifLayout
import com.google.android.setupdesign.util.LottieAnimationHelper
import com.google.android.setupdesign.util.ThemeHelper
import kotlinx.coroutines.launch

class SpEduFragment : Fragment(R.layout.sp_edu) {

    private val viewModel: SpEduViewModel by
        viewModels(extrasProducer = { requireActivity().defaultViewModelCreationExtras }) {
            defaultViewModelProviderFactory
        }
    private val setupResultViewModel: SpSetupResultViewModel by
        activityViewModels(extrasProducer = { requireActivity().defaultViewModelCreationExtras }) {
            defaultViewModelProviderFactory
        }
    private val metricsViewModel: FingerprintMetricsViewModel by activityViewModels()

    private var lottiePlayPauseUseCase: LottiePlayPauseUseCase? = null

    private val onScanItLaterClickListener: View.OnClickListener by lazy {
        View.OnClickListener {
            if (viewModel.isEnrolling) {
                Log.d(TAG, "do not set default, start enroll directly")
                metricsViewModel.appendAction(OnboardingAction.ACTION_SKIP)
                startEnroll()
            } else {
                Log.d(TAG, "finish directly")
                lifecycleScope.launch { setupResultViewModel.emit(SpSetupResult.SP_SKIP_BUTTON) }
            }
        }
    }

    private val onScanQrCodeClickListener: View.OnClickListener by lazy {
        View.OnClickListener {
            Log.d(TAG, "run to next scan")
            findNavController()
                .navigate(
                    R.id.action_sp_edu_to_sp_qr_code_scanner,
                    null,
                    NavOptionsUseCase.newNavOptions(),
                )
        }
    }

    private val onNoQrCodeClickListener: View.OnClickListener by lazy {
        View.OnClickListener {
            if (viewModel.isEnrolling) {
                Log.d(TAG, "show no qr code dialog")
                NoSpQrCodeDialog.showDialog(childFragmentManager)
                return@OnClickListener
            }
            Log.d(TAG, "set third party sp and finish")
            viewModel.setThirdPartyScreenProtector()
            lifecycleScope.launch { setupResultViewModel.emit(SpSetupResult.SP_SUCCESS_SET) }
        }
    }

    private val is25NewUi: Boolean by lazy {
        ThemeHelper.shouldApplyGlifExpressiveStyle(requireContext())
    }

    private val noQrCodeDialogDetachedCallback =
        object : FragmentManager.FragmentLifecycleCallbacks() {
            override fun onFragmentDetached(fm: FragmentManager, f: Fragment) {
                if (f is NoSpQrCodeDialog) {
                    Log.d(TAG, "set third party sp and next")
                    viewModel.setThirdPartyScreenProtector()
                    metricsViewModel.appendAction(OnboardingAction.ACTION_NEXT)
                    startEnroll()
                }
            }
        }

    private val _defaultViewModelProviderFactory: ViewModelProvider.Factory by lazy {
        UsudfpsViewModelFactory()
    }

    private fun startEnroll() {
        findNavController()
            .navigate(
                R.id.action_sp_edu_to_enrolling,
                null,
                NavOptionsUseCase.newBackToFindSensorNavOptions(),
            )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateLayout(view as GlifLayout)
        viewLifecycleOwner.lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onStart(owner: LifecycleOwner) {
                    metricsViewModel.setScreen(OnboardingScreen.SCREEN_CAPYBARA_EDUCATION)
                    lottiePlayPauseUseCase =
                        LottiePlayPauseUseCase(
                                view.requireViewById<LottieAnimationView>(R.id.illustration_lottie)
                            )
                            .apply { startAnimationAndSetupAccessibility() }
                    childFragmentManager.registerFragmentLifecycleCallbacks(
                        noQrCodeDialogDetachedCallback,
                        false,
                    )
                }

                override fun onStop(owner: LifecycleOwner) {
                    lottiePlayPauseUseCase?.clearResources()
                    lottiePlayPauseUseCase = null
                    childFragmentManager.unregisterFragmentLifecycleCallbacks(
                        noQrCodeDialogDetachedCallback
                    )
                }
            }
        )
    }

    private fun updateLayout(glifLayout: GlifLayout) {
        if (glifLayout.getMixin(FooterBarMixin::class.java).secondaryButton != null) {
            return
        }
        Log.d(TAG, "$this.updateLayout()")
        val glifLayoutUseCase = GlifLayoutUseCase(glifLayout)
        glifLayoutUseCase.setHeaderText(
            requireActivity(),
            getString(R.string.screen_protector_edu_title),
        )
        glifLayoutUseCase.setDescriptionText(getString(R.string.protector_edu_subtitle))
        val lottieView = glifLayout.requireViewById<LottieAnimationView>(R.id.illustration_lottie)
        if (is25NewUi) {
            lottieView.setAnimation(R.raw.fingerprint_sp_expressive)
            val colors =
                requireContext()
                    .resources
                    .getStringArray(R.array.touch_sensor_screen_protector)
                    .toList()
            LottieAnimationHelper.get().applyColor(requireContext(), lottieView, colors)
        } else {
            LottieColorUtils.applyDynamicColors(context, lottieView)
        }
        lottieView.visibility = View.VISIBLE
        val learnMore = glifLayout.requireViewById<TextView>(R.id.msg_learn_more)
        learnMore.movementMethod = LinkMovementMethod.getInstance()
        learnMore.text =
            Html.fromHtml(
                getString(R.string.screen_protector_edu_learn_more),
                Html.FROM_HTML_MODE_LEGACY,
            )
        val threeButtonsData =
            GlifLayoutUseCase.ThreeButtonsData(
                R.string.screen_protector_edu_scan_qr_code,
                onScanQrCodeClickListener,
                R.string.screen_protector_edu_no_qr_code,
                onNoQrCodeClickListener,
                R.string.screen_protector_edu_scan_it_later,
                onScanItLaterClickListener,
            )
        if (is25NewUi) {
            GlifLayoutUseCase(glifLayout).applyThreeButtonsInFooter(threeButtonsData)
            return
        }
        GlifLayoutUseCase(glifLayout)
            .addExtraFooter(layoutInflater, requireContext().display.rotation, threeButtonsData)
    }

    override val defaultViewModelProviderFactory: ViewModelProvider.Factory
        get() =
            activity?.defaultViewModelProviderFactory as? UsudfpsViewModelFactory
                ?: _defaultViewModelProviderFactory

    private companion object {
        const val TAG = "SpEdu"
    }
}
