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
package com.gemalto.eziomobilesampleapp.helpers.ezio

import android.util.Log
import com.gemalto.eziomobilesampleapp.Configuration
import com.gemalto.eziomobilesampleapp.helpers.Main
import com.gemalto.eziomobilesampleapp.helpers.Protocols.GenericHandler
import com.gemalto.idp.mobile.core.IdpException
import com.gemalto.idp.mobile.core.IdpStorageException
import com.gemalto.idp.mobile.core.util.SecureString
import com.gemalto.idp.mobile.oob.registration.OobRegistrationCallback
import com.gemalto.idp.mobile.oob.registration.OobRegistrationResponse
import com.gemalto.idp.mobile.otp.OtpModule
import com.gemalto.idp.mobile.otp.Token
import com.gemalto.idp.mobile.otp.TokenManager
import com.gemalto.idp.mobile.otp.devicefingerprint.DeviceFingerprintTokenPolicy
import com.gemalto.idp.mobile.otp.oath.OathService
import com.gemalto.idp.mobile.otp.oath.OathToken
import com.gemalto.idp.mobile.otp.oath.OathTokenManager
import com.gemalto.idp.mobile.otp.provisioning.EpsConfigurationBuilder
import com.gemalto.idp.mobile.otp.provisioning.MobileProvisioningProtocol
import java.net.MalformedURLException
import java.net.URL

/**
 * Main provision / enroll and un-enroll flow. It also keep instance of OATH device.
 */
class TokenManager {
    //region Defines
    interface ProvisionerHandler {
        fun onProvisionerFinished(token: OathToken?, error: String?)
    }

    private val mOathManager: OathTokenManager

    //endregion
    //region Public API
    var tokenDevice: TokenDevice? = null
        private set

    //endregion
    //region Life Cycle
    init {
        val token: OathToken
        mOathManager = OathService.create(OtpModule.create()).getTokenManager()

        // Check if we have some enrolled token.
        var tokenName: String? = null
        try {
            if (!mOathManager.getTokenNames().isEmpty()) {
                tokenName = mOathManager.getTokenNames().iterator().next()
            }
        } catch (e: IdpStorageException) {
            // Ignore. TokenName is already null.
        }


        // If there is no token saved, we can skip rest.
        if (tokenName != null) {
            // Try to get instance of saved token.
            try {
                token = mOathManager.getToken<OathToken>(
                    tokenName,
                    Configuration.CUSTOM_FINGERPRINT_DATA
                )
                this.tokenDevice = TokenDevice(token)
            } catch (exception: IdpException) {
                // Error in such case mean, that we have broken configuration or some internal state of SDK.
                // Most probably wrong license, different fingerprint etc. We should not continue at that point.
                Main.sharedInstance()?.currentListener?.finish()
            }
        }
    }

    fun deleteTokenWithCompletionHandler(handler: GenericHandler) {
        val syncHandler = GenericHandler.Sync(handler)
        try {
            val removed = mOathManager.removeToken(tokenDevice?.token)
            if (removed) {
                synchronized(this) {
                    this.tokenDevice = null
                }
            }

            syncHandler.onFinished(removed, null)
        } catch (e: IdpException) {
            syncHandler.onFinished(false, e.getLocalizedMessage())
        }
    }

    fun provisionWithUserId(
        userId: String?,
        regCode: SecureString?,
        completionHandler: ProvisionerHandler?
    ) {
        // Check input, because ezio does throw runtime exception.
        if (!regCode.toString().matches("[0-9]+".toRegex())) {
            if (completionHandler != null) {
                completionHandler.onProvisionerFinished(
                    null,
                    "Registration code can only contain digits."
                )
            }
            return
        } else if (regCode.toString().length < 2 || regCode.toString().length > 11) {
            if (completionHandler != null) {
                completionHandler.onProvisionerFinished(null, "Invalid registration code length.")
            }
            return
        }


        // First try to register Client Id on OOB server.
        Main.sharedInstance()?.managerPush?.registerOOBWithUserId(
            userId,
            regCode,
            OobRegistrationCallback { oobRegistrationResponse: OobRegistrationResponse ->
                Log.d(TokenManager::class.java.getName(), oobRegistrationResponse.getMessage())
                val clientId: String

                // If OOB registration was successful we can provision token.
                if (oobRegistrationResponse?.isSucceeded() == true) {
                    clientId = oobRegistrationResponse.getClientId()
                    doProvisioningWithUserId(userId, regCode, clientId, completionHandler)
                } else {
                    // Notify about failure.
                    if (completionHandler != null) {
                        completionHandler.onProvisionerFinished(
                            null,
                            oobRegistrationResponse?.getMessage()
                        )
                    }
                }
            })
    }

    //endregion
    //region Private Helpers
    private fun doProvisioningWithUserId(
        userId: String?,
        regCode: SecureString?,
        clientId: String,
        completionHandler: ProvisionerHandler?
    ) {
        var provisionUrl: URL? = null
        try {
            provisionUrl = URL(Configuration.CFG_OTP_PROVISION_URL)
        } catch (e: MalformedURLException) {
            // Invalid configuration
            Main.sharedInstance()?.currentListener?.finish()
        }

        // Prepare provisioning configuration based on app data.
        val config = EpsConfigurationBuilder(
            regCode,
            provisionUrl,
            Configuration.DOMAIN,
            MobileProvisioningProtocol.PROVISIONING_PROTOCOL_V5,
            Configuration.CFG_OTP_RSA_KEY_ID,
            Configuration.CFG_OTP_RSA_KEY_EXPONENT,
            Configuration.CFG_OTP_RSA_KEY_MODULUS
        )
            .setTlsConfiguration(Configuration.CFG_SDK_TLS_CONFIGURATION).build()

        val deviceFingerprintTokenPolicy = DeviceFingerprintTokenPolicy(
            true,
            Configuration.CFG_SDK_DEVICE_FINGERPRINT_SOURCE
        )

        mOathManager.createToken(
            userId,
            config,
            deviceFingerprintTokenPolicy,
            object : TokenManager.TokenCreationCallback {
                override fun onSuccess(
                    token: Token?,
                    map: MutableMap<String?, String?>?
                ) {
                    try {
                        tokenDevice =
                            TokenDevice((token as com.gemalto.idp.mobile.otp.oath.OathToken))
                    } catch (exception: IdpException) {
                        if (completionHandler != null) {
                            completionHandler.onProvisionerFinished(
                                null,
                                exception.getLocalizedMessage()
                            )
                        }
                    }

                    val main = Main.sharedInstance()
                    main?.managerPush?.registerClientId(clientId)

                    if (completionHandler != null) {
                        completionHandler.onProvisionerFinished(token as OathToken, null)
                    }
                }

                override fun onError(exception: IdpException) {
                    completionHandler?.onProvisionerFinished(
                        null,
                        exception.getLocalizedMessage()
                    )
                }
            })
    } //endregion
}
