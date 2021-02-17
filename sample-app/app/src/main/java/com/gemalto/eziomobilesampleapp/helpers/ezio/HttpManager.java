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
import android.support.annotation.Nullable;
import android.util.Base64;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.gemalto.eziomobilesampleapp.Configuration;
import com.gemalto.eziomobilesampleapp.MainActivity;
import com.gemalto.eziomobilesampleapp.R;
import com.gemalto.eziomobilesampleapp.helpers.Main;
import com.gemalto.eziomobilesampleapp.helpers.Protocols;
import com.gemalto.idp.mobile.authentication.AuthInput;
import com.gemalto.idp.mobile.core.ApplicationContextHolder;
import com.gemalto.idp.mobile.core.util.SecureString;

import java.util.HashMap;
import java.util.Map;

/**
 * Class used for http communication with demo server etc...
 */
public class HttpManager {

    //region Defines

    private static final String API_AUTH_RESPONSE_OK = "Authentication succeeded";
    private static final String API_SIGN_RESPONSE_OK = "Signature verification succeeded";

    private static final String CFG_TUTO_XML_AUTH = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \n" +
            "<AuthenticationRequest> \n" +
            "  <UserID>%s</UserID> \n" +
            "  <OTP>%s</OTP> \n" +
            "</AuthenticationRequest>";

    private static final String CFG_TUTO_XML_SIGN = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<SignatureRequest>\n" +
            "   <Transaction>\n" +
            "       <Amount>%s</Amount>\n" +
            "       <Beneficiary>%s</Beneficiary>\n" +
            "   </Transaction>\n" +
            "   <UserID>%s</UserID>\n" +
            "   <OTP>%s</OTP>\n" +
            "</SignatureRequest>";

    //endregion

    //region Public API

    public void sendAuthRequest(
            @Nullable final SecureString otp,
            @Nullable final String error,
            @Nullable final AuthInput authInput,
            @Nullable final Protocols.PostMessageInterface delegate
    ) {
        final MainActivity currentListener = Main.sharedInstance().getCurrentListener();

        // Send only valid results
        if (otp != null && error == null) {
            // Display loading indicator if UI is still valid.
            if (currentListener != null) {
                currentListener.loadingIndicatorShow(Main.getString(R.string.LOADING_MESSAGE_VALIDATING));
            }

            // Demo app use user name for token name since it's unique.
            final String userName = Main.sharedInstance().getManagerToken().getTokenDevice().getToken().getName();
            // Final application might use some XML parser rather that simple format, to ensure data integrity.
            final String body = String.format(CFG_TUTO_XML_AUTH, userName, otp.toString());

            // We don't need otp any more. Wipe it.
            otp.wipe();

            // Post message and wait for results.
            doPostMessage(Configuration.CFG_TUTO_URL_AUTH, "text/xml", authHeaders(), body, delegate);
        } else if (currentListener != null) {
            currentListener.showErrorIfExists(error);
        }

        // Wipe all sensitive data.
        if (authInput != null) {
            authInput.wipe();
        }
    }

    public void sendAuthRequestForChangePin(
            @Nullable final SecureString otp,
            @Nullable final String error,
            @Nullable final Protocols.PostMessageInterface delegate
    ) {
        final MainActivity currentListener = Main.sharedInstance().getCurrentListener();

        // Send only valid results
        if (otp != null && error == null) {
            // Display loading indicator if UI is still valid.
            if (currentListener != null) {
                currentListener.loadingIndicatorShow(Main.getString(R.string.LOADING_MESSAGE_VERIFYING_PIN));
            }

            // Demo app use user name for token name since it's unique.
            final String userName = Main.sharedInstance().getManagerToken().getTokenDevice().getToken().getName();
            // Final application might use some XML parser rather that simple format, to ensure data integrity.
            final String body = String.format(CFG_TUTO_XML_AUTH, userName, otp.toString());

            // We don't need otp any more. Wipe it.
            otp.wipe();

            // Post message and wait for results.
            doPostMessage(Configuration.CFG_TUTO_URL_AUTH, "text/xml", authHeaders(), body, delegate);
        } else if (currentListener != null) {
            currentListener.showErrorIfExists(error);
        }
    }

    public void sendSignRequest(
            @Nullable final SecureString otp,
            @Nullable final String error,
            @Nullable final AuthInput authInput,
            @Nullable final String amount,
            @Nullable final String beneficiary,
            @Nullable final Protocols.PostMessageInterface delegate
    ) {

        final MainActivity currentListener = Main.sharedInstance().getCurrentListener();

        // Send only valid results
        if (otp != null && error == null) {
            // Display loading indicator if UI is still valid.
            if (currentListener != null) {
                currentListener.loadingIndicatorShow(Main.getString(R.string.LOADING_MESSAGE_VALIDATING));
            }

            // Demo app use user name for token name since it's unique.
            final String userName = Main.sharedInstance().getManagerToken().getTokenDevice().getToken().getName();
            // Final application might use some XML parser rather that simple format, to ensure data integrity.
            final String body = String.format(CFG_TUTO_XML_SIGN, getValidString(amount), getValidString(beneficiary), userName, otp.toString());

            // We don't need otp any more. Wipe it.
            otp.wipe();

            // Post message and wait for results.
            doPostMessage(Configuration.CFG_TUTO_URL_SIGN, "text/xml", authHeaders(), body, delegate);
        } else if (currentListener != null) {
            currentListener.showErrorIfExists(error);
        }

        // Wipe all sensitive data.
        if (authInput != null) {
            authInput.wipe();
        }
    }

    //endregion

    //region Private Helpers

    private static String getValidString(final String string) {
        return string == null ? "" : string;
    }

    private Map<String, String> authHeaders() {
        final String hash = "Basic " + new String(Base64.encode((Configuration.CFG_TUTO_BASICAUTH_USERNAME + ":" + Configuration.CFG_TUTO_BASICAUTH_PASSWORD).getBytes(), 0));
        final HashMap<String, String> retValue = new HashMap<>();
        retValue.put("Authorization", hash);
        return retValue;
    }

    private Response.Listener<String> getResponseListener(
            @Nullable final Protocols.PostMessageInterface delegate
    ) {
        return response -> {
            // Hide loading
            final MainActivity currentListener = Main.sharedInstance().getCurrentListener();
            if (currentListener != null) {
                currentListener.loadingIndicatorHide();
            }

            // Notify listener.
            if (delegate != null) {
                delegate.onPostFinished(
                        API_AUTH_RESPONSE_OK.equals(response) ||
                                API_SIGN_RESPONSE_OK.equals(response), response);
            }
        };
    }

    private Response.ErrorListener getResponseErrorListener(
            @Nullable final Protocols.PostMessageInterface delegate
    ) {
        return error -> {
            // Hide loading
            final MainActivity currentListener = Main.sharedInstance().getCurrentListener();
            if (currentListener != null) {
                currentListener.loadingIndicatorHide();
            }

            // Notify listener.
            if (delegate != null) {
                delegate.onPostFinished(false, error.toString());
            }
        };
    }

    private void doPostMessage(
            @NonNull final String url,
            @NonNull final String contentType,
            @Nullable final Map<String, String> headers,
            @Nullable final String body,
            @Nullable final Protocols.PostMessageInterface delegate
    ) {
        // Prepare post headers
        final Map<String, String> headerParams = new HashMap<>();
        headerParams.put("Content-Type", contentType);
        headerParams.put("Content-Length", body != null ? String.valueOf(body.length()) : "0");

        // Add optional one.
        if (headers != null) {
            for (final String key : headers.keySet()) {
                headerParams.put(key, headers.get(key));
            }
        }

        // Prepare post request
        final StringRequest postRequest = new StringRequest(Request.Method.POST, url,
                getResponseListener(delegate), getResponseErrorListener(delegate)
        ) {
            @Override
            public byte[] getBody() {
                // Set post message body.
                return body == null ? null : body.getBytes();
            }

            @Override
            public Map<String, String> getHeaders() {
                return headerParams;
            }
        };

        // Add the request to the RequestQueue.
        Volley.newRequestQueue(ApplicationContextHolder.getContext()).add(postRequest);
    }

    //endregion
}
