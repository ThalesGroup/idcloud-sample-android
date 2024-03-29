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

import android.os.Build;
import android.os.Bundle;

import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import com.gemalto.eziomobilesampleapp.R;

/**
 * Fragment used for bio fingerprint input. It allow user to
 * use pin authentication as fallback solution.
 */
public final class FragmentBioFingerprint extends DialogFragment {

    //region Defines

    private BioFpFragmentCallback mDelegate = null;
    private LinearLayout mLayoutAttempts = null;
    private TextView mLabelFailedAttemptsValue = null;
    private int mFailures = 0;

    /**
     * Notify handler about results.
     */
    public interface BioFpFragmentCallback {
        /**
         * User selected to use pin instead.
         */
        void onPinFallback();

        /**
         * User canceled OTP generation.
         */
        void onCancel();
    }

    //endregion

    //region Life Cycle

    public static FragmentBioFingerprint create(final BioFpFragmentCallback delegate) {
        final FragmentBioFingerprint retValue = new FragmentBioFingerprint();
        retValue.mDelegate = delegate;
        return retValue;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Material_Light_Dialog);
        }
    }

    @Override
    public View onCreateView(
            @NonNull final LayoutInflater inflater,
            final ViewGroup container,
            final Bundle savedInstanceState
    ) {
        getDialog().requestWindowFeature(STYLE_NO_TITLE);
        setCancelable(false);

        final View retValue = inflater.inflate(R.layout.fragment_fingerprint, container, false);

        retValue.findViewById(R.id.button_use_pin).setOnClickListener(this::onButtonPressedUsePin);
        retValue.findViewById(R.id.button_cancel).setOnClickListener(this::onButtonPressedCancel);

        retValue.setFocusableInTouchMode(true);
        retValue.requestFocus();
        retValue.setOnKeyListener((view, keyCode, keyEvent) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK && keyEvent.getAction() == KeyEvent.ACTION_UP) {
                onButtonPressedBack();
            }
            return false;
        });

        mLayoutAttempts = retValue.findViewById(R.id.layout_attempts);
        mLabelFailedAttemptsValue = retValue.findViewById(R.id.label_failed_attempts_value);

        setFailures(0);

        return retValue;
    }

    //endregion

    //region User Interface

    private void onButtonPressedUsePin(final View sender) {
        dismiss();

        if (mDelegate != null) {
            mDelegate.onPinFallback();
        }
    }

    private void onButtonPressedCancel(final View sender) {
        dismiss();

        if (mDelegate != null) {
            mDelegate.onCancel();
        }
    }

    private void onButtonPressedBack() {
        dismiss();

        if (mDelegate != null) {
            mDelegate.onCancel();
        }
    }

    //endregion

    //region Private Helpers

    private void setFailures(final int failures) {
        mFailures = failures;

        if (mFailures > 0) {
            mLayoutAttempts.setVisibility(View.VISIBLE);
            mLabelFailedAttemptsValue.setText(String.valueOf(failures));
        } else {
            mLayoutAttempts.setVisibility(View.GONE);
        }
    }

    //endregion

    //region Public API

    public void onFailure() {
        setFailures(mFailures + 1);
    }

    public void onSuccess() {
        setFailures(0);
    }

    //endregion

}
