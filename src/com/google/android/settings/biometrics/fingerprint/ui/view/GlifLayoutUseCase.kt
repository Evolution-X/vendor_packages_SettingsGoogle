package com.google.android.settings.biometrics.fingerprint.ui.view

import android.app.Activity
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RelativeLayout
import com.google.android.settings.R
import com.google.android.setupcompat.template.FooterBarMixin
import com.google.android.setupcompat.template.FooterButton
import com.google.android.setupdesign.GlifLayout

class GlifLayoutUseCase(private val glifLayout: GlifLayout) {

    fun setHeaderText(activity: Activity, headerResId: Int) {
        setHeaderText(activity, activity.getText(headerResId))
    }

    fun setHeaderText(activity: Activity, headerText: CharSequence) {
        val headerTextView = glifLayout.getHeaderTextView()
        val text = headerTextView.text
        headerTextView.hyphenationFrequency = 0
        if (text !== headerText) {
            if (!TextUtils.isEmpty(text)) {
                headerTextView.accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
            }
            glifLayout.setHeaderText(headerText)
            glifLayout.getHeaderTextView().contentDescription = headerText
            activity.setTitle(headerText)
        }
    }

    fun setDescriptionText(descriptionText: CharSequence?) {
        if (TextUtils.equals(glifLayout.getDescriptionText(), descriptionText)) {
            return
        }
        glifLayout.descriptionText = descriptionText
    }

    fun addExtraFooter(
        layoutInflater: LayoutInflater,
        rotation: Int,
        threeButtonsData: ThreeButtonsData,
    ): View? {
        val isPortrait = rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180
        val contentView =
            if (isPortrait) {
                glifLayout.findViewById(com.google.android.setupdesign.R.id.sud_scroll_view)
                    ?: return null
            } else {
                glifLayout
                    .findViewById<View>(
                        com.google.android.setupdesign.R.id.sud_landscape_content_area
                    )
                    ?.parent as View? ?: return null
            }
        val parent = contentView.parent
        if (parent !is LinearLayout || parent.orientation != LinearLayout.VERTICAL) {
            return null
        }
        val contentLayoutParams = contentView.layoutParams as LinearLayout.LayoutParams
        contentLayoutParams.weight = 1.0f
        contentLayoutParams.height = 0
        contentView.layoutParams = contentLayoutParams

        val footer =
            layoutInflater.inflate(
                if (rotation == Surface.ROTATION_0) {
                    R.layout.gliflayout_extra_footer
                } else {
                    R.layout.gliflayout_landscape_extra_footer
                },
                parent,
                false,
            )
        footer.requireViewById<Button>(R.id.button1).apply {
            setText(threeButtonsData.button1TextId)
            setOnClickListener(threeButtonsData.onButton1ClickListener)
        }
        footer.requireViewById<Button>(R.id.button2).apply {
            setText(threeButtonsData.button2TextId)
            setOnClickListener(threeButtonsData.onButton2ClickListener)
        }
        footer.requireViewById<Button>(R.id.button3).apply {
            setText(threeButtonsData.button3TextId)
            setOnClickListener(threeButtonsData.onButton3ClickListener)
        }
        parent.addView(
            footer,
            parent.indexOfChild(contentView) + 1,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ),
        )

        if (!isPortrait) {
            val landscapeContentArea =
                glifLayout.findViewById<View>(
                    com.google.android.setupdesign.R.id.sud_landscape_content_area
                )
            val endFooter = footer.findViewById<View>(R.id.end_footer)
            if (landscapeContentArea != null && endFooter != null) {
                landscapeContentArea.viewTreeObserver.addOnGlobalLayoutListener(
                    object : ViewTreeObserver.OnGlobalLayoutListener {
                        override fun onGlobalLayout() {
                            if (landscapeContentArea.width <= 0) {
                                return
                            }
                            if (
                                landscapeContentArea.isAttachedToWindow &&
                                    landscapeContentArea.width != endFooter.width
                            ) {
                                val layoutParams =
                                    endFooter.layoutParams as RelativeLayout.LayoutParams
                                layoutParams.width = landscapeContentArea.width
                                endFooter.layoutParams = layoutParams
                            }
                            landscapeContentArea.viewTreeObserver.removeOnGlobalLayoutListener(this)
                        }
                    }
                )
            }
        }
        Log.d(TAG, "Extra footer added")
        return footer
    }

    fun applyThreeButtonsInFooter(threeButtonsData: ThreeButtonsData) {
        val footerBarMixin = glifLayout.getMixin(FooterBarMixin::class.java)
        footerBarMixin.primaryButton =
            FooterButton.Builder(glifLayout.context)
                .setText(threeButtonsData.button1TextId)
                .setButtonType(FooterButton.ButtonType.NEXT)
                .setTheme(com.google.android.setupdesign.R.style.SudGlifButton_Primary)
                .build()
                .apply { setOnClickListener(threeButtonsData.onButton1ClickListener) }
        footerBarMixin.secondaryButton =
            FooterButton.Builder(glifLayout.context)
                .setText(threeButtonsData.button3TextId)
                .setButtonType(FooterButton.ButtonType.SKIP)
                .setTheme(com.google.android.setupdesign.R.style.SudGlifButton_Secondary)
                .build()
                .apply { setOnClickListener(threeButtonsData.onButton3ClickListener) }
        footerBarMixin.setTertiaryButton(
            FooterButton.Builder(glifLayout.context)
                .setText(threeButtonsData.button2TextId)
                .setButtonType(FooterButton.ButtonType.SKIP)
                .setTheme(com.google.android.setupdesign.R.style.SudGlifButton_Secondary)
                .build()
                .apply { setOnClickListener(threeButtonsData.onButton2ClickListener) },
            false,
        )
    }

    data class ThreeButtonsData(
        val button1TextId: Int,
        val onButton1ClickListener: View.OnClickListener,
        val button2TextId: Int,
        val onButton2ClickListener: View.OnClickListener,
        val button3TextId: Int,
        val onButton3ClickListener: View.OnClickListener,
    )

    private companion object {
        const val TAG = "GlifLayoutUseCase"
    }
}
