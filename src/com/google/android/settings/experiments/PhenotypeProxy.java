package com.google.android.settings.experiments;

import android.content.ContentProviderClient;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public abstract class PhenotypeProxy {
    private static final String TAG = "PhenotypeProxy";
    private static final Uri PROXY_AUTHORITY =
            new Uri.Builder()
                    .scheme("content")
                    .authority("com.google.android.settings.intelligence.provider.experimentflags")
                    .build();

    public static boolean getBooleanFlagByPackageAndKey(
            Context context, String packageName, String key, boolean defaultValue) {
        Bundle request = new Bundle(1);
        request.putBoolean("default_value", defaultValue);
        Bundle result =
                getFlagByPackageAndKey(
                        context, packageName, key, "getBooleanForPackageAndKey", request);
        return result == null ? defaultValue : result.getBoolean("value", defaultValue);
    }

    public static int getIntFlagByPackageAndKey(
            Context context, String packageName, String key, int defaultValue) {
        Bundle request = new Bundle(1);
        request.putInt("default_value", defaultValue);
        Bundle result =
                getFlagByPackageAndKey(
                        context, packageName, key, "getIntForPackageAndKey", request);
        return result == null ? defaultValue : result.getInt("value", defaultValue);
    }

    public static long getLongFlagByPackageAndKey(
            Context context, String packageName, String key, long defaultValue) {
        Bundle request = new Bundle(1);
        request.putLong("default_value", defaultValue);
        Bundle result =
                getFlagByPackageAndKey(
                        context, packageName, key, "getLongForPackageAndKey", request);
        return result == null ? defaultValue : result.getLong("value", defaultValue);
    }

    public static double getDoubleFlagByPackageAndKey(
            Context context, String packageName, String key, double defaultValue) {
        Bundle request = new Bundle(1);
        request.putDouble("default_value", defaultValue);
        Bundle result =
                getFlagByPackageAndKey(
                        context, packageName, key, "getDoubleForPackageAndKey", request);
        return result == null ? defaultValue : result.getDouble("value", defaultValue);
    }

    public static String getStringFlagByPackageAndKey(
            Context context, String packageName, String key, String defaultValue) {
        Bundle request = new Bundle(1);
        request.putString("default_value", defaultValue);
        Bundle result =
                getFlagByPackageAndKey(
                        context, packageName, key, "getStringForPackageAndKey", request);
        return result == null ? defaultValue : result.getString("value", defaultValue);
    }

    public static List<String> getStringListFlagByPackageAndKey(
            Context context, String packageName, String key, List<String> defaultValue) {
        Bundle request = new Bundle(1);
        request.putStringArrayList("default_value", new ArrayList<>(defaultValue));
        Bundle result =
                getFlagByPackageAndKey(
                        context, packageName, key, "getStringListForPackageAndKey", request);
        return result == null ? defaultValue : result.getStringArrayList("value");
    }

    private static Bundle getFlagByPackageAndKey(
            Context context, String packageName, String key, String method, Bundle extras) {
        Bundle request = new Bundle();
        request.putString("package_name", packageName);
        request.putString("key", key);
        request.putAll(extras);
        try (ContentProviderClient client =
                context.getApplicationContext()
                        .getContentResolver()
                        .acquireUnstableContentProviderClient(PROXY_AUTHORITY)) {
            if (client == null) {
                return null;
            }
            return client.call(method, null, request);
        } catch (Exception e) {
            Log.e(TAG, "Failed to query experiment provider", e);
            return null;
        }
    }
}
