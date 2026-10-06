package com.google.android.settings.biometrics.face;

import android.util.SparseIntArray;

public class Debouncer {
    private final int[] mDebounceWindow;
    private final SparseIntArray mStateConfiguration;

    public Debouncer(SparseIntArray stateConfiguration) {
        int maxWindow = 0;
        for (int i = 0; i < stateConfiguration.size(); i++) {
            if (stateConfiguration.valueAt(i) > maxWindow) {
                maxWindow = stateConfiguration.valueAt(i);
            }
        }
        mDebounceWindow = new int[maxWindow];
        mStateConfiguration = stateConfiguration;
    }

    public Debouncer(int windowLength) {
        mDebounceWindow = new int[windowLength];
        mStateConfiguration = null;
    }

    public void reset() {
        for (int i = 0; i < mDebounceWindow.length; i++) {
            mDebounceWindow[i] = 0;
        }
    }

    public void updateBuffer(int value) {
        for (int i = 1; i < mDebounceWindow.length; i++) {
            mDebounceWindow[i - 1] = mDebounceWindow[i];
        }
        mDebounceWindow[mDebounceWindow.length - 1] = value;
    }

    public boolean passesDebounce(int value) {
        int length =
                mStateConfiguration != null
                        ? mStateConfiguration.get(value, 0)
                        : mDebounceWindow.length;
        for (int i = mDebounceWindow.length - 1; i >= mDebounceWindow.length - length; i--) {
            if (mDebounceWindow[i] != value) {
                return false;
            }
        }
        return true;
    }
}
