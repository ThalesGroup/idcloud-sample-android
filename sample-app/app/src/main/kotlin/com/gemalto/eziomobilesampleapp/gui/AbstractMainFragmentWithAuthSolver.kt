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
package com.gemalto.eziomobilesampleapp.gui

import android.os.CancellationSignal
import androidx.annotation.StringRes
import androidx.biometric.BiometricPrompt
import com.gemalto.eziomobilesampleapp.R
import com.gemalto.eziomobilesampleapp.gui.overlays.FragmentSecureKeypad.Companion.create
import com.gemalto.eziomobilesampleapp.helpers.Main
import com.gemalto.eziomobilesampleapp.helpers.Protocols
import com.gemalto.eziomobilesampleapp.helpers.Protocols.AuthInputHandler
import com.gemalto.eziomobilesampleapp.helpers.Protocols.OTPDelegate
import com.gemalto.idp.mobile.authentication.AuthInput
import com.gemalto.idp.mobile.authentication.AuthMode
import com.gemalto.idp.mobile.authentication.AuthenticationModule
import com.gemalto.idp.mobile.authentication.mode.biometric.BiometricAuthInput
import com.gemalto.idp.mobile.authentication.mode.biometric.BiometricAuthService
import com.gemalto.idp.mobile.authentication.mode.biometric.BiometricAuthenticationCallbacks
import com.gemalto.idp.mobile.authentication.mode.pin.PinAuthInput
import com.gemalto.idp.mobile.core.IdpException
import com.gemalto.idp.mobile.core.util.SecureString

/**
 * Add auth input / OTP handlers to main fragment.
 */
abstract class AbstractMainFragmentWithAuthSolver : AbstractMainFragment() {
    //region Auto select
    /**
     * Retrieves the authentication method based on the priority: <br></br>
     *
     *
     *  1. Touch id
     *  2. PIN
     *
     *
     * @param handler Callback.
     */
    fun authInputGetMostComfortableOne(handler: AuthInputHandler) {
        // Check all auth mode states so we can pick proper auth mode.
        val status = Main.sharedInstance()?.managerToken?.tokenDevice?.tokenStatus

        if (status?.isTouchEnabled == true) {
            authInputGetTouchId(handler)
        } else {
            authInputGetPin(object : Protocols.SecureInputHandler {
                override fun onSecureInputFinished(firstPin: PinAuthInput, secondPin: PinAuthInput?) {
                    handler.onFinished(firstPin, null)
                }
            }, false)
        }
    }

    /**
     * Generates the OTP using the authentication method based on the priority: <br></br>
     *
     *
     *  1. Touch id
     *  2. PIN
     *
     *
     * @param handler Callback.
     */
    fun totpWithMostComfortableOne(
        serverChallenge: SecureString?,
        handler: OTPDelegate
    ) {
        authInputGetMostComfortableOne(object : Protocols.AuthInputHandler {
            override fun onFinished(authInput: AuthInput?, error: String?) {
                if (authInput != null) {
                    Main.sharedInstance()?.managerToken?.tokenDevice
                        ?.totpWithAuthInput(authInput, serverChallenge, handler)
                } else {
                    handler.onOTPDelegateFinished(null, error, null, serverChallenge)
                }
            }
        })
    }

    //endregion
    //region Pin
    /**
     * Creates and shows the Secure Pin-pad.
     *
     * @param handler   Callback.
     * @param changePin `True` if scenario is change pin, else `false`.
     */
    fun authInputGetPin(
        handler: Protocols.SecureInputHandler,
        changePin: Boolean
    ) {
        do {
            val secureKeypad = create(handler, changePin)
            val activity = getActivity()
            if (activity == null) break

            val fm = activity.getSupportFragmentManager()
            fm.beginTransaction()
                .setCustomAnimations(
                    R.anim.fade_in,
                    R.anim.fade_out,
                    R.anim.fade_in,
                    R.anim.fade_out
                )
                .replace(R.id.fragment_container, secureKeypad, null)
                .addToBackStack(null)
                .commit()
        } while (false)
    }

    protected fun authInputGetAndVerifyPin(handler: AuthInputHandler) {
        authInputGetPin(object : Protocols.SecureInputHandler {
            override fun onSecureInputFinished(firstPin: PinAuthInput, secondPin: PinAuthInput?) {
                // Get token device and generate an OTP with the entered PIN
                val tokenDevice = Main.sharedInstance()?.managerToken?.tokenDevice
                // once OTP generated
                tokenDevice?.totpWithAuthInput(
                    firstPin,
                    null,
                    object : Protocols.OTPDelegate {
                        override fun onOTPDelegateFinished(
                            otp: SecureString?,
                            error: String?,
                            authInput: AuthInput?,
                            serverChallenge: SecureString?
                        ) {
                            if (error == null) {
                                handler.onFinished(firstPin, null)
                            } else {
                                // OTP not verified => do not change the PIN
                                handler.onFinished(
                                    null, error ?: Main.getString(R.string.verify_pin_network_issue)
                                )
                            }
                        }
                    }
                )
            }
        }, false)
    }

    //endregion
    //region Touch id
    /**
     * Creates the authentication using Touch id.
     *
     * @param handler Callback.
     */
    @Suppress("deprecation")
    fun authInputGetTouchId(handler: AuthInputHandler) {
        val service = BiometricAuthService.create(AuthenticationModule.create())
        val container = service.getBiometricContainer()
        val cancelSignal = CancellationSignal()

        // Trigger system authentication
        container.authenticateUser(
            Main.sharedInstance()?.managerToken?.tokenDevice?.token,
            "Biometric",
            "Login with biometrics",
            "Verify Biometric",
            "Cancel",
            cancelSignal,
            object : BiometricAuthenticationCallbacks {
                override fun onSuccess(bioFingerprintAuthInput: BiometricAuthInput?) {
                    handler.onFinished(bioFingerprintAuthInput, null)
                }

                override fun onError(exception: IdpException) {
                    handler.onFinished(null, exception.getLocalizedMessage())
                }

                override fun onAuthenticationError(
                    errorCode: Int,
                    charSequence: CharSequence
                ) {
                    // We don't want to show cancel error, since it's obvious to user.
                    if (errorCode != BiometricPrompt.ERROR_CANCELED && errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                        mainActivity?.showMessage(charSequence.toString())
                    }

                    // Fallback to pin variant.
                    if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                        authInputGetPin(object : Protocols.SecureInputHandler {
                            override fun onSecureInputFinished(firstPin: PinAuthInput, secondPin: PinAuthInput?) {
                                handler.onFinished(firstPin, null)
                            }
                        }, false)
                    }
                }

                override fun onAuthenticationHelp(
                    helpCode: Int,
                    charSequence: CharSequence
                ) {
                    mainActivity?.showMessage(charSequence.toString())
                }

                override fun onAuthenticationSucceeded() {
                    // Handled in onSuccess
                }

                override fun onAuthenticationFailed() {
                    // Handled in onAuthenticationError
                }
            })
    }

    //endregion
    //region Auth mode
    /**
     * Enables the authentication mode.
     *
     * @param mode Authentication mode to enable.
     */
    protected fun enableAuthMode(
            mode: AuthMode?,
            @StringRes successMessageResId: Int,
//            handler: Protocols.GenericHandler
    ) {
        val device = Main.sharedInstance()?.managerToken?.tokenDevice

        // We must enable multi-auth mode before activating any specific one.
        // Since we need pin for both of those operations this method will ask for it and return one directly.
        enableMultiauthWithCompletionHandler(object : Protocols.SecureInputHandler {
            override fun onSecureInputFinished(firstPin: PinAuthInput, secondPin: PinAuthInput?) {
                try {
                    device?.token?.activateAuthMode(mode, firstPin)
                    mainActivity?.showMessage(Main.getString(successMessageResId))
//                    handler.onFinished(true, null)
                } catch (e: IdpException) {
                    mainActivity?.showErrorIfExists(e.getLocalizedMessage())
//                    handler.onFinished(false, e.localizedMessage)
                }
            }
        })
    }

    /**
     * Disables the authentication mode.
     *
     * @param mode Authentication mode to disable.
     */
    protected fun disableAuthMode(mode: AuthMode?) {
        val device = Main.sharedInstance()?.managerToken?.tokenDevice
        try {
            if (device?.token?.isAuthModeActive(mode) == true) {
                device.token.deactivateAuthMode(mode)
            }
//            handler.onFinished(true, null)
        } catch (exception: IdpException) {
            mainActivity?.showErrorIfExists(exception.getLocalizedMessage())
//            handler.onFinished(false, exception.localizedMessage)
        }
    }

    /**
     * Enables multi authentication mode.
     *
     * @param handler Callback.
     */
    private fun enableMultiauthWithCompletionHandler(
        handler: Protocols.SecureInputHandler
    ) {
        val device = Main.sharedInstance()?.managerToken?.tokenDevice

        // Check whenever multi-authMode is already enabled.
        try {
            val isEnabled = device?.token?.isMultiAuthModeEnabled()

            // In both cases we will need auth pin, because it's used for
            // multi-auth upgrade as well as enabling specific authmodes.
            authInputGetAndVerifyPin(object : Protocols.AuthInputHandler {
                override fun onFinished(authInput: AuthInput?, error: String?) {
                    do {
                        if (error != null) {
                            mainActivity?.showErrorIfExists(error)
                            break
                        }

                        // If multi-auth is not enabled and we do have pin, we can try to upgrade it.
                        isEnabled?.let {
                            if (!it) {
                                try {
                                    device.token.upgradeToMultiAuthMode(authInput as PinAuthInput?)
                                } catch (e: IdpException) {
                                    mainActivity?.showErrorIfExists(e.getLocalizedMessage())
                                }
                            }
                        }

                        // Notify handler
                        if (authInput != null) {
                            handler.onSecureInputFinished(authInput as PinAuthInput, null)
                        }
                    } while (false)
                }
            })

        } catch (exception: IdpException) {
            mainActivity?.showErrorIfExists(exception.getLocalizedMessage())
        }
    } //endregion
}
