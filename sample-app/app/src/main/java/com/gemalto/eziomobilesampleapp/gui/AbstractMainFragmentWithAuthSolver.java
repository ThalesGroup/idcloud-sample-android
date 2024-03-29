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

package com.gemalto.eziomobilesampleapp.gui;

import android.os.CancellationSignal;


import com.gemalto.eziomobilesampleapp.R;
import com.gemalto.eziomobilesampleapp.gui.overlays.FragmentBioFingerprint;
import com.gemalto.eziomobilesampleapp.gui.overlays.FragmentSecureKeypad;
import com.gemalto.eziomobilesampleapp.helpers.Main;
import com.gemalto.eziomobilesampleapp.helpers.Protocols;
import com.gemalto.eziomobilesampleapp.helpers.ezio.TokenDevice;
import com.gemalto.idp.mobile.authentication.AuthMode;
import com.gemalto.idp.mobile.authentication.AuthenticationModule;
import com.gemalto.idp.mobile.authentication.mode.biofingerprint.BioFingerprintAuthInput;
import com.gemalto.idp.mobile.authentication.mode.biofingerprint.BioFingerprintAuthService;
import com.gemalto.idp.mobile.authentication.mode.biofingerprint.BioFingerprintAuthenticationCallbacks;
import com.gemalto.idp.mobile.authentication.mode.biofingerprint.BioFingerprintContainer;
import com.gemalto.idp.mobile.authentication.mode.pin.PinAuthInput;
import com.gemalto.idp.mobile.core.IdpException;
import com.gemalto.idp.mobile.core.util.SecureString;

import static android.hardware.fingerprint.FingerprintManager.FINGERPRINT_ERROR_CANCELED;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

/**
 * Add auth input / OTP handlers to main fragment.
 */
public abstract class AbstractMainFragmentWithAuthSolver extends AbstractMainFragment {


    //region Auto select

    /**
     * Retrieves the authentication method based on the priority: <br>
     *
     * <ol>
     * <li>Touch id</li>
     * <li>PIN</li>
     * </ol>
     *
     * @param handler Callback.
     */
    public void authInputGetMostComfortableOne(@NonNull final Protocols.AuthInputHandler handler) {
        // Check all auth mode states so we can pick proper auth mode.
        final TokenDevice.TokenStatus status = Main.sharedInstance().getManagerToken().getTokenDevice()
                .getTokenStatus();

        if (status.isTouchEnabled) {
            authInputGetTouchId(handler);
        } else {
            authInputGetPin((firstPin, secondPin) -> handler.onFinished(firstPin, null), false);
        }
    }

    /**
     * Generates the OTP using the authentication method based on the priority: <br>
     *
     * <ol>
     * <li>Touch id</li>
     * <li>PIN</li>
     * </ol>
     *
     * @param handler Callback.
     */
    public void totpWithMostComfortableOne(
            @Nullable final SecureString serverChallenge,
            @NonNull final Protocols.OTPDelegate handler
    ) {
        authInputGetMostComfortableOne((authInput, error) -> {
            if (authInput != null) {
                Main.sharedInstance().getManagerToken().getTokenDevice()
                        .totpWithAuthInput(authInput, serverChallenge, handler);
            } else {
                handler.onOTPDelegateFinished(null, error, null, serverChallenge);
            }
        });
    }

    //endregion

    //region Pin

    /**
     * Creates and shows the Secure Pin-pad.
     *
     * @param handler   Callback.
     * @param changePin {@code True} if scenario is change pin, else {@code false}.
     */
    public void authInputGetPin(
            @NonNull Protocols.SecureInputHandler handler,
            boolean changePin
    ) {
        do {
            final FragmentSecureKeypad secureKeypad = FragmentSecureKeypad.create(handler, changePin);
            FragmentActivity activity = getActivity();
            if (activity == null) break;

            FragmentManager fm = activity.getSupportFragmentManager();
            fm.beginTransaction()
                    .setCustomAnimations(R.anim.fade_in, R.anim.fade_out, R.anim.fade_in, R.anim.fade_out)
                    .replace(R.id.fragment_container, secureKeypad, null)
                    .addToBackStack(null)
                    .commit();

        } while (false);
    }

    protected void authInputGetAndVerifyPin(final Protocols.AuthInputHandler handler) {
        authInputGetPin((firstPin, secondPin) -> {
            // Get token device and generate an OTP with the entered PIN
            final TokenDevice tokenDevice = Main.sharedInstance().getManagerToken().getTokenDevice();
            // once OTP generated
            tokenDevice.totpWithAuthInput(firstPin, null, (otp, error, authInput, serverChallenge) -> {

                if (error == null) {
                    handler.onFinished(firstPin, null);
                } else {
                    // OTP not verified => do not change the PIN
                    handler.onFinished(null, error != null
                            ? error
                            : Main.getString(R.string.verify_pin_network_issue));
                }

            });
        }, false);
    }

    //endregion

    //region Touch id

    /**
     * Creates the authentication using Touch id.
     *
     * @param handler Callback.
     */
    @SuppressWarnings("deprecation")
    public void authInputGetTouchId(@NonNull final Protocols.AuthInputHandler handler) {
        final BioFingerprintAuthService service = BioFingerprintAuthService.create(AuthenticationModule.create());
        final BioFingerprintContainer container = service.getBioFingerprintContainer();
        final CancellationSignal cancelSignal = new CancellationSignal();
        final FragmentBioFingerprint fpFragment = FragmentBioFingerprint
                .create(new FragmentBioFingerprint.BioFpFragmentCallback() {
                    @Override
                    public void onPinFallback() {
                        cancelSignal.cancel();

                        // Fallback to pin variant.
                        authInputGetPin((firstPin, secondPin) -> handler.onFinished(firstPin, null), false);
                    }

                    @Override
                    public void onCancel() {
                        cancelSignal.cancel();
                    }
                });

        // Trigger system authentication
        container.authenticateUser(Main.sharedInstance().getManagerToken().getTokenDevice().getToken(),
                cancelSignal,
                new BioFingerprintAuthenticationCallbacks() {
                    @Override
                    public void onSuccess(final BioFingerprintAuthInput bioFingerprintAuthInput) {
                        fpFragment.dismiss();
                        handler.onFinished(bioFingerprintAuthInput, null);
                    }

                    @Override
                    public void onStartFPSensor() {
                        getMainActivity().getSupportFragmentManager().beginTransaction()
                                .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
                                .add(fpFragment, null)
                                .commit();
                    }

                    @Override
                    public void onError(final IdpException exception) {
                        handler.onFinished(null, exception.getLocalizedMessage());
                    }

                    @Override
                    public void onAuthenticationError(final int errorCode,
                                                      final CharSequence charSequence) {
                        // We don't want to show cancel error, since it's obvious to user.
                        if (errorCode != FINGERPRINT_ERROR_CANCELED) {
                            getMainActivity().showMessage(charSequence.toString());
                        }
                        fpFragment.dismiss();
                    }

                    @Override
                    public void onAuthenticationHelp(final int helpCode,
                                                     final CharSequence charSequence) {
                        getMainActivity().showMessage(charSequence.toString());
                    }

                    @Override
                    public void onAuthenticationSucceeded() {
                        fpFragment.onSuccess();
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        fpFragment.onFailure();
                    }
                });
    }

    //endregion

    //region Auth mode

    /**
     * Enables the authentication mode.
     *
     * @param mode Authentication mode to enable.
     */
    protected void enableAuthMode(
            final AuthMode mode,
            @StringRes final int successMessageResId
    ) {
        final TokenDevice device = Main.sharedInstance().getManagerToken().getTokenDevice();

        // We must enable multi-auth mode before activating any specific one.
        // Since we need pin for both of those operations this metod will ask for it and return one directly.
        enableMultiauthWithCompletionHandler((firstPin, secondPin) -> {
            try {
                device.getToken().activateAuthMode(mode, firstPin);
                getMainActivity().showMessage(Main.getString(successMessageResId));

            } catch (IdpException e) {
                getMainActivity().showErrorIfExists(e.getLocalizedMessage());
            } finally {
                reloadGUI();
            }
        });
    }

    /**
     * Disables the authentication mode.
     *
     * @param mode Authentication mode to disable.
     */
    protected void disableAuthMode(final AuthMode mode) {
        final TokenDevice device = Main.sharedInstance().getManagerToken().getTokenDevice();

        try {
            if (device.getToken().isAuthModeActive(mode)) {
                device.getToken().deactivateAuthMode(mode);
            }
        } catch (IdpException exception) {
            getMainActivity().showErrorIfExists(exception.getLocalizedMessage());
        } finally {
            reloadGUI();
        }
    }

    /**
     * Enables multi authentication mode.
     *
     * @param handler Callback.
     */
    private void enableMultiauthWithCompletionHandler(
            @NonNull final Protocols.SecureInputHandler handler
    ) {
        final TokenDevice device = Main.sharedInstance().getManagerToken().getTokenDevice();

        // Check whenever multi-authMode is already enabled.
        try {
            final boolean isEnabled = device.getToken().isMultiAuthModeEnabled();

            // In both cases we will need auth pin, because it's used for
            // multi-auth upgrade as well as enabling specific authmodes.
            authInputGetAndVerifyPin((authInput, error) -> {
                do {
                    if (error != null) {
                        getMainActivity().showErrorIfExists(error);
                        break;
                    }

                    // If multi-auth is not enabled and we do have pin, we can try to upgrade it.
                    if (!isEnabled) {
                        try {
                            device.getToken().upgradeToMultiAuthMode((PinAuthInput) authInput);
                        } catch (IdpException e) {
                            getMainActivity().showErrorIfExists(e.getLocalizedMessage());
                        }
                    }

                    // Notify handler
                    if (authInput != null) {
                        handler.onSecureInputFinished((PinAuthInput) authInput, null);
                    }
                } while (false);
            });
        } catch (IdpException exception) {
            getMainActivity().showErrorIfExists(exception.getLocalizedMessage());
        }
    }

    //endregion

}
