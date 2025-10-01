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
package com.gemalto.eziomobilesampleapp

import android.net.Uri
import com.gemalto.idp.mobile.core.devicefingerprint.DeviceFingerprintSource
import com.gemalto.idp.mobile.core.net.TlsConfiguration
import com.gemalto.idp.mobile.msp.MspSignatureKey
import com.gemalto.idp.mobile.oob.OobConfiguration
import com.gemalto.idp.mobile.otp.OtpConfiguration

object Configuration {
    //region Common SDK
    val CUSTOM_FINGERPRINT_DATA = "CUSTOM_FINGERPRINT_DATA".toByteArray()

    /**
     * Activation code is used to enable OOB features.
     * It should be provided by application.
     */
    val CFG_SDK_ACTIVATION_CODE = byteArrayOf(
        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte()
    )

    /**
     * Domain.
     */
    const val DOMAIN = ""

    /**
     * Optional value with custom finger print data. Used as input of encryption calculation
     */
    val CFG_SDK_DEVICE_FINGERPRINT_SOURCE = DeviceFingerprintSource(
        CUSTOM_FINGERPRINT_DATA,
        *if (DeviceFingerprintSource.isHardwareKeySupported()) arrayOf(
            DeviceFingerprintSource.Type.HARDWARE_KEY,
            DeviceFingerprintSource.Type.SOFT
        ) else arrayOf(DeviceFingerprintSource.Type.SOFT)
    )

    /**
     * For debug purposes we can weaken TLS configuration.
     * In release mode all values must be set to NO. Otherwise it will cause runtime exception.
     */
    val CFG_SDK_TLS_CONFIGURATION = TlsConfiguration()

    //endregion
    //region OTP
    /**
     * Define Token related behaviour on rooted devices.
     * See OtpConfiguration.TokenRootPolicy for more details.
     */
    val CFG_OTP_ROOT_POLICY = OtpConfiguration.TokenRootPolicy.IGNORE

    /**
     * Replace this byte array with your own EPS key modulus unless you are using the EPS 2.X default key pair.
     * The EPS' RSA modulus. This is specific to the configuration of the bank's system.
     * Therefore other values should be used here.
     */
    val CFG_OTP_RSA_KEY_MODULUS = byteArrayOf(
        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte()
    )

    /**
     * Replace this byte array with your own EPS key exponent.
     * The EPS' RSA exponent. This is specific to the configuration of the bank's system. Therefore other values should be used here.
     */
    val CFG_OTP_RSA_KEY_EXPONENT = byteArrayOf(0x01.toByte(), 0x00.toByte(), 0x01.toByte())

    /**
     * Replace this URL with your EPS URL.
     */
    const val CFG_OTP_PROVISION_URL = ""

    /**
     * Replace this string with your own EPS key ID.
     * This is specific to the configuration of the bank's system. Therefore other values should be used here.
     */
    const val CFG_OTP_RSA_KEY_ID = ""

    /**
     * Configuration of example OCRA suite used in this demo.
     */
    const val CFG_OTP_OCRA_SUITE = "OCRA-1:HOTP-SHA256-8:QH64-T30S"

    /**
     * OTP Value lifespan used for graphical representation.
     */
    const val CFG_OTP_LIFESPAN = 30

    //endregion
    //region OOB
    /**
     * Define OOB related behaviour on rooted devices.
     * See OobConfiguration.OobRootPolicy for more details.
     */
    val CFG_OOB_ROOT_POLICY = OobConfiguration.OobRootPolicy.IGNORE

    /**
     * Replace this byte array with your own OOB key modulus unless you are using the default key pair.
     * This is specific to the configuration of the bank's system. Therefore other values should be used here.
     */
    val CFG_OOB_RSA_KEY_MODULUS = byteArrayOf(
        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte()
    )

    /**
     * Replace this byte array with your own OOB key exponent.
     * This is specific to the configuration of the bank's system.  Therefore other values should be used here.
     */
    val CFG_OOB_RSA_KEY_EXPONENT = byteArrayOf(0x01.toByte(), 0x00.toByte(), 0x01.toByte())

    /**
     * Replace this URL with your OOB server URL.
     * This is specific to the configuration of the bank's system.  Therefore other values should be used here.
     */
    const val CFG_OOB_URL = ""

    /**
     * Replace this domain with your OOB server domain.
     * This is specific to the configuration of the bank's system.  Therefore other values should be used here.
     */
    const val CFG_OOB_DOMAIN = ""

    /**
     * Replace this app id with your OOB server app id.
     * This is specific to the configuration of the bank's system.  Therefore other values should be used here.
     */
    const val CFG_OOB_APP_ID = "0"

    /**
     * Replace this push channel with your OOB server push channel.
     * This is specific to the configuration of the bank's system.  Therefore other values should be used here.
     */
    const val CFG_OOB_CHANNEL = "FCM" // FCM
    /**
     * Replace this provider id with your OOB server provider id.
     * This is specific to the configuration of the bank's system.  Therefore other values should be used here.
     */
    const val CFG_OOB_PROVIDER_ID = "0"

    //endregion
    //region MSP
    /**
     * This sample app does not use MSP encryption.
     */
    val CFG_MSP_OBFUSCATION_CODE: List<ByteArray>? = null

    /**
     * This sample app does not use MSP encryption.
     */
    val CFG_MSP_SIGN_KEYS: List<MspSignatureKey>? = null

    //endregion
    //region APP CONFIG
    /**
     * This value is optional. In case that URL is not null,
     * it will display privacy policy button on settings page.
     */
    val CFG_PRIVACY_POLICY_URL = Uri.parse("")

    //endregion
    //region SecureLog configuration
    /**
     * Retrieve the public key's modulus for SecureLog configuration
     */
    val CFG_SLOG_MODULUS = byteArrayOf(
        0x00.toByte(),
        0xd4.toByte(),
        0x6d.toByte(),
        0x5c.toByte(),
        0x06.toByte(),
        0x35.toByte(),
        0xb0.toByte(),
        0x52.toByte(),
        0x2f.toByte(),
        0x3e.toByte(),
        0xf4.toByte(),
        0x14.toByte(),
        0xd8.toByte(),
        0x3d.toByte(),
        0xf2.toByte(),
        0xd7.toByte(),
        0xf5.toByte(),
        0x1b.toByte(),
        0x54.toByte(),
        0x7e.toByte(),
        0x01.toByte(),
        0x0b.toByte(),
        0x1c.toByte(),
        0x23.toByte(),
        0x60.toByte(),
        0x04.toByte(),
        0xde.toByte(),
        0x4c.toByte(),
        0x67.toByte(),
        0x3e.toByte(),
        0xf8.toByte(),
        0x3b.toByte(),
        0x2b.toByte(),
        0xdd.toByte(),
        0xfa.toByte(),
        0x50.toByte(),
        0x87.toByte(),
        0xe7.toByte(),
        0xb3.toByte(),
        0x03.toByte(),
        0x22.toByte(),
        0x93.toByte(),
        0x87.toByte(),
        0xdd.toByte(),
        0xaf.toByte(),
        0x0a.toByte(),
        0xdd.toByte(),
        0xf9.toByte(),
        0xee.toByte(),
        0x8b.toByte(),
        0x60.toByte(),
        0x45.toByte(),
        0x1a.toByte(),
        0x6b.toByte(),
        0xf9.toByte(),
        0x49.toByte(),
        0xfd.toByte(),
        0x64.toByte(),
        0x0f.toByte(),
        0xbd.toByte(),
        0xe1.toByte(),
        0x85.toByte(),
        0x7e.toByte(),
        0x40.toByte(),
        0xe1.toByte(),
        0x52.toByte(),
        0x10.toByte(),
        0xec.toByte(),
        0xae.toByte(),
        0x93.toByte(),
        0xfd.toByte(),
        0x61.toByte(),
        0xb7.toByte(),
        0xfc.toByte(),
        0xdb.toByte(),
        0x5f.toByte(),
        0x60.toByte(),
        0xa0.toByte(),
        0xbf.toByte(),
        0x10.toByte(),
        0x94.toByte(),
        0x76.toByte(),
        0x15.toByte(),
        0x8c.toByte(),
        0x9b.toByte(),
        0x7c.toByte(),
        0xcd.toByte(),
        0xd7.toByte(),
        0xa7.toByte(),
        0xa5.toByte(),
        0x29.toByte(),
        0x1f.toByte(),
        0x31.toByte(),
        0x9a.toByte(),
        0xd0.toByte(),
        0x2e.toByte(),
        0xa2.toByte(),
        0x4f.toByte(),
        0x26.toByte(),
        0xe9.toByte(),
        0x14.toByte(),
        0x98.toByte(),
        0x99.toByte(),
        0xa6.toByte(),
        0x12.toByte(),
        0x1c.toByte(),
        0xb5.toByte(),
        0xac.toByte(),
        0x19.toByte(),
        0x99.toByte(),
        0xae.toByte(),
        0x23.toByte(),
        0xc8.toByte(),
        0x75.toByte(),
        0xea.toByte(),
        0xc0.toByte(),
        0xe0.toByte(),
        0x10.toByte(),
        0x31.toByte(),
        0x02.toByte(),
        0xf1.toByte(),
        0x4a.toByte(),
        0x97.toByte(),
        0xa5.toByte(),
        0xe2.toByte(),
        0xb0.toByte(),
        0xfd.toByte(),
        0x06.toByte(),
        0x70.toByte(),
        0xd2.toByte(),
        0xa5.toByte(),
        0x5a.toByte(),
        0xed.toByte(),
        0xe2.toByte(),
        0x9e.toByte(),
        0xea.toByte(),
        0x6f.toByte(),
        0x05.toByte(),
        0x06.toByte(),
        0x64.toByte(),
        0xa0.toByte(),
        0xf3.toByte(),
        0x5d.toByte(),
        0xba.toByte(),
        0x48.toByte(),
        0x4b.toByte(),
        0x18.toByte(),
        0xd1.toByte(),
        0x7b.toByte(),
        0xef.toByte(),
        0x48.toByte(),
        0x22.toByte(),
        0x8f.toByte(),
        0xdb.toByte(),
        0x5c.toByte(),
        0x07.toByte(),
        0xf0.toByte(),
        0x96.toByte(),
        0xfe.toByte(),
        0xfb.toByte(),
        0xac.toByte(),
        0xf1.toByte(),
        0xb0.toByte(),
        0x13.toByte(),
        0x0d.toByte(),
        0x3f.toByte(),
        0xe0.toByte(),
        0x8e.toByte(),
        0x81.toByte(),
        0xae.toByte(),
        0x73.toByte(),
        0xef.toByte(),
        0x5c.toByte(),
        0xd4.toByte(),
        0x11.toByte(),
        0x37.toByte(),
        0x85.toByte(),
        0x80.toByte(),
        0x9f.toByte(),
        0xdc.toByte(),
        0x19.toByte(),
        0x05.toByte(),
        0x49.toByte(),
        0xde.toByte(),
        0x34.toByte(),
        0xfe.toByte(),
        0x20.toByte(),
        0x54.toByte(),
        0x2d.toByte(),
        0xe6.toByte(),
        0xcc.toByte(),
        0x33.toByte(),
        0x19.toByte(),
        0x82.toByte(),
        0x0c.toByte(),
        0xc5.toByte(),
        0x9e.toByte(),
        0x42.toByte(),
        0xbe.toByte(),
        0x27.toByte(),
        0xf2.toByte(),
        0x7b.toByte(),
        0xaa.toByte(),
        0xfc.toByte(),
        0x7f.toByte(),
        0x11.toByte(),
        0x43.toByte(),
        0x83.toByte(),
        0x8c.toByte(),
        0xde.toByte(),
        0x71.toByte(),
        0xdd.toByte(),
        0x8b.toByte(),
        0xd5.toByte(),
        0x08.toByte(),
        0xb7.toByte(),
        0xcc.toByte(),
        0xc5.toByte(),
        0x0a.toByte(),
        0xf9.toByte(),
        0x91.toByte(),
        0xdc.toByte(),
        0x78.toByte(),
        0x68.toByte(),
        0x12.toByte(),
        0x64.toByte(),
        0x9d.toByte(),
        0x35.toByte(),
        0x89.toByte(),
        0x1e.toByte(),
        0xcc.toByte(),
        0x23.toByte(),
        0x7a.toByte(),
        0x11.toByte(),
        0x21.toByte(),
        0x77.toByte(),
        0x2a.toByte(),
        0xc4.toByte(),
        0xad.toByte(),
        0xc4.toByte(),
        0x2f.toByte(),
        0xcf.toByte(),
        0xec.toByte(),
        0x21.toByte(),
        0x50.toByte(),
        0x9e.toByte(),
        0x32.toByte(),
        0xf9.toByte(),
        0xa3.toByte(),
        0x2a.toByte(),
        0x27.toByte(),
        0x33.toByte(),
        0x27.toByte(),
        0x4d.toByte(),
        0x24.toByte(),
        0x78.toByte(),
        0x59.toByte()
    )

    /**
     * Retrieve the public key's exponent for SecureLog configuration
     */
    val CFG_SLOG_EXPONENT = byteArrayOf(0x01.toByte(), 0x00.toByte(), 0x01.toByte()) //endregion
}
