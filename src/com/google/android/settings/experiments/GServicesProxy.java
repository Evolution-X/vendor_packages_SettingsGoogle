package com.google.android.settings.experiments;

import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public abstract class GServicesProxy {
    private static final String TAG = "GServicesProxy";
    private static final Uri PROXY_AUTHORITY =
            new Uri.Builder()
                    .scheme("content")
                    .authority("com.google.android.settings.intelligence.provider.serviceflags")
                    .build();

    public static boolean getBoolean(
            ContentResolver contentResolver, String key, boolean defaultValue) {
        Bundle request = buildRequest(key);
        request.putBoolean("default", defaultValue);
        Bundle result = getResult(contentResolver, "getBooleanForKey", request);
        return result == null ? defaultValue : result.getBoolean("value", defaultValue);
    }

    public static String getString(
            ContentResolver contentResolver, String key, String defaultValue) {
        Bundle request = buildRequest(key);
        request.putString("default", defaultValue);
        Bundle result = getResult(contentResolver, "getStringForKey", request);
        return result == null ? defaultValue : result.getString("value", defaultValue);
    }

    public static long getLong(ContentResolver contentResolver, String key, long defaultValue) {
        Bundle request = buildRequest(key);
        request.putLong("default", defaultValue);
        Bundle result = getResult(contentResolver, "getLongForKey", request);
        return result == null ? defaultValue : result.getLong("value", defaultValue);
    }

    private static Bundle buildRequest(String key) {
        Bundle bundle = new Bundle();
        bundle.putString("key", key);
        return bundle;
    }

    private static Bundle getResult(
            final ContentResolver contentResolver, final String method, final Bundle extras) {
        try {
            FutureTask<Bundle> futureTask =
                    new FutureTask<>(
                            () -> {
                                try (ContentProviderClient client =
                                        contentResolver.acquireUnstableContentProviderClient(
                                                PROXY_AUTHORITY)) {
                                    if (client == null) {
                                        return null;
                                    }
                                    return client.call(method, null, extras);
                                }
                            });
            Executors.newSingleThreadExecutor().submit(futureTask);
            return futureTask.get(100L, TimeUnit.MILLISECONDS);
        } catch (TimeoutException unused) {
            Log.w(TAG, "Timeout to query service flag provider for method " + method);
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Failed to query service flag provider for method " + method, e);
            return null;
        }
    }
}
