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

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.gemalto.eziomobilesampleapp.Configuration;
import com.gemalto.eziomobilesampleapp.R;
import com.gemalto.eziomobilesampleapp.gui.overlays.FragmentQRCodeReader;
import com.gemalto.eziomobilesampleapp.helpers.CMain;
import com.gemalto.eziomobilesampleapp.helpers.Protocols;
import com.gemalto.eziomobilesampleapp.helpers.ezio.TokenManager;
import com.gemalto.idp.mobile.core.util.SecureByteArray;
import com.gemalto.idp.mobile.core.util.SecureString;
import com.gemalto.idp.mobile.otp.oath.OathToken;

// IMPORTANT: This source code is intended to serve training information purposes only. Please make sure to review our IdCloud documentation, including security guidelines.

/**
 * Enrolling and provisioning of new token using user id + registration code, or QR code.
 */
public class FragmentTabProvision extends MainFragment implements FragmentQRCodeReader.QRCodeReaderDelegate {

    //region Defines

    TextView mLabelDomainValue = null;
    private Button mButtonEnrollWithQr = null;
    private Button mButtonEnrollManually = null;
    private EditText mTextUserId = null;
    private EditText mTextRegCode = null;

    //endregion

    //region Override

    @Nullable
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater,
                             @Nullable final ViewGroup container,
                             @Nullable final Bundle savedInstanceState) {
        final View retValue = initGui(inflater, R.layout.fragment_provision);

        mLabelDomainValue = retValue.findViewById(R.id.label_domain_value);
        mLabelDomainValue.setText(Configuration.CFG_OOB_DOMAIN);

        return retValue;
    }

    //endregion

    //region MainFragment

    /**
     * {@inheritDoc}
     */
    @Override
    protected View initGui(final LayoutInflater inflater, final int fragmentId) {
        final View retValue = super.initGui(inflater, fragmentId);

        mButtonEnrollWithQr = getButonWithListener(R.id.button_scan_and_enroll, new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                onButtonPressedEnrollQr();
            }
        });

        mButtonEnrollManually = getButonWithListener(R.id.button_enroll, new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                onButtonPressedEnrollManually();
            }
        });

        mTextUserId = retValue.findViewById(R.id.text_user_id);
        mTextUserId.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(final CharSequence charSequence, final int start, final int count, final int after) {
                // Unused
            }

            @Override
            public void onTextChanged(final CharSequence charSequence, final int start, final int count, final int after) {
                onTextChangedUserId();
            }

            @Override
            public void afterTextChanged(final Editable editable) {
                // Unused
            }
        });

        mTextRegCode = retValue.findViewById(R.id.text_reg_code);
        mTextRegCode.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(final CharSequence charSequence, final int start, final int count, final int after) {
                // Unused
            }

            @Override
            public void onTextChanged(final CharSequence charSequence, final int start, final int count, final int after) {
                onTextChangedRegCode();
            }

            @Override
            public void afterTextChanged(final Editable editable) {
                // Unused
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

        if (getMainActivity().loadingIndicatorIsPresent()) {
            disableGUI();
        } else {
            updateUIAvailability();
        }

        hideToolbar();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void disableGUI() {
        super.disableGUI();

        mButtonEnrollManually.setEnabled(false);
        mButtonEnrollWithQr.setEnabled(false);
        mTextUserId.setEnabled(false);
        mTextRegCode.setEnabled(false);
    }

    //endregion

    //region Helpers

    /**
     * Updates UI.
     */
    private void updateUIAvailability() {
        // Enable provision button only when both user id and registration code are provided.
        mButtonEnrollManually.setEnabled(mTextUserId.getText().length() > 0 && mTextRegCode.getText().length() > 0);
        mButtonEnrollWithQr.setEnabled(true);
        mTextUserId.setEnabled(true);
        mTextRegCode.setEnabled(true);
    }

    /**
     * Enrolls with user ID.
     * @param userId User ID.
     * @param registrationCode Registration code.
     */
    private void enrollWithUserId(final String userId, final SecureString registrationCode) {
        final CMain main = CMain.sharedInstance();

        // Disable whole UI and display loading indicator.
        getMainActivity().hideKeyboard();
        getMainActivity().loadingIndicatorShow(CMain.getString(R.string.LOADING_MESSAGE_ENROLLING));

        // Do provisioning and wait for response.
        main.getManagerToken().provisionWithUserId(userId, registrationCode, new TokenManager.ProvisionerHandler() {
            @Override
            public void onProvisionerFinished(final OathToken token, final String error) {
                // Hide loading indicator and reload gui.
                getMainActivity().loadingIndicatorHide();

                // Token was created. Switch tabs.
                if (token != null) {
                    getMainActivity().tabBarSwitchToCurrentState();
                } else {
                    getMainActivity().showErrorIfExists(error);
                }
            }
        });
    }

    //endregion

    //region  User Interface

    /**
     * On button pressed listener for enroll using QR code.
     */
    private void onButtonPressedEnrollQr() {

        final FragmentQRCodeReader fragment = new FragmentQRCodeReader();
        fragment.init(this, 0);

        getActivity().getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
                .replace(R.id.fragment_container, fragment, null)
                .addToBackStack(null)
                .commit();
    }

    /**
     * On button pressed listener for manual enrollment.
     */
    private void onButtonPressedEnrollManually() {
        enrollWithUserId(mTextUserId.getText().toString(), CMain.secureStringFromString(mTextRegCode.getText().toString()));
    }

    /**
     * Updates UI after user ID changed.
     */
    private void onTextChangedUserId() {
        // Provision button is enabled only with both textboxes filled.
        updateUIAvailability();
    }

    /**
     * Updates UI after registration code changed.
     */
    private void onTextChangedRegCode() {
        // Provision button is enabled only with both textboxes filled.
        updateUIAvailability();
    }

    //endregion


    //region QRCodeReaderDelegate

    /**
     * {@inheritDoc}
     */
    @Override
    public void onQRCodeFinished(final FragmentQRCodeReader sender,
                                 final SecureByteArray qrCodeData,
                                 final String error) {

        // First check parser result.
        if (qrCodeData == null) {
            getMainActivity().showErrorIfExists(error);
            return;
        }

        // Try to parse data from provided QR Code. Actual operation is synchronous. We can use self in block directly.
        CMain.sharedInstance().getManagerQRCode().parseQRCode(qrCodeData, new Protocols.QRCodeManagerHandler() {
            @Override
            public void onParseFinished(final boolean success, final String userId, final SecureString regCode, final String error) {
                if (success) {
                    enrollWithUserId(userId, regCode);
                } else {
                    getMainActivity().showErrorIfExists(error);
                    updateUIAvailability();
                }
            }
        });
    }

    //endregion


}
