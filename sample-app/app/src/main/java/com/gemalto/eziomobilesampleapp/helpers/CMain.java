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

package com.gemalto.eziomobilesampleapp.helpers;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.PermissionChecker;
import android.widget.Toast;

import com.gemalto.eziomobilesampleapp.Configuration;
import com.gemalto.eziomobilesampleapp.EzioSampleApp;
import com.gemalto.eziomobilesampleapp.MainActivity;
import com.gemalto.eziomobilesampleapp.R;
import com.gemalto.eziomobilesampleapp.helpers.app.storage.SharedPreferences;
import com.gemalto.eziomobilesampleapp.helpers.ezio.HttpManager;
import com.gemalto.eziomobilesampleapp.helpers.ezio.PushManager;
import com.gemalto.eziomobilesampleapp.helpers.ezio.QRCodeManager;
import com.gemalto.eziomobilesampleapp.helpers.ezio.TokenManager;
import com.gemalto.eziomobilesampleapp.helpers.ezio.storage.SecureStorage;
import com.gemalto.idp.mobile.authentication.IdpAuthException;
import com.gemalto.idp.mobile.authentication.mode.face.FaceAuthInitializeCallback;
import com.gemalto.idp.mobile.authentication.mode.face.FaceAuthLicense;
import com.gemalto.idp.mobile.authentication.mode.face.FaceAuthLicenseConfigurationCallback;
import com.gemalto.idp.mobile.authentication.mode.face.FaceAuthService;
import com.gemalto.idp.mobile.authentication.mode.face.ui.FaceManager;
import com.gemalto.idp.mobile.core.ApplicationContextHolder;
import com.gemalto.idp.mobile.core.IdpCore;
import com.gemalto.idp.mobile.core.IdpException;
import com.gemalto.idp.mobile.core.passwordmanager.PasswordManager;
import com.gemalto.idp.mobile.core.passwordmanager.PasswordManagerException;
import com.gemalto.idp.mobile.core.util.SecureByteArray;
import com.gemalto.idp.mobile.core.util.SecureString;
import com.gemalto.idp.mobile.msp.MspConfiguration;
import com.gemalto.idp.mobile.oob.OobConfiguration;
import com.gemalto.idp.mobile.otp.OtpConfiguration;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

// IMPORTANT: This source code is intended to serve training information purposes only. Please make sure to review our IdCloud documentation, including security guidelines.

/**
 * Main app singleton. With instances of all managers.
 * It is available whole app lifetime.
 */
public class CMain {

    //region Defines

    private static CMain sInstance = null;

    private boolean mInited = false;
    private Protocols.StorageProtocol mStorageSecure = null;
    private Protocols.StorageProtocol mStorageFast = null;
    private PushManager mManagerPush = null;
    private TokenManager mManagerToken = null;
    private QRCodeManager mManagerQRCode = null;
    private HttpManager mManagerHttp = null;
    private IdpCore mCore = null;
    private GemaloFaceIdState mGemaloFaceIdState = GemaloFaceIdState.GemaloFaceIdStateUndefined;

    public enum GemaloFaceIdState {
        // Face Id service was not even started.
        GemaloFaceIdStateUndefined(R.string.GEMALTO_FACE_ID_STATE_UNDEFINED, R.color.red),
        // Face id is not supported
        GemaloFaceIdStateNotSupported(R.string.GEMALTO_FACE_ID_STATE_NOT_SUPPORTED, R.color.red),
        // Failed to registered.
        GemaloFaceIdStateUnlicensed(R.string.GEMALTO_FACE_ID_STATE_UNLICENSED, R.color.red),
        // Successfully registered.
        GemaloFaceIdStateLicensed(R.string.GEMALTO_FACE_ID_STATE_LICENSED, R.color.orange),
        // Failed to init service.
        GemaloFaceIdStateInitFailed(R.string.GEMALTO_FACE_ID_STATE_INIT_FAILED, R.color.red),
        // Registered and initialised.
        GemaloFaceIdStateInited(R.string.GEMALTO_FACE_ID_STATE_INITED, R.color.orange),
        // Registered, initialised and configured with at least one user enrolled.
        GemaloFaceIdStateReadyToUse(R.string.GEMALTO_FACE_ID_STATE_READY, R.color.green);

        GemaloFaceIdState(final int valueString, final int valueColor) {
            mValueString = valueString;
            mValueColor = valueColor;
        }

        private final int mValueString;
        private final int mValueColor;

        public int getValueString() {
            return mValueString;
        }

        public int getValueColor() {
            return mValueColor;
        }
    }

    //endregion

    //region Life Cycle

    /**
     * Gets the singleton instance of {@code CMain}.
     * @return Singleton instance of {@code CMain}.
     */
    public static synchronized CMain sharedInstance() {
        if (sInstance == null) {
            sInstance = new CMain();
        }

        return sInstance;
    }

    /**
     * Initializes the singleton instance of {@code CMain}.
     * @param context Android application context.
     */
    public void init(final Context context) {
        // App context used in all sdk levels.
        ApplicationContextHolder.setContext(context);

        // Initialise basic managers without all permissions yet.
        mStorageFast = new SharedPreferences();
        mManagerPush = new PushManager();
        mManagerQRCode = new QRCodeManager();
        mManagerHttp = new HttpManager();
    }

    /**
     * Initializes the singleton instance of {@code CMain}. Need to request runtime permissions for this API call.
     * @param handler Callback.
     */
    public void initWithPermissions(@NonNull final Protocols.GenericHandler handler) {

        // Sync handler will all all methods in ui thread.
        final Protocols.GenericHandler uiHandler = new Protocols.GenericHandler.Sync(handler);

        // Make sure, that we will always check isConfigured first. Multiple call of init will cause crash / run time exception.
        if (IdpCore.isConfigured()) {
            uiHandler.onFinished(true, null);
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                mCore = IdpCore.configure(Configuration.CFG_SDK_ACTIVATION_CODE,
                        getConfigurationOob(),
                        getConfigurationOtp(),
                        getConfigurationMsp());

                final SecureString password = mCore.getSecureContainerFactory().fromString("SecureStoragePassword");

                try {
                    // Login so we can use secure storage, OOB etc..
                    final PasswordManager passwordManager = mCore.getPasswordManager();
                    if (!passwordManager.isPasswordSet()) {
                        passwordManager.setPassword(password);
                    }
                    passwordManager.login(password);


                    // This will also register and activate licence.
                    FaceManager.initInstance();
                    CMain.this.updateGemaltoFaceIdStatus();

                    // Init rest of the managers and update basic one with new permissions.
                    mStorageSecure = new SecureStorage();
                    mManagerToken = new TokenManager();
                    mManagerPush.initWithPermissions();

                    // Mark helper as prepared. So we don't have to call this ever again.
                    mInited = true;

                    // Notify handler
                    uiHandler.onFinished(true, null);
                } catch (PasswordManagerException | MalformedURLException e) {
                    uiHandler.onFinished(true, e.getLocalizedMessage());
                }
            }
        }).start();
    }

    /**
     * Resets the singleton instance.
     */
    public static void end() {
        sInstance = null;
    }

    //endregion

    //region Static Helpers

    /**
     * Creates a new {@code SecureString} from {@code String}.
     * @param value Input {@code String}.
     * @return Created {@code SecureString} instance.
     */
    public static SecureString secureStringFromString(final String value) {
        return sInstance.mCore.getSecureContainerFactory().fromString(value);
    }

    /**
     * Creates a new {@code SecureByteArray} from {@code byte[]} input.
     * @param value Input {@code byte[]} array.
     * @param wipeSource {@code True} if input {@code byte[]} array should be wiped, else {@code false}.
     * @return Created {@code SecureByteArray} instance.
     */
    public static SecureByteArray secureByteArrayFromBytes(final byte[] value, final boolean wipeSource) {
        return sInstance.mCore.getSecureContainerFactory().createByteArray(value, wipeSource);
    }

    /**
     * Gets string resource by name.
     * @param aString Resource name.
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
     * Gets string resource by id.
     * @param stringId String resource id.
     * @return String resource.
     */
    public static String getString(final int stringId) {
        return ApplicationContextHolder.getContext().getString(stringId);
    }

    /**
     * Checks for runtime permissions.
     * @param activity Application context.
     * @param askForThem {@code True} if permission should be requested, else {@code false}.
     * @param permissions Permissions to check.
     * @return {@code True} if permission requested successfully.
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
     * @return True in case that SDK is ready to use.
     */
    public boolean isInited() {
        return mInited;
    }

    /**
     * @return Instance of secure storage. (Ezio SecureStorage)
     */
    public Protocols.StorageProtocol getStorageSecure() {
        return mStorageSecure;
    }

    /**
     * @return Instance of fast insecure storage. (iOS UserDefaults)
     */
    public Protocols.StorageProtocol getStorageFast() {
        return mStorageFast;
    }

    /**
     * @return Manager used for handling all push related actions.
     */
    public PushManager getManagerPush() {
        return mManagerPush;
    }

    /**
     * @return Manager used for handling all token related actions.
     */
    public TokenManager getManagerToken() {
        return mManagerToken;
    }

    /**
     * @return Manager used for handling http communication.
     */
    public HttpManager getManagerHttp() {
        return mManagerHttp;
    }

    /**
     * @return Manager used for handling QR codes.
     */
    public QRCodeManager getManagerQRCode() {
        return mManagerQRCode;
    }

    /**
     * Gemalto face id does have multiple step async activation.
     * Check this value to see current state.
     *
     * @return Current state.
     */
    public GemaloFaceIdState getFaceIdState() {
        return mGemaloFaceIdState;
    }

    public IdpCore getCore() {
        return mCore;
    }

    /**
     * Force reload gemalto face id status.
     */
    public void updateGemaltoFaceIdStatus() {
        final FaceAuthService faceIdService = FaceManager.getInstance().getFaceAuthService();

        if (!faceIdService.isSupported()) {
            setGemaloFaceIdState(GemaloFaceIdState.GemaloFaceIdStateNotSupported);
            return;
        }

        // Support sample app even without face id.
        if (Configuration.CFG_FACE_ID_PRODUCT_KEY == null || Configuration.CFG_FACE_ID_PRODUCT_KEY.isEmpty() ||
                Configuration.CFG_FACE_ID_SERVER_URL == null || Configuration.CFG_FACE_ID_SERVER_URL.isEmpty()) {
            setGemaloFaceIdState(GemaloFaceIdState.GemaloFaceIdStateUnlicensed);
            return;
        }

        final FaceAuthLicense license = new FaceAuthLicense.Builder()
                .setProductKey(Configuration.CFG_FACE_ID_PRODUCT_KEY)
                .setServerUrl(Configuration.CFG_FACE_ID_SERVER_URL)
                .build();

        faceIdService.configureLicense(license, new FaceAuthLicenseConfigurationCallback() {
            @Override
            public void onLicenseConfigurationSuccess() {
                setGemaloFaceIdState(GemaloFaceIdState.GemaloFaceIdStateLicensed);

                // Already inited.
                if (faceIdService.isInitialized()) {
                    updateGemaltoFaceIdStatusConfigured(faceIdService);
                } else {
                    // With license we can activate face id service.
                    faceIdService.initialize(new FaceAuthInitializeCallback() {
                        @Override
                        public String onInitializeCamera(final String[] strings) {
                            // Select one from the given list by returning null,
                            // the SDK will pick a default camera which will be:
                            // the first in the list which contains 'front'
                            // or the first one if no 'front' is found
                            return null;
                        }

                        @Override
                        public void onInitializeSuccess() {
                            updateGemaltoFaceIdStatusConfigured(faceIdService);
                        }

                        @Override
                        public void onInitializeError(final IdpException exception) {
                            setGemaloFaceIdState(GemaloFaceIdState.GemaloFaceIdStateInitFailed);
                        }
                    });
                }
            }

            @Override
            public void onLicenseConfigurationFailure(final IdpAuthException exception) {
                setGemaloFaceIdState(GemaloFaceIdState.GemaloFaceIdStateUnlicensed);
            }
        });
    }

    @Nullable
    public MainActivity getCurrentListener() {
        final Activity currentActivity = ((EzioSampleApp) ApplicationContextHolder.getContext().getApplicationContext()).getCurrentActivity();
        if (currentActivity != null && (currentActivity instanceof MainActivity)) {
            return (MainActivity) currentActivity;
        } else {
            return null;
        }
    }

    //endregion

    //region Private Helpers

    /**
     * Retrieves OTP configuration.
     * @return OTP configuration.
     */
    private OtpConfiguration getConfigurationOtp() {
        // OTP module is required for token management and OTP calculation.
        return new OtpConfiguration.Builder().setRootPolicy(Configuration.CFG_OTP_ROOT_POLICY).build();
    }

    /**
     * Retrieves OOB configuration.
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
     * Retrieves MSP configuration.
     * @return MSP configuratin.
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

    /**
     * Sets face id status.
     * @param service {@code FaceAuthService}.
     */
    private void updateGemaltoFaceIdStatusConfigured(final FaceAuthService service) {
        // Configured at this point mean, that there is at least one user enrolled.
        if (service.isConfigured()) {
            setGemaloFaceIdState(GemaloFaceIdState.GemaloFaceIdStateReadyToUse);
        } else {
            setGemaloFaceIdState(GemaloFaceIdState.GemaloFaceIdStateInited);
        }
    }

    /**
     * Sets face id status.
     * @param state State.
     */
    private void setGemaloFaceIdState(final GemaloFaceIdState state) {
        if (mGemaloFaceIdState.equals(state)) {
            return;
        }

        mGemaloFaceIdState = state;

        final MainActivity listener = getCurrentListener();
        if (listener != null) {
            listener.updateFaceIdSupport();
        }
    }

    //endregion

}
