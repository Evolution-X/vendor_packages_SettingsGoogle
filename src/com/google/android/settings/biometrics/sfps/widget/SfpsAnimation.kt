package com.google.android.settings.biometrics.sfps.widget

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.util.TypedValue
import android.view.View
import androidx.core.view.animation.PathInterpolatorCompat
import androidx.interpolator.view.animation.FastOutSlowInInterpolator

abstract class SfpsAnimation {
    companion object {
        fun enrollShakeAnimation(view: View): Animator {
            val dp =
                TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    10.0f,
                    view.context.resources.displayMetrics,
                )
            val left = -1.0f * dp
            val right = 2.0f * dp
            val first = ObjectAnimator.ofFloat(view, "translationX", left).setDuration(67L)
            val second = ObjectAnimator.ofFloat(view, "translationX", right).setDuration(100L)
            val third = ObjectAnimator.ofFloat(view, "translationX", -2.0f * dp).setDuration(100L)
            val fourth = ObjectAnimator.ofFloat(view, "translationX", right).setDuration(100L)
            val fifth = ObjectAnimator.ofFloat(view, "translationX", left).setDuration(150L)
            val interpolator = PathInterpolatorCompat.create(0.6f, 0.0f, 0.4f, 1.0f)
            first.interpolator = interpolator
            second.interpolator = interpolator
            third.interpolator = interpolator
            fourth.interpolator = interpolator
            fifth.interpolator = FastOutSlowInInterpolator()
            first.setAutoCancel(false)
            second.setAutoCancel(false)
            third.setAutoCancel(false)
            fourth.setAutoCancel(false)
            fifth.setAutoCancel(false)
            return AnimatorSet().apply { playSequentially(first, second, third, fourth, fifth) }
        }
    }
}
