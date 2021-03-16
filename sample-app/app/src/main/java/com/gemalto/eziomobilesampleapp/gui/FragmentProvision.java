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
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
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
import com.gemalto.eziomobilesampleapp.helpers.Main;
import com.gemalto.idp.mobile.core.util.SecureByteArray;
import com.gemalto.idp.mobile.core.util.SecureString;

/**
 * Enrolling and provisioning of new token using user id + registration code, or QR code.
 */
public class FragmentProvision extends AbstractMainFragment implements FragmentQRCodeReader.QRCodeReaderDelegate {

    //region Defines

    private Button mButtonEnrollWithQr = null;
    private Button mButtonEnrollManually = null;
    private EditText mTextUserId = null;
    private EditText mTextRegCode = null;

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
        final View retValue = inflater.inflate(R.layout.fragment_provision, null);

        mButtonEnrollWithQr = retValue.findViewById(R.id.button_enroll_with_qr);
        mButtonEnrollWithQr.setOnClickListener(this::onButtonPressedEnrollQr);

        mButtonEnrollManually = retValue.findViewById(R.id.button_enroll);
        mButtonEnrollManually.setOnClickListener(this::onButtonPressedEnrollManually);

        final TextView domainTextView = retValue.findViewById(R.id.tv_fragment_description);
        if (domainTextView != null) {
            domainTextView.setText(String.format("Domain: %s", Configuration.CFG_OOB_DOMAIN));
        }

        mTextUserId = retValue.findViewById(R.id.text_amount);
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

        mTextRegCode = retValue.findViewById(R.id.text_beneficiary);
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

    //endregion

    //region MainFragment

    /**
     * {@inheritDoc}
     */
    @Override
    public void reloadGUI() {
        if (getMainActivity().isOverlayViewVisible()) {
            disableGUI();
        } else {
            updateUIAvailability();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void disableGUI() {
        mButtonEnrollManually.setEnabled(false);
        mButtonEnrollWithQr.setEnabled(false);
        mTextUserId.setEnabled(false);
        mTextRegCode.setEnabled(false);
    }

    //endregion

    //region Helpers

    /**
     * Updates the UI.
     */
    private void updateUIAvailability() {
        // Enable provision button only when both user id and registration code are provided.
        mButtonEnrollManually.setEnabled(mTextUserId.getText().length() > 0 && mTextRegCode.getText().length() > 0);
        mButtonEnrollWithQr.setEnabled(true);
        mTextUserId.setEnabled(true);
        mTextRegCode.setEnabled(true);
    }

    /**
     * Enrolls the user.
     *
     * @param userId           User id.
     * @param registrationCode Registration code.
     */
    private void enrollWithUserId(
            final String userId,
            final SecureString registrationCode
    ) {
        final Main main = Main.sharedInstance();

        // Disable whole UI and display loading indicator.
        getMainActivity().hideKeyboard();
        getMainActivity().loadingIndicatorShow(Main.getString(R.string.LOADING_MESSAGE_ENROLLING));

        // Do provisioning and wait for response.
        main.getManagerToken().provisionWithUserId(userId, registrationCode, (token, error) -> {
            // Hide loading indicator and reload gui.
            getMainActivity().loadingIndicatorHide();

            // Token was created. Switch tabs.
            if (token != null) {
                getMainActivity().showAuthenticationFragment();
            } else {
                getMainActivity().showErrorIfExists(error);
            }
        });
    }

    //endregion

    //region QRCodeReaderDelegate

    /**
     * {@inheritDoc}
     */
    @Override
    public void onQRCodeFinished(
            final FragmentQRCodeReader sender,
            final SecureByteArray qrCodeData,
            final String error
    ) {

        // Hide any previous dialogs if exists.
        dialogFragmentHide();

        // First check parser result.
        if (qrCodeData == null) {
            getMainActivity().showErrorIfExists(error);
            return;
        }

        // Try to parse data from provided QR Code. Actual operation is synchronous. We can use self in block directly.
        Main.sharedInstance().getManagerQRCode().parseQRCode(qrCodeData, (success, userId, regCode, error1) -> {
            if (success) {
                enrollWithUserId(userId, regCode);
            } else {
                getMainActivity().showErrorIfExists(error1);
                updateUIAvailability();
            }
        });
    }

    //endregion

    //region  User Interface

    /**
     * Enrolls the user using the QR code.
     */
    private void onButtonPressedEnrollQr(final View sender) {
        final FragmentQRCodeReader fragment = new FragmentQRCodeReader();
        fragment.init(FragmentProvision.this, 0);

        if (getActivity() != null)
            dialogFragmentShow(fragment, DIALOG_TAG_QR_CODE_READER, true);
    }

    private String mLastDialogFragmentTag = null;
    private static final String DIALOG_TAG_QR_CODE_READER = "DIALOG_TAG_QR_CODE_READER";
    public void dialogFragmentShow(
            DialogFragment dialog,
            String dialogTag,
            boolean fullscreen
    ) {
        // Hide any previous dialogs if exists.
        dialogFragmentHide();

        // If desired make dialog appear in fullscreen.
        if (fullscreen) {
            dialog.setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        }

        // Save last tag and display fragment.
        mLastDialogFragmentTag = dialogTag;
        dialog.show(getActivity().getSupportFragmentManager(), mLastDialogFragmentTag);
    }

    public void dialogFragmentHide() {
        // Hide fragment if exists
        if (mLastDialogFragmentTag != null) {
            Fragment fragment = getActivity().getSupportFragmentManager().findFragmentByTag(mLastDialogFragmentTag);
            if (fragment instanceof DialogFragment) {
                ((DialogFragment) fragment).dismiss();
            }
            mLastDialogFragmentTag = null; // NOPMD
        }
    }

    /**
     * Enrolls the user with user name and registration code.
     */
    private void onButtonPressedEnrollManually(final View sender) {
        enrollWithUserId(mTextUserId.getText().toString(), Main.sharedInstance().secureStringFromString(mTextRegCode.getText().toString()));
    }

    /**
     * Updates the UI after text input.
     */
    private void onTextChangedUserId() {
        // Provision button is enabled only with both textboxes filled.
        updateUIAvailability();
    }

    /**
     * Updates the UI after text input.
     */
    private void onTextChangedRegCode() {
        // Provision button is enabled only with both textboxes filled.
        updateUIAvailability();
    }

    //endregion

}
