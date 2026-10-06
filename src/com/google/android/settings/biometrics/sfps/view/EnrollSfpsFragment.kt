package com.google.android.settings.biometrics.sfps.view

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.Animatable2
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.hardware.biometrics.BiometricFingerprintConstants.FINGERPRINT_ACQUIRED_IMMOBILE
import android.hardware.biometrics.BiometricFingerprintConstants.FINGERPRINT_ACQUIRED_VENDOR_BASE
import android.hardware.biometrics.BiometricFingerprintConstants.FINGERPRINT_ERROR_TIMEOUT
import android.hardware.biometrics.BiometricFingerprintConstants.FINGERPRINT_ERROR_UNABLE_TO_PROCESS
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.Interpolator
import android.widget.ProgressBar
import android.widget.RelativeLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieCompositionFactory
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.model.KeyPath
import com.android.settings.R as AR
import com.android.settings.biometrics.BiometricsOnboardingProto.OnboardingAction
import com.android.settings.biometrics.BiometricsOnboardingProto.OnboardingScreen
import com.android.settings.biometrics.fingerprint.FingerprintEnrollEnrolling
import com.android.settings.biometrics.fingerprint2.lib.model.FingerEnrollState
import com.google.android.settings.R
import com.google.android.settings.biometrics.fingerprint.modules.sfps.model.SfpsStageModel
import com.google.android.settings.biometrics.fingerprint.modules.sfps.viewmodel.EnrollSfpsViewModel
import com.google.android.settings.biometrics.fingerprint.modules.sfps.widget.FingerprintTouchDialog
import com.google.android.settings.biometrics.fingerprint.ui.view.GlifLayoutUseCase
import com.google.android.settings.biometrics.fingerprint.ui.view.NavOptionsUseCase
import com.google.android.settings.biometrics.fingerprint.ui.viewmodel.FingerprintEnrollResult
import com.google.android.settings.biometrics.fingerprint.ui.viewmodel.FingerprintMetricsViewModel
import com.google.android.settings.biometrics.fingerprint.ui.viewmodel.SetEnrollResultViewModel
import com.google.android.settings.biometrics.sfps.factory.SfpsViewModelFactory
import com.google.android.settings.biometrics.sfps.model.EnrollOperationModel
import com.google.android.settings.biometrics.sfps.widget.FingerprintErrorDialog
import com.google.android.settings.biometrics.sfps.widget.FingerprintExtUtils
import com.google.android.settings.biometrics.sfps.widget.ImmobileHelpDialog
import com.google.android.settings.biometrics.sfps.widget.SfpsAnimation
import com.google.android.setupcompat.template.FooterBarMixin
import com.google.android.setupcompat.template.FooterButton
import com.google.android.setupdesign.GlifLayout
import com.google.android.setupdesign.template.DescriptionMixin
import com.google.android.setupdesign.template.HeaderMixin
import com.google.android.setupdesign.util.LottieAnimationHelper
import com.google.android.setupdesign.util.ThemeHelper
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class EnrollSfpsFragment : Fragment(R.layout.sfps_enroll) {

    private val viewModel: EnrollSfpsViewModel by
        viewModels(extrasProducer = { requireActivity().defaultViewModelCreationExtras }) {
            defaultViewModelProviderFactory
        }

    private val useExpressStyle: Boolean by lazy {
        ThemeHelper.shouldApplyGlifExpressiveStyle(requireContext())
    }

    private val pauseEnrollThreshold: Int = FingerprintExtUtils.getPauseEnrollThreshold()

    private val setEnrollResultViewModel: SetEnrollResultViewModel by activityViewModels()
    private val metricsViewModel: FingerprintMetricsViewModel by activityViewModels()

    private lateinit var glifLayout: GlifLayout
    private lateinit var glifUseCase: GlifLayoutUseCase
    private lateinit var progressBar: ProgressBar
    private lateinit var progressLottieLayout: RelativeLayout
    private lateinit var illustrationLottie: LottieAnimationView
    private lateinit var helpAnimation: Animator

    private var enrollSuccessfully = false
    private var iconTouchCount = 0
    private var immobileFpsCount = 0
    private var shouldBackToPreviousPageWhenRestart = false

    private var desktopProgressBarBackgroundAnimationDrawable: AnimatedVectorDrawable? = null
    private var desktopProgressBarBackgroundBlinksDrawable: AnimatedVectorDrawable? = null
    private var desktopProgressBarBackgroundAnimationCanceled = false

    private val mShowDialogRunnable = Runnable { showIconTouchDialog() }

    private val skipClickListener = View.OnClickListener {
        lifecycleScope.launch {
            viewModel.finishEnrollment()
            setEnrollResultViewModel.emit(FingerprintEnrollResult.FIND_SENSOR_SKIP_BUTTON)
        }
    }

    private val desktopProgressBarBackgroundAnimationCallback =
        object : Animatable2.AnimationCallback() {
            override fun onAnimationEnd(drawable: Drawable?) {
                if (desktopProgressBarBackgroundAnimationCanceled) {
                    return
                }
                progressBar.post { desktopProgressBarBackgroundAnimationDrawable?.start() }
            }
        }

    private fun getProgressInPercentage(progress: Int): Int =
        (progress.toDouble() / PROGRESS_BAR_MAX * 100.0).toInt()

    // Stock gates the desktop sensor-location UI behind a flag that is compiled out (always
    // false) in the reference build.
    private fun isDesktopFingerprintSensorLocationEnabled(): Boolean = false

    override val defaultViewModelProviderFactory: ViewModelProvider.Factory
        get() = SfpsViewModelFactory()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? =
        inflater.inflate(
            if (useExpressStyle) R.layout.sfps_enroll_expressive else R.layout.sfps_enroll,
            container,
            false,
        )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        immobileFpsCount = savedInstanceState?.getInt(KEY_IMMOBILE_DIALOG_COUNT, 0) ?: 0
        Log.d(TAG, "onViewCreated: immobile=$immobileFpsCount")
        shouldBackToPreviousPageWhenRestart =
            savedInstanceState?.getBoolean(KEY_BACK_WHEN_RESTART, false) ?: false
        glifLayout = view.requireViewById(R.id.setup_wizard_layout)
        glifUseCase = GlifLayoutUseCase(glifLayout)
        viewModel.onConfigurationChanged(resources.configuration)
        viewModel.onUpdateOrientation(requireContext().display.rotation)
        progressLottieLayout = view.requireViewById(AR.id.progress_lottie)
        illustrationLottie = view.requireViewById(R.id.illustration_lottie)
        helpAnimation = SfpsAnimation.enrollShakeAnimation(progressLottieLayout)
        progressBar = view.requireViewById(AR.id.fingerprint_progress_bar)
        progressBar.progressBackgroundTintMode = PorterDuff.Mode.SRC
        var animator: ObjectAnimator? = null
        val fastOutSlowIn =
            AnimationUtils.loadInterpolator(context, android.R.interpolator.fast_out_slow_in)
        if (useExpressStyle) {
            setupEnrollIllustrationAnim(illustrationLottie)
            val color =
                requireContext().getColor(AR.color.sfps_enrollment_progress_bar_bg_color_expressive)
            progressBar.progressBackgroundTintList = ColorStateList.valueOf(color)
        }
        if (isDesktopFingerprintSensorLocationEnabled()) {
            progressBar.background =
                ContextCompat.getDrawable(requireContext(), AR.drawable.fp_illustration)
            illustrationLottie.visibility = View.GONE
            val background = progressBar.background as LayerDrawable
            desktopProgressBarBackgroundAnimationDrawable =
                background.findDrawableByLayerId(AR.id.fingerprint_animation)
                    as AnimatedVectorDrawable
            desktopProgressBarBackgroundBlinksDrawable =
                background.findDrawableByLayerId(AR.id.fingerprint_background)
                    as AnimatedVectorDrawable
        }
        glifLayout.getMixin(DescriptionMixin::class.java).textView.visibility = View.GONE
        val headerMixin = glifLayout.getMixin(HeaderMixin::class.java)
        applyDynamicColor(illustrationLottie, requireContext())
        setupSecondaryButton(glifLayout.getMixin(FooterBarMixin::class.java))
        viewModel.readyForEnrollment()

        val progressAnimationListener =
            object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (!enrollSuccessfully || progressBar.progress < PROGRESS_BAR_MAX) {
                        return
                    }
                    Log.d(TAG, "progressAnim#End: will leave in 250 ms")
                    viewModel.finishEnrollment()
                    progressBar.postDelayed(
                        {
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
                        },
                        LEAVE_DELAY_MS,
                    )
                }
            }
        val lottieAnimationListener =
            object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    updateProgressBarContentDescription()
                }

                override fun onAnimationPause(animation: Animator) {
                    updateProgressBarContentDescription()
                }

                override fun onAnimationResume(animation: Animator) {
                    updateProgressBarContentDescription()
                }
            }

        viewLifecycleOwner.lifecycleScope.launch {
            launch {
                val lastProgress = viewModel.lastProgress.value
                if (lastProgress != null) {
                    animator?.cancel()
                    applyDynamicColor(progressBar, requireContext(), false)
                    animator =
                        animateProgressBar(
                            getProgressInNumber(lastProgress),
                            0L,
                            fastOutSlowIn,
                            progressAnimationListener,
                        )
                }
                illustrationLottie.addAnimatorListener(lottieAnimationListener)
                illustrationLottie.addAnimatorPauseListener(lottieAnimationListener)
            }
            launch {
                addProgressLottieTouchListener(progressLottieLayout)
                progressLottieLayout.setOnClickListener {
                    if (illustrationLottie.isAnimating) {
                        illustrationLottie.pauseAnimation()
                    } else {
                        illustrationLottie.resumeAnimation()
                    }
                    val drawable = desktopProgressBarBackgroundAnimationDrawable
                    if (drawable != null) {
                        if (desktopProgressBarBackgroundAnimationCanceled) {
                            drawable.start()
                        } else {
                            drawable.stop()
                        }
                        desktopProgressBarBackgroundAnimationCanceled =
                            !desktopProgressBarBackgroundAnimationCanceled
                    }
                }
            }
            launch { viewModel.shouldUseMaxSizeText.collect { adjustText(headerMixin, it) } }
            launch {
                viewModel.isTalkbackEnabled.collect {
                    viewModel.interruptAccessibility()
                    updateProgressBarContentDescription()
                }
            }
            launch {
                viewModel.enrollStageLottie.collect { stage ->
                    Log.d(TAG, "Collecting stage $stage")
                    if (!isDesktopFingerprintSensorLocationEnabled()) {
                        playLottie(illustrationLottie, requireContext(), stage)
                    }
                }
            }
            launch {
                viewModel.helpActual.collect { help ->
                    if (help is FingerEnrollState.EnrollHelp) {
                        applyDynamicColor(progressBar, requireContext(), true)
                        if (!helpAnimation.isRunning) {
                            helpAnimation.start()
                        }
                        if (
                            help.helpMsgId == FINGERPRINT_ACQUIRED_IMMOBILE ||
                                help.helpMsgId == FINGERPRINT_ACQUIRED_VENDOR_BASE
                        ) {
                            return@collect
                        }
                        glifUseCase.setHeaderText(requireActivity(), help.helpString)
                    } else if (help is SfpsStageModel) {
                        setHeaderText(glifUseCase, requireActivity(), help)
                    }
                }
            }
            launch {
                viewModel.enrollProgress.collect { progress ->
                    animator?.cancel()
                    enrollSuccessfully = progress.remainingSteps == 0
                    applyDynamicColor(progressBar, requireContext(), false)
                    val progressInNumber = getProgressInNumber(progress)
                    animator =
                        animateProgressBar(
                            progressInNumber,
                            PROGRESS_ANIMATION_MS,
                            fastOutSlowIn,
                            progressAnimationListener,
                        )
                    desktopProgressBarBackgroundBlinksDrawable?.start()
                    announceProgressWhenNecessary(progressInNumber)
                    updateProgressBarContentDescription(progressInNumber)
                }
            }
            launch {
                viewModel.enrollError.collect { error ->
                    viewModel.finishEnrollment()
                    val iconTouchDialog = parentFragmentManager.findFragmentByTag(ICON_TOUCH_DIALOG)
                    if (
                        iconTouchDialog is FingerprintEnrollEnrolling.IconTouchDialog &&
                            iconTouchDialog.isResumed
                    ) {
                        iconTouchDialog.dismiss()
                    }
                    if (activity?.isFinishing ?: true) {
                        return@collect
                    }
                    val dialog =
                        FingerprintErrorDialog.newInstance(
                            error.errorId,
                            viewModel.isSuw,
                            error.errorId == FINGERPRINT_ERROR_UNABLE_TO_PROCESS,
                        )
                    if (parentFragmentManager.findFragmentByTag(ERROR_DIALOG) == null) {
                        dialog.show(parentFragmentManager, ERROR_DIALOG)
                    }
                }
            }
            launch {
                viewModel.enrollGoodAcquired
                    .filter { acquired ->
                        val remainingSteps = viewModel.lastProgress.value?.remainingSteps ?: -1
                        acquired.acquiredGood && remainingSteps == 1
                    }
                    .onEach { Log.d(TAG, "enrollGoodAcquired: it=$it") }
                    .collect {
                        animator?.cancel()
                        applyDynamicColor(progressBar, requireContext(), false)
                        animator =
                            animateProgressBar(
                                PROGRESS_BAR_MAX,
                                PROGRESS_ANIMATION_MS,
                                fastOutSlowIn,
                                progressAnimationListener,
                            )
                        announceProgressWhenNecessary(PROGRESS_BAR_MAX)
                    }
            }
            launch { listenToTryAgainDialog() }
            launch { viewModel.shouldVibrateForError.collect { viewModel.doVibrate() } }
            launch {
                viewModel.enrollHelp
                    .filter { it.helpMsgId == FINGERPRINT_ACQUIRED_IMMOBILE }
                    .collect { help ->
                        if (!showPauseEnrollmentDialogIfNecessary(help.helpMsgId)) {
                            glifUseCase.setHeaderText(requireActivity(), help.helpString)
                        } else {
                            glifUseCase.setHeaderText(
                                requireActivity(),
                                getVendorString(requireContext(), 0) ?: help.helpString,
                            )
                        }
                    }
            }
            launch {
                viewModel.enrollOperationState.collect {
                    if (it == EnrollOperationModel.Idle) {
                        illustrationLottie.cancelAnimation()
                    }
                }
            }
        }
    }

    private fun announceProgressWhenNecessary(progress: Int) {
        if (viewModel.isTalkbackEnabled.value) {
            val announcement =
                requireActivity()
                    .getString(
                        AR.string.security_settings_sfps_animation_a11y_label,
                        getProgressInPercentage(progress),
                    )
            glifLayout.accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
            glifLayout.contentDescription = announcement
        }
    }

    private fun updateProgressBarContentDescription() {
        if (viewModel.isTalkbackEnabled.value) {
            updateProgressBarContentDescription(
                viewModel.lastProgress.value?.let { getProgressInNumber(it) } ?: 0
            )
        }
    }

    private fun updateProgressBarContentDescription(progress: Int) {
        if (viewModel.isTalkbackEnabled.value) {
            val progressLabel =
                requireActivity()
                    .getString(
                        AR.string.security_settings_sfps_animation_a11y_label,
                        getProgressInPercentage(progress),
                    )
            progressLottieLayout.contentDescription =
                requireActivity()
                    .getString(
                        R.string.security_settings_fps_enroll_progress_animation_a11y_label,
                        progressLabel,
                        requireActivity()
                            .getString(
                                if (illustrationLottie.isAnimating) {
                                    AR.string.pause_animation
                                } else {
                                    AR.string.resume_animation
                                }
                            ),
                    )
        }
    }

    private fun getProgressInNumber(progress: FingerEnrollState.EnrollProgress): Int =
        (progress.totalStepsRequired - progress.remainingSteps) * PROGRESS_BAR_MAX /
            progress.totalStepsRequired

    private fun animateProgressBar(
        progress: Int,
        duration: Long,
        interpolator: Interpolator,
        listener: Animator.AnimatorListener,
    ): ObjectAnimator =
        ObjectAnimator.ofInt(progressBar, "progress", progressBar.progress, progress).apply {
            this.interpolator = interpolator
            addListener(listener)
            this.duration = duration
            start()
        }

    override fun onStart() {
        super.onStart()
        if (
            !shouldBackToPreviousPageWhenRestart &&
                viewModel.enrollOperationState.value == EnrollOperationModel.Running
        ) {
            metricsViewModel.setScreen(OnboardingScreen.SCREEN_ENROLLING)
        } else {
            if (!viewModel.isSuw) {
                return
            }
            viewLifecycleOwner.lifecycleScope.launch { findNavController().popBackStack() }
        }
        desktopProgressBarBackgroundAnimationDrawable?.let {
            desktopProgressBarBackgroundAnimationCanceled = false
            it.start()
            it.registerAnimationCallback(desktopProgressBarBackgroundAnimationCallback)
        }
    }

    override fun onStop() {
        super.onStop()
        if (!requireActivity().isChangingConfigurations) {
            clearViewModelStore()
            viewModel.finishEnrollment()
            shouldBackToPreviousPageWhenRestart = true
        }
        desktopProgressBarBackgroundAnimationDrawable?.let {
            desktopProgressBarBackgroundAnimationCanceled = true
            it.stop()
            it.unregisterAnimationCallback(desktopProgressBarBackgroundAnimationCallback)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_BACK_WHEN_RESTART, shouldBackToPreviousPageWhenRestart)
        outState.putInt(KEY_IMMOBILE_DIALOG_COUNT, immobileFpsCount)
    }

    private fun clearViewModelStore() {
        viewModelStore.clear()
    }

    private fun adjustText(headerMixin: HeaderMixin, useMaxSizeText: Boolean) {
        headerMixin.textView.hyphenationFrequency = 0
        if (useMaxSizeText) {
            headerMixin.setAutoTextSizeEnabled(true)
            headerMixin.textView.minLines = 0
            headerMixin.textView.maxLines = 10
        } else {
            headerMixin.setAutoTextSizeEnabled(false)
            headerMixin.textView.setLines(4)
        }
    }

    private fun addProgressLottieTouchListener(layout: RelativeLayout) {
        layout.setOnTouchListener { view, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    iconTouchCount++
                    if (iconTouchCount == ICON_TOUCH_COUNT_SHOW_DIALOG) {
                        showIconTouchDialog()
                    } else {
                        progressBar.postDelayed(mShowDialogRunnable, ICON_TOUCH_DIALOG_DELAY_MS)
                    }
                }
                MotionEvent.ACTION_UP -> {
                    progressBar.removeCallbacks(mShowDialogRunnable)
                    view.performClick()
                }
                MotionEvent.ACTION_CANCEL -> progressBar.removeCallbacks(mShowDialogRunnable)
            }
            true
        }
    }

    private fun setHeaderText(
        glifLayoutUseCase: GlifLayoutUseCase,
        activity: Activity,
        stage: SfpsStageModel,
    ) {
        glifLayoutUseCase.setHeaderText(
            activity,
            when (stage) {
                SfpsStageModel.Unknown,
                SfpsStageModel.Center ->
                    R.string.security_settings_sfps_enroll_finger_center_title_immobile_overlay
                SfpsStageModel.Fingertip ->
                    R.string.security_settings_sfps_enroll_fingertip_title_immobile_overlay
                SfpsStageModel.LeftEdge ->
                    R.string.security_settings_sfps_enroll_left_edge_title_immobile_overlay
                SfpsStageModel.NoAnimation ->
                    R.string.security_settings_fingerprint_enroll_repeat_title_immobile_overlay
                SfpsStageModel.RightEdge ->
                    R.string.security_settings_sfps_enroll_right_edge_title_immobile_overlay
            },
        )
    }

    private fun playLottie(
        lottieView: LottieAnimationView,
        context: Context,
        stage: SfpsStageModel,
    ) {
        val lottieResId =
            if (useExpressStyle) {
                when (stage) {
                    SfpsStageModel.Unknown,
                    SfpsStageModel.Center,
                    SfpsStageModel.Fingertip,
                    SfpsStageModel.LeftEdge,
                    SfpsStageModel.NoAnimation,
                    SfpsStageModel.RightEdge -> R.raw.sfps_lift_then_touch_lottie_expressive
                }
            } else {
                when (stage) {
                    SfpsStageModel.Unknown,
                    SfpsStageModel.Center,
                    SfpsStageModel.Fingertip,
                    SfpsStageModel.LeftEdge,
                    SfpsStageModel.NoAnimation -> R.raw.sfps_lift_then_touch_lottie
                    SfpsStageModel.RightEdge -> R.raw.sfps_reposition_finger_right_lottie
                }
            }
        LottieCompositionFactory.fromRawRes(context, lottieResId).addListener { composition ->
            if (composition == null) {
                Log.e(TAG, "Failed to load lottie for : $stage")
                return@addListener
            }
            lottieView.setComposition(composition)
            lottieView.visibility = View.VISIBLE
            lottieView.playAnimation()
        }
    }

    private fun applyDynamicColor(lottieView: LottieAnimationView, context: Context) {
        val color =
            context.getColor(
                if (useExpressStyle) {
                    AR.color.sfps_enrollment_fp_captured_color_expressive
                } else {
                    AR.color.sfps_enrollment_fp_captured_color
                }
            )
        lottieView.addValueCallback(KeyPath(".blue100", "**"), LottieProperty.COLOR_FILTER) {
            PorterDuffColorFilter(color, PorterDuff.Mode.SRC_ATOP)
        }
        lottieView.invalidate()
    }

    private fun applyDynamicColor(progressBar: ProgressBar, context: Context, isError: Boolean) {
        val errorColor =
            context.getColor(
                if (useExpressStyle) {
                    AR.color.sfps_enrollment_progress_bar_error_color_expressive
                } else {
                    AR.color.sfps_enrollment_progress_bar_error_color
                }
            )
        val fillColor =
            context.getColor(
                if (useExpressStyle) {
                    AR.color.sfps_enrollment_progress_bar_fill_color_expressive
                } else {
                    AR.color.sfps_enrollment_progress_bar_fill_color
                }
            )
        progressBar.progressTintList =
            ColorStateList.valueOf(if (isError) errorColor else fillColor)
        progressBar.progressTintMode = PorterDuff.Mode.SRC
        progressBar.invalidate()
    }

    private fun showIconTouchDialog() {
        iconTouchCount = 0
        FingerprintTouchDialog().show(parentFragmentManager, ICON_TOUCH_DIALOG)
    }

    private fun showPauseEnrollmentDialogIfNecessary(helpMsgId: Int): Boolean {
        if (helpMsgId != FINGERPRINT_ACQUIRED_IMMOBILE) {
            return false
        }
        immobileFpsCount++
        if (immobileFpsCount != pauseEnrollThreshold) {
            return false
        }
        if (parentFragmentManager.findFragmentByTag(IMMOBILE_DIALOG) != null) {
            Log.d(TAG, "ignoring showing up pause enrollment dialog as one is already being shown.")
            return false
        }
        Log.d(TAG, "Immobile count=$immobileFpsCount, show dialog")
        ImmobileHelpDialog().show(parentFragmentManager, IMMOBILE_DIALOG)
        return true
    }

    private fun getVendorString(context: Context, vendorCode: Int): String? {
        if (vendorCode < 0) {
            return null
        }
        val vendorStrings = context.resources.getStringArray(R.array.fingerprint_acquired_vendor)
        return if (vendorCode < vendorStrings.size) vendorStrings[vendorCode] else null
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

    private fun listenToTryAgainDialog() {
        setFragmentResultListener(FingerprintErrorDialog.TRY_AGAIN_LISTENER) { _, _ ->
            viewLifecycleOwner.lifecycleScope.launch {
                findNavController().popBackStack()
                findNavController()
                    .navigate(
                        R.id.action_find_sensor_to_enroll,
                        null,
                        NavOptionsUseCase.newNavOptions(),
                    )
            }
        }
        setFragmentResultListener(FingerprintErrorDialog.RESULT_LISTENER) { _, bundle ->
            val errorMsgId = bundle.getInt(FingerprintErrorDialog.KEY_MESSAGE_ID)
            if (bundle.getBoolean(FingerprintErrorDialog.KEY_WAS_BACK_PRESSED)) {
                parentFragmentManager.popBackStack()
                return@setFragmentResultListener
            }
            metricsViewModel.setErrorCode(errorMsgId)
            val result =
                if (errorMsgId == FINGERPRINT_ERROR_TIMEOUT) {
                    FingerprintEnrollResult.FIND_SENSOR_ERROR_TIMEOUT
                } else {
                    FingerprintEnrollResult.FIND_SENSOR_ERROR_FINISH
                }
            viewLifecycleOwner.lifecycleScope.launch { setEnrollResultViewModel.emit(result) }
        }
    }

    private fun setupEnrollIllustrationAnim(lottieView: LottieAnimationView) {
        val colors =
            requireContext().resources.getStringArray(R.array.sfps_enroll_illustration).toList()
        LottieAnimationHelper.get().applyColor(requireContext(), lottieView, colors)
    }

    companion object {
        private const val TAG = "EnrollSfpsFragment"
        private const val KEY_BACK_WHEN_RESTART = "bundle_key_back_when_restart"
        private const val KEY_IMMOBILE_DIALOG_COUNT = "immobile_dialog_count_state"
        private const val ICON_TOUCH_DIALOG = "icon_touch_dialog"
        private const val ERROR_DIALOG = "ERROR_DIALOG"
        private const val IMMOBILE_DIALOG = "IMMOBILE_DIALOG"
        private const val PROGRESS_BAR_MAX = 10000
        private const val PROGRESS_ANIMATION_MS = 500L
        private const val LEAVE_DELAY_MS = 250L
        private const val ICON_TOUCH_COUNT_SHOW_DIALOG = 3
        private const val ICON_TOUCH_DIALOG_DELAY_MS = 500L
    }
}
