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

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.gemalto.eziomobilesampleapp.R;
import com.gemalto.eziomobilesampleapp.helpers.CMain;
import com.gemalto.eziomobilesampleapp.helpers.Protocols;
import com.gemalto.eziomobilesampleapp.helpers.ezio.KeyValue;
import com.gemalto.idp.mobile.authentication.AuthInput;
import com.gemalto.idp.mobile.core.util.SecureString;

import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

// IMPORTANT: This source code is intended to serve training information purposes only. Please make sure to review our IdCloud documentation, including security guidelines.

/**
 * Fragment is used for in-band transaction sign. It will generate OCRA TOTP with
 * selected auth input.
 */
public class FragmentTabTransaction extends MainFragmentWithAuthSolver {

    //region Defines

    final Protocols.OTPDelegate mSendSignRequest;

    EditText mTextAmount = null;
    EditText mTextBeneficiary = null;

    //endregion

    //region Life Cycle

    /**
     * Creates a new {@code FragmentTabTransaction} object.
     */
    public FragmentTabTransaction() {
        super();

        mSendSignRequest = new Protocols.OTPDelegate() {
            @Override
            public void onOTPDelegateFinished(@Nullable final SecureString otp, @Nullable final String error,
                                              @Nullable final AuthInput authInput, final SecureString serverChallenge) {
                CMain.sharedInstance().getManagerHttp().sendSignRequest(
                        otp, error, authInput, serverChallenge,
                        mTextAmount.getText().toString(),
                        mTextBeneficiary.getText().toString());
            }
        };
    }

    //endregion

    //region Override

    @Nullable
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater,
                             @Nullable final ViewGroup container,
                             @Nullable final Bundle savedInstanceState) {
        return initGui(inflater, R.layout.fragment_transaction);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected View initGui(final LayoutInflater inflater, final int fragmentId) {
        final View retValue = super.initGui(inflater, fragmentId);

        mTextAmount = retValue.findViewById(R.id.text_amount);
        mTextBeneficiary = retValue.findViewById(R.id.text_beneficiary);

        return retValue;
    }

    //endregion

    //region User Interface

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onButtonPressedOTPPin(final OTPHandlerType type) {
        totpWithPin(getServerChallenge(), type == OTPHandlerType.Offline ? null : mSendSignRequest);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onButtonPressedOTPFaceId(final OTPHandlerType type) {
        totpWithFaceId(getServerChallenge(), type == OTPHandlerType.Offline ? null : mSendSignRequest);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onButtonPressedOTPTouchId(final OTPHandlerType type) {
        totpWithTouchId(getServerChallenge(), type == OTPHandlerType.Offline ? null : mSendSignRequest);
    }

    //endregion

    //region Private Helpers

    /**
     * Retrieves the sever challenge.
     * @return Server challenge.
     */
    private SecureString getServerChallenge() {
        final List<KeyValue> values = new ArrayList<>();
        values.add(new KeyValue("amount", mTextAmount.getText().toString()));
        values.add(new KeyValue("beneficiary", mTextBeneficiary.getText().toString()));

        return getOcraChallenge(values);
    }

    /**
     * Calculate OCRA Challenge from array of key value objects.
     *
     * @param values List of key values object we want to use for ocra calculation.
     * @return SecureString representation of challenge or null in case of error.
     */
    private SecureString getOcraChallenge(final List<KeyValue> values) {
        SecureString retValue = null;

        // Use builder to append TLV
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        // Go through all values, calculate and append TLV for each one of them.
        for (int i = 0; i < values.size(); i++) {
            // Convert keyvalue to UTF8 string
            final byte[] keyValueUTF8 = values.get(i).getKeyValueUTF8();

            // Build TLV.
            buffer.write(0xDF);
            buffer.write(0x71 + i);
            buffer.write((byte) keyValueUTF8.length); // TODO: Do we need more than one byte?
            buffer.write(keyValueUTF8, 0, keyValueUTF8.length);
        }

        // Try to calculate digest from final string and build retValue.
        try {
            final MessageDigest digest = MessageDigest.getInstance("SHA-256");
            final byte[] hash = digest.digest(buffer.toByteArray());

            // Server challenge expect hex string not byte array.
            retValue = CMain.secureStringFromString(bytesToHex(hash));
        } catch (NoSuchAlgorithmException e) {
            // Ignore. In worst case it will generate invalid ocra.
        }

        return retValue;
    }

    private final static char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    /**
     * Converts the bytes to hexa string.
     * @param bytes Input bytes.
     * @return Hexa string.
     */
    public static String bytesToHex(final byte[] bytes) {
        final char[] hexChars = new char[bytes.length * 2];
        for (int index = 0; index < bytes.length; index++) {
            final int loopByte = bytes[index] & 0xFF;
            hexChars[index * 2] = HEX_ARRAY[loopByte >>> 4];
            hexChars[index * 2 + 1] = HEX_ARRAY[loopByte & 0x0F];
        }
        return new String(hexChars);
    }

    //endregion
}
