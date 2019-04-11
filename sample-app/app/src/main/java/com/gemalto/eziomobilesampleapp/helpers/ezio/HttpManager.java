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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Base64;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.gemalto.eziomobilesampleapp.Configuration;
import com.gemalto.eziomobilesampleapp.MainActivity;
import com.gemalto.eziomobilesampleapp.R;
import com.gemalto.eziomobilesampleapp.helpers.CMain;
import com.gemalto.eziomobilesampleapp.helpers.Protocols;
import com.gemalto.idp.mobile.authentication.AuthInput;
import com.gemalto.idp.mobile.core.ApplicationContextHolder;
import com.gemalto.idp.mobile.core.util.SecureString;

import java.util.HashMap;
import java.util.Map;

// IMPORTANT: This source code is intended to serve training information purposes only. Please make sure to review our IdCloud documentation, including security guidelines.

/**
 * Class used for http communication with demo server.
 */
public class HttpManager {

    //region Defines

    private static final String C_CFG_TUTO_XML_AUTH = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \n" +
            "<AuthenticationRequest> \n" +
            "  <UserID>%s</UserID> \n" +
            "  <OTP>%s</OTP> \n" +
            "</AuthenticationRequest>";

    private static final String C_CFG_TUTO_XML_SIGN = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
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

    /**
     * Sends the verify request to the server.
     * @param otp OTP to verify.
     * @param error Error.
     * @param authInput User authentication.
     * @param serverChallenge Server challenge.
     */
    public void sendAuthRequest(@Nullable final SecureString otp, @Nullable final String error,
                                @Nullable final AuthInput authInput, final SecureString serverChallenge) {

        final MainActivity currentListener = CMain.sharedInstance().getCurrentListener();

        // Send only valid results
        if (otp != null && error == null) {
            // Display loading indicator if UI is still valid.
            if (currentListener != null) {
                currentListener.loadingIndicatorShow(CMain.getString(R.string.LOADING_MESSAGE_SENDING));
            }

            // Demo app use user name for token name since it's unique.
            final String userName = CMain.sharedInstance().getManagerToken().getTokenDevice().getToken().getName();
            // Final application might use some XML parser rather that simple format, to ensure data integrity.
            final String body = String.format(C_CFG_TUTO_XML_AUTH, userName, otp.toString());

            // We don't need otp any more. Wipe it.
            otp.wipe();

            // Post message and wait for results in mProcessResponse.
            doPostMessage(Configuration.C_CFG_TUTO_URL_AUTH, "text/xml", authHeaders(), body, mProcessResponse);
        } else if (currentListener != null) {
            currentListener.showErrorIfExists(error);
        }

        // Wipe all sensitive data.
        if (authInput != null) {
            authInput.wipe();
        }
        if (serverChallenge != null) {
            serverChallenge.wipe();
        }
    }

    /**
     * Send the signed request to the verification server.
     * @param otp OTP.
     * @param error Error.
     * @param authInput User authentication.
     * @param serverChallenge Server challenge.
     * @param amount Amount.
     * @param beneficiary Beneficiary.
     */
    public void sendSignRequest(@Nullable final SecureString otp, @Nullable final String error,
                                @Nullable final AuthInput authInput, final SecureString serverChallenge,
                                @Nullable final String amount, @Nullable final String beneficiary) {

        final MainActivity currentListener = CMain.sharedInstance().getCurrentListener();

        // Send only valid results
        if (otp != null && error == null) {
            // Display loading indicator if UI is still valid.
            if (currentListener != null) {
                currentListener.loadingIndicatorShow(CMain.getString(R.string.LOADING_MESSAGE_SENDING));
            }

            // Demo app use user name for token name since it's unique.
            final String userName = CMain.sharedInstance().getManagerToken().getTokenDevice().getToken().getName();
            // Final application might use some XML parser rather that simple format, to ensure data integrity.
            final String body = String.format(C_CFG_TUTO_XML_SIGN, getValidString(amount), getValidString(beneficiary), userName, otp.toString());

            // We don't need otp any more. Wipe it.
            otp.wipe();

            // Post message and wait for results in mProcessResponse.
            doPostMessage(Configuration.C_CFG_TUTO_URL_SIGN, "text/xml", authHeaders(), body, mProcessResponse);
        } else if (currentListener != null) {
            currentListener.showErrorIfExists(error);
        }

        // Wipe all sensitive data.
        if (authInput != null) {
            authInput.wipe();
        }
        if (serverChallenge != null) {
            serverChallenge.wipe();
        }
    }

    //endregion

    //region Private Helpers

    /**
     * Returns an empty string on null string passed.
     * @param string String to validate.
     * @return Empty string if null string was passed as argument, else the original string is returned.
     */
    private static String getValidString(final String string) {
        return string == null ? "" : string;
    }

    /**
     * Post message handler implementation.
     */
    final private Protocols.PostMessageInterface mProcessResponse = new Protocols.PostMessageInterface() {
        @Override
        public void onPostFinished(final String response, final String error) {
            // Check if UI is still active.
            final MainActivity currentListener = CMain.sharedInstance().getCurrentListener();
            if (currentListener == null) {
                return;
            }

            // Hide loading bar and display results.
            currentListener.loadingIndicatorHide();

             // Display server response or possible error.
            if (response != null) {
                currentListener.showMessage(null, response);
            } else {
                currentListener.showErrorIfExists(error);
            }
        }
    };

    /**
     * Creates the authentication header for the server request.
     * @return Authentication header
     */
    @SuppressWarnings("Basic authentication is used for connecting to tutorial website. "
                      + "Note that this is only for the purpose of the sample application. ")
    private Map<String, String> authHeaders() {
        final String hash = "Basic " + new String(Base64.encode((Configuration.C_CFG_TUTO_BASICAUTH_USERNAME + ":" + Configuration.C_CFG_TUTO_BASICAUTH_PASSWORD).getBytes(), 0));
        final HashMap<String, String> retValue = new HashMap<>();
        retValue.put("Authorization", hash);
        return retValue;
    }

    /**
     * Creates a new response listener.
     * @param delegate Delegate.
     * @return Response listener.
     */
    private Response.Listener<String> getResponseListener(@Nullable final Protocols.PostMessageInterface delegate) {
        return new Response.Listener<String>() {
            @Override
            public void onResponse(final String response) {
                // Notify listener.
                if (delegate != null) {
                    delegate.onPostFinished(response, null);
                }
            }
        };
    }

    /**
     * Creates a new error listener.
     * @param delegate Delegate.
     * @return Error listener.
     */
    private Response.ErrorListener getResponseErrorListener(@Nullable final Protocols.PostMessageInterface delegate) {
        return new Response.ErrorListener() {
            @Override
            public void onErrorResponse(final VolleyError error) {
                // Notify listener.
                if (delegate != null) {
                    // TODO: Not use toString. Get some description based on error type.
                    delegate.onPostFinished(null, error.toString());
                }
            }
        };
    }

    /**
     * Performs the post request to the verification server.
     * @param url URL of the verification server.
     * @param contentType Content type.
     * @param headers Headers.
     * @param body Body.
     * @param delegate Delegate.
     */
    private void doPostMessage(@NonNull final String url,
                               @NonNull final String contentType,
                               @Nullable final Map<String, String> headers,
                               @Nullable final String body,
                               @Nullable final Protocols.PostMessageInterface delegate) {

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
