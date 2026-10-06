package com.google.android.settings.biometrics.face.anim;

import android.content.Context;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.widget.ImageView;

import com.google.android.settings.R;

public class FaceOutlineIndicatorController {
    private final Context mContext;
    private int mState = 0;
    private final ImageView mView;

    public FaceOutlineIndicatorController(Context context, ImageView imageView) {
        mContext = context;
        mView = imageView;
    }

    public void show() {
        if (mState == 1) {
            return;
        }
        AnimatedVectorDrawable animatedVectorDrawable =
                (AnimatedVectorDrawable) mContext.getDrawable(R.drawable.face_distance_fade_in);
        mView.setImageDrawable(animatedVectorDrawable);
        animatedVectorDrawable.start();
        mState = 1;
    }

    public void clear() {
        if (mState == 0) {
            return;
        }
        AnimatedVectorDrawable animatedVectorDrawable =
                (AnimatedVectorDrawable) mContext.getDrawable(R.drawable.face_distance_fade_out);
        mView.setImageDrawable(animatedVectorDrawable);
        animatedVectorDrawable.start();
        mState = 0;
    }
}
