/*
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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.widget.Toast;

import com.gemalto.eziomobilesampleapp.Configuration;
import com.gemalto.eziomobilesampleapp.MainActivity;
import com.gemalto.eziomobilesampleapp.R;
import com.gemalto.eziomobilesampleapp.helpers.CMain;
import com.gemalto.eziomobilesampleapp.helpers.Protocols;
import com.gemalto.idp.mobile.authentication.AuthInput;
import com.gemalto.idp.mobile.core.util.SecureString;
import com.gemalto.idp.mobile.msp.MspBaseAlgorithm;
import com.gemalto.idp.mobile.msp.MspData;
import com.gemalto.idp.mobile.msp.MspFactory;
import com.gemalto.idp.mobile.msp.MspField;
import com.gemalto.idp.mobile.msp.MspOathData;
import com.gemalto.idp.mobile.msp.MspParser;
import com.gemalto.idp.mobile.msp.exception.MspException;
import com.gemalto.idp.mobile.oob.OobException;
import com.gemalto.idp.mobile.oob.OobManager;
import com.gemalto.idp.mobile.oob.OobMessageResponse;
import com.gemalto.idp.mobile.oob.OobModule;
import com.gemalto.idp.mobile.oob.OobResponse;
import com.gemalto.idp.mobile.oob.message.OobFetchMessageCallback;
import com.gemalto.idp.mobile.oob.message.OobFetchMessageResponse;
import com.gemalto.idp.mobile.oob.message.OobIncomingMessage;
import com.gemalto.idp.mobile.oob.message.OobIncomingMessageType;
import com.gemalto.idp.mobile.oob.message.OobMessageManager;
import com.gemalto.idp.mobile.oob.message.OobSendMessageCallback;
import com.gemalto.idp.mobile.oob.message.OobTransactionSigningRequest;
import com.gemalto.idp.mobile.oob.message.OobTransactionSigningResponse;
import com.gemalto.idp.mobile.oob.notification.OobClearNotificationProfileCallback;
import com.gemalto.idp.mobile.oob.notification.OobNotificationManager;
import com.gemalto.idp.mobile.oob.notification.OobNotificationProfile;
import com.gemalto.idp.mobile.oob.notification.OobSetNotificationProfileCallback;
import com.gemalto.idp.mobile.oob.registration.OobRegistrationCallback;
import com.gemalto.idp.mobile.oob.registration.OobRegistrationManager;
import com.gemalto.idp.mobile.oob.registration.OobRegistrationRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;

// IMPORTANT: This source code is intended to serve training information purposes only. Please make sure to review our IdCloud documentation, including security guidelines.

/**
 * Manager handling all push related actions. Register / unregister from OOB, updating push token etc.
 */
public class PushManager {

    //region Defines

    private static final String TAG = "PushManager";


    public enum ClientIdState {
        Unregistered(0),
        Registered(1);

        ClientIdState(final int value) {
            mKey = value;
        }

        int getKey() {
            return mKey;
        }

        final private int mKey;
    }

    // Last token provided by application
    private final static String C_STORAGE_LAST_PROVIDED_TOKEN_ID = "LastProvidedTokenId";

    // Last token actually registered with current ClientId.
    // Since demo app is for single token only we don't care about relations.
    private final static String C_STORAGE_LAST_REGISTERED_TOKEN_ID = "LastRegistredTokenId";

    // Last registered OOB ClientId.
    private final static String C_STORAGE_KEY_CLIENT_ID = "ClientId";

    // Stored in fast storage to prevent reading encrypted data.
    private final static String C_STORAGE_KEY_CLIENT_ID_STAT = "ClientIdState";

    // Message type we want to handle. Contain message id to fetch and origin client id.
    private final static String C_PUSH_MESSAGE_TYPE = "com.gemalto.msm";
    private final static String C_PUSH_MESSAGE_CLIENT_ID = "clientId";
    private final static String C_PUSH_MESSAGE_MESSAGE_ID = "messageId";

    private String mCurrentPushToken = null;
    private OobManager mOobManager = null;

    //endregion

    //region Life Cycle

    /**
     * Creates a new {@code PushManager} object.
     */
    public PushManager()  {
        mCurrentPushToken = CMain.sharedInstance().getStorageFast().readString(C_STORAGE_LAST_PROVIDED_TOKEN_ID);
    }

    public void initWithPermissions() throws MalformedURLException {
        mOobManager = OobModule.create().createOobManager(
                new URL(Configuration.CFG_OOB_URL),
                Configuration.CFG_OOB_DOMAIN,
                Configuration.CFG_OOB_APP_ID,
                Configuration.CFG_OOB_RSA_KEY_EXPONENT,
                Configuration.CFG_OOB_RSA_KEY_MODULUS);

        // Call register just in case we did get token already.
        registerCurrent(null);
    }

    //endregion

    //region Public API

    /**
     * Checks if push token is already registered.
     * @return {@code True} if push token is registered, else {@code false}.
     */
    public boolean isPushTokenRegistered() {
        return CMain.sharedInstance().getStorageFast().readString(C_STORAGE_LAST_REGISTERED_TOKEN_ID) != null;
    }

    /**
     * Registers the token with OOB.
     * @param token Token name to register.
     */
    void registerToken(@Nullable final String token) {
        // Store provided token.
        if (mCurrentPushToken == null || (token != null && !mCurrentPushToken.equalsIgnoreCase(token))) {
            CMain.sharedInstance().getStorageFast().writeString(token, C_STORAGE_LAST_PROVIDED_TOKEN_ID);
            mCurrentPushToken = token;
        }

        // Check if new registration is needed even if token is same like last time.
        registerCurrent(null);
    }

    /**
     * Registers the client ID with OOB.
     * @param clientId Client ID to register.
     */
    void registerClientId(@NonNull final String clientId) {
        final CMain main = CMain.sharedInstance();

        // There is not much secure about Client Id, we are using secure storage just as showcase.
        // Because of that we will also update state in fast storage to not affect performance.
        main.getStorageSecure().writeString(clientId, C_STORAGE_KEY_CLIENT_ID);
        main.getStorageFast().writeInteger(ClientIdState.Registered.mKey, C_STORAGE_KEY_CLIENT_ID_STAT);

        // Check if new registration is needed.
        registerCurrent(clientId);
    }

    /**
     * Registers User ID with OOB.
     * @param userId User ID.
     * @param regCode Registration code.
     * @param completionHandler Callback returned back to the application on completion.
     */
    void registerOOBWithUserId(@NonNull final String userId, @Nullable final SecureString regCode, final OobRegistrationCallback completionHandler) {
        final OobRegistrationManager regManager = mOobManager.getOobRegistrationManager();
        final OobRegistrationRequest request = new OobRegistrationRequest(userId, userId, OobRegistrationRequest.RegistrationMethod.REGISTRATION_CODE, regCode);

        // We don't have to solve UI thread. It's handled by logic layer.
        regManager.register(request, completionHandler);
    }

    /**
     * Unregister with OOB.
     * @param completionHandler Callback returned back to the application on completion.
     */
    void unregisterOOBWithCompletionHandler(@NonNull final Protocols.GenericHandler completionHandler) {
        final CMain main = CMain.sharedInstance();

        // Push token is registered
        if (isPushTokenRegistered()) {
            // Call unregister
            unRegisterOOBClientId(main.getStorageSecure().readString(C_STORAGE_KEY_CLIENT_ID), new Protocols.GenericHandler() {
                @Override
                public void onFinished(final boolean success, final String error) {
                    if (success) {
                        // Remove all stored values.
                        main.getStorageFast().removeValue(C_STORAGE_LAST_REGISTERED_TOKEN_ID);
                        main.getStorageFast().removeValue(C_STORAGE_KEY_CLIENT_ID_STAT);
                        main.getStorageSecure().removeValue(C_STORAGE_KEY_CLIENT_ID);

                        notifyAboutStatusChange();
                    }

                    // Do not return in UI thread. It's used only from token manager.
                    completionHandler.onFinished(success, error);
                }
            });
        } else {
            returnSuccessToHandler(completionHandler);
        }
    }

    /**
     * Processes incoming push message
     * @param data Incoming push message data.
     */
    public void processIncommingPush(@Nullable final Map<String, String> data) {
        // We don't have all required permissions. Wait for init.
        // Full application should hande this scenario even if it might happen only when user
        // disable some permission after they was already acquires.
        if (mOobManager == null) {
            return;
        }

        final MainActivity listener = CMain.sharedInstance().getCurrentListener();

        // React on message type com.gemalto.msm with supported view controller on screen.
        // This is just to simplify sample app scenario. Real application should handle all notification all the time.
        if (listener == null || data == null || !data.containsKey(C_PUSH_MESSAGE_TYPE)) {
            return;
        }

        try {
            final JSONObject dataJSON = new JSONObject(data.get(C_PUSH_MESSAGE_TYPE));

            // Get client and message id out of it.
            final String msgClientId = dataJSON.getString(C_PUSH_MESSAGE_CLIENT_ID);
            final String msgMessageId = dataJSON.getString(C_PUSH_MESSAGE_MESSAGE_ID);
            final String locClientId = CMain.sharedInstance().getStorageSecure().readString(C_STORAGE_KEY_CLIENT_ID);

            // Find related token / client id on local. React only on current one.
            if (!msgClientId.equalsIgnoreCase(locClientId)) {
                return;
            }

            // Prepare manager with current client and provider id.
            final OobMessageManager oobMessageManager = mOobManager.getOobMessageManager(locClientId, Configuration.CFG_OOB_PROVIDER_ID);

            // Display loading bar to indicate message downloading.
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    listener.loadingIndicatorShow(CMain.getString(R.string.PUSH_PROCESSING));
                }
            });

            // Download message content.
            // Some messages might already be prefetched so we don't have to download them.
            // For simplicity we will download all of them.
            oobMessageManager.fetchMessage(msgMessageId, new OobFetchMessageCallback() {
                @Override
                public void onFetchMessageResult(final OobFetchMessageResponse oobFetchMessageResponse) {
                    // After fetch keep everything in UI thread since there is a lot of user interaction.
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            // Notify about possible error
                            if (!oobFetchMessageResponse.isSucceeded() || oobFetchMessageResponse.getOobIncomingMessage() == null) {
                                listener.loadingIndicatorHide();
                                listener.showErrorIfExists(oobFetchMessageResponse.getMessage());

                                return;
                            }

                            // Since we might support multiple message type, it's cleaner to have separate method for that.
                            if (!processIncommingMessage(oobFetchMessageResponse.getOobIncomingMessage(), oobMessageManager, listener)) {
                                // Hide indicator in case that message was not processed.a
                                // Otherwise indicator will be hidden by specific method.
                                new Handler(Looper.getMainLooper()).post(new Runnable() {
                                    @Override
                                    public void run() {
                                        listener.loadingIndicatorHide();
                                    }
                                });
                            }
                        }
                    });
                }
            });

        } catch (JSONException e) {
            // Ignore invalid message in demo.
        }
    }

    /**
     * Retrieves the current push token.
     * @return Current push token.
     */
    public String getCurrentPushToken() {
        return mCurrentPushToken;
    }

    //endregion

    //region Message handlers

    /**
     * Processes incoming OOB message.
     * @param message OOB message.
     * @param oobMessageManager {@code OobMessageManager}.
     * @param handler Underlying {@code Activity}.
     * @return {@code True} if incoming message was processed successfully, else {@code false}.
     */
    private boolean processIncommingMessage(@NonNull final OobIncomingMessage message,
                                            @NonNull final OobMessageManager oobMessageManager,
                                            @NonNull final MainActivity handler) {
        boolean retValue = false;

        // Sign request.
        if (message.getMessageType().equalsIgnoreCase(OobIncomingMessageType.TRANSACTION_SIGNING)) {
            retValue = processTransactionSigningRequest((OobTransactionSigningRequest) message, oobMessageManager, handler);
        }

        return retValue;
    }

    /**
     * Processes incoming OOB message.
     * @param oobMessageManager {@code OobMessageManager}.
     * @param handler Underlying {@code Activity}.
     * @return {@code True} if incoming message was processed successfully, else {@code false}.
     */
    private boolean processTransactionSigningRequest(@NonNull final OobTransactionSigningRequest request,
                                                     @NonNull final OobMessageManager oobMessageManager,
                                                     @NonNull final MainActivity handler) {
        final boolean[] retValue = {false};
        final String[] errorMessage = {null};

        // Get message subject key and fill in all values.
        String subject = CMain.getStringByKeyName(request.getSubject().toString());
        for (final Map.Entry<String, String> entry : request.getMeta().entrySet()) {
            final String placeholder = "%" + entry.getKey();
            subject = subject.replace(placeholder, entry.getValue());
        }

        try {
            // Try to parse frame.
            final MspParser parser = MspFactory.createMspParser();
            final MspData data = parser.parseMspData(parser.parse(request.getMspFrame().toByteArray()));

            // For purpose of this sample app we will support only OATH.
            if (data.getBaseAlgo() != MspBaseAlgorithm.BASE_OATH) {
                handler.loadingIndicatorHide();
                return false;
            }

            // Server challenge is send only for transaction sign. Not authentication.
            final MspField ocraServerChallenge = ((MspOathData) data).getOcraServerChallenge();
            SecureString serverChallange = null;
            if (ocraServerChallenge != null) {
                serverChallange = ocraServerChallenge.getValue();
            }

            handler.approveOTP(subject, serverChallange, new Protocols.OTPDelegate() {
                @Override
                public void onOTPDelegateFinished(final SecureString otp,
                                                  final String error,
                                                  final AuthInput authInput,
                                                  final SecureString serverChallenge) {
                    // If we get OTP it mean, that user did approved request.
                    try {
                        final OobTransactionSigningResponse response = request.createResponse(otp != null ?
                                OobTransactionSigningResponse.OobTransactionSigningResponseValue.ACCEPTED :
                                OobTransactionSigningResponse.OobTransactionSigningResponseValue.REJECTED, otp, null);
                        // Send message and wait display result.
                        oobMessageManager.sendMessage(response, new OobSendMessageCallback() {
                            @Override
                            public void onSendMessageResult(final OobMessageResponse oobMessageResponse) {
                                retValue[0] = notifyHandlerAboutPushSend(handler, oobMessageResponse);
                            }
                        });
                    } catch (final OobException exception) {
                        errorMessage[0] = exception.getLocalizedMessage();
                    }
                }
            });
        } catch (final MspException exception) {
            errorMessage[0] = exception.getLocalizedMessage();
        }

        if (!retValue[0]) {
            // Display possible parsing issue.
            handler.loadingIndicatorHide();
            handler.showErrorIfExists(errorMessage[0]);
        }

        return retValue[0];
    }

    //endregion


    // MARK: - Private Helpers

    /**
     * Hides the loading indicator on the underlying {@code Activity}.
     * @param handler Underlying {@code Activity}.
     * @param oobMessageResponse {@code OOBMessageResponse}.
     */
    private boolean notifyHandlerAboutPushSend(@NonNull final MainActivity handler,
                                               @Nullable final OobMessageResponse oobMessageResponse) {
        final boolean retValue = oobMessageResponse != null;

        if (retValue) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    handler.loadingIndicatorHide();
                    Toast.makeText(handler, CMain.getString(R.string.PUSH_SENT),
                            Toast.LENGTH_LONG).show();
                }
            });
        }

        return retValue;
    }

    /**
     * Registers the current client ID with OOB.
     * @param clientId Client ID to register.
     */
    private void registerCurrent(@Nullable final String clientId) {
        String processedClientId = clientId;

        // We don't have all required permissions. Wait for init.
        if (mOobManager == null) {
            return;
        }

        // We don't have token from app? Nothing to do without it.
        if (mCurrentPushToken == null) {
            // In full app this would not be an error, but in sample app we want to force registration OOB before token.
            return;
        }

        final CMain main = CMain.sharedInstance();

        // We don't have any client id at all.
        if (main.getStorageFast().readInteger(C_STORAGE_KEY_CLIENT_ID_STAT) == ClientIdState.Unregistered.mKey) {
            // This will probably happen when app will get push token without any enrolled token / client id.
            return;
        }

        // Last registered token is same as current one.
        final String lastReg = main.getStorageFast().readString(C_STORAGE_LAST_REGISTERED_TOKEN_ID);
        if (lastReg != null && lastReg.equalsIgnoreCase(mCurrentPushToken)) {
            return;
        }

        // Get current Client Id or use one provided from API to safe some time.
        if (processedClientId == null) {
            processedClientId = main.getStorageSecure().readString(C_STORAGE_KEY_CLIENT_ID);
        }

        // This should not happen. If client Id is registered, we should have it.
        if (processedClientId == null) {
            // Missing client id.
            return;
        }

        // Now we have everything to register token to OOB it self.
        registerOOBClientId(processedClientId, mCurrentPushToken, new Protocols.GenericHandler() {
            @Override
            public void onFinished(final boolean success, final String error) {
                if (success) {
                    main.getStorageFast().writeString(mCurrentPushToken, C_STORAGE_LAST_REGISTERED_TOKEN_ID);
                    notifyAboutStatusChange();
                }
            }
        });
    }

    /**
     * Calls the success callback.
     * @param completionHandler Completion handler.
     */
    private void returnSuccessToHandler(@Nullable final Protocols.GenericHandler completionHandler) {
        if (completionHandler != null) {
            completionHandler.onFinished(true, null);
        }
    }

    /**
     * Calls the error callback.
     * @param completionHandler Completion handler.
     */
    private void returnErrorToHandler(@Nullable final Protocols.GenericHandler completionHandler, @Nullable final String error) {
        if (completionHandler != null) {
            completionHandler.onFinished(false, TAG + ": " + error);
        }
    }

    /**
     * Registers the client ID with OOB.
     * @param clientId Client ID.
     * @param token Token.
     * @param completionHandler Callback returned back to the application on completion.
     */
    private void registerOOBClientId(@NonNull final String clientId, @NonNull final String token, @NonNull final Protocols.GenericHandler completionHandler) {
        final OobNotificationManager notifyManager = mOobManager.getOobNotificationManager(clientId);
        final List<OobNotificationProfile> arrProfiles = Collections.singletonList(new OobNotificationProfile(Configuration.CFG_OOB_CHANNEL, token));

        notifyManager.setNotificationProfiles(arrProfiles, new OobSetNotificationProfileCallback() {
            @Override
            public void onSetNotificationProfileResult(final OobResponse oobResponse) {
                final boolean success = oobResponse != null && oobResponse.isSucceeded();
                completionHandler.onFinished(success, oobResponse.getMessage());
            }
        });
    }

    /**
     * Unregisters the client ID from OOB.
     * @param clientId Client ID.
     * @param completionHandler Callback returned back to the application on completion.
     */
    private void unRegisterOOBClientId(@NonNull final String clientId, @NonNull final Protocols.GenericHandler completionHandler) {

        final OobNotificationManager notifyManager = mOobManager.getOobNotificationManager(clientId);
        notifyManager.clearNotificationProfiles(new OobClearNotificationProfileCallback() {
            @Override
            public void onClearNotificationProfileResult(final OobResponse oobResponse) {
                final boolean success = oobResponse != null && oobResponse.isSucceeded();
                completionHandler.onFinished(success, success ? null : oobResponse.getMessage());
            }
        });
    }

    /**
     * Notifies about changed status.
     */
    private void notifyAboutStatusChange() {
        // Notify in UI thread.
        final MainActivity listener = CMain.sharedInstance().getCurrentListener();
        if (listener != null) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    listener.updatePushRegistrationStatus();
                }
            });
        }
    }
}
