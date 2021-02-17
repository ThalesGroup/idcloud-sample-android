/*
 * MIT License
 *
 * Copyright (c) 2020 Thales DIS
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * IMPORTANT: This source code is intended to serve training information purposes only.
 *            Please make sure to review our IdCloud documentation, including security guidelines.
 */

package com.gemalto.eziomobilesampleapp.helpers.ezio;

import android.support.annotation.NonNull;
import android.util.Log;

import com.gemalto.eziomobilesampleapp.Configuration;
import com.gemalto.eziomobilesampleapp.helpers.Main;
import com.gemalto.eziomobilesampleapp.helpers.Protocols;
import com.gemalto.idp.mobile.core.IdpException;
import com.gemalto.idp.mobile.core.IdpStorageException;
import com.gemalto.idp.mobile.core.devicefingerprint.DeviceFingerprintSource;
import com.gemalto.idp.mobile.core.util.SecureString;
import com.gemalto.idp.mobile.otp.OtpModule;
import com.gemalto.idp.mobile.otp.Token;
import com.gemalto.idp.mobile.otp.devicefingerprint.DeviceFingerprintTokenPolicy;
import com.gemalto.idp.mobile.otp.oath.OathService;
import com.gemalto.idp.mobile.otp.oath.OathToken;
import com.gemalto.idp.mobile.otp.oath.OathTokenManager;
import com.gemalto.idp.mobile.otp.provisioning.EpsConfigurationBuilder;
import com.gemalto.idp.mobile.otp.provisioning.MobileProvisioningProtocol;
import com.gemalto.idp.mobile.otp.provisioning.ProvisioningConfiguration;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

/**
 * Main provision / enroll and un-enroll flow. It also keep instance of OATH device.
 */
public class TokenManager {
    //region Defines

    public interface ProvisionerHandler {
        void onProvisionerFinished(final OathToken token, final String error);
    }

    private final OathTokenManager mOathManager;
    private TokenDevice mTokenDevice = null;

    //endregion

    //region Life Cycle

    public TokenManager() {
        OathToken token;
        mOathManager = OathService.create(OtpModule.create()).getTokenManager();

        // Check if we have some enrolled token.
        String tokenName = null;
        try {
            if (!mOathManager.getTokenNames().isEmpty()) {
                tokenName = mOathManager.getTokenNames().iterator().next();
            }
        } catch (IdpStorageException e) {
            // Ignore. TokenName is already null.
        }


        // If there is no token saved, we can skip rest.
        if (tokenName != null) {
            // Try to get instance of saved token.
            try {
                token = mOathManager.getToken(tokenName, Configuration.getCustomFingerprintData());
                mTokenDevice = new TokenDevice(token);
            } catch (final IdpException exception) {
                // Error in such case mean, that we have broken configuration or some internal state of SDK.
                // Most probably wrong license, different fingerprint etc. We should not continue at that point.
                Main.sharedInstance().getCurrentListener().finish();
            }
        }
    }

    //endregion

    //region Public API

    public TokenDevice getTokenDevice() {
        return mTokenDevice;
    }

    public void deleteTokenWithCompletionHandler(@NonNull final Protocols.GenericHandler handler) {
        final Protocols.GenericHandler.Sync syncHandler = new Protocols.GenericHandler.Sync(handler);
        try {
            final boolean removed = mOathManager.removeToken(mTokenDevice.getToken());
            if (removed) {
                synchronized (this) {
                    mTokenDevice = null;
                }
            }

            syncHandler.onFinished(removed, null);
        } catch (final IdpException e) {
            syncHandler.onFinished(false, e.getLocalizedMessage());
        }
    }

    public void provisionWithUserId(final String userId,
                                    final SecureString regCode,
                                    final ProvisionerHandler completionHandler) {
        // Check input, because ezio does throw runtime exception.
        if (!regCode.toString().matches("[0-9]+")) {
            if (completionHandler != null) {
                completionHandler.onProvisionerFinished(null, "Registration code can only contain digits.");
            }
            return;
        } else if (regCode.toString().length() < 2 || regCode.toString().length() > 11) {
            if (completionHandler != null) {
                completionHandler.onProvisionerFinished(null, "Invalid registration code length.");
            }
            return;
        }


        // First try to register Client Id on OOB server.
        Main.sharedInstance().getManagerPush().registerOOBWithUserId(userId, regCode, oobRegistrationResponse -> {
            Log.d(TokenManager.class.getName(), oobRegistrationResponse.getMessage());

            String clientId;

            // If OOB registration was successful we can provision token.
            if (oobRegistrationResponse.isSucceeded()) {
                clientId = oobRegistrationResponse.getClientId();
                doProvisioningWithUserId(userId, regCode, clientId, completionHandler);
            } else {
                // Notify about failure.
                if (completionHandler != null) {
                    completionHandler.onProvisionerFinished(null, oobRegistrationResponse.getMessage());
                }
            }
        });
    }

    //endregion

    //region Private Helpers

    private void doProvisioningWithUserId(
            final String userId,
            final SecureString regCode,
            final String clientId,
            final ProvisionerHandler completionHandler
    ) {
        URL provisionUrl = null;
        try {
            provisionUrl = new URL(Configuration.CFG_OTP_PROVISION_URL);
        } catch (MalformedURLException e) {
            // Invalid configuration
            Main.sharedInstance().getCurrentListener().finish();
        }

        // Prepare provisioning configuration based on app data.
        final ProvisioningConfiguration config = new EpsConfigurationBuilder(regCode,
                provisionUrl,
                Configuration.DOMAIN,
                MobileProvisioningProtocol.PROVISIONING_PROTOCOL_V3,
                Configuration.CFG_OTP_RSA_KEY_ID,
                Configuration.CFG_OTP_RSA_KEY_EXPONENT,
                Configuration.CFG_OTP_RSA_KEY_MODULUS)
                .setTlsConfiguration(Configuration.CFG_SDK_TLS_CONFIGURATION).build();

        final DeviceFingerprintSource
                deviceFingerprintSource = new DeviceFingerprintSource(Configuration.getCustomFingerprintData(),
                DeviceFingerprintSource.Type.SOFT);
        final DeviceFingerprintTokenPolicy
                deviceFingerprintTokenPolicy = new DeviceFingerprintTokenPolicy(true,
                deviceFingerprintSource);

        mOathManager.createToken(userId,
                config,
                deviceFingerprintTokenPolicy,
                new com.gemalto.idp.mobile.otp.TokenManager.TokenCreationCallback() {
                    @Override
                    public void onSuccess(
                            Token token,
                            Map<String, String> map
                    ) {
                        try {
                            mTokenDevice = new TokenDevice((OathToken) token);
                        } catch (final IdpException exception) {
                            if (completionHandler != null) {
                                completionHandler.onProvisionerFinished(null,
                                        exception.getLocalizedMessage());
                            }
                        }

                        final Main main = Main.sharedInstance();
                        main.getManagerPush().registerClientId(clientId);

                        if (completionHandler != null) {
                            completionHandler.onProvisionerFinished((OathToken) token, null);
                        }
                    }

                    @Override
                    public void onError(final IdpException exception) {
                        completionHandler.onProvisionerFinished(null,
                                exception.getLocalizedMessage());
                    }
                });
    }

    //endregion
}
