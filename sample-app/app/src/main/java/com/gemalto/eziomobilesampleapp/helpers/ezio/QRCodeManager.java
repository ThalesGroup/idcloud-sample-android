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

import androidx.annotation.NonNull;

import com.gemalto.eziomobilesampleapp.R;
import com.gemalto.eziomobilesampleapp.helpers.Main;
import com.gemalto.eziomobilesampleapp.helpers.Protocols;
import com.gemalto.idp.mobile.core.util.SecureByteArray;

/**
 * Enrollment QR code reader
 */
public class QRCodeManager {

    //region Public API

    /**
     * Try to parse / decrypt provided QR Code data.
     *
     * @param qrCodeData QR Code to be parsed.
     * @param handler    Triggered once operation is done
     */
    public void parseQRCode(
            @NonNull final SecureByteArray qrCodeData,
            @NonNull final Protocols.QRCodeManagerHandler handler
    ) {
        // Two components in frame are user id and reg code.
        final String[] components = new String(qrCodeData.toByteArray()).split(",");

        // Get actual values.
        if (components.length == 2) {
            handler.onParseFinished(true, components[0], Main.sharedInstance().secureStringFromString(components[1]), null);
        } else {
            handler.onParseFinished(false, null, null, Main.getString(R.string.COMMON_MSG_WRONG_COMPONENTS));
        }
    }

    //endregion

}
