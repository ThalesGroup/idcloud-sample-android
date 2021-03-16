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

package com.gemalto.eziomobilesampleapp.gui.overlays;

import android.Manifest;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.gemalto.eziomobilesampleapp.MainActivity;
import com.gemalto.eziomobilesampleapp.helpers.Main;
import com.gemalto.idp.mobile.core.util.SecureByteArray;
import com.google.zxing.Result;

import java.math.BigInteger;

import me.dm7.barcodescanner.zxing.ZXingScannerView;

/**
 * Simple QR Code reader.
 */
public class FragmentQRCodeReader extends DialogFragment implements ZXingScannerView.ResultHandler {

    //region Defines

    /**
     * Notify about results.
     */
    public interface QRCodeReaderDelegate {
        /**
         * Triggered once QR scanning operation is done.
         * @param sender Instance of this class. Can be used to check custom flag etc.
         * @param qrCodeData Scanned qr code raw data.
         * @param error Description in case of failure.
         */
        void onQRCodeFinished(final FragmentQRCodeReader sender, final SecureByteArray qrCodeData, final String error);
    }

    private ZXingScannerView mScannerView = null;
    private QRCodeReaderDelegate mDelegate = null;
    private int mCustomTag = 0;

    //endregion

    //region Properties

    public int getCustomTag() {
        return mCustomTag;
    }

    //endregion

    //region Life Cycle

    @Nullable
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater,
                             @Nullable final ViewGroup container,
                             final Bundle savedInstanceState) {
        mScannerView = new ZXingScannerView(getActivity());

        Main.checkPermissions(getActivity(), true, Manifest.permission.CAMERA);

        return mScannerView;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (Main.checkPermissions(getActivity(), true, Manifest.permission.CAMERA)) {
            mScannerView.setResultHandler(this); // Register ourselves as a handler for scan results.
            mScannerView.startCamera(); // Start camera on resume
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mScannerView.stopCamera(); // Stop camera on pause
    }

    //endregion

    //region QRCodeReaderDelegate

    public void init(final QRCodeReaderDelegate delegate, final int customTag) {
        mDelegate = delegate;
        mCustomTag = customTag;
    }

    //endregion

    //region ResultHandler

    @Override
    public void handleResult(final Result result) {

        // Hide it self. This will also stop tracking after hide.
        // It's important to call before handler, because there might be some error popup etc.
        final MainActivity mainActivity = (MainActivity) getActivity();
        if (mainActivity != null) {
            mainActivity.hideLastStackFragment();
        }

        try {
            // Encrypt data and wipe source.
            final SecureByteArray qrCode = Main.sharedInstance().secureByteArrayFromBytes(new BigInteger(result.getText(), 16).toByteArray(), true);

            // Notify listener
            if (mDelegate != null) {
                mDelegate.onQRCodeFinished(this, qrCode, null);
            }

            // Wipe encrypted data.
            qrCode.wipe();
        } catch (NumberFormatException e) {
            // Notify listener
            if (mDelegate != null) {
                mDelegate.onQRCodeFinished(this, null, e.getLocalizedMessage());
            }
        }
    }

    //endregion

}
