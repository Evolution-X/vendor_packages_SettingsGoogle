package com.google.android.settings.biometrics.face.anim.curve;

import android.graphics.Path;

public class CellConfig {
    final boolean mFlipVertical;
    final Path mPath;
    final int mRotation;

    CellConfig(Path path, int i) {
        this(path, i, false);
    }

    CellConfig(Path path, int i, boolean z) {
        mPath = path;
        mRotation = i;
        mFlipVertical = z;
    }
}
