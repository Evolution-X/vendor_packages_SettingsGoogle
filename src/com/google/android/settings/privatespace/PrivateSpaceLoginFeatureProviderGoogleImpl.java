/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.settings.privatespace;

import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;

import com.android.settings.R;
import com.android.settings.privatespace.PrivateSpaceLoginFeatureProvider;

import java.io.IOException;

/** Java class for Private space to initiate account login during setup */
public class PrivateSpaceLoginFeatureProviderGoogleImpl implements PrivateSpaceLoginFeatureProvider {

    private static final String TAG = "PrivateSpaceGoogleImpl";

    private ActivityResultLauncher<Intent> mAddAccountToPrivateProfile;

    private final AccountManagerCallback<Bundle> mCallbackLocal =
            new AccountManagerCallback<>() {
                @Override
                public void run(AccountManagerFuture<Bundle> accountManagerFuture) {
                    try {
                        final Intent intent =
                                accountManagerFuture
                                        .getResult()
                                        .getParcelable(AccountManager.KEY_INTENT);
                        if (intent == null) {
                            Log.e(TAG, "Failed to retrieve add account intent from authenticator");
                        } else {
                            mAddAccountToPrivateProfile.launch(intent);
                        }
                    } catch (AuthenticatorException | OperationCanceledException | IOException e) {
                        Log.e(TAG, "Failed to get add account intent Activity:  ", e);
                    }
                }
            };

    @Override
    public boolean initiateAccountLogin(@NonNull Context context,
            @NonNull ActivityResultLauncher<Intent> resultLauncher) {
        mAddAccountToPrivateProfile = resultLauncher;
        final Bundle addAccountOptions = new Bundle();
        addAccountOptions.putBoolean("allow_skip", true);
        AccountManager.get(context)
                .addAccount(
                        "com.google", null, null, addAccountOptions, null, mCallbackLocal, null);
        return context.getResources().getBoolean(R.bool.config_privatespace_account_login_enabled);
    }
}
