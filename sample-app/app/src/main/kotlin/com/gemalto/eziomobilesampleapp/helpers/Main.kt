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
package com.gemalto.eziomobilesampleapp.helpers

import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import com.gemalto.eziomobilesampleapp.Configuration
import com.gemalto.eziomobilesampleapp.EzioSampleApp
import com.gemalto.eziomobilesampleapp.MainActivity
import com.gemalto.eziomobilesampleapp.R
import com.gemalto.eziomobilesampleapp.helpers.Protocols.GenericHandler
import com.gemalto.eziomobilesampleapp.helpers.Protocols.StorageProtocol
import com.gemalto.eziomobilesampleapp.helpers.app.storage.SharedPreferences
import com.gemalto.eziomobilesampleapp.helpers.ezio.PushManager
import com.gemalto.eziomobilesampleapp.helpers.ezio.QRCodeManager
import com.gemalto.eziomobilesampleapp.helpers.ezio.TokenManager
import com.gemalto.eziomobilesampleapp.helpers.ezio.storage.SecureStorage
import com.gemalto.idp.mobile.core.ApplicationContextHolder
import com.gemalto.idp.mobile.core.IdpCore
import com.gemalto.idp.mobile.core.SecurityDetectionService
import com.gemalto.idp.mobile.core.passwordmanager.PasswordManagerException
import com.gemalto.idp.mobile.core.util.SecureByteArray
import com.gemalto.idp.mobile.core.util.SecureString
import com.gemalto.idp.mobile.msp.MspConfiguration
import com.gemalto.idp.mobile.oob.OobConfiguration
import com.gemalto.idp.mobile.otp.OtpConfiguration
import com.thalesgroup.gemalto.securelog.SecureLogConfig
import java.net.MalformedURLException

/**
 * Main app singleton. With instances of all managers.
 * It is available whole app lifetime.
 */
class Main {
    /**
     * Check whenever SDK was successfully initialised with all permisions.
     *
     * @return True in case that SDK is ready to use.
     */
    @get:Synchronized
    var isInited: Boolean = false
        private set

    /**
     * @return Instance of secure storage. (Ezio SecureStorage)
     */
    @get:Synchronized
    var storageSecure: StorageProtocol? = null
        private set

    /**
     * @return Instance of fast insecure storage. (iOS UserDefaults)
     */
    @get:Synchronized
    var storageFast: StorageProtocol? = null
        private set

    /**
     * @return Manager used for handling all push related actions.
     */
    @get:Synchronized
    var managerPush: PushManager? = null
        private set

    /**
     * @return Manager used for handling all token related actions.
     */
    @get:Synchronized
    var managerToken: TokenManager? = null
        private set

    /**
     * @return Manager used for handling QR codes.
     */
    var managerQRCode: QRCodeManager? = null
        private set

    /**
     * Retrieves `IdpCore` instance.
     *
     * @return `IdpCore` instance.
     */
    @get:Synchronized
    var core: IdpCore? = null
        private set
    private var mUiHandler: GenericHandler? = null

    /**
     * Unregisters the UI handler.
     */
    @Synchronized
    fun unregisterUiHandler() {
        mUiHandler = null
    }

    /**
     * Initializes the main SDK.
     *
     * @param context Android application context.
     */
    fun init(context: Context) {
        // App context used in all sdk levels.
        ApplicationContextHolder.setContext(context)

        // Initialise basic managers without all permissions yet.
        this.storageFast = SharedPreferences()
        this.managerPush = PushManager()
        this.managerQRCode = QRCodeManager()
    }

    /**
     * Initializes the SDK after all mandatory permissions have been requested.
     *
     * @param handler Callback.
     */
    fun initWithPermissions(handler: GenericHandler) {
        // Sync handler will all all methods in ui thread.

        mUiHandler = GenericHandler.Sync(handler)

        SecurityDetectionService.setDebuggerDetection(false)

        // Make sure, that we will always check isConfigured first. Multiple call of init will cause crash / run time exception.
        if (IdpCore.isConfigured()) {
            if (mUiHandler != null) {
                mUiHandler?.onFinished(true, null)
            }

            return
        }
        Thread(object : Runnable {
            override fun run() {
                sInstance?.let {
                    synchronized(it) {
                        IdpCore.configureSecureLog(
                            SecureLogConfig.Builder(ApplicationContextHolder.getContext())
                                .publicKey(
                                    Configuration.CFG_SLOG_MODULUS,
                                    Configuration.CFG_SLOG_EXPONENT
                                )
                                .build()
                        )
                        core = IdpCore.configure(
                            Configuration.CFG_SDK_ACTIVATION_CODE,
                            configurationOob,
                            configurationOtp,
                            configurationMsp
                        )
                    }
                }

                val password =
                    core?.getSecureContainerFactory()?.fromString("SecureStoragePassword")

                SecurityDetectionService.setDebuggerDetection(false)
                try {
                    // Login so we can use secure storage, OOB etc..
                    val passwordManager = core?.getPasswordManager()
                    if (passwordManager?.isPasswordSet() == false) {
                        passwordManager?.setPassword(password)
                    }
                    passwordManager?.login(password)

                    sInstance?.let {
                        synchronized(it) {
                            // Init rest of the managers and update basic one with new permissions.
                            storageSecure = SecureStorage()
                            managerToken = TokenManager()
                            managerPush?.initWithPermissions()

                            // Mark helper as prepared. So we don't have to call this ever again.
                            isInited = true
                        }
                    }

                    // Notify handler
                    sInstance?.let {
                        synchronized(it) {
                            if (mUiHandler != null) {
                                mUiHandler?.onFinished(true, null)
                            }
                        }
                    }
                } catch (e: PasswordManagerException) {
                    sInstance?.let {
                        synchronized(it) {
                            if (mUiHandler != null) {
                                mUiHandler?.onFinished(true, e.getLocalizedMessage())
                            }
                        }
                    }
                } catch (e: MalformedURLException) {
                    sInstance?.let {
                        synchronized(it) {
                            if (mUiHandler != null) {
                                mUiHandler?.onFinished(true, e.getLocalizedMessage())
                            }
                        }
                    }
                }
            }
        }).start()
    }

    //endregion
    //region Static Helpers
    /**
     * Creates a `SecureString` from `String`.
     *
     * @param value `String` input.
     * @return `SecureString` output.
     */
    @Synchronized
    fun secureStringFromString(value: String?): SecureString? {
        return core?.getSecureContainerFactory()?.fromString(value)
    }

    /**
     * Creates a `SecureByteArray` from `byte[]`.
     *
     * @param value `byte[]` input.
     * @return `SecureByteArray` output.
     */
    @Synchronized
    fun secureByteArrayFromBytes(value: ByteArray?, wipeSource: Boolean): SecureByteArray? {
        return core?.getSecureContainerFactory()?.createByteArray(value, wipeSource)
    }

    //endregion
    //region Public API

    val currentListener: MainActivity?
        /**
         * Gets the current activity.
         *
         * @return Current activity.
         */
        get() {
            val currentActivity =
                (ApplicationContextHolder.getContext()
                    .getApplicationContext() as EzioSampleApp).currentActivity
            if ((currentActivity is MainActivity)) return currentActivity
            return null
        }

    //endregion
    //region Private Helpers
    private val configurationOtp: OtpConfiguration?
        /**
         * Gets the OTP configuration.
         *
         * @return OTP configuration.
         */
        get() =// OTP module is required for token management and OTP calculation.
            OtpConfiguration.Builder()
                .setRootPolicy(Configuration.CFG_OTP_ROOT_POLICY)
                .build()

    private val configurationOob: OobConfiguration?
        /**
         * Gets the OOB configuration.
         *
         * @return OOB configuration.
         */
        get() =// OOB module is required for push notifications.
            OobConfiguration.Builder() // Device fingerprint is used for security reason. This way app can add some additional input for internal encryption mechanism.
                // This value must remain the same all the time. Othewise all provisioned tokens will not be valid any more.
                .setDeviceFingerprintSource(Configuration.CFG_SDK_DEVICE_FINGERPRINT_SOURCE) // Jailbreak policy for OOB module. See EMOobJailbreakPolicyIgnore for more details.
                .setRootPolicy(Configuration.CFG_OOB_ROOT_POLICY) // For debug and ONLY debug reasons we might lower some TLS configuration.
                .setTlsConfiguration(Configuration.CFG_SDK_TLS_CONFIGURATION)
                .build()

    private val configurationMsp: MspConfiguration?
        /**
         * Gets the MSP configuration.
         *
         * @return MSP configuration.
         */
        get() {
            // Mobile Signing Protocol QR parsing, push messages etc..
            val builder =
                MspConfiguration.Builder()

            // Set obfuscation
            if (Configuration.CFG_MSP_OBFUSCATION_CODE != null) {
                builder.setObfuscationKeys(Configuration.CFG_MSP_OBFUSCATION_CODE)
            }
            // Set signature
            if (Configuration.CFG_MSP_SIGN_KEYS != null) {
                builder.setSignatureKeys(Configuration.CFG_MSP_SIGN_KEYS)
            }

            return builder.build()
        } //endregion

    companion object {
        //region Defines
        private var sInstance: Main? = null

        //endregion
        //region Life Cycle
        /**
         * Gets the singleton instance of `Main`.
         *
         * @return Singleton instance of `Main`.
         */
        @Synchronized
        fun sharedInstance(): Main? {
            if (sInstance == null) {
                sInstance = Main()
            }

            return sInstance
        }

        /**
         * Gets a string resource.
         *
         * @param aString Name.
         * @return String resource.
         */
        fun getStringByKeyName(aString: String?): String {
            val context = ApplicationContextHolder.getContext()
            val resId =
                context.getResources().getIdentifier(aString, "string", context.getPackageName())
            if (resId != 0) {
                return getString(resId)
            } else {
                return getString(R.string.message_not_found)
            }
        }

        /**
         * Gets a string resource.
         *
         * @param stringId Id.
         * @return String resource.
         */
        fun getString(@StringRes stringId: Int): String {
            return ApplicationContextHolder.getContext().getString(stringId)
        }

        /**
         * Checks for runtime permission.
         *
         * @param activity    Calling activity.
         * @param askForThem  `True` if missing permission should be requested, else `false`.
         * @param permissions List of permissions.
         * @return `True` if permissions are present, else `false`.
         */
        @TargetApi(23)
        fun checkPermissions(
            activity: Activity,
            askForThem: Boolean,
            vararg permissions: String
        ): Boolean {
            // Old SDK version does not have dynamic permissions.

            if (Build.VERSION.SDK_INT < 23) {
                return true
            }

            val permissionsToCheck: MutableList<String?> = ArrayList<String?>()

            for (permission in permissions) {
                if (ContextCompat.checkSelfPermission(
                        activity,
                        permission
                    ) != PermissionChecker.PERMISSION_GRANTED
                ) {
                    // Should we show an explanation?
                    if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                        // Show an explanation to the user *asynchronously* -- don't block
                        // this thread waiting for the user's response! After the user
                        // sees the explanation, try again to request the permission.
                        Toast.makeText(
                            activity,
                            "Requesting permission - " + permission,
                            Toast.LENGTH_LONG
                        ).show()
                    }

                    permissionsToCheck.add(permission)
                }
            }

            if (!permissionsToCheck.isEmpty() && askForThem) {
                ActivityCompat
                    .requestPermissions(activity, permissionsToCheck.toTypedArray<String?>(), 0)
            }

            return permissionsToCheck.isEmpty()
        }
    }
}
