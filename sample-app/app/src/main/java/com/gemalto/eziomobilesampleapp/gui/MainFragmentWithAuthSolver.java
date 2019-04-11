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

package com.gemalto.eziomobilesampleapp.gui;

import android.content.DialogInterface;
import android.os.CancellationSignal;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.widget.Toast;

import com.gemalto.eziomobilesampleapp.R;
import com.gemalto.eziomobilesampleapp.gui.overlays.FragmentBioFingerprint;
import com.gemalto.eziomobilesampleapp.helpers.CMain;
import com.gemalto.eziomobilesampleapp.helpers.Protocols;
import com.gemalto.eziomobilesampleapp.helpers.ezio.TokenDevice;
import com.gemalto.idp.mobile.authentication.AuthInput;
import com.gemalto.idp.mobile.authentication.AuthenticationModule;
import com.gemalto.idp.mobile.authentication.mode.biofingerprint.BioFingerprintAuthInput;
import com.gemalto.idp.mobile.authentication.mode.biofingerprint.BioFingerprintAuthService;
import com.gemalto.idp.mobile.authentication.mode.biofingerprint.BioFingerprintAuthenticationCallbacks;
import com.gemalto.idp.mobile.authentication.mode.biofingerprint.BioFingerprintContainer;
import com.gemalto.idp.mobile.authentication.mode.face.FaceAuthStatus;
import com.gemalto.idp.mobile.authentication.mode.face.ui.FaceManager;
import com.gemalto.idp.mobile.authentication.mode.face.ui.VerificationCallback;
import com.gemalto.idp.mobile.authentication.mode.face.ui.VerifyFragment;
import com.gemalto.idp.mobile.authentication.mode.pin.PinAuthInput;
import com.gemalto.idp.mobile.core.IdpException;
import com.gemalto.idp.mobile.core.util.SecureString;
import com.gemalto.idp.mobile.ui.UiModule;
import com.gemalto.idp.mobile.ui.secureinput.SecureInputBuilderV2;
import com.gemalto.idp.mobile.ui.secureinput.SecureInputService;
import com.gemalto.idp.mobile.ui.secureinput.SecureInputUi;
import com.gemalto.idp.mobile.ui.secureinput.SecurePinpadListenerV2;

// IMPORTANT: This source code is intended to serve training information purposes only. Please make sure to review our IdCloud documentation, including security guidelines.

/**
 * Add auth input / OTP handlers to main fragment.
 */
public class MainFragmentWithAuthSolver extends MainFragment {

    //region MainFragment

    /**
     * Shows an {@code AlertDialog} for OTP approval.
     * @param message Message.
     * @param serverChallenge Server challenge.
     * @param handler Callback.
     */
    public void approveOTP(@NonNull final String message,
                           @Nullable final SecureString serverChallenge,
                           @NonNull final Protocols.OTPDelegate handler) {
        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getMainActivity());
        dialogBuilder.setTitle(getString(R.string.PUSH_APPROVE_QUESTION));
        dialogBuilder.setMessage(message);
        dialogBuilder.setPositiveButton(getString(R.string.PUSH_APPROVE_QUESTION_APPROVE), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialogInterface, final int which) {
                totpWithMostComfortableOne(serverChallenge, handler);
            }
        });
        dialogBuilder.setNegativeButton(getString(R.string.PUSH_APPROVE_QUESTION_DENY), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialogInterface, final int which) {
                handler.onOTPDelegateFinished(null, null, null, null);
            }
        });
        dialogBuilder.setCancelable(true);
        dialogBuilder.create().show();
    }

    //endregion

    //region Auth Inputs

    //region Auto select

    /**
     * Retrieves the most comfortable authentication method:
     * <ol>
     *     <li>Face</li>
     *     <li>Touch</li>
     *     <li>PIN</li>
     * </ol>
     *
     * @param handler Callback.
     */
    public void authInputGetMostComfortableOne(@NonNull final Protocols.AuthInputHandler handler) {
        // Check all auth mode states so we can pick proper auth mode.
        final TokenDevice.TokenStatus status  = CMain.sharedInstance().getManagerToken().getTokenDevice().getTokenStatus();

        if (status.isFaceEnabled) {
            authInputGetFaceId(handler);
        }
        else if (status.isTouchEnabled) {
            authInputGetTouchId(handler);
        }
        else {
            authInputGetPin(new Protocols.SecureInputHandler() {
                @Override
                public void onSecureInputFinished(@NonNull final PinAuthInput firstPin, final PinAuthInput secondPin) {
                    handler.onFinished(firstPin, null);
                }
            }, false);
        }
    }

    /**
     * Generates an OTP with the most comfortable authentication method:
     *
     * <ol>
     *     <li>Face</li>
     *     <li>Touch</li>
     *     <li>PIN</li>
     * </ol>
     *
     * @param serverChallenge
     * @param customHandler
     */
    public void totpWithMostComfortableOne(final SecureString serverChallenge, final Protocols.OTPDelegate customHandler) {
        final Protocols.OTPDelegate listener = customHandler != null ? customHandler : this;
        authInputGetMostComfortableOne(new Protocols.AuthInputHandler() {
            @Override
            public void onFinished(final AuthInput authInput, final String error) {
                if (authInput != null) {
                    CMain.sharedInstance().getManagerToken().getTokenDevice().totpWithAuthInput(authInput, serverChallenge, listener);
                } else {
                    listener.onOTPDelegateFinished(null, error, authInput, serverChallenge);
                }
            }
        });
    }

    //endregion

    //region Pin

    /**
     * Creates and shows the SecureKeypad.
     *
     * @param handler Callback which passes the created PIN ({@code AuthInput} object).
     * @param changePin {@True} if we want to change the PIN, else {@false}.
     */
    public void authInputGetPin(@NonNull final Protocols.SecureInputHandler handler, final boolean changePin) {
        // Get secure keypad builder.
        final SecureInputBuilderV2 builder = SecureInputService.create(UiModule.create()).getSecureInputBuilderV2();

        // Configure secure keypat behavior and visual.
        builder.setOkButtonBehavior(SecureInputBuilderV2.OkButtonBehavior.ALWAYS_ENABLED);
        builder.setButtonPressVisibility(true);

        // We are using the same method also for change pin.
        if (changePin) {
            builder.setFirstLabel(CMain.getString(R.string.SECURE_KEY_PAD_OLD_PIN));
            builder.setSecondLabel(CMain.getString(R.string.SECURE_KEY_PAD_NEW_PIN));
        }

        final SecureInputUi[] secureInput = {null};
        secureInput[0] = builder.buildPinpad(false, changePin, false, new SecurePinpadListenerV2() {
            @Override
            public void onKeyPressedCountChanged(final int newCount, final int inputField) {
                // Unused
            }

            @Override
            public void onInputFieldSelected(final int inputField) {
                // Unused
            }

            @Override
            public void onOkButtonPressed() {
                // Unused
            }

            @Override
            public void onDeleteButtonPressed() {
                // Unused
            }

            @Override
            public void onFinish(final PinAuthInput pinAuthInput, final PinAuthInput pinAuthInput1) {
                secureInput[0].getDialogFragment().dismiss();
                secureInput[0] = null;

                handler.onSecureInputFinished(pinAuthInput, pinAuthInput1);
            }

            @Override
            public void onError(final String error) {
                secureInput[0].getDialogFragment().dismiss();
                secureInput[0] = null;

                getMainActivity().showErrorIfExists(error);
            }
        });

        builder.wipe();

        secureInput[0].getDialogFragment().show(getMainActivity().getSupportFragmentManager(), null);
    }

    /**
     * Creates a TOTP.
     * @param serverChallenge Server challenge.
     * @param customHandler Callback to return the TOTP.
     */
    public void totpWithPin(final SecureString serverChallenge, final Protocols.OTPDelegate customHandler) {
        final Protocols.OTPDelegate delegate = this;
        authInputGetPin(new Protocols.SecureInputHandler() {
            @Override
            public void onSecureInputFinished(@NonNull final PinAuthInput firstPin, final PinAuthInput secondPin) {
                CMain.sharedInstance().getManagerToken().getTokenDevice().totpWithAuthInput(firstPin, serverChallenge, customHandler != null ? customHandler : delegate);
            }
        }, false);
    }

    //endregion

    //region Face id

    /**
     * Retrieves the {@code AuthInput} using FaceId.
     * @param handler Callback to return the {@code AuthInput}.
     */
    public void authInputGetFaceId(@NonNull final Protocols.AuthInputHandler handler) {
        final VerifyFragment verifier = FaceManager.getInstance().getVerificationFragment(
                CMain.sharedInstance().getManagerToken().getTokenDevice().getToken(), 0, 1);
        verifier.setVerificationCallback(new VerificationCallback() {
            @Override
            public void onVerificationSuccess(final AuthInput authInput) {
                getMainActivity().hideLastStackFragment();
                handler.onFinished(authInput, null);
            }

            @Override
            public void onCancel() {
                getMainActivity().hideLastStackFragment();
            }

            @Override
            public void onVerificationFailed(final FaceAuthStatus status) {
                getMainActivity().hideLastStackFragment();
                handler.onFinished(null, status.toString());
            }

            @Override
            public void onVerificationRetry(final FaceAuthStatus status, final int remainingRetries) {
                // Unused
            }

            @Override
            public void onError(final IdpException exception) {
                getMainActivity().hideLastStackFragment();
                handler.onFinished(null, exception.getLocalizedMessage());
            }
        });

        // Display verifier as main fragment.
        getMainActivity().getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
                .replace(R.id.fragment_container, verifier, null)
                .addToBackStack(null)
                .commit();
    }

    /**
     * Generates TOTP using FaceId.
     * @param serverChallenge Server challenge.
     * @param customHandler Callback to return the TOTP.
     */
    public void totpWithFaceId(final SecureString serverChallenge, final Protocols.OTPDelegate customHandler) {
        final Protocols.OTPDelegate delegate = this;
        authInputGetFaceId(new Protocols.AuthInputHandler() {
            @Override
            public void onFinished(final AuthInput authInput, final String error) {
                if (authInput != null) {
                    CMain.sharedInstance().getManagerToken().getTokenDevice().totpWithAuthInput(authInput, serverChallenge, customHandler != null ? customHandler : delegate);
                } else {
                    getMainActivity().showErrorIfExists(error);
                }
            }
        });
    }

    //endregion

    //region Touch id

    /**
     * Retrieves the {@code AuthInput} using TouchId.
     * @param handler Callback to return the {@code AuthInput}.
     */
    public void authInputGetTouchId(@NonNull final Protocols.AuthInputHandler handler) {
        final BioFingerprintAuthService service = BioFingerprintAuthService.create(AuthenticationModule.create());
        final BioFingerprintContainer container = service.getBioFingerprintContainer();
        final CancellationSignal cancelSignal = new CancellationSignal();
        final FragmentBioFingerprint fpFragment = FragmentBioFingerprint.create(new FragmentBioFingerprint.BioFpFragmentCallback() {
            @Override
            public void onPinFallback() {
                cancelSignal.cancel();

                // Fallback to pin variant.
                authInputGetPin(new Protocols.SecureInputHandler() {
                    @Override
                    public void onSecureInputFinished(@NonNull final PinAuthInput firstPin, final PinAuthInput secondPin) {
                        handler.onFinished(firstPin, null);
                    }
                }, false);
            }

            @Override
            public void onCancel() {
                cancelSignal.cancel();
            }
        });

        // Trigger system authentication
        container.authenticateUser(CMain.sharedInstance().getManagerToken().getTokenDevice().getToken()
                , cancelSignal, new BioFingerprintAuthenticationCallbacks() {
                    @Override
                    public void onSuccess(final BioFingerprintAuthInput bioFingerprintAuthInput) {
                        fpFragment.dismiss();
                        handler.onFinished(bioFingerprintAuthInput, null);
                    }

                    @Override
                    public void onStartFPSensor() {
                        getMainActivity().getSupportFragmentManager().beginTransaction()
                                .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
                                .add(fpFragment, null).commit();
                    }

                    @Override
                    public void onError(final IdpException exception) {
                        handler.onFinished(null, exception.getLocalizedMessage());
                    }

                    @Override
                    public void onAuthenticationError(final int errorCode, final CharSequence charSequence) {
                        Toast.makeText(getMainActivity(), charSequence, Toast.LENGTH_LONG).show();
                        fpFragment.dismiss();
                    }

                    @Override
                    public void onAuthenticationHelp(final int helpCode, final CharSequence charSequence) {
                        Toast.makeText(getMainActivity(), charSequence, Toast.LENGTH_LONG).show();
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

    /**
     * Generates TOTP using TouchId.
     * @param serverChallenge Server challenge.
     * @param customHandler Callback to return the TOTP.
     */
    public void totpWithTouchId(final SecureString serverChallenge, final Protocols.OTPDelegate customHandler) {
        final Protocols.OTPDelegate delegate = this;
        authInputGetTouchId(new Protocols.AuthInputHandler() {
            @Override
            public void onFinished(final AuthInput authInput, final String error) {
                if (authInput != null) {
                    CMain.sharedInstance().getManagerToken().getTokenDevice().totpWithAuthInput(authInput, serverChallenge, customHandler != null ? customHandler : delegate);
                } else {
                    getMainActivity().showErrorIfExists(error);
                }
            }
        });
    }

    //endregion

    //endregion
}
