package com.google.android.settings.biometrics.usudfps.ui.view

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.Matrix
import android.graphics.Outline
import android.graphics.SurfaceTexture
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.TextureView
import android.view.View
import android.view.ViewOutlineProvider
import android.view.ViewTreeObserver
import android.view.accessibility.AccessibilityEvent
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.android.settings.R as AR
import com.android.settings.biometrics.BiometricsOnboardingProto.OnboardingAction
import com.android.settingslib.qrcode.QrCamera
import com.google.android.settings.R
import com.google.android.settings.biometrics.fingerprint.ui.view.GlifLayoutUseCase
import com.google.android.settings.biometrics.fingerprint.ui.view.NavOptionsUseCase
import com.google.android.settings.biometrics.fingerprint.ui.viewmodel.AccessibilityViewModel
import com.google.android.settings.biometrics.fingerprint.ui.viewmodel.FingerprintMetricsViewModel
import com.google.android.settings.biometrics.usudfps.factory.UsudfpsViewModelFactory
import com.google.android.settings.biometrics.usudfps.ui.model.SpSetupResult
import com.google.android.settings.biometrics.usudfps.ui.viewmodel.SpQrCodeScannerViewModel
import com.google.android.settings.biometrics.usudfps.ui.viewmodel.SpSetupResultViewModel
import com.google.android.setupcompat.template.FooterBarMixin
import com.google.android.setupcompat.template.FooterButton
import com.google.android.setupdesign.GlifLayout
import kotlinx.coroutines.launch

class SpQrCodeScannerFragment :
    Fragment(R.layout.sp_qr_code_scanner),
    TextureView.SurfaceTextureListener,
    QrCamera.ScannerCallback {

    private val viewModel: SpQrCodeScannerViewModel by
        viewModels(extrasProducer = { requireActivity().defaultViewModelCreationExtras }) {
            defaultViewModelProviderFactory
        }
    private val setupResultViewModel: SpSetupResultViewModel by
        activityViewModels(extrasProducer = { requireActivity().defaultViewModelCreationExtras }) {
            defaultViewModelProviderFactory
        }
    private val metricsViewModel: FingerprintMetricsViewModel by activityViewModels()
    private val accessibilityViewModel: AccessibilityViewModel by activityViewModels()

    private val glifLayout: GlifLayout by lazy { view as GlifLayout }

    private val glifUseCase: GlifLayoutUseCase by lazy { GlifLayoutUseCase(glifLayout) }

    private val footerBar: FooterBarMixin by lazy {
        glifLayout.getMixin(FooterBarMixin::class.java)
    }

    private val previewViewContainer: LinearLayout by lazy {
        view!!.requireViewById(R.id.preview_view_container)
    }

    private val previewView: TextureView by lazy { view!!.requireViewById(AR.id.preview_view) }

    private val checkedImage: ImageView by lazy { view!!.requireViewById(R.id.checked) }

    private val errorMessage: TextView by lazy { view!!.requireViewById(AR.id.error_message) }

    private val scannerBgAccent: Drawable by lazy {
        ContextCompat.getDrawable(requireContext(), R.drawable.sp_qr_code_scanner_bg_accent)!!
    }

    private val scannerBgError: Drawable by lazy {
        ContextCompat.getDrawable(requireContext(), R.drawable.sp_qr_code_scanner_bg_error)!!
    }

    private var qrCamera: QrCamera? = null

    private var isPreviousQrCodeValid: Boolean? = null

    private val landContentAreaView: View
        get() = view!!.requireViewById<View>(R.id.constraint_layout).parent as View

    private val adjustLandContentHeightListener = ViewTreeObserver.OnGlobalLayoutListener {
        val contentAreaView = landContentAreaView
        val contentHeight = contentAreaView.height
        val sudScrollHeight =
            view!!.requireViewById<View>(com.google.android.setupdesign.R.id.sud_scroll_view).height
        if (sudScrollHeight <= 0 || contentHeight <= sudScrollHeight) {
            return@OnGlobalLayoutListener
        }
        val previewLayoutParams = previewViewContainer.layoutParams as ConstraintLayout.LayoutParams
        val minPreviewHeight =
            previewLayoutParams.matchConstraintMinHeight +
                previewLayoutParams.topMargin +
                previewLayoutParams.bottomMargin
        val errorLayoutParams = errorMessage.layoutParams as ConstraintLayout.LayoutParams
        if (
            minPreviewHeight +
                errorMessage.height +
                errorLayoutParams.topMargin +
                errorLayoutParams.bottomMargin > sudScrollHeight
        ) {
            if (previewViewContainer.height > previewLayoutParams.matchConstraintMinHeight) {
                Log.d(
                    TAG,
                    "Update preview height when contentHeight:$contentHeight," +
                        " sudScrollHeight:$sudScrollHeight",
                )
                previewLayoutParams.height = previewLayoutParams.matchConstraintMinHeight
                previewViewContainer.layoutParams = previewLayoutParams
                previewViewContainer.requestLayout()
            }
            return@OnGlobalLayoutListener
        }
        Log.d(
            TAG,
            "Update content height when contentHeight:$contentHeight," +
                " sudScrollHeight:$sudScrollHeight",
        )
        val contentLayoutParams = contentAreaView.layoutParams
        contentLayoutParams.height = sudScrollHeight
        contentAreaView.layoutParams = contentLayoutParams
        contentAreaView.requestLayout()
    }

    private val debugBroadcastReceiver: BroadcastReceiver by lazy {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val content = intent?.data?.toString() ?: ""
                if (isValid(content)) {
                    handleSuccessfulResult(content)
                }
            }
        }
    }

    private val onSetQrSpClickListener: View.OnClickListener by lazy {
        View.OnClickListener {
            val result = viewModel.setQrCodeScreenProtector()
            if (viewModel.isEnrolling) {
                Log.d(TAG, "set qr code screen protector result: $result and enroll")
                metricsViewModel.appendAction(OnboardingAction.ACTION_NEXT)
                startEnroll()
                return@OnClickListener
            }
            Log.d(TAG, "set qr code screen protector result: $result and finish")
            lifecycleScope.launch { setupResultViewModel.emit(SpSetupResult.SP_SUCCESS_SET) }
        }
    }

    private val onNoSpClickListener: View.OnClickListener by lazy {
        View.OnClickListener {
            if (viewModel.isEnrolling) {
                Log.d(TAG, "do not set default, start enroll directly")
                metricsViewModel.appendAction(OnboardingAction.ACTION_SKIP)
                startEnroll()
            } else {
                Log.d(TAG, "set result and finish")
                lifecycleScope.launch { setupResultViewModel.emit(SpSetupResult.SP_SKIP_BUTTON) }
            }
        }
    }

    private val _defaultViewModelProviderFactory: ViewModelProvider.Factory by lazy {
        UsudfpsViewModelFactory()
    }

    private fun trimSudHeaderBottomMargin() {
        val header =
            view?.findViewById<View>(com.google.android.setupdesign.R.id.sud_layout_header)
                ?: return
        val layoutParams = header.layoutParams
        if (layoutParams !is LinearLayout.LayoutParams || layoutParams.bottomMargin <= 0) {
            return
        }
        Log.d(TAG, "Update sudHeader bottomMargin from ${layoutParams.bottomMargin} to 0")
        layoutParams.bottomMargin = 0
        header.layoutParams = layoutParams
        header.requestLayout()
    }

    private fun startEnroll() {
        findNavController()
            .navigate(
                R.id.action_sp_qr_code_scanner_to_enrolling,
                null,
                NavOptionsUseCase.newBackToFindSensorNavOptions(),
            )
    }

    private fun configureCameraPreviewContainer() {
        previewViewContainer.outlineProvider =
            object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setRoundRect(
                        0,
                        0,
                        view.width,
                        view.height,
                        view.resources.getDimensionPixelSize(
                            R.dimen.sp_qr_code_scanner_corner_radius
                        ) + 6.0f,
                    )
                }
            }
        previewViewContainer.clipToOutline = true
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        glifUseCase.setHeaderText(requireActivity(), R.string.fingerprint_sp_title_scan_qr_code)
        glifUseCase.setDescriptionText(
            getString(R.string.fingerprint_sp_subtitle_position_code_in_camera)
        )
        previewView.surfaceTextureListener = this
        configureCameraPreviewContainer()
        viewLifecycleOwner.lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onStart(owner: LifecycleOwner) {
                    if (footerBar.secondaryButton == null) {
                        footerBar.secondaryButton =
                            FooterButton.Builder(requireContext())
                                .setText(AR.string.skip_label)
                                .setButtonType(FooterButton.ButtonType.SKIP)
                                .setTheme(
                                    com.google.android.setupdesign.R.style.SudGlifButton_Secondary
                                )
                                .build()
                                .apply { setOnClickListener(onNoSpClickListener) }
                    }
                    if (
                        resources.configuration.orientation != Configuration.ORIENTATION_LANDSCAPE
                    ) {
                        trimSudHeaderBottomMargin()
                    } else {
                        landContentAreaView.viewTreeObserver.addOnGlobalLayoutListener(
                            adjustLandContentHeightListener
                        )
                    }
                    super.onStart(owner)
                    metricsViewModel.skipNextCancelAction = true
                }

                override fun onResume(owner: LifecycleOwner) {
                    super.onResume(owner)
                    lifecycleScope.launch { restartCamera() }
                    if (Build.IS_DEBUGGABLE) {
                        ContextCompat.registerReceiver(
                            requireActivity(),
                            debugBroadcastReceiver,
                            IntentFilter(Intent.ACTION_VIEW).apply {
                                addDataScheme(DEBUG_SCHEME)
                                addDataAuthority(DEBUG_AUTHORITY, null)
                            },
                            ContextCompat.RECEIVER_EXPORTED,
                        )
                    }
                }

                override fun onPause(owner: LifecycleOwner) {
                    if (Build.IS_DEBUGGABLE) {
                        requireActivity().unregisterReceiver(debugBroadcastReceiver)
                    }
                    lifecycleScope.launch { qrCamera?.stop() }
                    super.onPause(owner)
                }

                override fun onStop(owner: LifecycleOwner) {
                    if (
                        resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
                    ) {
                        landContentAreaView.viewTreeObserver.removeOnGlobalLayoutListener(
                            adjustLandContentHeightListener
                        )
                    }
                    metricsViewModel.skipNextCancelAction = false
                    super.onStop(owner)
                }
            }
        )
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        initCamera(surface)
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        destroyCamera()
        return true
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}

    override fun getViewSize(): Size = Size(previewView.width, previewView.height)

    override fun setTransform(transform: Matrix) {
        activity?.runOnUiThread {
            view?.findViewById<TextureView>(AR.id.preview_view)?.setTransform(transform)
        }
    }

    override fun isValid(qrCode: String): Boolean {
        val isValid = setQrCodeAndCheckValid(qrCode)
        lifecycleScope.launch {
            if (isPreviousQrCodeValid != isValid) {
                if (isValid) {
                    previewViewContainer.foreground = scannerBgAccent
                    checkedImage.visibility = View.VISIBLE
                    errorMessage.visibility = View.INVISIBLE
                } else {
                    previewViewContainer.foreground = scannerBgError
                    checkedImage.visibility = View.INVISIBLE
                    errorMessage.visibility = View.VISIBLE
                    if (accessibilityViewModel.isAnyAccessibilityServiceEnabled.value) {
                        errorMessage.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED)
                    }
                }
            }
            isPreviousQrCodeValid = isValid
        }
        return isValid
    }

    private fun setQrCodeAndCheckValid(qrCode: String): Boolean {
        viewModel.setQrCodeContent(qrCode)
        val isValidFormat = viewModel.isQrCodeValidFormat()
        val isAllowed = viewModel.isQrCodeAllowed()
        Log.d(TAG, "check data, content:$qrCode, isValid:($isValidFormat, $isAllowed)")
        return isValidFormat && isAllowed
    }

    override fun handleSuccessfulResult(result: String) {
        if (!setQrCodeAndCheckValid(result)) {
            Log.e(TAG, "error qr code")
            return
        }
        footerBar.secondaryButton.visibility = View.GONE
        glifUseCase.setHeaderText(requireActivity(), R.string.fingerprint_sp_title_got_it)
        val productInfo = viewModel.qrCodeProductInfo!!
        glifUseCase.setDescriptionText(
            getString(R.string.fingerprint_sp_subtitle_apply_sp_combined, productInfo.modelName)
        )
        if (footerBar.primaryButton == null) {
            footerBar.primaryButton =
                FooterButton.Builder(requireContext())
                    .setText(AR.string.next_label)
                    .setButtonType(FooterButton.ButtonType.NEXT)
                    .setTheme(com.google.android.setupdesign.R.style.SudGlifButton_Primary)
                    .build()
                    .apply { setOnClickListener(onSetQrSpClickListener) }
        }
    }

    override fun handleCameraFailure() {
        destroyCamera()
    }

    private fun initCamera(surfaceTexture: SurfaceTexture) {
        if (qrCamera == null) {
            Log.d(TAG, "start camera, surface:$surfaceTexture")
            qrCamera = QrCamera(context, this).apply { start(surfaceTexture) }
        }
    }

    private fun destroyCamera() {
        qrCamera?.stop()
        qrCamera = null
    }

    private fun restartCamera() {
        val camera = qrCamera
        if (camera == null) {
            Log.d(TAG, "qrCamera is not available for restarting camera")
            return
        }
        if (camera.isDecodeTaskAlive) {
            camera.stop()
        }
        val surfaceTexture =
            checkNotNull(previewView.surfaceTexture) {
                "SurfaceTexture is not ready for restarting camera"
            }
        camera.start(surfaceTexture)
    }

    override val defaultViewModelProviderFactory: ViewModelProvider.Factory
        get() =
            activity?.defaultViewModelProviderFactory as? UsudfpsViewModelFactory
                ?: _defaultViewModelProviderFactory

    companion object {
        private const val TAG = "SpQrCodeScanner"
        private const val DEBUG_SCHEME = "mfg-sp"
        private const val DEBUG_AUTHORITY = "pixel"
    }
}
