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

package com.gemalto.eziomobilesampleapp;

import android.net.Uri;

import com.gemalto.idp.mobile.core.devicefingerprint.DeviceFingerprintSource;
import com.gemalto.idp.mobile.core.net.TlsConfiguration;
import com.gemalto.idp.mobile.msp.MspSignatureKey;
import com.gemalto.idp.mobile.oob.OobConfiguration;
import com.gemalto.idp.mobile.otp.OtpConfiguration;
import com.gemalto.idp.mobile.otp.devicefingerprint.DeviceFingerprintTokenPolicy;

import java.util.List;

// IMPORTANT: This source code is intended to serve training information purposes only. Please make sure to review our IdCloud documentation, including security guidelines.

/**
 * Class containing all values needed to properly configure Ezio SDK.
 */
public class Configuration {

    //region Common SDK

    /**
     Activation code is used to enable OOB features.
     It should be provided by application.
     */
    public static final byte[] C_CFG_SDK_ACTIVATION_CODE = new byte[]{
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            // ...
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00
    };

    /**
     Optional value with custom finger print data. Used as input of encryption calculation
     */
    public static final DeviceFingerprintSource C_CFG_SDK_DEVICE_FINGERPRINT_SOURCE = new DeviceFingerprintSource(
            "SDK_DEVICE_FINGERPRINT_SOURCE".getBytes(), DeviceFingerprintSource.Type.SOFT);

    /**
     For debug purposes we can weaken TLS configuration.
     In release mode all values must be set to NO. Otherwise it will cause runtime exception.
     */
    public static final TlsConfiguration C_CFG_SDK_TLS_CONFIGURATION = new TlsConfiguration();

    //endregion

    //region OTP

    /**
     Define Token related behaviour on rooted devices.
     See OtpConfiguration.TokenRootPolicy for more details.
     */
    public static final OtpConfiguration.TokenRootPolicy C_CFG_OTP_ROOT_POLICY = OtpConfiguration.TokenRootPolicy.IGNORE;

    /**
     Replace this byte array with your own EPS key modulus..
     The EPS' RSA modulus. This is specific to the configuration of the bank's system. Therefore other values should be used here.
     */
    public static final byte[] C_CFG_OTP_RSA_KEY_MODULUS = new byte[]{
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            // ...
            (byte) 0x00, (byte) 0x00
    };

    /**
     Replace this byte array with your own EPS key exponent.
     The EPS' RSA exponent. This is specific to the configuration of the bank's system. Therefore other values should be used here.
     */
    public static final byte[] C_CFG_OTP_RSA_KEY_EXPONENT = new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x00};

    /**
     Replace this URL with your EPS URL.
     */
    public static final String C_CFG_OTP_PROVISION_URL = "OTP_PROVISION_URL";

    /**
     Replace this string with your own EPS key ID.
     This is specific to the configuration of the bank's system. Therefore other values should be used here.
     */
    public static final String C_CFG_OTP_RSA_KEY_ID = "RSA_KEY_ID";

    /**
     The custom fingerprint data that seals all the token credentials in this example.
     */
    public static final DeviceFingerprintTokenPolicy C_CFG_OTP_DEVICE_FINGERPRINT_SOURCE = new DeviceFingerprintTokenPolicy(true, C_CFG_SDK_DEVICE_FINGERPRINT_SOURCE);

    /**
     Configuration of example OCRA suite used in this demo.
     */
    public static final String C_CFG_OTP_OCRA_SUITE = "OTP_OCRA_SUITE";

    //endregion

    //region OOB

    /**
     Define OOB related behaviour on rooted devices.
     See OobConfiguration.OobRootPolicy for more details.
     */
    public static final OobConfiguration.OobRootPolicy C_CFG_OOB_ROOT_POLICY = OobConfiguration.OobRootPolicy.IGNORE;

    /**
     Replace this byte array with your own OOB key modulus unless you are using the default key pair.
     This is specific to the configuration of the bank's system. Therefore other values should be used here.
     */
    public static final byte[] C_CFG_OOB_RSA_KEY_MODULUS = new byte[]{
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            // ...
            (byte) 0x00, (byte) 0x0b};

    /**
     Replace this byte array with your own OOB key exponent.
     This is specific to the configuration of the bank's system. Therefore other values should be used here.
     */
    public static final byte[] C_CFG_OOB_RSA_KEY_EXPONENT = new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x00};

    /**
     Replace this URL with your OOB server URL.
     This is specific to the configuration of the bank's system. Therefore other values should be used here.
     */
    public static final String C_CFG_OOB_URL = "OOB_URL";

    /**
     Replace this domain with your OOB server domain.
     This is specific to the configuration of the bank's system. Therefore other values should be used here.
     */
    public static final String C_CFG_OOB_DOMAIN = "OOB_DOMAIN";

    /**
     Replace this app id with your OOB server app id.
     This is specific to the configuration of the bank's system. Therefore other values should be used here.
     */
    public static final String C_CFG_OOB_APP_ID = "0";

    /**
     Replace this push channel with your OOB server push channel.
     This is specific to the configuration of the bank's system. Therefore other values should be used here.
     */
    public static final String C_CFG_OOB_CHANNEL = "FCM"; // FCM

    /**
     Replace this provider id with your OOB server provider id.
     This is specific to the configuration of the bank's system. Therefore other values should be used here.
     */
    public static final String C_CFG_OOB_PROVIDER_ID = "0";

    //endregion

    //region GEMALTO FACE ID

    /**
     Use in order to activate Gemalto Face ID support.
     */
    public static final String C_CFG_FACE_ID_PRODUCT_KEY = "FACE_ID_PRODUCT_KEY";

    /**
     Use in order to activate Gemalto Face ID support.
     */
    public static final String C_CFG_FACE_ID_SERVER_URL = "FACE_ID_SERVER_URL";

    //endregion

    //region MSP

    /**
     This sample app does not use MSP encryption.
     */
    public static final List<byte[]> C_CFG_MSP_OBFUSCATION_CODE = null;

    /**
     This sample app does not use MSP encryption.
     */
    public static final List<MspSignatureKey> C_CFG_MSP_SIGN_KEYS = null;

    //endregion

    //region APP CONFIG

    /**
     This value is optional. In case that URL is not null,
     it will display privacy policy button on settings page.
     */
    public static final Uri C_CFG_PRIVACY_POLICY_URL = Uri.parse("PRIVACY_POLICY_URL");

    //endregion


    //region TUTO PAGE CONFIG
    /**
     * Tuto page does require authentication.
     */
    public static final String C_CFG_TUTO_BASICAUTH_USERNAME = "BASICAUTH_USERNAME";

    /**
     * Tuto page does require authentication.
     */
    public static final String C_CFG_TUTO_BASICAUTH_PASSWORD = "BASICAUTH_PASSWORD";

    /**
     * Base tuto page URL. Used for In Band cases.
     */
    public static final String C_CFG_TUTO_URL_ROOT = "TUTO_URL_ROOT";

    /**
     *  Auth API url used for In Band cases.
     */
    public static final String C_CFG_TUTO_URL_AUTH = "TUTO_URL_AUTH";

    /**
     * Transaction sign API url used for In Band cases.
     */
    public static final String C_CFG_TUTO_URL_SIGN = "TUTO_URL_SIGN";

    //endregion

}
