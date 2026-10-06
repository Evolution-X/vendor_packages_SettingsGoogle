package com.google.android.settings.biometrics.ui.view

import android.animation.Animator
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.airbnb.lottie.LottieAnimationView
import com.android.settingslib.widget.preference.illustration.R

class LottiePlayPauseUseCase(private val lottie: LottieAnimationView) {

    private val accessibilityUpdater =
        object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {}

            override fun onAnimationEnd(animation: Animator) {
                forceConfigureAccessibilityDelegate(false)
            }

            override fun onAnimationCancel(animation: Animator) {}

            override fun onAnimationRepeat(animation: Animator) {}
        }

    fun startAnimationAndSetupAccessibility() {
        lottie.addAnimatorListener(accessibilityUpdater)
        configureAccessibilityDelegate(true)
        lottie.playAnimation()
        lottie.setOnClickListener {
            if (lottie.isAnimating) {
                forceConfigureAccessibilityDelegate(false)
                lottie.pauseAnimation()
            } else {
                forceConfigureAccessibilityDelegate(true)
                lottie.playAnimation()
            }
        }
    }

    fun clearResources() {
        lottie.removeAnimatorListener(accessibilityUpdater)
        lottie.accessibilityDelegate = null
        lottie.setOnClickListener(null)
    }

    fun forceConfigureAccessibilityDelegate(isPlaying: Boolean) {
        configureAccessibilityDelegate(isPlaying)
        lottie.sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED)
    }

    private fun configureAccessibilityDelegate(isPlaying: Boolean) {
        lottie.accessibilityDelegate =
            object : View.AccessibilityDelegate() {
                override fun onInitializeAccessibilityNodeInfo(
                    host: View,
                    info: AccessibilityNodeInfo,
                ) {
                    super.onInitializeAccessibilityNodeInfo(host, info)
                    info.className = null
                    info.addAction(
                        AccessibilityNodeInfo.AccessibilityAction(
                            AccessibilityNodeInfo.ACTION_CLICK,
                            lottie.context.getString(
                                if (isPlaying) {
                                    R.string.settingslib_action_label_pause
                                } else {
                                    R.string.settingslib_action_label_resume
                                }
                            ),
                        )
                    )
                }
            }
    }
}
