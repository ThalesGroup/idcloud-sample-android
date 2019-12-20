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

package com.gemalto.eziomobilesampleapp.gui.overlays;

import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.gemalto.eziomobilesampleapp.R;

// IMPORTANT: This source code is intended to serve training information purposes only. Please make sure to review our IdCloud documentation, including security guidelines.

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

    /**
     * Creates a new {@code FragmentBioFingerprint}.
     * @param delegate Delegate.
     * @return {@code FragmentBioFingerprint}.
     */
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
    public View onCreateView(@NonNull final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {
        getDialog().requestWindowFeature(STYLE_NO_TITLE);
        setCancelable(false);

        final View retValue = inflater.inflate(R.layout.fragment_fingerprint, container, false);

        retValue.findViewById(R.id.button_use_pin).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                onButtonPressedUsePin();
            }
        });

        retValue.findViewById(R.id.button_cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                onButtonPressedCancel();
            }
        });

        retValue.setFocusableInTouchMode(true);
        retValue.requestFocus();
        retValue.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(final View view, final int keyCode, final KeyEvent keyEvent) {
                if (keyCode == KeyEvent.KEYCODE_BACK && keyEvent.getAction() == KeyEvent.ACTION_UP ) {
                    onButtonPressedBack();
                }
                return false;
            }
        });

        mLayoutAttempts = retValue.findViewById(R.id.layout_attempts);
        mLabelFailedAttemptsValue = retValue.findViewById(R.id.label_failed_attempts_value);

        setFailures(0);

        return retValue;
    }

    //endregion

    //region User Interface

    /**
     * On button pressed listener for user PIN.
     */
    private void onButtonPressedUsePin() {
        dismiss();

        if (mDelegate != null) {
            mDelegate.onPinFallback();
        }
    }

    /**
     * On button pressed listener for cancel.
     */
    private void onButtonPressedCancel() {
        dismiss();

        if (mDelegate != null) {
            mDelegate.onCancel();
        }
    }

    /**
     * On button pressed listener for back pressed.
     */
    private void onButtonPressedBack() {
        dismiss();

        if (mDelegate != null) {
            mDelegate.onCancel();
        }
    }

    //endregion

    /**
     * Sets the failure.
     * @param failures
     */
    private void setFailures(final int failures) {
        mFailures = failures;

        if (mFailures > 0) {
            mLayoutAttempts.setVisibility(View.VISIBLE);
            mLabelFailedAttemptsValue.setText(String.valueOf(failures));
        } else {
            mLayoutAttempts.setVisibility(View.GONE);
        }
    }

    /**
     * Increments the failures.
     */
    public void onFailure() {
        setFailures(mFailures + 1);
    }

    /**
     * Resets the failures.
     */
    public void onSuccess() {
        setFailures(0);
    }
}
