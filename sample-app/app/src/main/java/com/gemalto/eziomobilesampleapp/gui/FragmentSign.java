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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gemalto.eziomobilesampleapp.Configuration;
import com.gemalto.eziomobilesampleapp.R;
import com.gemalto.eziomobilesampleapp.helpers.Main;
import com.gemalto.eziomobilesampleapp.helpers.ezio.KeyValue;
import com.gemalto.idp.mobile.core.util.SecureString;

import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;


/**
 * Fragment to retrieve the Amount and Beneficiary for transaction signing.
 */
public class FragmentSign extends AbstractMainFragmentWithAuthSolver {

    //region Defines

    private final static char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
    private TextView mTextAmount;
    private TextView mTextBeneficiary;
    private Button mButtonProceed;

    //endregion

    //region Life Cycle

    @Nullable
    @Override
    @SuppressLint("InflateParams")
    public View onCreateView(
            @NonNull final LayoutInflater inflater,
            @Nullable final ViewGroup container,
            @Nullable final Bundle savedInstanceState
    ) {
        final View retValue = inflater.inflate(R.layout.fragment_sign, null);

        mTextAmount = retValue.findViewById(R.id.text_amount);
        mTextBeneficiary = retValue.findViewById(R.id.text_beneficiary);
        mButtonProceed = retValue.findViewById(R.id.button_enroll);
        mButtonProceed.setOnClickListener(this::onButtonPressedProceed);

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

        return retValue;
    }

    //endregion

    //region MainFragment methods

    /**
     * {@inheritDoc}
     */
    @Override
    public void reloadGUI() {
        // No matter what was set. If there is a loading indicator, we should disable everything.
        if (getMainActivity().isOverlayViewVisible()) {
            disableGUI();
        } else {
            mTextAmount.setEnabled(true);
            mTextBeneficiary.setEnabled(true);
            mButtonProceed.setEnabled(true);
        }

        getMainActivity().enableDrawer(false);
        getMainActivity().reloadGui();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void disableGUI() {
        mTextAmount.setEnabled(false);
        mTextBeneficiary.setEnabled(false);
        mButtonProceed.setEnabled(false);
    }

    //endregion

    //region Private Helpers

    /**
     * Creates the challenge to be signed.
     *
     * @return Challenge
     */
    private SecureString getServerChallenge() {
        final List<KeyValue> values = new ArrayList<>();
        values.add(new KeyValue("amount", mTextAmount.getText().toString()));
        values.add(new KeyValue("beneficiary", mTextBeneficiary.getText().toString()));

        return Main.sharedInstance().secureStringFromString(getOcraChallenge(values));
    }

    /**
     * Calculate OCRA Challenge from array of key value objects.
     *
     * @param values List of key values object we want to use for ocra calculation.
     * @return SecureString representation of challenge or null in case of error.
     */
    private String getOcraChallenge(final List<KeyValue> values) {
        String retValue = null;

        // Use builder to append TLV
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        // Go through all values, calculate and append TLV for each one of them.
        for (int i = 0; i < values.size(); i++) {
            // Convert key-value to UTF8 string
            final byte[] keyValueUTF8 = values.get(i).getKeyValueUTF8();

            // Build TLV.
            buffer.write(0xDF);
            buffer.write(0x71 + i);
            buffer.write((byte) keyValueUTF8.length);
            buffer.write(keyValueUTF8, 0, keyValueUTF8.length);
        }

        // Try to calculate digest from final string and build retValue.
        try {
            final MessageDigest digest = MessageDigest.getInstance("SHA-256");
            final byte[] hash = digest.digest(buffer.toByteArray());

            // Server challenge expect hex string not byte array.
            retValue = bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            // Ignore. In worst case it will generate invalid ocra.
        }

        return retValue;
    }

    /**
     * Creates hexa string from bytes.
     *
     * @param bytes Bytes from which to create the hexa string.
     * @return Hexa string.
     */
    public String bytesToHex(final byte[] bytes) {
        final char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            final int value = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[value >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[value & 0x0F];
        }
        return new String(hexChars);
    }

    //endregion

    //region User Interface

    /**
     * On pressed proceed button.
     */
    private void onButtonPressedProceed(final View sender) {
        if (mTextAmount.getText().toString().isEmpty() || mTextBeneficiary.getText().toString().isEmpty()) {
            return;
        }

        authInputGetMostComfortableOne((authInput, error) -> {
            if (authInput != null) {

                // Remove current fragment from stack before displaying OTP.
                getMainActivity().hideLastStackFragment();

                // Display OTP.
                getMainActivity().showOtpFragment(authInput,
                        getServerChallenge(),
                        mTextAmount.getText().toString(),
                        mTextBeneficiary.getText().toString());
            }

            getMainActivity().showErrorIfExists(error);
        });
    }

    //endregion

}
