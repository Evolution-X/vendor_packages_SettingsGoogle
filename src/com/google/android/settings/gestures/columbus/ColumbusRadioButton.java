package com.google.android.settings.gestures.columbus;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.RadioButton;

import com.google.android.settings.R;

/** Radio button carrying the Secure setting value it selects. */
public class ColumbusRadioButton extends RadioButton {
    private String mSecureValue;

    public ColumbusRadioButton(Context context) {
        super(context);
    }

    public ColumbusRadioButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        mSecureValue = readSecureValue(context, attrs, 0, 0);
    }

    public ColumbusRadioButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mSecureValue = readSecureValue(context, attrs, defStyleAttr, 0);
    }

    public ColumbusRadioButton(
            Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        mSecureValue = readSecureValue(context, attrs, defStyleAttr, defStyleRes);
    }

    private static String readSecureValue(
            Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        TypedArray a =
                context.getTheme()
                        .obtainStyledAttributes(
                                attrs, R.styleable.ColumbusRadioButton, defStyleAttr, defStyleRes);
        String secureValue = a.getString(R.styleable.ColumbusRadioButton_secureValue);
        a.recycle();
        return secureValue;
    }

    String getSecureValue() {
        if (mSecureValue == null) {
            throw new IllegalStateException("Secure value was never set");
        }
        return mSecureValue;
    }

    void setSecureValue(String secureValue) {
        mSecureValue = secureValue;
    }
}
