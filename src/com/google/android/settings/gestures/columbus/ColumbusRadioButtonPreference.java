package com.google.android.settings.gestures.columbus;

import android.content.Context;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.preference.PreferenceViewHolder;

import com.android.settingslib.widget.SelectorWithWidgetPreference;

import com.google.android.settings.R;

public class ColumbusRadioButtonPreference extends SelectorWithWidgetPreference {
    private final int mIconSize;
    private ContextualSummaryProvider mContextualSummaryProvider;
    private ImageView mExtraWidgetView;
    private int mMetric;

    public interface ContextualSummaryProvider {
        CharSequence getSummary(Context context);
    }

    public ColumbusRadioButtonPreference(Context context) {
        super(context);
        mIconSize =
                context.getResources()
                        .getDimensionPixelSize(
                                com.android.settingslib.widget.theme.R.dimen
                                        .secondary_app_icon_size);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        mExtraWidgetView =
                (ImageView)
                        holder.findViewById(
                                com.android.settingslib.widget.preference.selector.R.id
                                        .selector_extra_widget);
        ImageView icon = (ImageView) holder.findViewById(android.R.id.icon);
        if (icon != null) {
            makeIconFixedSize(icon);
        }
        updateAccessibilityDescription();
    }

    @Override
    public void setTitle(CharSequence title) {
        super.setTitle(title);
        updateAccessibilityDescription();
    }

    void setMetric(int metric) {
        mMetric = metric;
    }

    int getMetric() {
        return mMetric;
    }

    void setContextualSummaryProvider(ContextualSummaryProvider provider) {
        mContextualSummaryProvider = provider;
    }

    void updateSummary(Context context) {
        setSummary(
                mContextualSummaryProvider == null
                        ? null
                        : mContextualSummaryProvider.getSummary(context));
    }

    private void makeIconFixedSize(ImageView icon) {
        ViewGroup.LayoutParams params = icon.getLayoutParams();
        if (params != null) {
            params.width = mIconSize;
            params.height = mIconSize;
        }
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
    }

    private void updateAccessibilityDescription() {
        if (mExtraWidgetView != null) {
            mExtraWidgetView.setContentDescription(
                    getContext()
                            .getString(
                                    R.string.columbus_radio_button_extra_widget_a11y_label,
                                    getTitle()));
        }
    }
}
