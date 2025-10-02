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

import com.gemalto.eziomobilesampleapp.Configuration
import com.gemalto.eziomobilesampleapp.helpers.Main
import com.gemalto.eziomobilesampleapp.helpers.Protocols.OTPDelegate
import com.gemalto.idp.mobile.authentication.AuthInput
import com.gemalto.idp.mobile.authentication.AuthenticationModule
import com.gemalto.idp.mobile.authentication.mode.biometric.BiometricAuthService
import com.gemalto.idp.mobile.authentication.mode.pin.PinAuthInput
import com.gemalto.idp.mobile.core.IdpException
import com.gemalto.idp.mobile.core.util.SecureString
import com.gemalto.idp.mobile.otp.OtpModule
import com.gemalto.idp.mobile.otp.oath.OathDevice
import com.gemalto.idp.mobile.otp.oath.OathService
import com.gemalto.idp.mobile.otp.oath.OathToken
import com.gemalto.idp.mobile.otp.oath.soft.SoftOathToken


/**
 * To enable all TOTP features like last OTP lifespan we need to keep instance of OATH device.
 * This class will help us keep everything on one place.
 */
class TokenDevice internal constructor(token: OathToken) {
    //region Defines
    /**
     * Keep all auth mode status in one struct.
     */
    class TokenStatus {
        var isTouchSupported: Boolean = false
        var isTouchEnabled: Boolean = false
    }

    val token: OathToken

    //endregion
    //region Public API
    val device: OathDevice

    //endregion
    //region Life Cycle
    init {
        val factory = OathService.create(OtpModule.create()).getFactory()

        // Create device based on specific ocra suite.
        val oathSettings = factory.createSoftOathSettings()
        oathSettings.setOcraSuite(
            Main.sharedInstance()?.secureStringFromString(Configuration.CFG_OTP_OCRA_SUITE)
        )
        this.device = factory.createSoftOathDevice(token as SoftOathToken?, oathSettings)
        this.token = token
    }


    @get:Suppress("deprecation")
    val tokenStatus: TokenStatus
        get() {
            val retValue = TokenStatus()

            // Check all auth mode states so we can enable / disable proper buttons.
            val authMoule =
                AuthenticationModule.create()
            val touchService =
                BiometricAuthService.create(
                    authMoule
                )

            retValue.isTouchSupported = touchService.canAuthenticate() == 1
            try {
                retValue.isTouchEnabled = token.isAuthModeActive(touchService.getAuthMode())
            } catch (e: IdpException) {
                retValue.isTouchEnabled = false
            }

            return retValue
        }

    // Generate OTP with any supported auth input.
    fun totpWithAuthInput(
        authInput: AuthInput?,
        serverChallenge: SecureString?,
        handler: OTPDelegate?
    ) {
        var otp: SecureString? = null
        var error: String? = null

        try {
            if (serverChallenge != null) {
                // Ocra does require multiauth enabled.
                if (!token.isMultiAuthModeEnabled()) {
                    token.upgradeToMultiAuthMode(authInput as PinAuthInput?)
                }
                otp = device.getOcraOtp(authInput, serverChallenge, null, null, null)
            } else {
                otp = device.getTotp(authInput)
            }
        } catch (exception: IdpException) {
            error = exception.getLocalizedMessage()
        }

        // Notify listener
        if (handler != null) {
            handler.onOTPDelegateFinished(otp, error, authInput, serverChallenge)
        }

        // Wipe for security reasons.
        if (otp != null) {
            otp.wipe()
        }
    } //endregion
}
