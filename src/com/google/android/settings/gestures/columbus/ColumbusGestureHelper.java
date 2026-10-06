package com.google.android.settings.gestures.columbus;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Log;

import com.google.android.systemui.columbus.IColumbusService;
import com.google.android.systemui.columbus.IColumbusServiceGestureListener;

/** Receives Quick Tap gestures from SystemUI while a Quick Tap settings page is showing. */
public class ColumbusGestureHelper {
    private static final String TAG = "ColumbusGestureHelper";

    private final Context mContext;
    private final IBinder mToken = new Binder();
    private boolean mBoundToService;
    private GestureListener mGestureListener;
    private IColumbusService mService;

    private final IColumbusServiceGestureListener mColumbusServiceGestureListener =
            new IColumbusServiceGestureListener.Stub() {
                @Override
                public void onTrigger() {
                    if (mGestureListener != null) {
                        mGestureListener.onTrigger();
                    }
                }
            };

    private final ServiceConnection mServiceConnection =
            new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    mService = IColumbusService.Stub.asInterface(service);
                    if (mGestureListener != null) {
                        try {
                            mService.registerGestureListener(
                                    mToken, mColumbusServiceGestureListener.asBinder());
                        } catch (RemoteException e) {
                            Log.e(TAG, "registerGestureListener()", e);
                        }
                    }
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {
                    mService = null;
                }
            };

    public interface GestureListener {
        void onTrigger();
    }

    public ColumbusGestureHelper(Context context) {
        mContext = context;
    }

    public void setListener(GestureListener listener) {
        mGestureListener = listener;
        if (mService == null) {
            Log.w(TAG, "Service is null, should try to reconnect");
            return;
        }
        try {
            mService.registerGestureListener(
                    mToken, listener != null ? mColumbusServiceGestureListener.asBinder() : null);
        } catch (RemoteException e) {
            Log.e(
                    TAG,
                    "Failed to " + (listener == null ? "unregister" : "register") + " listener",
                    e);
        }
    }

    public void bindToColumbusServiceProxy() {
        if (mService != null) {
            return;
        }
        try {
            Intent intent = new Intent();
            intent.setComponent(
                    new ComponentName(
                            "com.android.systemui",
                            "com.google.android.systemui.columbus.ColumbusServiceProxy"));
            mContext.bindServiceAsUser(
                    intent, mServiceConnection, Context.BIND_AUTO_CREATE, UserHandle.SYSTEM);
            mBoundToService = true;
        } catch (SecurityException e) {
            Log.e(TAG, "Unable to bind to ColumbusService", e);
        }
    }

    public void unbindFromColumbusServiceProxy() {
        if (mBoundToService) {
            mContext.unbindService(mServiceConnection);
            mBoundToService = false;
        }
    }
}
