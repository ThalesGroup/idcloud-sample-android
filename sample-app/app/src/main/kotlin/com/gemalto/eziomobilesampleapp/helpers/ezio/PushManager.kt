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

import android.content.Intent
import android.os.AsyncTask
import android.os.Handler
import android.os.Looper
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.gemalto.eziomobilesampleapp.Configuration
import com.gemalto.eziomobilesampleapp.MainActivity
import com.gemalto.eziomobilesampleapp.R
import com.gemalto.eziomobilesampleapp.helpers.Main
import com.gemalto.eziomobilesampleapp.helpers.Protocols.GenericHandler
import com.gemalto.eziomobilesampleapp.helpers.Protocols.OTPDelegate
import com.gemalto.idp.mobile.authentication.AuthInput
import com.gemalto.idp.mobile.core.ApplicationContextHolder
import com.gemalto.idp.mobile.core.util.SecureString
import com.gemalto.idp.mobile.msp.MspBaseAlgorithm
import com.gemalto.idp.mobile.msp.MspFactory
import com.gemalto.idp.mobile.msp.MspOathData
import com.gemalto.idp.mobile.msp.exception.MspException
import com.gemalto.idp.mobile.oob.OobException
import com.gemalto.idp.mobile.oob.OobManager
import com.gemalto.idp.mobile.oob.OobMessageResponse
import com.gemalto.idp.mobile.oob.OobModule
import com.gemalto.idp.mobile.oob.OobResponse
import com.gemalto.idp.mobile.oob.message.OobFetchMessageCallback
import com.gemalto.idp.mobile.oob.message.OobFetchMessageResponse
import com.gemalto.idp.mobile.oob.message.OobIncomingMessage
import com.gemalto.idp.mobile.oob.message.OobIncomingMessageType
import com.gemalto.idp.mobile.oob.message.OobMessageManager
import com.gemalto.idp.mobile.oob.message.OobSendMessageCallback
import com.gemalto.idp.mobile.oob.message.OobTransactionSigningRequest
import com.gemalto.idp.mobile.oob.message.OobTransactionSigningResponse
import com.gemalto.idp.mobile.oob.notification.OobClearNotificationProfileCallback
import com.gemalto.idp.mobile.oob.notification.OobNotificationProfile
import com.gemalto.idp.mobile.oob.notification.OobSetNotificationProfileCallback
import com.gemalto.idp.mobile.oob.registration.OobRegistrationCallback
import com.gemalto.idp.mobile.oob.registration.OobRegistrationRequest
import org.json.JSONException
import org.json.JSONObject
import java.net.MalformedURLException
import java.net.URL

/**
 * Manager handling all push related actions. Register / unregister from OOB, updating push token etc.
 */
class PushManager {
    enum class ClientIdState(val mKey: Int) {
        Unregistered(0),
        Registered(1)
    }

    private var mCurrentPushToken: String
    private var mOobManager: OobManager? = null

    @Throws(MalformedURLException::class)
    fun initWithPermissions() {
        mOobManager = OobModule.create().createOobManager(
            URL(Configuration.CFG_OOB_URL),
            Configuration.CFG_OOB_DOMAIN,
            Configuration.CFG_OOB_APP_ID,
            Configuration.CFG_OOB_RSA_KEY_EXPONENT,
            Configuration.CFG_OOB_RSA_KEY_MODULUS
        )

        // Call register just in case we did get token already.
        registerCurrent(null)
    }

    val isPushTokenRegistered: Boolean
        //endregion
        get() = Main.sharedInstance()?.storageFast
            ?.readString(STORAGE_LAST_REGISTERED_TOKEN_ID) != null

    val isIncomingMessageInQueue: Boolean
        /**
         * Whenever there is some incoming message from server ready to be fetched.
         *
         * @return True if there is an message in queue.
         */
        get() = lastMessageIdRead() != null

    fun registerToken(token: String) {
        // Store provided token.
        if (mCurrentPushToken == null || (token != null && !mCurrentPushToken.equals(
                token,
                ignoreCase = true
            ))
        ) {
            Main.sharedInstance()?.storageFast
                ?.writeString(token, STORAGE_LAST_PROVIDED_TOKEN_ID)
            mCurrentPushToken = token
        }

        // Check if new registration is needed even if token is same like last time.
        registerCurrent(null)
    }

    fun registerClientId(clientId: String) {
        val main = Main.sharedInstance()

        // There is not much secure about Client Id, we are using secure storage just as showcase.
        // Because of that we will also update state in fast storage to not affect performance.
        main?.storageSecure?.writeString(clientId, STORAGE_KEY_CLIENT_ID)
        main?.storageFast
            ?.writeInteger(ClientIdState.Registered.mKey, STORAGE_KEY_CLIENT_ID_STAT)

        // Check if new registration is needed.
        registerCurrent(clientId)
    }

    fun registerOOBWithUserId(
        userId: String?,
        regCode: SecureString?,
        completionHandler: OobRegistrationCallback?
    ) {
        val regManager = mOobManager?.getOobRegistrationManager()
        val request = OobRegistrationRequest(
            userId,
            userId,
            OobRegistrationRequest.RegistrationMethod.REGISTRATION_CODE,
            regCode
        )

        // We don't have to solve UI thread. It's handled by logic layer.
        regManager?.register(request, completionHandler)
    }

    fun unregisterOOBWithCompletionHandler(handler: GenericHandler) {
        val main = Main.sharedInstance()
        val syncHandler = GenericHandler.Sync(handler)

        // Push token is registered
        if (this.isPushTokenRegistered) {
            // Call unregister
            unRegisterOOBClientId(
                main?.storageSecure?.readString(STORAGE_KEY_CLIENT_ID),
                object : GenericHandler {
                    override fun onFinished(success: Boolean, error: String?) {
                        if (success) {
                            // Remove all stored values.
                            main?.storageFast?.removeValue(STORAGE_LAST_REGISTERED_TOKEN_ID)
                            main?.storageFast?.removeValue(STORAGE_KEY_CLIENT_ID_STAT)
                            main?.storageSecure?.removeValue(STORAGE_KEY_CLIENT_ID)
                        }
                        // Do not return in UI thread. It's used only from token manager.
                        syncHandler.onFinished(success, error)
                    }
                })
        } else {
            syncHandler.onFinished(true, null)
        }
    }

    fun fetchMessage() {
        // We don't have all required permissions. Wait for init.
        // Full application should handle this scenario even if it might happen only when user
        // disable some permission after they was already acquires.
        if (mOobManager == null) {
            return
        }

        val listener = Main.sharedInstance()?.currentListener

        // React on message type com.gemalto.msm with supported view controller on screen.
        // This is just to simplify sample app scenario. Real application should handle all notification all the time.
        if (listener == null) {
            return
        }

        // Display loading bar to indicate message downloading.
        if (Looper.getMainLooper() == Looper.myLooper()) {
            listener.loadingIndicatorShow(Main.getString(R.string.PUSH_PROCESSING))
        } else {
            Handler(Looper.getMainLooper()).post(Runnable {
                listener.loadingIndicatorShow(
                    Main.getString(
                        R.string.PUSH_PROCESSING
                    )
                )
            })
        }

        // Operation is asynchronous, but it still block UI since it's slow.
        // Run it in background directly to have UI smoother.
        AsyncTask.execute(Runnable {
            val locClientId =
                Main.sharedInstance()?.storageSecure?.readString(STORAGE_KEY_CLIENT_ID)
            // Prepare manager with current client and provider id.
            val oobMessageManager =
                mOobManager?.getOobMessageManager(locClientId, Configuration.CFG_OOB_PROVIDER_ID)

            // Download message content.
            // Some messages might already be pre-fetched so we don't have to download them.
            // For simplicity we will download all of them.
            val fetchMessageCallback =
                OobFetchMessageCallback { oobFetchMessageResponse: OobFetchMessageResponse? ->
                    // After fetch keep everything in UI thread since there is a lot of user interaction.
                    Handler(Looper.getMainLooper()).post(Runnable post@{
                        // Notify about possible error
                        if (oobFetchMessageResponse?.isSucceeded() == false || oobFetchMessageResponse?.getOobIncomingMessage() == null) {
                            listener.loadingIndicatorHide()
                            listener.showMessage(R.string.PUSH_NOTHING_TO_FETCH)
                            return@post
                        }

                        // Since we might support multiple message type, it's cleaner to have separate method for that.
                        if (!processIncomingMessage(
                                oobFetchMessageResponse.getOobIncomingMessage(),
                                oobMessageManager,
                                listener
                            )
                        ) {
                            // Hide indicator in case that message was not processed.a
                            // Otherwise indicator will be hidden by specific method.
                            Handler(Looper.getMainLooper()).post(Runnable { listener.loadingIndicatorHide() })
                        }
                    })
                }

            // Check if there is any stored incoming message id.
            val messageId = this.lastMessageIdRead()
            if (messageId != null) {
                // Remove last stored id and notify UI.
                this.lastMessageIdDelete()

                oobMessageManager?.fetchMessage(messageId, fetchMessageCallback)
            } else {
                // Try to fetch any possible messages on server.
                oobMessageManager?.fetchMessage(30, fetchMessageCallback)
            }
        })
    }

    fun processIncomingPush(data: MutableMap<String?, String?>?) {
        // React on message type com.gemalto.msm
        if (data == null || !data.containsKey(PUSH_MESSAGE_TYPE)) {
            return
        }

        try {
            // Get client and message id out of it.
            val dataJSON = JSONObject(data.get(PUSH_MESSAGE_TYPE))
            val msgClientId = dataJSON.getString(PUSH_MESSAGE_CLIENT_ID)
            val msgMessageId = dataJSON.getString(PUSH_MESSAGE_MESSAGE_ID)
            val locClientId = Main.sharedInstance()?.storageSecure?.readString(
                STORAGE_KEY_CLIENT_ID
            )

            // Find related token / client id on local. React only on current one.
            if (!msgClientId.equals(locClientId, ignoreCase = true)) {
                return
            }

            // Store current id and send local notification to UI.
            this.lastMessageIdWrite(msgMessageId)
        } catch (exception: JSONException) {
            // Ignore invalid message in demo.
        }
    }

    //endregion
    //region Message handlers
    private fun processIncomingMessage(
        message: OobIncomingMessage,
        oobMessageManager: OobMessageManager?,
        handler: MainActivity
    ): Boolean {
        var retValue = false

        // Sign request.
        if (message.getMessageType()
                .equals(OobIncomingMessageType.TRANSACTION_SIGNING, ignoreCase = true)
        ) {
            retValue = processTransactionSigningRequest(
                message as OobTransactionSigningRequest,
                oobMessageManager,
                handler
            )
        }

        return retValue
    }

    private fun processTransactionSigningRequest(
        request: OobTransactionSigningRequest,
        oobMessageManager: OobMessageManager?,
        handler: MainActivity
    ): Boolean {
        val retValue = booleanArrayOf(false)
        val errorMessage = arrayOf<String?>(null)

        // Get message subject key and fill in all values.
        var subject = Main.getStringByKeyName(request.getSubject().toString())
        val origSubject = subject
        var params = ""
        if (request.getMeta().size > 0) {
            for (entry in request.getMeta().entries) {
                val placeholder = "%" + entry.key
                subject = subject.replace(placeholder, entry.value)
                params += "\n" + entry.key + ": " + entry.value
            }
            if (origSubject == subject) {
                // Message string does not contain the request fields, append them to message instead
                subject = Main.getString(R.string.message_subject_transaction_default) + params
            }
        }

        try {
            // Try to parse frame.
            val parser = MspFactory.createMspParser()
            val data = parser.parseMspData(parser.parse(request.getMspFrame().toByteArray()))

            // For purpose of this sample app we will support only OATH.
            if (data.getBaseAlgo() != MspBaseAlgorithm.BASE_OATH) {
                handler.loadingIndicatorHide()
                return false
            }

            // Server challenge is send only for transaction sign. Not authentication.
            val ocraServerChallenge = (data as MspOathData).getOcraServerChallenge()
            var serverChallange: SecureString? = null
            if (ocraServerChallenge != null) {
                serverChallange = ocraServerChallenge.getValue()
            }

            handler.approveOTP(
                subject,
                serverChallange,
                object : OTPDelegate {
                    override fun onOTPDelegateFinished(
                        otp: SecureString?,
                        error: String?,
                        authInput: AuthInput?,
                        serverChallenge: SecureString?
                    ) {
                        // If we get OTP it means user approved request.
                        try {
                            val response = request.createResponse(
                                if (otp != null)
                                    OobTransactionSigningResponse.OobTransactionSigningResponseValue.ACCEPTED
                                else
                                    OobTransactionSigningResponse.OobTransactionSigningResponseValue.REJECTED,
                                otp,
                                null
                            )
                            // Send message and wait display result.
                            oobMessageManager?.sendMessage(
                                response,
                                OobSendMessageCallback { oobMessageResponse: OobMessageResponse? ->
                                    retValue[0] =
                                        notifyHandlerAboutPushSend(handler, oobMessageResponse)
                                })
                        } catch (exception: OobException) {
                            errorMessage[0] = exception.getLocalizedMessage()
                        }
                    }
                })
        } catch (exception: MspException) {
            errorMessage[0] = exception.getLocalizedMessage()
        }

        if (!retValue[0]) {
            // Display possible parsing issue.
            handler.loadingIndicatorHide()
            handler.showErrorIfExists(errorMessage[0])
        }

        return retValue[0]
    }


    //endregion
    // MARK: - Private Helpers
    private fun notifyHandlerAboutPushSend(
        handler: MainActivity,
        oobMessageResponse: OobMessageResponse?
    ): Boolean {
        val retValue = oobMessageResponse != null

        if (retValue) {
            Handler(Looper.getMainLooper()).post(Runnable {
                handler.loadingIndicatorHide()
                handler.showMessage(R.string.PUSH_SENT)
            })
        }

        return retValue
    }

    private fun registerCurrent(clientId: String?) {
        var processedClientId = clientId

        // We don't have all required permissions. Wait for init.
        if (mOobManager == null) {
            return
        }

        // We don't have token from app? Nothing to do without it.
        if (mCurrentPushToken == null) {
            // In full app this would not be an error, but in sample app we want to force registration OOB before token.
            return
        }

        val main = Main.sharedInstance()

        // We don't have any client id at all.
        if (main?.storageFast
                ?.readInteger(STORAGE_KEY_CLIENT_ID_STAT) == ClientIdState.Unregistered.mKey
        ) {
            // This will probably happen when app will get push token without any enrolled token / client id.
            return
        }

        // Last registered token is same as current one.
        val lastReg = main?.storageFast?.readString(STORAGE_LAST_REGISTERED_TOKEN_ID)
        if (lastReg != null && lastReg.equals(mCurrentPushToken, ignoreCase = true)) {
            return
        }

        // Get current Client Id or use one provided from API to safe some time.
        if (processedClientId == null) {
            processedClientId = main?.storageSecure?.readString(STORAGE_KEY_CLIENT_ID)
        }

        // This should not happen. If client Id is registered, we should have it.
        if (processedClientId == null) {
            // Missing client id.
            return
        }

        // Now we have everything to register token to OOB it self.
        registerOOBClientId(
            processedClientId,
            mCurrentPushToken,
            object : GenericHandler {
                override fun onFinished(success: Boolean, error: String?) {
                    if (success) {
                        main?.storageFast?.writeString(mCurrentPushToken, STORAGE_LAST_REGISTERED_TOKEN_ID)
                    }
                }
            })
    }

    private fun registerOOBClientId(
        clientId: String,
        token: String,
        completionHandler: GenericHandler
    ) {
        val notifyManager = mOobManager?.getOobNotificationManager(clientId)
        val arrProfiles = mutableListOf<OobNotificationProfile?>(
            OobNotificationProfile(
                Configuration.CFG_OOB_CHANNEL, token
            )
        )

        notifyManager?.setNotificationProfiles(
            arrProfiles,
            OobSetNotificationProfileCallback { oobResponse: OobResponse? ->
                val success = oobResponse != null && oobResponse.isSucceeded()
                completionHandler.onFinished(
                    success,
                    if (oobResponse == null) null else oobResponse.getMessage()
                )
            })
    }

    private fun unRegisterOOBClientId(
        clientId: String?,
        completionHandler: GenericHandler
    ) {
        val notifyManager = mOobManager!!.getOobNotificationManager(clientId)
        notifyManager.clearNotificationProfiles(OobClearNotificationProfileCallback { oobResponse: OobResponse? ->
            val success = oobResponse != null && oobResponse.isSucceeded()
            completionHandler.onFinished(
                success,
                if (oobResponse == null) null else oobResponse.getMessage()
            )
        })
    }


    //endregion
    //region Life Cycle
    init {
        mCurrentPushToken = Main.sharedInstance()?.storageFast?.readString(
            STORAGE_LAST_PROVIDED_TOKEN_ID
        ).toString()
    }

    private fun lastMessageIdWrite(messageId: String?) {
        val retValue =
            Main.sharedInstance()?.storageFast?.writeString(messageId, STORAGE_LAST_MESSAGE_ID)
        if (retValue == true) {
            notifyAboutIncomingMessageStatusChange()
        }
    }

    private fun lastMessageIdRead(): String? {
        return Main.sharedInstance()?.storageFast?.readString(STORAGE_LAST_MESSAGE_ID)
    }

    private fun lastMessageIdDelete() {
        val retValue = Main.sharedInstance()?.storageFast?.removeValue(STORAGE_LAST_MESSAGE_ID)
        if (retValue == true) {
            notifyAboutIncomingMessageStatusChange()
        }
    }

    private fun notifyAboutIncomingMessageStatusChange() {
        // Notify about status change only when SDK is initialized with proper context.
        val context = ApplicationContextHolder.getContext()
        if (context != null) {
            val intent: Intent = Intent(NOTIFICATION_ID_INCOMING_MESSAGE)
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
        }
    } //endregion

    companion object {
        //region Defines
        const val NOTIFICATION_ID_INCOMING_MESSAGE: String = "NotificationIdIncomingMessage"


        // Last token provided by application
        private const val STORAGE_LAST_PROVIDED_TOKEN_ID = "LastProvidedTokenId"

        // Last token actually registered with current ClientId.
        // Since demo app is for singe token only we don't care about relations.
        private const val STORAGE_LAST_REGISTERED_TOKEN_ID = "LastRegistredTokenId"

        // Last registered OOB ClientId.
        private const val STORAGE_KEY_CLIENT_ID = "ClientId"

        // Stored in fast storage to prevent reading encrypted data.
        private const val STORAGE_KEY_CLIENT_ID_STAT = "ClientIdState"

        // Message type we want to handle. Contain message id to fetch and origin client id.
        const val PUSH_MESSAGE_TYPE: String = "com.gemalto.msm"
        private const val PUSH_MESSAGE_CLIENT_ID = "clientId"
        private const val PUSH_MESSAGE_MESSAGE_ID = "messageId"

        //region Storage - Message Id
        private const val STORAGE_LAST_MESSAGE_ID = "LastIncomingMessageId"
    }
}
