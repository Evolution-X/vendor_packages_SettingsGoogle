package com.google.android.settings.biometrics.sfps.view

import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
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
import com.android.settings.biometrics.fingerprint.FingerprintEnrollFinish
import com.google.android.settings.R
import com.google.android.settings.biometrics.fingerprint.ui.view.GlifLayoutUseCase
import com.google.android.settings.biometrics.fingerprint.ui.view.NavOptionsUseCase
import com.google.android.settings.biometrics.fingerprint.ui.viewmodel.FingerprintEnrollResult
import com.google.android.settings.biometrics.fingerprint.ui.viewmodel.FingerprintMetricsViewModel
import com.google.android.settings.biometrics.fingerprint.ui.viewmodel.SetEnrollResultViewModel
import com.google.android.settings.biometrics.sfps.factory.SfpsViewModelFactory
import com.google.android.settings.biometrics.sfps.viewmodel.ConfirmSfpsViewModel
import com.google.android.settings.biometrics.ui.view.LottiePlayPauseUseCase
import com.google.android.setupcompat.template.FooterBarMixin
import com.google.android.setupcompat.template.FooterButton
import com.google.android.setupdesign.GlifLayout
import com.google.android.setupdesign.util.LottieAnimationHelper
import com.google.android.setupdesign.util.ThemeHelper
import kotlinx.coroutines.launch

class ConfirmSfpsFragment : Fragment(AR.layout.fingerprint_enroll_finish) {

    private val viewModel: ConfirmSfpsViewModel by
        viewModels(extrasProducer = { requireActivity().defaultViewModelCreationExtras }) {
            defaultViewModelProviderFactory
        }

    private val useExpressStyle: Boolean by lazy {
        ThemeHelper.shouldApplyGlifExpressiveStyle(requireContext())
    }

    private val setEnrollResultViewModel: SetEnrollResultViewModel by activityViewModels()
    private val metricsViewModel: FingerprintMetricsViewModel by activityViewModels()

    private var lottiePlayPauseUseCase: LottiePlayPauseUseCase? = null

    private val nextClickListener = View.OnClickListener {
        lifecycleScope.launch {
            Log.d(TAG, "User finishing enrollment")
            updateFingerprintSuggestionEnableState()
            setEnrollResultViewModel.emit(FingerprintEnrollResult.CONFIRMATION_NEXT_BUTTON)
        }
    }

    private val addAnother = View.OnClickListener {
        lifecycleScope.launch {
            Log.d(TAG, "User adding another fingerprint")
            updateFingerprintSuggestionEnableState()
            metricsViewModel.appendAction(OnboardingAction.ACTION_ADD_ANOTHER_FINGERPRINT)
            findNavController()
                .navigate(
                    R.id.action_finish_to_enrolling,
                    null,
                    NavOptionsUseCase.newBackToEnrollNavOptions(),
                )
        }
    }

    override val defaultViewModelProviderFactory: ViewModelProvider.Factory
        get() = SfpsViewModelFactory()

    private val footerBarMixin: FooterBarMixin
        get() = (requireView() as GlifLayout).getMixin(FooterBarMixin::class.java)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? =
        inflater.inflate(
            if (useExpressStyle) {
                R.layout.sfps_enroll_finish_expressive
            } else {
                AR.layout.sfps_enroll_finish
            },
            container,
            false,
        )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated")
        val glifLayoutUseCase =
            GlifLayoutUseCase(requireView().requireViewById<GlifLayout>(R.id.setup_wizard_layout))
        val activity = requireActivity()
        view.requireViewById<View>(AR.id.sfps_enrollment_finish_content_frame)
        lifecycleScope.launch {
            val enrolledCount = viewModel.getEnrolledFingerprints()
            glifLayoutUseCase.setHeaderText(
                activity,
                if (enrolledCount == 1) {
                    R.string.fingerprint_enroll_finish_title_one_fingerprint_enrolled
                } else {
                    R.string.fingerprint_enroll_finish_title
                },
            )
            viewModel.isPrivateProfile.collect { isPrivateProfile ->
                glifLayoutUseCase.setDescriptionText(
                    getString(
                        if (isPrivateProfile) {
                            AR.string.private_space_fingerprint_enroll_finish_message
                        } else if (enrolledCount == 1) {
                            R.string.fingerprint_enroll_finish_subtitle_one_fingerprint_enrolled
                        } else if (enrolledCount == 2) {
                            R.string.fingerprint_enroll_finish_subtitle_two_fingerprints_enrolled
                        } else {
                            R.string
                                .fingerprint_enroll_finish_subtitle_three_and_more_fingerprints_enrolled
                        }
                    )
                )
            }
            if (viewModel.canEnrollMoreFingerprints()) {
                footerBarMixin.secondaryButton =
                    FooterButton.Builder(requireContext())
                        .setText(R.string.fingerprint_enroll_finish_footer_button_add)
                        .setButtonType(FooterButton.ButtonType.SKIP)
                        .setListener(addAnother)
                        .setTheme(com.google.android.setupdesign.R.style.SudGlifButton_Secondary)
                        .build()
            }
            footerBarMixin.primaryButton =
                FooterButton.Builder(requireContext())
                    .setText(
                        if (viewModel.request.isSuw) {
                            AR.string.next_label
                        } else {
                            AR.string.security_settings_fingerprint_enroll_done
                        }
                    )
                    .setListener(nextClickListener)
                    .setButtonType(FooterButton.ButtonType.NEXT)
                    .setTheme(com.google.android.setupdesign.R.style.SudGlifButton_Primary)
                    .build()
        }
        updateFingerprintSuggestionEnableState()
        lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onStart(owner: LifecycleOwner) {
                    metricsViewModel.setScreen(OnboardingScreen.SCREEN_CONFIRMATION)
                    if (useExpressStyle) {
                        setupExpressiveStyleAnim(view)
                    }
                }

                override fun onStop(owner: LifecycleOwner) {
                    if (lottiePlayPauseUseCase != null) {
                        lottiePlayPauseUseCase?.clearResources()
                        lottiePlayPauseUseCase = null
                    }
                }
            }
        )
    }

    private fun setupExpressiveStyleAnim(view: View) {
        val lottieView = view.findViewById<LottieAnimationView>(R.id.sfps_enroll_finish_lottie)!!
        lottieView.setAnimation(R.raw.fingerprint_enroll_finish_expressive)
        lottieView.visibility = View.VISIBLE
        lottiePlayPauseUseCase =
            LottiePlayPauseUseCase(lottieView).apply { startAnimationAndSetupAccessibility() }
        val colors =
            requireContext().resources.getStringArray(R.array.add_fingerprint_success).toList()
        LottieAnimationHelper.get().applyColor(requireContext(), lottieView, colors)
    }

    private fun updateFingerprintSuggestionEnableState() {
        lifecycleScope.launch {
            val enrolledCount = viewModel.getEnrolledFingerprints()
            requireActivity()
                .packageManager
                .setComponentEnabledSetting(
                    ComponentName(
                        requireActivity().application,
                        FingerprintEnrollFinish.FINGERPRINT_SUGGESTION_ACTIVITY,
                    ),
                    if (enrolledCount == 1) {
                        PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                    } else {
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                    },
                    PackageManager.DONT_KILL_APP,
                )
            Log.d(
                TAG,
                "${FingerprintEnrollFinish.FINGERPRINT_SUGGESTION_ACTIVITY} enabled state = " +
                    "${enrolledCount == 1}",
            )
        }
    }

    companion object {
        private const val TAG = "ConfirmFragment"
    }
}
