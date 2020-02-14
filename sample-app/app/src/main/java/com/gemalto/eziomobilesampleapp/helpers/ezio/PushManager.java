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

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;

import com.gemalto.eziomobilesampleapp.Configuration;
import com.gemalto.eziomobilesampleapp.MainActivity;
import com.gemalto.eziomobilesampleapp.R;
import com.gemalto.eziomobilesampleapp.helpers.Main;
import com.gemalto.eziomobilesampleapp.helpers.Protocols;
import com.gemalto.idp.mobile.core.ApplicationContextHolder;
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

/**
 * Manager handling all push related actions. Register / unregister from OOB, updating push token etc.
 */
public class PushManager {

    //region Defines

    public static final String NOTIFICATION_ID_INCOMING_MESSAGE = "NotificationIdIncomingMessage";


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
    private final static String STORAGE_LAST_PROVIDED_TOKEN_ID = "LastProvidedTokenId";

    // Last token actually registered with current ClientId.
    // Since demo app is for singe token only we don't care about relations.
    private final static String STORAGE_LAST_REGISTERED_TOKEN_ID = "LastRegistredTokenId";

    // Last registered OOB ClientId.
    private final static String STORAGE_KEY_CLIENT_ID = "ClientId";

    // Stored in fast storage to prevent reading encrypted data.
    private final static String STORAGE_KEY_CLIENT_ID_STAT = "ClientIdState";

    // Message type we want to handle. Contain message id to fetch and origin client id.
    public final static String PUSH_MESSAGE_TYPE = "com.gemalto.msm";
    private final static String PUSH_MESSAGE_CLIENT_ID = "clientId";
    private final static String PUSH_MESSAGE_MESSAGE_ID = "messageId";

    private String mCurrentPushToken = null;
    private OobManager mOobManager = null;

    //endregion

    //region Life Cycle

    public PushManager()  {
        mCurrentPushToken = Main.sharedInstance().getStorageFast().readString(STORAGE_LAST_PROVIDED_TOKEN_ID);
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

    public boolean isPushTokenRegistered() {
        return Main.sharedInstance().getStorageFast().readString(STORAGE_LAST_REGISTERED_TOKEN_ID) != null;
    }

    /**
     * Whenever there is some incoming message from server ready to be fetched.
     * @return True if there is an message in queue.
     */
    public boolean isIncomingMessageInQueue() {
        return lastMessageIdRead() != null;
    }

    void registerToken(@Nullable final String token) {
        // Store provided token.
        if (mCurrentPushToken == null || (token != null && !mCurrentPushToken.equalsIgnoreCase(token))) {
            Main.sharedInstance().getStorageFast().writeString(token, STORAGE_LAST_PROVIDED_TOKEN_ID);
            mCurrentPushToken = token;
        }

        // Check if new registration is needed even if token is same like last time.
        registerCurrent(null);
    }

    void registerClientId(@NonNull final String clientId) {
        final Main main = Main.sharedInstance();

        // There is not much secure about Client Id, we are using secure storage just as showcase.
        // Because of that we will also update state in fast storage to not affect performance.
        main.getStorageSecure().writeString(clientId, STORAGE_KEY_CLIENT_ID);
        main.getStorageFast().writeInteger(ClientIdState.Registered.mKey, STORAGE_KEY_CLIENT_ID_STAT);

        // Check if new registration is needed.
        registerCurrent(clientId);
    }

    void registerOOBWithUserId(@NonNull final String userId, @Nullable final SecureString regCode, final OobRegistrationCallback completionHandler) {
        final OobRegistrationManager regManager = mOobManager.getOobRegistrationManager();
        final OobRegistrationRequest request = new OobRegistrationRequest(userId, userId, OobRegistrationRequest.RegistrationMethod.REGISTRATION_CODE, regCode);

        // We don't have to solve UI thread. It's handled by logic layer.
        regManager.register(request, completionHandler);
    }

    public void unregisterOOBWithCompletionHandler(@NonNull final Protocols.GenericHandler handler) {
        final Main main = Main.sharedInstance();
        final Protocols.GenericHandler.Sync syncHandler = new Protocols.GenericHandler.Sync(handler);

        // Push token is registered
        if (isPushTokenRegistered()) {
            // Call unregister
            unRegisterOOBClientId(main.getStorageSecure().readString(STORAGE_KEY_CLIENT_ID), (success, error) -> {
                if (success) {
                    // Remove all stored values.
                    main.getStorageFast().removeValue(STORAGE_LAST_REGISTERED_TOKEN_ID);
                    main.getStorageFast().removeValue(STORAGE_KEY_CLIENT_ID_STAT);
                    main.getStorageSecure().removeValue(STORAGE_KEY_CLIENT_ID);
                }

                // Do not return in UI thread. It's used only from token manager.
                syncHandler.onFinished(success, error);
            });
        } else {
            syncHandler.onFinished(true, null);
        }
    }

    public void fetchMessage() {
        // We don't have all required permissions. Wait for init.
        // Full application should handle this scenario even if it might happen only when user
        // disable some permission after they was already acquires.
        if (mOobManager == null) {
            return;
        }

        final MainActivity listener = Main.sharedInstance().getCurrentListener();

        // React on message type com.gemalto.msm with supported view controller on screen.
        // This is just to simplify sample app scenario. Real application should handle all notification all the time.
        if (listener == null) {
            return;
        }

        // Display loading bar to indicate message downloading.
        if (Looper.getMainLooper().equals(Looper.myLooper())) {
            listener.loadingIndicatorShow(Main.getString(R.string.PUSH_PROCESSING));
        } else {
            new Handler(Looper.getMainLooper()).post(() -> {
                listener.loadingIndicatorShow(Main.getString(R.string.PUSH_PROCESSING));
            });
        }

        // Operation is asynchronous, but it still block UI since it's slow.
        // Run it in background directly to have UI smoother.
        AsyncTask.execute(() -> {
            final String locClientId = Main.sharedInstance().getStorageSecure().readString(STORAGE_KEY_CLIENT_ID);


            // Prepare manager with current client and provider id.
            final OobMessageManager oobMessageManager = mOobManager.getOobMessageManager(locClientId, Configuration.CFG_OOB_PROVIDER_ID);

            // Download message content.
            // Some messages might already be pre-fetched so we don't have to download them.
            // For simplicity we will download all of them.
            final OobFetchMessageCallback fetchMessageCallback = oobFetchMessageResponse -> {
                // After fetch keep everything in UI thread since there is a lot of user interaction.
                new Handler(Looper.getMainLooper()).post(() -> {
                    // Notify about possible error
                    if (!oobFetchMessageResponse.isSucceeded() || oobFetchMessageResponse.getOobIncomingMessage() == null) {
                        listener.loadingIndicatorHide();
                        listener.showMessage(R.string.PUSH_NOTHING_TO_FETCH);
                        return;
                    }

                    // Since we might support multiple message type, it's cleaner to have separate method for that.
                    if (!processIncomingMessage(oobFetchMessageResponse.getOobIncomingMessage(), oobMessageManager, listener)) {
                        // Hide indicator in case that message was not processed.a
                        // Otherwise indicator will be hidden by specific method.
                        new Handler(Looper.getMainLooper()).post(listener::loadingIndicatorHide);
                    }
                });
            };

            // Check if there is any stored incoming message id.
            final String messageId = this.lastMessageIdRead();
            if (messageId != null) {
                // Remove last stored id and notify UI.
                this.lastMessageIdDelete();

                oobMessageManager.fetchMessage(messageId, fetchMessageCallback);
            } else {
                // Try to fetch any possible messages on server.
                oobMessageManager.fetchMessage(30, fetchMessageCallback);
            }
        });
    }

    public void processIncomingPush(@Nullable final Map<String, String> data) {
        // React on message type com.gemalto.msm
        if (data == null || !data.containsKey(PUSH_MESSAGE_TYPE)) {
            return;
        }

        try {
            // Get client and message id out of it.
            final JSONObject dataJSON = new JSONObject(data.get(PUSH_MESSAGE_TYPE));
            final String msgClientId = dataJSON.getString(PUSH_MESSAGE_CLIENT_ID);
            final String msgMessageId = dataJSON.getString(PUSH_MESSAGE_MESSAGE_ID);
            final String locClientId = Main.sharedInstance().getStorageSecure().readString(STORAGE_KEY_CLIENT_ID);

            // Find related token / client id on local. React only on current one.
            if (!msgClientId.equalsIgnoreCase(locClientId)) {
                return;
            }

            // Store current id and send local notification to UI.
            this.lastMessageIdWrite(msgMessageId);
        } catch (final JSONException exception) {
            // Ignore invalid message in demo.
        }
    }

    public String getCurrentPushToken() {
        return mCurrentPushToken;
    }

    //endregion

    //region Message handlers

    private boolean processIncomingMessage(@NonNull final OobIncomingMessage message,
                                           @NonNull final OobMessageManager oobMessageManager,
                                           @NonNull final MainActivity handler) {
        boolean retValue = false;

        // Sign request.
        if (message.getMessageType().equalsIgnoreCase(OobIncomingMessageType.TRANSACTION_SIGNING)) {
            retValue = processTransactionSigningRequest((OobTransactionSigningRequest) message, oobMessageManager, handler);
        }

        return retValue;
    }

    private boolean processTransactionSigningRequest(@NonNull final OobTransactionSigningRequest request,
                                                     @NonNull final OobMessageManager oobMessageManager,
                                                     @NonNull final MainActivity handler) {
        final boolean[] retValue = {false};
        final String[] errorMessage = {null};

        // Get message subject key and fill in all values.
        String subject = Main.getStringByKeyName(request.getSubject().toString());
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

            handler.approveOTP(subject, serverChallange, (otp, error, authInput, serverChallenge) -> {
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

    private boolean notifyHandlerAboutPushSend(@NonNull final MainActivity handler,
                                               @Nullable final OobMessageResponse oobMessageResponse) {
        final boolean retValue = oobMessageResponse != null;

        if (retValue) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    handler.loadingIndicatorHide();
                    handler.showMessage(R.string.PUSH_SENT);
                }
            });
        }

        return retValue;
    }

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

        final Main main = Main.sharedInstance();

        // We don't have any client id at all.
        if (main.getStorageFast().readInteger(STORAGE_KEY_CLIENT_ID_STAT) == ClientIdState.Unregistered.mKey) {
            // This will probably happen when app will get push token without any enrolled token / client id.
            return;
        }

        // Last registered token is same as current one.
        final String lastReg = main.getStorageFast().readString(STORAGE_LAST_REGISTERED_TOKEN_ID);
        if (lastReg != null && lastReg.equalsIgnoreCase(mCurrentPushToken)) {
            return;
        }

        // Get current Client Id or use one provided from API to safe some time.
        if (processedClientId == null) {
            processedClientId = main.getStorageSecure().readString(STORAGE_KEY_CLIENT_ID);
        }

        // This should not happen. If client Id is registered, we should have it.
        if (processedClientId == null) {
            // Missing client id.
            return;
        }

        // Now we have everything to register token to OOB it self.
        registerOOBClientId(processedClientId, mCurrentPushToken, (success, error) -> {
            if (success) {
                main.getStorageFast().writeString(mCurrentPushToken, STORAGE_LAST_REGISTERED_TOKEN_ID);
            }
        });
    }

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


    //region Storage - Message Id

    private static final String STORAGE_LAST_MESSAGE_ID = "LastIncomingMessageId";

    private boolean lastMessageIdWrite(final String messageId) {
        final boolean retValue = Main.sharedInstance().getStorageFast().writeString(messageId, STORAGE_LAST_MESSAGE_ID);
        if (retValue) {
            notifyAboutIncomingMessageStatusChange();
        }
        return retValue;
    }

    private String lastMessageIdRead() {
        return Main.sharedInstance().getStorageFast().readString(STORAGE_LAST_MESSAGE_ID);
    }

    private boolean lastMessageIdDelete() {
        final boolean retValue = Main.sharedInstance().getStorageFast().removeValue(STORAGE_LAST_MESSAGE_ID);
        if (retValue) {
            notifyAboutIncomingMessageStatusChange();
        }
        return retValue;
    }

    private void notifyAboutIncomingMessageStatusChange() {
        // Notify about status change only when SDK is initialized with proper context.
        final Context context = ApplicationContextHolder.getContext();
        if (context != null) {
            final Intent intent = new Intent(NOTIFICATION_ID_INCOMING_MESSAGE);
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
        }
    }

    //endregion
}
