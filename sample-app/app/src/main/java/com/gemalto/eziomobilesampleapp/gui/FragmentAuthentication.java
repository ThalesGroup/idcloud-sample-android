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

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.gemalto.eziomobilesampleapp.Configuration;
import com.gemalto.eziomobilesampleapp.R;
import com.gemalto.eziomobilesampleapp.helpers.Main;
import com.gemalto.eziomobilesampleapp.helpers.ezio.TokenDevice;
import com.gemalto.idp.mobile.authentication.AuthenticationModule;
import com.gemalto.idp.mobile.authentication.mode.biofingerprint.BioFingerprintAuthService;
import com.gemalto.idp.mobile.authentication.mode.pin.PinAuthInput;
import com.gemalto.idp.mobile.authentication.mode.pin.PinRule;
import com.gemalto.idp.mobile.authentication.mode.pin.PinRuleException;
import com.gemalto.idp.mobile.authentication.mode.pin.PinRuleIdentical;
import com.gemalto.idp.mobile.authentication.mode.pin.PinRuleLength;
import com.gemalto.idp.mobile.authentication.mode.pin.PinRulePalindrome;
import com.gemalto.idp.mobile.authentication.mode.pin.PinRuleSeries;
import com.gemalto.idp.mobile.authentication.mode.pin.PinRuleUniform;
import com.gemalto.idp.mobile.core.IdpException;

import static com.gemalto.idp.mobile.core.IdpResultCode.TOKEN_PIN_RULE_ERROR;

/**
 * Fragment is used for in-band authentication. It will generate TOTP with selected auth input.
 */
public class FragmentAuthentication extends AbstractMainFragmentWithAuthSolver {

    //region Defines

    private Button mButtonOTPSign = null;
    private Button mButtonOTPPinOffline = null;
    private Button mButtonPull = null;

    //endregion

    //region Life cycle

    @Nullable
    @Override
    @SuppressLint("InflateParams")
    public View onCreateView(
            @NonNull final LayoutInflater inflater,
            @Nullable final ViewGroup container,
            @Nullable final Bundle savedInstanceState
    ) {
        final View retValue = inflater.inflate(R.layout.fragment_authentication, null);

        final TextView domainTextView = retValue.findViewById(R.id.tv_fragment_description);
        if (domainTextView != null) {
            domainTextView.setText(String.format("Domain: %s", Configuration.CFG_OOB_DOMAIN));
        }

        if (Main.sharedInstance().getManagerToken() != null
                && Main.sharedInstance().getManagerToken().getTokenDevice() != null) {
            final String userName = Main.sharedInstance().getManagerToken().getTokenDevice().getToken().getName();
            final TextView userNameTextView = retValue.findViewById(R.id.tv_fragment_caption);
            if (userNameTextView != null) {
                userNameTextView.setText(userName);
            }
        }

        mButtonOTPSign = retValue.findViewById(R.id.button_otp_sign);
        mButtonOTPSign.setOnClickListener(this::onButtonPressedSign);

        mButtonOTPPinOffline = retValue.findViewById(R.id.button_otp_pin_offline);
        mButtonOTPPinOffline.setOnClickListener(this::onButtonPressedAuthentication);

        mButtonPull = retValue.findViewById(R.id.button_otp_pull);
        mButtonPull.setOnClickListener(this::onButtonPressedPull);

        return retValue;
    }

    @Override
    public void onPause() {
        super.onPause();

        getMainActivity().enableDrawer(false);
    }

    //endregion

    //region MainFragment methods

    /**
     * {@inheritDoc}
     */
    @Override
    public void disableGUI() {
        setButtonOTPPinEnabled(false);
        setButtonSign(false);
        setButtonPullEnabled(false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reloadGUI() {
        // No matter what was set. If there is a loading indicator, we should disable everything.
        if (getMainActivity().isOverlayViewVisible()) {
            disableGUI();
        } else {
            final TokenDevice device = Main.sharedInstance().getManagerToken().getTokenDevice();

            // Those values are only for token based views.
            if (device != null && (mButtonOTPPinOffline != null || mButtonOTPSign != null)) {
                setButtonOTPPinEnabled(true);
                setButtonSign(true);
                setButtonPullEnabled(true);
            }
        }

        // Update title on pull message button.
        if (Main.sharedInstance().getManagerPush().isIncomingMessageInQueue()) {
            mButtonPull.setText(R.string.ui_button_otp_open);
        } else {
            mButtonPull.setText(R.string.ui_button_otp_pull);
        }

        getMainActivity().enableDrawer(true);

        getMainActivity().reloadGui();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void toggleTouchId() {
        final TokenDevice.TokenStatus status = Main.sharedInstance().getManagerToken().getTokenDevice()
                .getTokenStatus();
        final BioFingerprintAuthService service = BioFingerprintAuthService.create(AuthenticationModule.create());

        if (status.isTouchEnabled) {
            disableAuthMode(service.getAuthMode());
        } else {
            enableAuthMode(service.getAuthMode(), R.string.AUTH_MODE_TOUCH_ID_ENABLED);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void changePin() {
        authInputGetAndVerifyPin((authInput, error) -> {
            if (authInput != null) {
                changePinWithValidatedInput((PinAuthInput) authInput);
            }

            // Display reason of possible failure.
            getMainActivity().showErrorIfExists(error);
        });
    }

    private void changePinWithValidatedInput(@NonNull final PinAuthInput validPin) {
        final TokenDevice tokenDevice = Main.sharedInstance().getManagerToken().getTokenDevice();

        authInputGetPin((firstPin, secondPin) -> {
            if (!firstPin.equals(secondPin)) {
                getMainActivity().showMessage(R.string.PIN_CHANGE_NO_MATCH);
                return;
            }

            try {
                // Update to new PIN
                tokenDevice.getToken().changePin(validPin, firstPin);
                getMainActivity().showMessage(Main.getString(R.string.PIN_CHANGE_DESCRIPTION));
            } catch (final IdpException exception) {
                // Pin rule error does not have any readable message. Display custom one.
                if (exception.getCode() == TOKEN_PIN_RULE_ERROR) {
                    final PinRule pinRule = ((PinRuleException) exception).getOffendingPinRule();
                    getMainActivity().showErrorIfExists(getPinRuleErrorDescription(pinRule));
                } else {
                    getMainActivity().showErrorIfExists(exception.getLocalizedMessage());
                }
            }
        }, true);
    }

    private String getPinRuleErrorDescription(final PinRule rule) {
        if (rule instanceof PinRuleIdentical)
            return Main.getString(R.string.pin_rule_error_identical);

        if (rule instanceof PinRuleLength)
            return Main.getString(R.string.pin_rule_error_length);

        if (rule instanceof PinRulePalindrome)
            return Main.getString(R.string.pin_rule_error_palindrome);

        if (rule instanceof PinRuleSeries)
            return Main.getString(R.string.pin_rule_error_series);

        if (rule instanceof PinRuleUniform)
            return Main.getString(R.string.pin_rule_error_uniform);

        return Main.getString(R.string.pin_rule_error_unknown);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteToken() {
        new AlertDialog.Builder(getActivity(), R.style.AppTheme_Dialog).setTitle(R.string.delete_token_title)
                .setMessage(R.string.delete_token_message)
                .setPositiveButton(android.R.string.yes,
                        (dialog, which) -> deleteToken_SecondStep_UnregisterOob())

                // A null listener allows the button to dismiss the dialog and take no
                // further action.
                .setNegativeButton(android.R.string.no, null).show();
    }

    //endregion

    //region Private Helpers

    private void deleteToken_SecondStep_UnregisterOob() {
        Main.sharedInstance().getManagerPush().unregisterOOBWithCompletionHandler((success, error) -> {
            if (success) {
                deleteToken_ThirdStep_RemoveToken();
            } else {
                getMainActivity().loadingIndicatorHide();
                getMainActivity().showMessage(R.string.delete_token_oob_error);
            }
        });
    }

    private void deleteToken_ThirdStep_RemoveToken() {
        Main.sharedInstance().getManagerToken().deleteTokenWithCompletionHandler((success, error) -> {
            getMainActivity().loadingIndicatorHide();
            if (success) {
                getMainActivity().showProvisioningFragment();
            } else {
                getMainActivity().loadingIndicatorHide();
                getMainActivity().showErrorIfExists(error);
            }
        });
    }


    /**
     * Enables or disables the OTP button.
     *
     * @param enabled {@code True} if enables, else {@code false}.
     */
    private void setButtonOTPPinEnabled(final boolean enabled) {
        if (mButtonOTPPinOffline != null) {
            mButtonOTPPinOffline.setEnabled(enabled);
        }
    }

    /**
     * Enables or disables the Sign button.
     *
     * @param enabled {@code True} if enables, else {@code false}.
     */
    private void setButtonSign(final boolean enabled) {
        if (mButtonOTPSign != null) {
            mButtonOTPSign.setEnabled(enabled);
        }
    }

    /**
     * Enables or disables the Pull message button.
     *
     * @param enabled {@code True} if enables, else {@code false}.
     */
    private void setButtonPullEnabled(final boolean enabled) {
        if (mButtonPull != null) {
            mButtonPull.setEnabled(enabled);
        }
    }

    //endregion

    //region User Interface

    /**
     * On pressed OTP button.
     */
    private void onButtonPressedAuthentication(final View sender) {
        authInputGetMostComfortableOne((authInput, error) -> {
            if (authInput != null) {
                getMainActivity().showOtpFragment(authInput, null, null, null);
            }
            getMainActivity().showErrorIfExists(error);
        });
    }

    /**
     * On pressed Sign button.
     */
    private void onButtonPressedSign(final View sender) {
        getMainActivity().showSignFragment();
    }

    /**
     * On pressed pull message button.
     */
    private void onButtonPressedPull(final View sender) {
        Main.sharedInstance().getManagerPush().fetchMessage();
    }

    //endregion

}
