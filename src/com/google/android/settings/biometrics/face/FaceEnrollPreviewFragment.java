package com.google.android.settings.biometrics.face;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.Size;
import android.util.TypedValue;
import android.view.Surface;
import android.view.TextureView;
import android.widget.ImageView;

import androidx.fragment.app.Fragment;

import com.google.android.settings.R;
import com.google.android.settings.biometrics.face.anim.FaceEnrollAnimationBase;
import com.google.android.settings.biometrics.face.anim.FaceEnrollAnimationMultiAngleDrawable;
import com.google.android.settings.biometrics.face.anim.FaceEnrollAnimationSingleCaptureDrawable;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

public class FaceEnrollPreviewFragment extends Fragment
        implements FaceEnrollSidecar.Listener, FaceEnrollSidecar.PreviewSurfaceProvider {
    private static final String TAG = "FaceEnroll/PreviewFragment";

    private FaceEnrollAnimationBase mAnimationDrawable;
    private CameraDevice mCameraDevice;
    private String mCameraId;
    private CameraManager mCameraManager;
    private CameraCaptureSession mCaptureSession;
    private ImageView mCircleView;
    private FaceEnrollAnimationBase.AnimationListener mClientAnimationListener;
    private boolean mFromSetupWizard;
    private CaptureRequest mPreviewRequest;
    private CaptureRequest.Builder mPreviewRequestBuilder;
    private Size mPreviewSize;
    private boolean mRequireDiversity;
    private boolean mShouldManagePreview;
    private SquareTextureView mTextureView;
    private boolean mTextureViewDestroyed;
    private Handler mHandler = new Handler();

    private final FaceEnrollAnimationBase.AnimationListener mLocalAnimationListener =
            new FaceEnrollAnimationBase.AnimationListener() {
                @Override
                public void onEnrollAnimationStarted() {
                    mClientAnimationListener.onEnrollAnimationStarted();
                }

                @Override
                public void onEnrollAnimationFinished() {
                    mClientAnimationListener.onEnrollAnimationFinished();
                    if (mShouldManagePreview) {
                        mHandler.post(FaceEnrollPreviewFragment.this::closeCamera);
                    }
                }

                @Override
                public void showHelp(CharSequence help) {
                    mClientAnimationListener.showHelp(help);
                }

                @Override
                public void clearHelp() {
                    mClientAnimationListener.clearHelp();
                }
            };

    private final TextureView.SurfaceTextureListener mSurfaceTextureListener =
            new TextureView.SurfaceTextureListener() {
                @Override
                public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {}

                @Override
                public void onSurfaceTextureAvailable(
                        SurfaceTexture surfaceTexture, int width, int height) {
                    mTextureViewDestroyed = false;
                    setUpPreview(width, height);
                }

                @Override
                public void onSurfaceTextureSizeChanged(
                        SurfaceTexture surfaceTexture, int width, int height) {
                    configureTransform(width, height);
                }

                @Override
                public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
                    mTextureViewDestroyed = true;
                    return true;
                }
            };

    private final CameraCaptureSession.CaptureCallback mCaptureCallback =
            new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureStarted(
                        CameraCaptureSession session,
                        CaptureRequest request,
                        long timestamp,
                        long frameNumber) {
                    super.onCaptureStarted(session, request, timestamp, frameNumber);
                    if (frameNumber == 1) {
                        mAnimationDrawable.onFirstFrameReceived();
                    }
                }
            };

    private final CameraDevice.StateCallback mCameraStateCallback =
            new CameraDevice.StateCallback() {
                @Override
                public void onOpened(CameraDevice cameraDevice) {
                    mCameraDevice = cameraDevice;
                    try {
                        if (mTextureViewDestroyed) {
                            Log.e(TAG, "Texture view destroyed but camera is open");
                        }
                        if (!mTextureView.isAvailable()) {
                            Log.e(TAG, "Error the surface texture was not attached to the window");
                        }
                        SurfaceTexture texture = mTextureView.getSurfaceTexture();
                        texture.setDefaultBufferSize(
                                mPreviewSize.getWidth(), mPreviewSize.getHeight());
                        Surface surface = new Surface(texture);
                        mPreviewRequestBuilder =
                                mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                        mPreviewRequestBuilder.set(
                                CaptureRequest.STATISTICS_FACE_DETECT_MODE,
                                CaptureRequest.STATISTICS_FACE_DETECT_MODE_OFF);
                        mPreviewRequestBuilder.addTarget(surface);
                        mCameraDevice.createCaptureSession(
                                Arrays.asList(surface),
                                new CameraCaptureSession.StateCallback() {
                                    @Override
                                    public void onConfigured(CameraCaptureSession session) {
                                        if (mCameraDevice == null) {
                                            return;
                                        }
                                        mCaptureSession = session;
                                        try {
                                            mPreviewRequestBuilder.set(
                                                    CaptureRequest.CONTROL_AF_MODE,
                                                    CaptureRequest
                                                            .CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                                            mPreviewRequest = mPreviewRequestBuilder.build();
                                            mCaptureSession.setRepeatingRequest(
                                                    mPreviewRequest, mCaptureCallback, mHandler);
                                        } catch (CameraAccessException e) {
                                            Log.e(TAG, "Unable to access camera", e);
                                        }
                                    }

                                    @Override
                                    public void onConfigureFailed(CameraCaptureSession session) {
                                        Log.e(TAG, "Unable to configure camera");
                                    }
                                },
                                null);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onDisconnected(CameraDevice cameraDevice) {
                    cameraDevice.close();
                    mCameraDevice = null;
                }

                @Override
                public void onError(CameraDevice cameraDevice, int error) {
                    cameraDevice.close();
                    mCameraDevice = null;
                }
            };

    public void setFromSetupWizard(boolean fromSetupWizard) {
        mFromSetupWizard = fromSetupWizard;
    }

    void setShouldManagePreview(boolean shouldManagePreview) {
        mShouldManagePreview = shouldManagePreview;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mTextureView = getActivity().findViewById(R.id.texture_view);
        mCircleView = getActivity().findViewById(R.id.circle_view);
        if (savedInstanceState != null) {
            mRequireDiversity = savedInstanceState.getBoolean("accessibility_diversity");
            mFromSetupWizard = savedInstanceState.getBoolean("is_suw");
            mShouldManagePreview = savedInstanceState.getBoolean("should_manage_preview");
        }
        if (mRequireDiversity) {
            mAnimationDrawable =
                    new FaceEnrollAnimationMultiAngleDrawable(
                            getContext(),
                            mLocalAnimationListener,
                            getActivity().findViewById(R.id.indicator_view),
                            getActivity().findViewById(R.id.distance_indicator_view),
                            mFromSetupWizard,
                            savedInstanceState);
        } else {
            mAnimationDrawable =
                    new FaceEnrollAnimationSingleCaptureDrawable(
                            getContext(),
                            mLocalAnimationListener,
                            getActivity().findViewById(R.id.distance_indicator_view),
                            mFromSetupWizard);
        }
        mCircleView.setImageDrawable(mAnimationDrawable);
        mCameraManager = (CameraManager) getContext().getSystemService(Context.CAMERA_SERVICE);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mTextureView.isAvailable()) {
            setUpPreview(mTextureView.getWidth(), mTextureView.getHeight());
        } else {
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mShouldManagePreview) {
            closeCamera();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("accessibility_diversity", mRequireDiversity);
        mAnimationDrawable.onSaveInstanceState(outState);
        outState.putBoolean("is_suw", mFromSetupWizard);
        outState.putBoolean("should_manage_preview", mShouldManagePreview);
    }

    @Override
    public void onEnrollmentError(int errMsgId, CharSequence errString) {
        if (mAnimationDrawable != null) {
            mAnimationDrawable.onEnrollmentError(errMsgId, errString);
        }
    }

    @Override
    public void onEnrollmentHelp(int helpMsgId, CharSequence helpString) {
        if (mAnimationDrawable != null) {
            mAnimationDrawable.onEnrollmentHelp(helpMsgId, helpString);
        }
    }

    @Override
    public void onEnrollmentProgressChange(int steps, int remaining) {
        if (mAnimationDrawable != null) {
            mAnimationDrawable.onEnrollmentProgressChange(steps, remaining);
        }
    }

    public void setAnimationListener(FaceEnrollAnimationBase.AnimationListener listener) {
        mClientAnimationListener = listener;
    }

    public void setAnimationDrawableMode(boolean requireDiversity) {
        mRequireDiversity = requireDiversity;
    }

    private void setUpCameraOutputs() {
        try {
            for (String cameraId : mCameraManager.getCameraIdList()) {
                CameraCharacteristics characteristics =
                        mCameraManager.getCameraCharacteristics(cameraId);
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing == null || facing != CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                }
                mCameraId = cameraId;
                StreamConfigurationMap map =
                        characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class));
                return;
            }
        } catch (CameraAccessException e) {
            Log.e(TAG, "Unable to access camera", e);
        }
    }

    private void setUpPreview(int width, int height) {
        try {
            setUpCameraOutputs();
            if (mShouldManagePreview) {
                mCameraManager.openCamera(mCameraId, mCameraStateCallback, mHandler);
            } else {
                mAnimationDrawable.onFirstFrameReceived();
            }
            configureTransform(width, height);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Unable to open camera", e);
        }
    }

    private Size chooseOptimalSize(Size[] choices) {
        return Collections.min(
                Arrays.asList(choices),
                new Comparator<Size>() {
                    @Override
                    public int compare(Size size1, Size size2) {
                        if (size1.getHeight() < 480 && size2.getHeight() >= 480) {
                            return 1;
                        }
                        if (size1.getHeight() >= 480 && size2.getHeight() < 480) {
                            return -1;
                        }
                        int ratioCompare =
                                Float.compare(
                                        Math.abs(
                                                (size1.getWidth() / (float) size1.getHeight())
                                                        - 1.3f),
                                        Math.abs(
                                                (size2.getWidth() / (float) size2.getHeight())
                                                        - 1.3f));
                        if (ratioCompare != 0) {
                            return ratioCompare;
                        }
                        return Integer.compare(size1.getHeight(), size2.getHeight());
                    }
                });
    }

    private void configureTransform(int viewWidth, int viewHeight) {
        if (mTextureView == null) {
            return;
        }
        float scaleWidth = viewWidth / (float) mPreviewSize.getWidth();
        float scaleHeight = viewHeight / (float) mPreviewSize.getHeight();
        float scaleFactor = Math.min(scaleWidth, scaleHeight);
        float scaledWidth = scaleWidth / scaleFactor;
        float scaledHeight = scaleHeight / scaleFactor;
        TypedValue scaleValue = new TypedValue();
        getResources().getValue(com.android.settings.R.dimen.face_preview_scale, scaleValue, true);
        Matrix matrix = new Matrix();
        mTextureView.getTransform(matrix);
        TypedValue translateXValue = new TypedValue();
        TypedValue translateYValue = new TypedValue();
        getResources().getValue(R.dimen.face_preview_translate_x, translateXValue, true);
        getResources().getValue(R.dimen.face_preview_translate_y, translateYValue, true);
        matrix.setScale(scaledWidth * scaleValue.getFloat(), scaledHeight * scaleValue.getFloat());
        matrix.postTranslate(translateXValue.getFloat(), translateYValue.getFloat());
        mTextureView.setTransform(matrix);
    }

    private void closeCamera() {
        if (mCaptureSession != null) {
            mCaptureSession.close();
            mCaptureSession = null;
        }
        if (mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
    }

    @Override
    public Surface getPreviewSurface() {
        if (mTextureViewDestroyed) {
            Log.e(TAG, "Failed to get the preview surface, the surface texture is destroyed.");
            return null;
        }
        SurfaceTexture texture = mTextureView.getSurfaceTexture();
        texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        return new Surface(texture);
    }
}
