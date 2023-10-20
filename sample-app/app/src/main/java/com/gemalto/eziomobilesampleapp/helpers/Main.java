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

package com.gemalto.eziomobilesampleapp.helpers;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.PermissionChecker;

import android.widget.Toast;

import com.gemalto.eziomobilesampleapp.Configuration;
import com.gemalto.eziomobilesampleapp.EzioSampleApp;
import com.gemalto.eziomobilesampleapp.MainActivity;
import com.gemalto.eziomobilesampleapp.R;
import com.gemalto.eziomobilesampleapp.helpers.app.storage.SharedPreferences;
import com.gemalto.eziomobilesampleapp.helpers.ezio.PushManager;
import com.gemalto.eziomobilesampleapp.helpers.ezio.QRCodeManager;
import com.gemalto.eziomobilesampleapp.helpers.ezio.TokenManager;
import com.gemalto.eziomobilesampleapp.helpers.ezio.storage.SecureStorage;
import com.gemalto.idp.mobile.core.ApplicationContextHolder;
import com.gemalto.idp.mobile.core.IdpCore;
import com.gemalto.idp.mobile.core.SecurityDetectionService;
import com.gemalto.idp.mobile.core.passwordmanager.PasswordManager;
import com.gemalto.idp.mobile.core.passwordmanager.PasswordManagerException;
import com.gemalto.idp.mobile.core.util.SecureByteArray;
import com.gemalto.idp.mobile.core.util.SecureString;
import com.gemalto.idp.mobile.msp.MspConfiguration;
import com.gemalto.idp.mobile.oob.OobConfiguration;
import com.gemalto.idp.mobile.otp.OtpConfiguration;
import com.thalesgroup.gemalto.securelog.SecureLogConfig;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Main app singleton. With instances of all managers.
 * It is available whole app lifetime.
 */
public class Main {

    //region Defines

    private static Main sInstance = null;

    private boolean mInited = false;
    private Protocols.StorageProtocol mStorageSecure = null;
    private Protocols.StorageProtocol mStorageFast = null;
    private PushManager mManagerPush = null;
    private TokenManager mManagerToken = null;
    private QRCodeManager mManagerQRCode = null;

    private IdpCore mCore = null;
    private Protocols.GenericHandler mUiHandler;

    //endregion

    //region Life Cycle

    /**
     * Gets the singleton instance of {@code Main}.
     *
     * @return Singleton instance of {@code Main}.
     */
    public static synchronized Main sharedInstance() {
        if (sInstance == null) {
            sInstance = new Main();
        }

        return sInstance;
    }

    /**
     * Unregisters the UI handler.
     */
    public synchronized void unregisterUiHandler() {
        mUiHandler = null;
    }

    /**
     * Initializes the main SDK.
     *
     * @param context Android application context.
     */
    public void init(final Context context) {
        // App context used in all sdk levels.
        ApplicationContextHolder.setContext(context);

        // Initialise basic managers without all permissions yet.
        mStorageFast = new SharedPreferences();
        mManagerPush = new PushManager();
        mManagerQRCode = new QRCodeManager();
    }

    /**
     * Initializes the SDK after all mandatory permissions have been requested.
     *
     * @param handler Callback.
     */
    public void initWithPermissions(@NonNull final Protocols.GenericHandler handler) {

        // Sync handler will all all methods in ui thread.
        mUiHandler = new Protocols.GenericHandler.Sync(handler);

        SecurityDetectionService.setDebuggerDetection(false);

        // Make sure, that we will always check isConfigured first. Multiple call of init will cause crash / run time exception.
        if (IdpCore.isConfigured()) {
            if (mUiHandler != null) {
                mUiHandler.onFinished(true, null);
            }

            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (sInstance) {
                    IdpCore.configureSecureLog(new SecureLogConfig.Builder(ApplicationContextHolder.getContext())
                            .publicKey(Configuration.CFG_SLOG_MODULUS, Configuration.CFG_SLOG_EXPONENT)
                            .build());

                    mCore = IdpCore.configure(Configuration.CFG_SDK_ACTIVATION_CODE,
                            getConfigurationOob(),
                            getConfigurationOtp(),
                            getConfigurationMsp());
                }

                final SecureString password = mCore.getSecureContainerFactory().fromString("SecureStoragePassword");

                SecurityDetectionService.setDebuggerDetection(false);
                try {
                    // Login so we can use secure storage, OOB etc..
                    final PasswordManager passwordManager = mCore.getPasswordManager();
                    if (!passwordManager.isPasswordSet()) {
                        passwordManager.setPassword(password);
                    }
                    passwordManager.login(password);

                    synchronized (sInstance) {
                        // Init rest of the managers and update basic one with new permissions.
                        mStorageSecure = new SecureStorage();
                        mManagerToken = new TokenManager();
                        mManagerPush.initWithPermissions();

                        // Mark helper as prepared. So we don't have to call this ever again.
                        mInited = true;
                    }

                    // Notify handler
                    synchronized (sInstance) {
                        if (mUiHandler != null) {
                            mUiHandler.onFinished(true, null);
                        }
                    }
                } catch (PasswordManagerException | MalformedURLException e) {
                    synchronized (sInstance) {
                        if (mUiHandler != null) {
                            mUiHandler.onFinished(true, e.getLocalizedMessage());
                        }
                    }
                }
            }
        }).start();
    }

    //endregion

    //region Static Helpers

    /**
     * Creates a {@code SecureString} from {@code String}.
     *
     * @param value {@code String} input.
     * @return {@code SecureString} output.
     */
    public synchronized SecureString secureStringFromString(final String value) {
        return mCore.getSecureContainerFactory().fromString(value);
    }

    /**
     * Creates a {@code SecureByteArray} from {@code byte[]}.
     *
     * @param value {@code byte[]} input.
     * @return {@code SecureByteArray} output.
     */
    public synchronized SecureByteArray secureByteArrayFromBytes(final byte[] value, final boolean wipeSource) {
        return mCore.getSecureContainerFactory().createByteArray(value, wipeSource);
    }

    /**
     * Gets a string resource.
     *
     * @param aString Name.
     * @return String resource.
     */
    public static String getStringByKeyName(final String aString) {
        final Context context = ApplicationContextHolder.getContext();
        final int resId = context.getResources().getIdentifier(aString, "string", context.getPackageName());
        if (resId != 0) {
            return getString(resId);
        } else {
            return getString(R.string.message_not_found);
        }
    }

    /**
     * Gets a string resource.
     *
     * @param stringId Id.
     * @return String resource.
     */
    public static String getString(@StringRes final int stringId) {
        return ApplicationContextHolder.getContext().getString(stringId);
    }

    /**
     * Checks for runtime permission.
     *
     * @param activity    Calling activity.
     * @param askForThem  {@code True} if missing permission should be requested, else {@code false}.
     * @param permissions List of permissions.
     * @return {@code True} if permissions are present, else {@code false}.
     */
    @TargetApi(23)
    public static boolean checkPermissions(final Activity activity,
                                           final boolean askForThem,
                                           final String... permissions) {

        // Old SDK version does not have dynamic permissions.
        if (Build.VERSION.SDK_INT < 23) {
            return true;
        }

        final List<String> permissionsToCheck = new ArrayList<>();

        for (final String permission : permissions) {
            if (ContextCompat.checkSelfPermission(activity, permission) != PermissionChecker.PERMISSION_GRANTED) {
                // Should we show an explanation?
                if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                    // Show an explanation to the user *asynchronously* -- don't block
                    // this thread waiting for the user's response! After the user
                    // sees the explanation, try again to request the permission.
                    Toast.makeText(activity, "Requesting permission - " + permission, Toast.LENGTH_LONG).show();
                }

                permissionsToCheck.add(permission);
            }
        }

        if (!permissionsToCheck.isEmpty() && askForThem) {
            ActivityCompat
                    .requestPermissions(activity, permissionsToCheck.toArray(new String[permissionsToCheck.size()]), 0);
        }

        return permissionsToCheck.isEmpty();
    }

    //endregion

    //region Public API


    /**
     * Check whenever SDK was successfully initialised with all permisions.
     *
     * @return True in case that SDK is ready to use.
     */
    public synchronized boolean isInited() {
        return mInited;
    }

    /**
     * @return Instance of secure storage. (Ezio SecureStorage)
     */
    public synchronized Protocols.StorageProtocol getStorageSecure() {
        return mStorageSecure;
    }

    /**
     * @return Instance of fast insecure storage. (iOS UserDefaults)
     */
    public synchronized Protocols.StorageProtocol getStorageFast() {
        return mStorageFast;
    }

    /**
     * @return Manager used for handling all push related actions.
     */
    public synchronized PushManager getManagerPush() {
        return mManagerPush;
    }

    /**
     * @return Manager used for handling all token related actions.
     */
    public synchronized TokenManager getManagerToken() {
        return mManagerToken;
    }

    /**
     * @return Manager used for handling QR codes.
     */
    public QRCodeManager getManagerQRCode() {
        return mManagerQRCode;
    }

    /**
     * Retrieves {@code IdpCore} instance.
     *
     * @return {@code IdpCore} instance.
     */
    public synchronized IdpCore getCore() {
        return mCore;
    }

    /**
     * Gets the current activity.
     *
     * @return Current activity.
     */
    @Nullable
    public MainActivity getCurrentListener() {
        final Activity currentActivity = ((EzioSampleApp) ApplicationContextHolder.getContext().getApplicationContext()).getCurrentActivity();
        if ((currentActivity instanceof MainActivity))
            return (MainActivity) currentActivity;
        return null;
    }

    //endregion

    //region Private Helpers

    /**
     * Gets the OTP configuration.
     *
     * @return OTP configuration.
     */
    private OtpConfiguration getConfigurationOtp() {
        // OTP module is required for token management and OTP calculation.
        return new OtpConfiguration.Builder().setRootPolicy(Configuration.CFG_OTP_ROOT_POLICY).build();
    }

    /**
     * Gets the OOB configuration.
     *
     * @return OOB configuration.
     */
    private OobConfiguration getConfigurationOob() {
        // OOB module is required for push notifications.
        return new OobConfiguration.Builder()
                // Device fingerprint is used for security reason. This way app can add some additional input for internal encryption mechanism.
                // This value must remain the same all the time. Othewise all provisioned tokens will not be valid any more.
                .setDeviceFingerprintSource(Configuration.CFG_SDK_DEVICE_FINGERPRINT_SOURCE)
                // Jailbreak policy for OOB module. See EMOobJailbreakPolicyIgnore for more details.
                .setRootPolicy(Configuration.CFG_OOB_ROOT_POLICY)
                // For debug and ONLY debug reasons we might lower some TLS configuration.
                .setTlsConfiguration(Configuration.CFG_SDK_TLS_CONFIGURATION).build();
    }

    /**
     * Gets the MSP configuration.
     *
     * @return MSP configuration.
     */
    private MspConfiguration getConfigurationMsp() {
        // Mobile Signing Protocol QR parsing, push messages etc..
        final MspConfiguration.Builder builder = new MspConfiguration.Builder();

        // Set obfuscation
        if (Configuration.CFG_MSP_OBFUSCATION_CODE != null) {
            builder.setObfuscationKeys(Configuration.CFG_MSP_OBFUSCATION_CODE);
        }
        // Set signature
        if (Configuration.CFG_MSP_SIGN_KEYS != null) {
            builder.setSignatureKeys(Configuration.CFG_MSP_SIGN_KEYS);
        }

        return builder.build();
    }

    //endregion

}
