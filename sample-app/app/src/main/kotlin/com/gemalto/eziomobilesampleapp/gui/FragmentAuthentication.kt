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

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.gemalto.eziomobilesampleapp.Configuration
import com.gemalto.eziomobilesampleapp.R
import com.gemalto.eziomobilesampleapp.helpers.Main
import com.gemalto.eziomobilesampleapp.helpers.Protocols
import com.gemalto.idp.mobile.authentication.AuthInput
import com.gemalto.idp.mobile.authentication.AuthenticationModule
import com.gemalto.idp.mobile.authentication.mode.biometric.BiometricAuthService
import com.gemalto.idp.mobile.authentication.mode.pin.PinAuthInput
import com.gemalto.idp.mobile.authentication.mode.pin.PinRule
import com.gemalto.idp.mobile.authentication.mode.pin.PinRuleException
import com.gemalto.idp.mobile.authentication.mode.pin.PinRuleIdentical
import com.gemalto.idp.mobile.authentication.mode.pin.PinRuleLength
import com.gemalto.idp.mobile.authentication.mode.pin.PinRulePalindrome
import com.gemalto.idp.mobile.authentication.mode.pin.PinRuleSeries
import com.gemalto.idp.mobile.authentication.mode.pin.PinRuleUniform
import com.gemalto.idp.mobile.core.IdpException
import com.gemalto.idp.mobile.core.IdpResultCode

/**
 * Fragment is used for in-band authentication. It will generate TOTP with selected auth input.
 */
class FragmentAuthentication : AbstractMainFragmentWithAuthSolver() {
    //region Defines
    private var mButtonOTPSign: Button? = null
    private var mButtonOTPPinOffline: Button? = null
    private var mButtonPull: Button? = null

    //endregion
    //region Life cycle
    @SuppressLint("InflateParams")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val retValue = inflater.inflate(R.layout.fragment_authentication, null)

        val domainTextView = retValue.findViewById<TextView?>(R.id.tv_fragment_description)
        if (domainTextView != null) {
            domainTextView.setText(String.format("Domain: %s", Configuration.CFG_OOB_DOMAIN))
        }

        if (Main.sharedInstance()?.managerToken != null
            && Main.sharedInstance()?.managerToken?.tokenDevice != null
        ) {
            val userName =
                Main.sharedInstance()?.managerToken?.tokenDevice?.token?.getName()
            val userNameTextView = retValue.findViewById<TextView?>(R.id.tv_fragment_caption)
            if (userNameTextView != null) {
                userNameTextView.setText(userName)
            }
        }

        mButtonOTPSign = retValue.findViewById<Button?>(R.id.button_otp_sign)
        mButtonOTPSign?.setOnClickListener(View.OnClickListener { sender: View? ->
            this.onButtonPressedSign(
                sender
            )
        })

        mButtonOTPPinOffline = retValue.findViewById<Button?>(R.id.button_otp_pin_offline)
        mButtonOTPPinOffline?.setOnClickListener(View.OnClickListener { sender: View? ->
            this.onButtonPressedAuthentication(
                sender
            )
        })

        mButtonPull = retValue.findViewById<Button?>(R.id.button_otp_pull)
        mButtonPull?.setOnClickListener(View.OnClickListener { sender: View? ->
            this.onButtonPressedPull(
                sender
            )
        })

        return retValue
    }

    override fun onPause() {
        super.onPause()

        mainActivity?.enableDrawer(false)
    }

    //endregion
    //region MainFragment methods
    /**
     * {@inheritDoc}
     */
    public override fun disableGUI() {
        setButtonOTPPinEnabled(false)
        setButtonSign(false)
        setButtonPullEnabled(false)
    }

    /**
     * {@inheritDoc}
     */
    public override fun reloadGUI() {
        // No matter what was set. If there is a loading indicator, we should disable everything.
        if (mainActivity?.isOverlayViewVisible == true) {
            disableGUI()
        } else {
            val device = Main.sharedInstance()?.managerToken?.tokenDevice

            // Those values are only for token based views.
            if (device != null && (mButtonOTPPinOffline != null || mButtonOTPSign != null)) {
                setButtonOTPPinEnabled(true)
                setButtonSign(true)
                setButtonPullEnabled(true)
            }
        }

        // Update title on pull message button.
        if (Main.sharedInstance()?.managerPush?.isIncomingMessageInQueue == true) {
            mButtonPull?.setText(R.string.ui_button_otp_open)
        } else {
            mButtonPull?.setText(R.string.ui_button_otp_pull)
        }

        mainActivity?.enableDrawer(true)

        mainActivity?.reloadGui()
    }

    @Suppress("deprecation")
    public override fun toggleTouchId() {
        val status = Main.sharedInstance()?.managerToken?.tokenDevice?.tokenStatus
        val service = BiometricAuthService.create(AuthenticationModule.create())

        if (status?.isTouchEnabled == true) {
            disableAuthMode(service.getAuthMode())
        } else {
            enableAuthMode(service.getAuthMode(), R.string.AUTH_MODE_BIOMETRICS_ENABLED)
        }
    }

    /**
     * {@inheritDoc}
     */
    public override fun changePin() {
        authInputGetAndVerifyPin(object : Protocols.AuthInputHandler {
            override fun onFinished(authInput: AuthInput?, error: String?) {
                if (authInput != null) {
                    changePinWithValidatedInput(authInput as PinAuthInput)
                }
                // Display reason of possible failure.
                mainActivity?.showErrorIfExists(error)
            }
        })
    }

    private fun changePinWithValidatedInput(validPin: PinAuthInput) {
        val tokenDevice = Main.sharedInstance()?.managerToken?.tokenDevice

        authInputGetPin(object : Protocols.SecureInputHandler {
            override fun onSecureInputFinished(firstPin: PinAuthInput, secondPin: PinAuthInput?) {
                firstPin.equals(secondPin).let {
                    if (!it) {
                        mainActivity?.showMessage(R.string.PIN_CHANGE_NO_MATCH)
                        return
                    }
                }
                try {
                    // Update to new PIN
                    tokenDevice?.token?.changePin(validPin, firstPin)
                    mainActivity?.showMessage(Main.getString(R.string.PIN_CHANGE_DESCRIPTION))
                } catch (exception: IdpException) {
                    // Pin rule error does not have any readable message. Display custom one.
                    if (exception.getCode() == IdpResultCode.TOKEN_PIN_RULE_ERROR) {
                        val pinRule = (exception as PinRuleException).getOffendingPinRule()
                        mainActivity?.showErrorIfExists(getPinRuleErrorDescription(pinRule))
                    } else {
                        mainActivity?.showErrorIfExists(exception.getLocalizedMessage())
                    }
                }
            }
        }, true)
    }

    private fun getPinRuleErrorDescription(rule: PinRule?): String {
        if (rule is PinRuleIdentical) return Main.getString(R.string.pin_rule_error_identical)

        if (rule is PinRuleLength) return Main.getString(R.string.pin_rule_error_length)

        if (rule is PinRulePalindrome) return Main.getString(R.string.pin_rule_error_palindrome)

        if (rule is PinRuleSeries) return Main.getString(R.string.pin_rule_error_series)

        if (rule is PinRuleUniform) return Main.getString(R.string.pin_rule_error_uniform)

        return Main.getString(R.string.pin_rule_error_unknown)
    }

    /**
     * {@inheritDoc}
     */
    public override fun deleteToken() {
        AlertDialog.Builder(getActivity(), R.style.AppTheme_Dialog)
            .setTitle(R.string.delete_token_title)
            .setMessage(R.string.delete_token_message)
            .setPositiveButton(
                android.R.string.yes,
                DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int -> deleteToken_SecondStep_UnregisterOob() })
            // A null listener allows the button to dismiss the dialog and take no
            // further action.

            .setNegativeButton(android.R.string.no, null).show()
    }

    //endregion
    //region Private Helpers
    private fun deleteToken_SecondStep_UnregisterOob() {
        Main.sharedInstance()?.managerPush
            ?.unregisterOOBWithCompletionHandler(object : Protocols.GenericHandler {
                override fun onFinished(success: Boolean, error: String?) {
                    if (success) {
                        deleteToken_ThirdStep_RemoveToken()
                    } else {
                        mainActivity?.loadingIndicatorHide()
                        mainActivity?.showMessage(R.string.delete_token_oob_error)
                    }
                }
            })
    }

    private fun deleteToken_ThirdStep_RemoveToken() {
        Main.sharedInstance()?.managerToken?.deleteTokenWithCompletionHandler(object : Protocols.GenericHandler {
            override fun onFinished(success: Boolean, error: String?) {
                mainActivity?.loadingIndicatorHide()
                if (success) {
                    mainActivity?.showProvisioningFragment()
                } else {
                    mainActivity?.loadingIndicatorHide()
                    mainActivity?.showErrorIfExists(error)
                }
            }
        })
    }


    /**
     * Enables or disables the OTP button.
     *
     * @param enabled `True` if enables, else `false`.
     */
    private fun setButtonOTPPinEnabled(enabled: Boolean) {
        if (mButtonOTPPinOffline != null) {
            mButtonOTPPinOffline?.setEnabled(enabled)
        }
    }

    /**
     * Enables or disables the Sign button.
     *
     * @param enabled `True` if enables, else `false`.
     */
    private fun setButtonSign(enabled: Boolean) {
        if (mButtonOTPSign != null) {
            mButtonOTPSign?.setEnabled(enabled)
        }
    }

    /**
     * Enables or disables the Pull message button.
     *
     * @param enabled `True` if enables, else `false`.
     */
    private fun setButtonPullEnabled(enabled: Boolean) {
        if (mButtonPull != null) {
            mButtonPull?.setEnabled(enabled)
        }
    }

    //endregion
    //region User Interface
    /**
     * On pressed OTP button.
     */
    private fun onButtonPressedAuthentication(sender: View?) {
        authInputGetMostComfortableOne(object : Protocols.AuthInputHandler {
            override fun onFinished(authInput: AuthInput?, error: String?) {
                if (authInput != null) {
                    mainActivity?.showOtpFragment(authInput, null, null, null)
                }
                mainActivity?.showErrorIfExists(error)
            }
        })
    }

    /**
     * On pressed Sign button.
     */
    private fun onButtonPressedSign(sender: View?) {
        mainActivity?.showSignFragment()
    }

    /**
     * On pressed pull message button.
     */
    private fun onButtonPressedPull(sender: View?) {
        Main.sharedInstance()?.managerPush?.fetchMessage()
    } //endregion
}
