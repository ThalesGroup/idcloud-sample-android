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

package com.gemalto.eziomobilesampleapp.gui;

import android.content.Intent;
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
import com.gemalto.eziomobilesampleapp.helpers.CMain;
import com.gemalto.eziomobilesampleapp.helpers.Protocols;
import com.gemalto.eziomobilesampleapp.helpers.ezio.TokenDevice;
import com.gemalto.idp.mobile.authentication.AuthMode;
import com.gemalto.idp.mobile.authentication.AuthenticationModule;
import com.gemalto.idp.mobile.authentication.mode.biofingerprint.BioFingerprintAuthService;
import com.gemalto.idp.mobile.authentication.mode.face.FaceAuthService;
import com.gemalto.idp.mobile.authentication.mode.pin.PinAuthInput;
import com.gemalto.idp.mobile.core.IdpException;

// IMPORTANT: This source code is intended to serve training information purposes only. Please make sure to review our IdCloud documentation, including security guidelines.

/**
 * Common token setting like multi-auth mode + deleting token.
 */
public class FragmentTabSettings extends MainFragmentWithAuthSolver {

    //region Defines

    private TextView mLabelDomainValue = null;
    private TextView mLabelUserIdValue = null;

    private Button mButtonDeleteToken = null;
    private Button mButtonToggleTouchId = null;
    private Button mButtonToggleFaceId = null;
    private Button mButtonChangePin = null;
    private Button mButtonPrivacyPolicy = null;

    //endregion

    //region Override

    @Nullable
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater,
                             @Nullable final ViewGroup container,
                             @Nullable final Bundle savedInstanceState) {
        return initGui(inflater, R.layout.fragment_settings);
    }

    //endregion

    //region MainFragment

    /**
     * {@inheritDoc}
     */
    @Override
    protected View initGui(final LayoutInflater inflater, final int fragmentId) {
        final View retValue = super.initGui(inflater, fragmentId);

        mLabelDomainValue = retValue.findViewById(R.id.label_domain_value);
        mLabelUserIdValue = retValue.findViewById(R.id.label_user_id_value);

        mButtonDeleteToken = getButonWithListener(R.id.button_delete_token, new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                onButtonPressedDeleteToken();
            }
        });

        mButtonToggleTouchId = getButonWithListener(R.id.button_toggle_touch_id, new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                onButtonPressedToggleTouchId();
            }
        });

        mButtonToggleFaceId = getButonWithListener(R.id.button_toggle_face_id, new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                onButtonPressedToggleFaceId();
            }
        });

        mButtonChangePin = getButonWithListener(R.id.button_change_pin, new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                onButtonPressedChangePin();
            }
        });

        mButtonPrivacyPolicy = getButonWithListener(R.id.button_privacy_policy, new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                onButtonPressedPrivacyPolicy();
            }
        });

        return retValue;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reloadGUI() {
        super.reloadGUI();

        // Get current device.
        final TokenDevice device = CMain.sharedInstance().getManagerToken().getTokenDevice();

        // This might happened during token deletion. Once it's removed reload gui is still triggered.
        if (device == null) {
            disableGUI();
            return;
        }

        mLabelDomainValue.setText(Configuration.CFG_OOB_DOMAIN);    // Domain is fixed from config.
        mLabelUserIdValue.setText(device.getToken().getName());     // We use user id as token name in sample app.

        // Non of the button is enabled with loading in place.
        final boolean enabled = !getMainActivity().loadingIndicatorIsPresent();

        // Check all auth mode states so we can enable / disable proper buttons.
        final TokenDevice.TokenStatus status = device.getTokenStatus();

        // Buttons can do both. Enable and disable modes.
        if (status.isTouchEnabled) {
            mButtonToggleTouchId.setText(R.string.AUTH_MODE_TOUCH_ID_DISABLE);
        } else {
            mButtonToggleTouchId.setText(R.string.AUTH_MODE_TOUCH_ID_ENABLE);
        }
        if (status.isFaceEnabled) {
            mButtonToggleFaceId.setText(R.string.AUTH_MODE_FACE_ID_DISABLE);
        } else {
            mButtonToggleFaceId.setText(R.string.AUTH_MODE_FACE_ID_ENABLE);
        }

        mButtonChangePin.setEnabled(enabled);
        mButtonDeleteToken.setEnabled(enabled);
        mButtonToggleTouchId.setEnabled(status.isTouchSupported && enabled);
        mButtonToggleFaceId.setEnabled(status.isFaceSupported && enabled);

        mButtonPrivacyPolicy.setVisibility(Configuration.CFG_PRIVACY_POLICY_URL == null ? View.INVISIBLE : View.VISIBLE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void disableGUI() {
        super.disableGUI();

        mButtonToggleFaceId.setEnabled(false);
        mButtonToggleTouchId.setEnabled(false);
        mButtonChangePin.setEnabled(false);
        mButtonDeleteToken.setEnabled(false);
    }

    //endregion

    //region  User Interface

    /**
     * Enables / disables FaceId auth mode.
     */
    private void onButtonPressedToggleFaceId() {
        final TokenDevice.TokenStatus status = CMain.sharedInstance().getManagerToken().getTokenDevice().getTokenStatus();
        final FaceAuthService service = FaceAuthService.create(AuthenticationModule.create());

        if (status.isFaceEnabled) {
            disableAuthMode(service.getAuthMode());
        } else {
            enableAuthMode(service.getAuthMode());
        }
    }

    /**
     * Enables / disables TouchId auth mode.
     */
    private void onButtonPressedToggleTouchId() {
        final TokenDevice.TokenStatus status = CMain.sharedInstance().getManagerToken().getTokenDevice().getTokenStatus();
        final BioFingerprintAuthService service = BioFingerprintAuthService.create(AuthenticationModule.create());

        if (status.isTouchEnabled) {
            disableAuthMode(service.getAuthMode());
        } else {
            enableAuthMode(service.getAuthMode());
        }
    }

    /**
     * Button pressed listener for change PIN
     */
    private void onButtonPressedChangePin() {
        authInputGetPin(new Protocols.SecureInputHandler() {
            @Override
            public void onSecureInputFinished(@NonNull final PinAuthInput firstPin, final PinAuthInput secondPin) {
                try {
                    CMain.sharedInstance().getManagerToken().getTokenDevice().getToken().changePin(firstPin, secondPin);
                    getMainActivity().showMessage(CMain.getString(R.string.PIN_CHANGE_CAPTION), CMain.getString(R.string.PIN_CHANGE_DESCRIPTION));
                } catch (IdpException e) {
                    getMainActivity().showErrorIfExists(e.getLocalizedMessage());
                }
            }
        }, true);
    }

    /**
     * Button pressed listener for Privacy policy.
     */
    private void onButtonPressedPrivacyPolicy() {
        if (Configuration.CFG_PRIVACY_POLICY_URL != null) {
            final Intent browserIntent = new Intent(Intent.ACTION_VIEW, Configuration.CFG_PRIVACY_POLICY_URL);
            startActivity(browserIntent);
        }
    }

    /**
     * Button pressed listener for delete token.
     */
    private void onButtonPressedDeleteToken() {
        // Disable whole UI and display loading indicator.
        getMainActivity().loadingIndicatorShow(CMain.getString(R.string.LOADING_MESSAGE_REMOVING));

        // Try to unregister and remove token.
        CMain.sharedInstance().getManagerToken().deleteTokenWithCompletionHandler(new Protocols.GenericHandler() {
            @Override
            public void onFinished(final boolean success, final String error) {
                getMainActivity().loadingIndicatorHide();
                if (success) {
                    getMainActivity().tabBarSwitchToCurrentState();
                } else {
                    getMainActivity().showErrorIfExists(error);
                }
            }
        });
    }

    //endregion

    //region Private Helpers

    /**
     * Disables {@code AuthMode}.
     * @param mode {@code AuthMode} to disable.
     */
    private void disableAuthMode(final AuthMode mode) {
        final TokenDevice device = CMain.sharedInstance().getManagerToken().getTokenDevice();

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
     * Enables {@code AuthMode}.
     * @param mode {@code AuthMode to disable}.
     */
    private void enableAuthMode(final AuthMode mode) {
        final TokenDevice device = CMain.sharedInstance().getManagerToken().getTokenDevice();

        // We must enable multi-auth mode before activating any specific one.
        // Since we need pin for both of those operations this metod will ask for it and return one directly.
        enableMultiauthWithCompletionHandler(new Protocols.SecureInputHandler() {
            @Override
            public void onSecureInputFinished(@NonNull final PinAuthInput firstPin, final PinAuthInput secondPin) {
                try {
                    device.getToken().activateAuthMode(mode, firstPin);
                } catch (IdpException e) {
                    getMainActivity().showErrorIfExists(e.getLocalizedMessage());
                } finally {
                    reloadGUI();
                }
            }
        });
    }

    /**
     * Enables multi auth mode.
     * @param handler Callback.
     */
    private void enableMultiauthWithCompletionHandler(@NonNull final Protocols.SecureInputHandler handler) {
        final TokenDevice device = CMain.sharedInstance().getManagerToken().getTokenDevice();

        // Check whenever multiauthmode is already enabled.
        try {
            final boolean isEnabled = device.getToken().isMultiAuthModeEnabled();

            // In both cases we will need auth pin, because it's used for
            // multi-auth upgrade as well as enabling specific authmodes.
            authInputGetPin(new Protocols.SecureInputHandler() {
                @Override
                public void onSecureInputFinished(@NonNull final PinAuthInput firstPin, final PinAuthInput secondPin) {
                    // If multi-auth is not enabled and we do have pin, we can try to upgrade it.
                    if (!isEnabled) {
                        try {
                            device.getToken().upgradeToMultiAuthMode(firstPin);
                        } catch (IdpException e) {
                            getMainActivity().showErrorIfExists(e.getLocalizedMessage());
                            reloadGUI();
                        }
                    }
                    // Notify handler
                    handler.onSecureInputFinished(firstPin, secondPin);
                }

            }, false);
        } catch (IdpException exception) {
            getMainActivity().showErrorIfExists(exception.getLocalizedMessage());
            reloadGUI();
        }
    }

    //endregion
}
