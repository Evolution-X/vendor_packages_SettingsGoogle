package com.google.android.settings.biometrics.udfps.ui.view

import android.os.Bundle
import android.text.Layout
import android.util.Log
import android.view.View
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieCompositionFactory
import com.android.settings.R as AR
import com.android.settings.biometrics.BiometricsOnboardingProto.OnboardingAction
import com.android.settings.biometrics.BiometricsOnboardingProto.OnboardingScreen
import com.android.settings.biometrics.fingerprint2.lib.model.FingerEnrollState
import com.android.settingslib.display.DisplayDensityUtils
import com.android.systemui.biometrics.shared.model.FingerprintSensor
import com.google.android.settings.R
import com.google.android.settings.biometrics.combination.ui.viewmodel.VibratorViewModel
import com.google.android.settings.biometrics.fingerprint.ui.view.NavOptionsUseCase
import com.google.android.settings.biometrics.fingerprint.ui.viewmodel.FingerprintEnrollResult
import com.google.android.settings.biometrics.fingerprint.ui.viewmodel.FingerprintMetricsViewModel
import com.google.android.settings.biometrics.fingerprint.ui.viewmodel.SetEnrollResultViewModel
import com.google.android.settings.biometrics.udfps.factory.UdfpsViewModelFactory
import com.google.android.settings.biometrics.udfps.ui.model.EnrollStage
import com.google.android.settings.biometrics.udfps.ui.viewmodel.EnrollUdfpsViewModel
import com.google.android.settings.biometrics.udfps.ui.widget.UdfpsEnrollEnrollingView
import com.google.android.settings.biometrics.udfps.ui.widget.UdfpsEnrollHelper
import com.google.android.settings.biometrics.udfps.ui.widget.UdfpsEnrollView
import com.google.android.setupcompat.template.FooterBarMixin
import com.google.android.setupcompat.template.FooterButton
import com.google.android.setupdesign.util.LottieAnimationHelper
import com.google.android.setupdesign.util.ThemeHelper
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class EnrollUdfpsFragment : Fragment(R.layout.enroll_udfps_fragment) {

    private val setEnrollResultViewModel: SetEnrollResultViewModel by activityViewModels()
    private val metricsViewModel: FingerprintMetricsViewModel by activityViewModels()
    private val mVibratorViewModel: VibratorViewModel by activityViewModels()
    private val viewModel: EnrollUdfpsViewModel by
        viewModels(extrasProducer = { requireActivity().defaultViewModelCreationExtras }) {
            defaultViewModelProviderFactory
        }

    private val useExpressStyle: Boolean by lazy {
        ThemeHelper.shouldApplyGlifExpressiveStyle(requireContext())
    }

    private val haveShownTipLottie = atomic(false)
    private val haveShownLeftEdgeLottie = atomic(false)
    private val haveShownRightEdgeLottie = atomic(false)
    private val haveShownCenterLottie = atomic(false)
    private val haveShownGuideLottie = atomic(false)

    private var rotation: Int? = null
    private var rotationJob: Job? = null
    private var progressJob: Job? = null
    private var helpMsgJob: Job? = null
    private var errorMsgJob: Job? = null
    private var acquiredJob: Job? = null
    private var pointerDownJob: Job? = null
    private var pointerUpJob: Job? = null

    private val enrollHelper: UdfpsEnrollHelper by lazy { UdfpsEnrollHelper(requireContext()) }

    private val onSkipClickListener = View.OnClickListener {
        cancelEnroll()
        lifecycleScope.launch {
            setEnrollResultViewModel.emit(FingerprintEnrollResult.ENROLL_SKIP_BUTTON)
        }
    }

    private val progressCollector =
        FlowCollector<FingerEnrollState.EnrollProgress?> { enrollProgress ->
            if (enrollProgress != null && enrollProgress.totalStepsRequired >= 0) {
                updateProgress(isFromFlow = true, enrollProgress = enrollProgress)
                updateTitleAndDescription()
                if (viewModel.isAccessibilityEnabled()) {
                    val totalSteps = enrollProgress.totalStepsRequired
                    val percent =
                        ((totalSteps - enrollProgress.remainingSteps).toFloat() / totalSteps * 100f)
                            .toInt()
                    val a11yMsg =
                        activity!!.getString(
                            AR.string.security_settings_udfps_enroll_progress_a11y_message,
                            percent,
                        )
                    Companion.getUdfpsEnrollView(view!!).contentDescription = a11yMsg
                }
            }
        }

    private val helpMsgCollector =
        FlowCollector<FingerEnrollState.EnrollHelp?> { enrollHelp ->
            if (enrollHelp != null) {
                Log.d(TAG, "helpMsgCollector(${enrollHelp.helpMsgId}, ${enrollHelp.helpString})")
                if (enrollHelp.helpMsgId == 3) {
                    val dirtyMsg =
                        resources.getString(AR.string.fingerprint_acquired_imager_dirty_udfps)
                    showError(dirtyMsg)
                    enrollHelper.onEnrollmentHelp()
                } else if (enrollHelp.helpString.isNotEmpty()) {
                    showError(enrollHelp.helpString)
                    enrollHelper.onEnrollmentHelp()
                }
                adjustScrollableHeaderIfNeeded(view as UdfpsEnrollEnrollingView)
            }
        }

    private val errorMsgCollector =
        FlowCollector<FingerEnrollState.EnrollError?> { enrollError ->
            if (enrollError != null) {
                Log.d(TAG, "errorMsgCollector(${enrollError.errorId})")
                cancelEnroll()
                EnrollUdfpsErrorDialog.newInstance(enrollError.errorId, viewModel.isSuw)
                    .show(parentFragmentManager, EnrollUdfpsErrorDialog::class.java.name)
            }
        }

    private val acquiredCollector =
        FlowCollector<FingerEnrollState.Acquired?> { acquired ->
            if (acquired != null) {
                enrollHelper.onAcquired(acquired.acquiredGood)
                Log.d(TAG, "onAcquired(), acquiredGood:${acquired.acquiredGood}")
            }
        }

    private val pointerDownCollector =
        FlowCollector<FingerEnrollState.PointerDown?> { pointerDown ->
            if (pointerDown != null) {
                enrollHelper.onPointerDown(pointerDown.fingerId)
            }
        }

    private val pointerUpCollector =
        FlowCollector<FingerEnrollState.PointerUp?> { pointerUp ->
            if (pointerUp != null) {
                enrollHelper.onPointerUp(pointerUp.fingerId)
            }
        }

    private val delayedFinishRunnable = Runnable {
        metricsViewModel.appendAction(OnboardingAction.ACTION_NEXT)
        lifecycleScope.launch {
            findNavController()
                .navigate(
                    AR.id.action_enrolling_to_finish,
                    null,
                    if (viewModel.isEnrollable() || viewModel.isFastEnroll) {
                        NavOptionsUseCase.newSkipEnrollNavOptions()
                    } else {
                        NavOptionsUseCase.newPopAllScreensNavOptions()
                    },
                )
        }
    }

    private val dialogDetachedCallback =
        object : FragmentManager.FragmentLifecycleCallbacks() {
            override fun onFragmentDetached(fm: FragmentManager, f: Fragment) {
                Log.d(TAG, "onFragmentDetached(), activityIsFinish:${activity?.isFinishing}, $f")
                if (f is EnrollUdfpsErrorDialog && activity?.isFinishing == false) {
                    activity?.recreate()
                }
            }
        }

    override val defaultViewModelProviderFactory: ViewModelProvider.Factory
        get() = UdfpsViewModelFactory()

    private fun getIllustrationLottieView(): LottieAnimationView? {
        val currentRotation = rotation
        if ((currentRotation != 0 && currentRotation != 2) || !getShouldShowLottie()) {
            return null
        }
        return view!!.requireViewById(R.id.illustration_lottie)
    }

    private fun getShouldShowLottie(): Boolean {
        val displayDensityUtils = DisplayDensityUtils(requireContext())
        val currentIndex = displayDensityUtils.currentIndex
        val values = displayDensityUtils.values
        val currentDensity = values?.get(currentIndex)
        val defaultDensity = displayDensityUtils.defaultDensity
        Log.d(
            TAG,
            "shouldShowLottie, defaultDensity: $defaultDensity, currentDensity: $currentDensity",
        )
        return requireContext().resources.configuration.fontScale <= 1.0f &&
            currentDensity != null &&
            defaultDensity >= currentDensity
    }

    private fun isErrorDialogShown(): Boolean {
        return childFragmentManager.findFragmentByTag(EnrollUdfpsErrorDialog::class.java.name) !=
            null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lifecycleScope.launch {
            rotation = viewModel.rotation.first()
            Companion.bindView(
                view as UdfpsEnrollEnrollingView,
                viewModel.getSensorProp(),
                enrollHelper,
                FooterButton.Builder(requireContext()),
                onSkipClickListener,
            )
            Companion.getTitleText(view).hyphenationFrequency = Layout.HYPHENATION_FREQUENCY_NONE
            Companion.getTitleText(view).accessibilityLiveRegion =
                View.ACCESSIBILITY_LIVE_REGION_POLITE
            adjustScrollableHeaderIfNeeded(view)

            val lottieView = getIllustrationLottieView()
            lottieView?.setOnClickListener {
                if (lottieView.isAnimating) {
                    lottieView.pauseAnimation()
                } else {
                    lottieView.resumeAnimation()
                }
            }
            Log.d(TAG, "onViewCreated(), bindView finished")
        }

        parentFragmentManager.registerFragmentLifecycleCallbacks(dialogDetachedCallback, true)
        lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onCreate(owner: LifecycleOwner) {
                    observeVibratorStatus()
                }

                override fun onStart(owner: LifecycleOwner) {
                    Log.d(
                        TAG,
                        "onStart(), isEnrolling:${viewModel.isEnrolling()}, isErrorDialog:${isErrorDialogShown()}",
                    )
                    if (!isErrorDialogShown()) {
                        if (viewModel.isEnrolling()) {
                            collectEnrollFlows()
                        } else {
                            startEnroll()
                        }
                    }
                    updateProgressAndHelpMessageWithoutAnimation()
                    rotationJob = collectInLifecycleScope(viewModel.rotation, ::onRotationChanged)
                    metricsViewModel.setScreen(OnboardingScreen.SCREEN_ENROLLING)
                }

                override fun onStop(owner: LifecycleOwner) {
                    rotationJob?.cancel()
                    cancelEnrollFlows()
                }

                override fun onDestroy(owner: LifecycleOwner) {
                    parentFragmentManager.unregisterFragmentLifecycleCallbacks(
                        dialogDetachedCallback
                    )
                    val isChangingConfigs = requireActivity().isChangingConfigurations
                    Log.d(
                        TAG,
                        "onDestroy(), enrolling:${viewModel.isEnrolling()}, isChangingConfig:$isChangingConfigs",
                    )
                    if (viewModel.isEnrolling() && !isChangingConfigs) {
                        cancelEnroll()
                    }
                }
            }
        )

        Companion.getUdfpsEnrollView(view).accessibilityLiveRegion =
            View.ACCESSIBILITY_LIVE_REGION_POLITE
    }

    fun updateProgressAndHelpMessageWithoutAnimation() {
        updateProgress(isFromFlow = false, enrollProgress = viewModel.progressFlow.value)
        val enrollHelp = viewModel.helpFlow.replayCache.first()
        if (enrollHelp != null) {
            lifecycleScope.launch { helpMsgCollector.emit(enrollHelp) }
        } else {
            updateTitleAndDescription()
        }
    }

    private fun collectEnrollFlows() {
        cancelEnrollFlows()
        progressJob = collectInLifecycleScope(viewModel.progressFlow, progressCollector)
        helpMsgJob = collectInLifecycleScope(viewModel.helpFlow, helpMsgCollector)
        errorMsgJob = collectInLifecycleScope(viewModel.errorFlow, errorMsgCollector)
        acquiredJob = collectInLifecycleScope(viewModel.acquiredFlow, acquiredCollector)
        pointerDownJob = collectInLifecycleScope(viewModel.pointerDownFlow, pointerDownCollector)
        pointerUpJob = collectInLifecycleScope(viewModel.pointerUpFlow, pointerUpCollector)
    }

    private fun cancelEnrollFlows() {
        errorMsgJob?.cancel()
        errorMsgJob = null
        progressJob?.cancel()
        progressJob = null
        helpMsgJob?.cancel()
        helpMsgJob = null
        acquiredJob?.cancel()
        acquiredJob = null
        pointerDownJob?.cancel()
        pointerDownJob = null
        pointerUpJob?.cancel()
        pointerUpJob = null
    }

    private fun startEnroll() {
        if (viewModel.startEnroll()) {
            Log.d(TAG, "startEnroll(), success")
            collectEnrollFlows()
        } else {
            Log.e(TAG, "startEnroll(), failed")
        }
    }

    private fun cancelEnroll() {
        if (!viewModel.isEnrolling()) {
            Log.d(TAG, "cancelEnroll(), failed because isEnrolling is false")
        } else {
            cancelEnrollFlows()
            viewModel.cancelEnroll()
        }
    }

    private fun updateProgress(
        isFromFlow: Boolean,
        enrollProgress: FingerEnrollState.EnrollProgress?,
    ) {
        if (!viewModel.isEnrolling()) {
            Log.d(TAG, "Enroll not started yet")
            return
        }
        val finished = isFinished(enrollProgress)
        Log.d(TAG, "updateProgress($isFromFlow, $enrollProgress), isFinished:$finished")
        if (enrollProgress != null) {
            enrollHelper.onEnrollmentProgress(
                enrollProgress.totalStepsRequired,
                enrollProgress.remainingSteps,
            )
            if (finished) {
                if (isFromFlow) {
                    lifecycleScope.launch {
                        delay(400L)
                        delayedFinishRunnable.run()
                    }
                } else {
                    delayedFinishRunnable.run()
                }
            }
        }
    }

    private fun updateTitleAndDescription() {
        val titleText = Companion.getTitleText(view!!)
        val subTitleText = Companion.getSubTitleText(view!!)
        val enrollStage = viewModel.stageFlow.value
        Log.d(TAG, "updateTitleAndDescription($enrollStage)")

        when (enrollStage) {
            EnrollStage.CENTER -> {
                titleText.setText(AR.string.security_settings_fingerprint_enroll_repeat_title)
                val lottieView = getIllustrationLottieView()
                val animRes =
                    if (useExpressStyle) R.raw.udfps_center_hint_lottie_expressive
                    else AR.raw.udfps_center_hint_lottie
                if (useExpressStyle && lottieView != null) {
                    setupIllustrationAnim(getIllustrationLottieView(), animRes)
                }
                if (viewModel.isAccessibilityEnabled() || lottieView == null) {
                    val msgRes =
                        if (mVibratorViewModel.isVibratorEnabled().value) {
                            R.string.security_settings_udfps_enroll_start_message_new
                        } else {
                            R.string.security_settings_udfps_enroll_start_message_without_haptic
                        }
                    subTitleText.setText(msgRes)
                } else if (haveShownCenterLottie.compareAndSet(false, true)) {
                    configureEnrollStage(
                        lottieView,
                        AR.string.security_settings_sfps_enroll_finger_center_title,
                        animRes,
                    )
                }
                if (viewModel.isAccessibilityEnabled()) {
                    (view as UdfpsEnrollEnrollingView?)?.setFocusOnDescription()
                }
                adjustScrollableHeaderIfNeeded(view as UdfpsEnrollEnrollingView)
            }
            EnrollStage.GUIDED -> {
                titleText.setText(AR.string.security_settings_fingerprint_enroll_repeat_title)
                val lottieView = getIllustrationLottieView()
                val animRes =
                    if (useExpressStyle) R.raw.udfps_center_hint_lottie_expressive
                    else AR.raw.udfps_center_hint_lottie
                if (useExpressStyle && lottieView != null) {
                    setupIllustrationAnim(getIllustrationLottieView(), animRes)
                }
                if (viewModel.isAccessibilityEnabled() || lottieView == null) {
                    subTitleText.setText(
                        AR.string.security_settings_udfps_enroll_repeat_a11y_message
                    )
                } else if (haveShownGuideLottie.compareAndSet(false, true)) {
                    configureEnrollStage(
                        lottieView,
                        AR.string.security_settings_fingerprint_enroll_repeat_message,
                        animRes,
                    )
                }
                if (viewModel.isAccessibilityEnabled()) {
                    (view as UdfpsEnrollEnrollingView?)?.setFocusOnDescription()
                }
                adjustScrollableHeaderIfNeeded(view as UdfpsEnrollEnrollingView)
            }
            EnrollStage.FINGERTIP -> {
                titleText.setText(AR.string.security_settings_udfps_enroll_fingertip_title)
                val lottieView = getIllustrationLottieView()
                val animRes =
                    if (useExpressStyle) R.raw.udfps_tip_hint_lottie_expressive
                    else AR.raw.udfps_tip_hint_lottie
                if (useExpressStyle && lottieView != null) {
                    setupIllustrationAnim(getIllustrationLottieView(), animRes)
                }
                if (lottieView != null && haveShownTipLottie.compareAndSet(false, true)) {
                    configureEnrollStage(
                        lottieView,
                        AR.string.security_settings_udfps_tip_fingerprint_help,
                        animRes,
                    )
                }
                if (viewModel.isAccessibilityEnabled()) {
                    (view as UdfpsEnrollEnrollingView?)?.setFocusOnDescription()
                }
                adjustScrollableHeaderIfNeeded(view as UdfpsEnrollEnrollingView)
            }
            EnrollStage.LEFT_EDGE -> {
                titleText.setText(AR.string.security_settings_udfps_enroll_left_edge_title)
                val lottieView = getIllustrationLottieView()
                val animRes =
                    if (useExpressStyle) R.raw.udfps_left_edge_hint_lottie_expressive
                    else AR.raw.udfps_left_edge_hint_lottie
                if (useExpressStyle && lottieView != null) {
                    setupIllustrationAnim(getIllustrationLottieView(), animRes)
                }
                if (lottieView != null && haveShownLeftEdgeLottie.compareAndSet(false, true)) {
                    configureEnrollStage(
                        lottieView,
                        AR.string.security_settings_udfps_side_fingerprint_help,
                        animRes,
                    )
                } else if (lottieView == null) {
                    val msgRes =
                        if (viewModel.isStageHalfCompletedFlow.value) {
                            AR.string.security_settings_fingerprint_enroll_repeat_message
                        } else {
                            AR.string.security_settings_udfps_enroll_edge_message
                        }
                    subTitleText.setText(msgRes)
                }
                if (viewModel.isAccessibilityEnabled()) {
                    (view as UdfpsEnrollEnrollingView?)?.setFocusOnDescription()
                }
                adjustScrollableHeaderIfNeeded(view as UdfpsEnrollEnrollingView)
            }
            EnrollStage.RIGHT_EDGE -> {
                titleText.setText(AR.string.security_settings_udfps_enroll_right_edge_title)
                val lottieView = getIllustrationLottieView()
                val animRes =
                    if (useExpressStyle) R.raw.udfps_right_edge_hint_lottie_expressive
                    else AR.raw.udfps_right_edge_hint_lottie
                if (useExpressStyle && lottieView != null) {
                    setupIllustrationAnim(getIllustrationLottieView(), animRes)
                }
                if (lottieView != null && haveShownRightEdgeLottie.compareAndSet(false, true)) {
                    configureEnrollStage(
                        lottieView,
                        AR.string.security_settings_udfps_side_fingerprint_help,
                        animRes,
                    )
                } else if (lottieView == null) {
                    val msgRes =
                        if (viewModel.isStageHalfCompletedFlow.value) {
                            AR.string.security_settings_fingerprint_enroll_repeat_message
                        } else {
                            AR.string.security_settings_udfps_enroll_edge_message
                        }
                    subTitleText.setText(msgRes)
                }
                if (viewModel.isAccessibilityEnabled()) {
                    (view as UdfpsEnrollEnrollingView?)?.setFocusOnDescription()
                }
                adjustScrollableHeaderIfNeeded(view as UdfpsEnrollEnrollingView)
            }
            EnrollStage.UNKNOWN -> {
                titleText.setText(AR.string.security_settings_fingerprint_enroll_udfps_title)
                val msgRes =
                    if (mVibratorViewModel.isVibratorEnabled().value) {
                        R.string.security_settings_udfps_enroll_start_message_new
                    } else {
                        R.string.security_settings_udfps_enroll_start_message_without_haptic
                    }
                subTitleText.setText(msgRes)
                requireActivity().title = getString(AR.string.security_settings_udfps_enroll_a11y)
            }
        }
    }

    private fun adjustScrollableHeaderIfNeeded(udfpsEnrollEnrollingView: UdfpsEnrollEnrollingView) {
        val currentRotation = rotation
        if (currentRotation == 0 || currentRotation == 2) {
            adjustScrollableHeader(udfpsEnrollEnrollingView)
        }
    }

    private fun setupIllustrationAnim(lottieAnimationView: LottieAnimationView?, animRes: Int) {
        lottieAnimationView?.setAnimation(animRes)
        val colorTokens = requireContext().resources.getStringArray(R.array.udfps_enroll_enrolling)
        LottieAnimationHelper.get()
            .applyColor(requireContext(), lottieAnimationView, colorTokens.toList())
    }

    private fun configureEnrollStage(
        lottieAnimationView: LottieAnimationView,
        descRes: Int,
        animRes: Int,
    ) {
        if (viewModel.isAccessibilityEnabled()) {
            return
        }
        lottieAnimationView.contentDescription = getString(descRes)
        Companion.getSubTitleText(view!!).text = ""
        LottieCompositionFactory.fromRawRes(requireContext(), animRes).addListener { composition ->
            lottieAnimationView.setComposition(composition)
            Log.d(TAG, "Set lottie visible for ${lottieAnimationView.contentDescription}")
            lottieAnimationView.visibility = View.VISIBLE
            lottieAnimationView.playAnimation()
        }
    }

    private fun showError(errorMsg: CharSequence) {
        val titleText = Companion.getTitleText(view!!)
        titleText.text = errorMsg
        titleText.contentDescription = errorMsg
        Companion.getSubTitleText(view!!).contentDescription = ""
    }

    private fun onRotationChanged(newRotation: Int) {
        Log.d(TAG, "onRotationChanged(), newRotation: $newRotation oldRotation:$rotation")
        if (rotation == (newRotation + 2) % 4) {
            rotation = newRotation
            (view as UdfpsEnrollEnrollingView?)?.relayoutForFingerprintSensor()
            lifecycleScope.launch {
                Companion.bindView(
                    view as UdfpsEnrollEnrollingView,
                    viewModel.getSensorProp(),
                    enrollHelper,
                    FooterButton.Builder(requireContext()),
                    onSkipClickListener,
                )
                Log.d(TAG, "onRotationChanged(), bindView finished")
            }
        }
    }

    private fun <T> collectInLifecycleScope(flow: Flow<T>, collector: FlowCollector<T>): Job {
        return lifecycleScope.launch { flow.collect(collector) }
    }

    private fun isFinished(enrollProgress: FingerEnrollState.EnrollProgress?): Boolean {
        if (enrollProgress == null) return false
        val total = enrollProgress.totalStepsRequired + 1
        val completed = (total - enrollProgress.remainingSteps).coerceAtLeast(0)
        return (completed * 10000) / total >= 10000
    }

    private fun adjustScrollableHeader(udfpsEnrollEnrollingView: UdfpsEnrollEnrollingView) {
        val scrollView =
            udfpsEnrollEnrollingView.findViewById<ScrollView>(
                com.google.android.setupdesign.R.id.sud_header_scroll_view
            )
        if (scrollView != null) {
            val duration =
                udfpsEnrollEnrollingView.resources
                    .getInteger(AR.integer.config_biometrics_header_scroll_duration)
                    .toLong()
            udfpsEnrollEnrollingView.adjustScrollableHeaderHeight(scrollView, getShouldShowLottie())
            udfpsEnrollEnrollingView.headerVerticalScrolling(scrollView, duration)
        }
    }

    private fun observeVibratorStatus() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                mVibratorViewModel.isVibratorEnabled().collect { isEnabled ->
                    Log.d(TAG, "Vibrator enabled: $isEnabled")
                }
            }
        }
    }

    companion object {
        private const val TAG = "EnrollUdfpsFragment"

        fun getTitleText(view: View): TextView {
            return view.requireViewById(com.google.android.setupdesign.R.id.suc_layout_title)
        }

        fun getSubTitleText(view: View): TextView {
            return view.requireViewById(com.google.android.setupdesign.R.id.sud_layout_subtitle)
        }

        fun getUdfpsEnrollView(view: View): UdfpsEnrollView {
            return view.requireViewById(AR.id.udfps_animation_view)
        }

        fun bindView(
            udfpsEnrollEnrollingView: UdfpsEnrollEnrollingView,
            fingerprintSensor: FingerprintSensor,
            udfpsEnrollHelper: UdfpsEnrollHelper,
            builder: FooterButton.Builder,
            onClickListener: View.OnClickListener,
        ) {
            udfpsEnrollEnrollingView.initView(fingerprintSensor, udfpsEnrollHelper)
            val footerBarMixin = udfpsEnrollEnrollingView.getMixin(FooterBarMixin::class.java)
            footerBarMixin?.secondaryButton =
                builder
                    .setText(AR.string.security_settings_fingerprint_enroll_enrolling_skip)
                    .setListener(onClickListener)
                    .setButtonType(FooterButton.ButtonType.SKIP)
                    .setTheme(com.google.android.setupdesign.R.style.SudGlifButton_Secondary)
                    .build()
        }
    }
}
