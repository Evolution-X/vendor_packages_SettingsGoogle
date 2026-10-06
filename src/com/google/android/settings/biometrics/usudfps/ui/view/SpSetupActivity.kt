package com.google.android.settings.biometrics.usudfps.ui.view

import android.Manifest
import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.navigation.fragment.NavHostFragment
import com.android.settings.R as AR
import com.android.settings.biometrics.BiometricEnrollBase
import com.google.android.settings.R
import com.google.android.settings.biometrics.fingerprint.factory.FingerprintViewModelFactory
import com.google.android.settings.biometrics.fingerprint.ui.model.CredentialModelImpl
import com.google.android.settings.biometrics.fingerprint.ui.view.ThemeUseCase
import com.google.android.settings.biometrics.usudfps.factory.UsudfpsViewModelFactory
import com.google.android.settings.biometrics.usudfps.ui.model.SpSetupResult
import com.google.android.settings.biometrics.usudfps.ui.viewmodel.SpSetupResultViewModel
import com.google.android.settings.biometrics.usudfps.ui.viewmodel.SpSetupViewModel
import kotlinx.coroutines.launch

class SpSetupActivity : FragmentActivity(R.layout.nav_fragment_activity) {

    private val viewModel: SpSetupViewModel by viewModels()
    private val setupResultViewModel: SpSetupResultViewModel by viewModels()

    private val _defaultViewModelProviderFactory: ViewModelProvider.Factory by lazy {
        UsudfpsViewModelFactory()
    }

    override val defaultViewModelProviderFactory: ViewModelProvider.Factory =
        _defaultViewModelProviderFactory

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeUseCase(this).applyTheme()
        Log.d(TAG, "onCreate() savedInstance:${savedInstanceState != null}")
        if (!viewModel.isValidToEditSp()) {
            Log.d(TAG, "finish activity due to permission")
            setResult(RESULT_CANCELED)
            finish()
        }
        val navController =
            (supportFragmentManager.findFragmentById(R.id.nav_host_fragment)!! as NavHostFragment)
                .navController
        val navGraph = navController.navInflater.inflate(R.navigation.sp_setup)
        if (viewModel.shouldSkipIntroFragment) {
            navGraph.setStartDestination(R.id.sp_qr_code_scanner)
        }
        navController.graph = navGraph
        lifecycleScope.launch { setupResultViewModel.resultFlow.collect(::onSetupResult) }
        lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onStart(owner: LifecycleOwner) {
                    sendBroadcast(
                        Intent(ACTION_SETUP_START).setPackage(SYSTEMUI_PACKAGE),
                        Manifest.permission.USE_BIOMETRIC_INTERNAL,
                    )
                }

                override fun onPause(owner: LifecycleOwner) {
                    if (isFinishing || isChangingConfigurations) {
                        return
                    }
                    Log.d(TAG, "finish self during onPause")
                    setResult(BiometricEnrollBase.RESULT_TIMEOUT)
                    finish()
                }

                override fun onStop(owner: LifecycleOwner) {
                    if (isFinishing) {
                        sendBroadcast(
                            Intent(ACTION_SETUP_STOP).setPackage(SYSTEMUI_PACKAGE),
                            Manifest.permission.USE_BIOMETRIC_INTERNAL,
                        )
                    }
                }
            }
        )
    }

    private fun onSetupResult(result: SpSetupResult) {
        Log.d(TAG, "onSetupResult($result)")
        setResult(
            when (result) {
                SpSetupResult.SP_SKIP_BUTTON -> RESULT_CANCELED
                SpSetupResult.SP_SUCCESS_SET -> RESULT_OK
            }
        )
        finish()
    }

    override fun onApplyThemeResource(theme: Resources.Theme, resid: Int, first: Boolean) {
        theme.applyStyle(AR.style.SetupWizardPartnerResource, true)
        super.onApplyThemeResource(theme, resid, first)
    }

    override val defaultViewModelCreationExtras: CreationExtras
        get() =
            MutableCreationExtras(super.defaultViewModelCreationExtras).apply {
                set(
                    FingerprintViewModelFactory.CREDENTIAL_MODEL_KEY,
                    CredentialModelImpl(intent.extras, SystemClock.elapsedRealtimeClock()),
                )
            }

    companion object {
        private const val TAG = "SpSetupActivity"
        private const val SYSTEMUI_PACKAGE = "com.android.systemui"
        private const val ACTION_SETUP_START =
            "com.google.android.biometric.screenprotector.setup.start"
        private const val ACTION_SETUP_STOP =
            "com.google.android.biometric.screenprotector.setup.stop"
    }
}
