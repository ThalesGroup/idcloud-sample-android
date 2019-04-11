/**
 *
 * MIT License
 *
 * Copyright (c) 2019 Thales DIS
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
 */

package com.gemalto.eziomobilesampleapp.helpers.ezio;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.gemalto.eziomobilesampleapp.Configuration;
import com.gemalto.eziomobilesampleapp.helpers.CMain;
import com.gemalto.eziomobilesampleapp.helpers.Protocols;
import com.gemalto.idp.mobile.core.IdpException;
import com.gemalto.idp.mobile.core.IdpStorageException;
import com.gemalto.idp.mobile.core.util.SecureString;
import com.gemalto.idp.mobile.oob.registration.OobRegistrationCallback;
import com.gemalto.idp.mobile.oob.registration.OobRegistrationResponse;
import com.gemalto.idp.mobile.otp.OtpModule;
import com.gemalto.idp.mobile.otp.oath.OathService;
import com.gemalto.idp.mobile.otp.oath.OathToken;
import com.gemalto.idp.mobile.otp.oath.OathTokenManager;
import com.gemalto.idp.mobile.otp.provisioning.EpsConfigurationBuilder;
import com.gemalto.idp.mobile.otp.provisioning.MobileProvisioningProtocol;
import com.gemalto.idp.mobile.otp.provisioning.ProvisioningConfiguration;

import java.net.MalformedURLException;
import java.net.URL;

// IMPORTANT: This source code is intended to serve training information purposes only. Please make sure to review our IdCloud documentation, including security guidelines.

/**
 * Main provision / enroll and un-enroll flow. It also keep instance of OATH device.
 */
public class TokenManager {
    //region Defines

    /**
     * Provisioning callback.
     */
    public interface ProvisionerHandler {
        /**
         * Returns when provisioning is finished.
         * @param token Provisioned token.
         * @param error Error.
         */
        void onProvisionerFinished(final OathToken token, final String error);
    }

    private final OathTokenManager mOathManager;
    private TokenDevice mTokenDevice = null;

    //endregion

    //region Life Cycle

    /**
     * Creates a new {@code TokenManager} object.
     */
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
                token = mOathManager.getToken(tokenName, Configuration.C_CFG_SDK_DEVICE_FINGERPRINT_SOURCE.getCustomData());
                mTokenDevice = new TokenDevice(token);
            } catch (final IdpException exception) {
                // Error in such case mean, that we have broken configuration or some internal state of SDK.
                // Most probably wrong license, different fingerprint etc. We should not continue at that point.
                CMain.sharedInstance().getCurrentListener().finish();
            }
        }
    }

    //endregion

    //region Public API

    /**
     * Retrieves the {@code TokenDevice}.
     * @return {@code TokenDevice}.
     */
    public TokenDevice getTokenDevice() {
        return mTokenDevice;
    }

    /**
     * Deletes the token.
     * @param completionHandler Callback returned back to the application on completion.
     */
    public void deleteTokenWithCompletionHandler(final Protocols.GenericHandler completionHandler) {
        // First we should unregister from oob and then delete token it self.
        CMain.sharedInstance().getManagerPush().unregisterOOBWithCompletionHandler(new Protocols.GenericHandler() {
            @Override
            public void onFinished(final boolean success, final String error) {
                boolean removed = false;
                String processedError = error;
                // In case of successful unregister, we can try to delete token it self.
                if (success) {
                    try {
                        removed = mOathManager.removeToken(mTokenDevice.getToken());
                    } catch (IdpException e) {
                        processedError = e.getLocalizedMessage();
                    }
                }

                // Remove stored reference
                if (removed) {
                    mTokenDevice = null;
                }

                // Notify in UI thread.
                if (completionHandler != null) {
                    final boolean finalRemoved = removed;
                    final String finalError = processedError;
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            completionHandler.onFinished(finalRemoved, finalError);
                        }
                    });
                }
            }
        });
    }

    /**
     * Provisions a new token.
     * @param userId User ID.
     * @param regCode Registration code.
     * @param completionHandler Callback returned back to the application on completion.
     */
    public void provisionWithUserId(final String userId,
                                    final SecureString regCode,
                                    final ProvisionerHandler completionHandler) {
        // Check inputs as SDK does throw runtime exception.
        if (!regCode.toString().matches("[0-9]+")) {
            if (completionHandler != null) {
                completionHandler.onProvisionerFinished(null, "Registration code can only contain digits.");
            }
            return;
        } else if (regCode.toString().length() < 2 || regCode.toString().length() > 14) {
            if (completionHandler != null) {
                completionHandler.onProvisionerFinished(null, "Invalid registration code length.");
            }
            return;
        }



        // First try to register Client Id on OOB server.
        CMain.sharedInstance().getManagerPush().registerOOBWithUserId(userId, regCode, new OobRegistrationCallback() {
            @Override
            public void onOobRegistrationResponse(final OobRegistrationResponse oobRegistrationResponse) {

                Log.d(TokenManager.class.getName(), oobRegistrationResponse.getMessage());

                String clientId = "test_client";

                // If OOB registration was successful we can provision token.
                if (oobRegistrationResponse != null && oobRegistrationResponse.isSucceeded()) {
                    clientId = oobRegistrationResponse.getClientId();
                }

                doProvisioningWithUserId(userId, regCode, clientId , completionHandler);


                // Notify about failure.
//                else if (completionHandler != null) {
//                    // Notify in UI thread.
//                    new Handler(Looper.getMainLooper()).post(new Runnable() {
//                        @Override
//                        public void run() {
//                            completionHandler.onProvisionerFinished(null, oobRegistrationResponse.getMessage());
//                        }
//                    });
//                }


            }
        });
    }

    //endregion

    //region Private Helpers

    /**
     * Provision a new token.
     * @param userId User ID.
     * @param regCode Registration code.
     * @param clientId Client ID.
     * @param completionHandler Callback returned back to the application on completion.
     */
    private void doProvisioningWithUserId(final String userId,
                                          final SecureString regCode,
                                          final String clientId,
                                          final ProvisionerHandler completionHandler) {
        URL provisionUrl = null;
        try {
            provisionUrl = new URL(Configuration.C_CFG_OTP_PROVISION_URL);
        } catch (MalformedURLException e) {
            // Invalid configuration
            CMain.sharedInstance().getCurrentListener().finish();
        }

        // Prepare provisioning configuration based on app data.
        final ProvisioningConfiguration config = new EpsConfigurationBuilder(regCode,
                provisionUrl,
                MobileProvisioningProtocol.PROVISIONING_PROTOCOL_V3,
                Configuration.C_CFG_OTP_RSA_KEY_ID,
                Configuration.C_CFG_OTP_RSA_KEY_EXPONENT,
                Configuration.C_CFG_OTP_RSA_KEY_MODULUS)
                .setTlsConfiguration(Configuration.C_CFG_SDK_TLS_CONFIGURATION).build();

        final OathToken[] token = new OathToken[1];
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    token[0] = mOathManager.createToken(userId, config, OathToken.TokenCapability.OTP, Configuration.C_CFG_OTP_DEVICE_FINGERPRINT_SOURCE);

                    // Save client id only in case of successful registration.
                    final CMain main = CMain.sharedInstance();
                    main.getManagerPush().registerClientId(clientId);

                    // Store current token.
                    mTokenDevice = new TokenDevice(token[0]);

                    // Notify in UI thread.
                    if (completionHandler != null) {
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                completionHandler.onProvisionerFinished(token[0], null);
                            }
                        });
                    }
                } catch (final IdpException exception) {
                    // Notify in UI thread.
                    if (completionHandler != null) {
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                completionHandler.onProvisionerFinished(null, exception.getLocalizedMessage());
                            }
                        });
                    }
                }
            }
        }).start();
    }

    //endregion
}
