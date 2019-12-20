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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.gemalto.eziomobilesampleapp.R;
import com.gemalto.eziomobilesampleapp.helpers.CMain;
import com.gemalto.eziomobilesampleapp.helpers.Protocols;
import com.gemalto.idp.mobile.authentication.AuthInput;
import com.gemalto.idp.mobile.core.util.SecureString;

// IMPORTANT: This source code is intended to serve training information purposes only. Please make sure to review our IdCloud documentation, including security guidelines.

/**
 * Fragment is used for in-band authentication. It will generate TOTP with
 * selected auth input.
 */
public class FragmentTabAuthentication extends MainFragmentWithAuthSolver {


    //region Defines

    final Protocols.OTPDelegate mSendAuthRequest;

    //endregion

    //region Life Cycle

    public FragmentTabAuthentication() {
        super();

        mSendAuthRequest = new Protocols.OTPDelegate() {
            @Override
            public void onOTPDelegateFinished(@Nullable final SecureString otp, @Nullable final String error,
                                              @Nullable final AuthInput authInput, final SecureString serverChallenge) {
                CMain.sharedInstance().getManagerHttp().sendAuthRequest(otp, error, authInput, serverChallenge);
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
        return initGui(inflater, R.layout.fragment_authentication);
    }

    //endregion

    //region User Interface

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onButtonPressedOTPPin(final OTPHandlerType type) {
        totpWithPin(null, type == OTPHandlerType.Offline ? null : mSendAuthRequest);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onButtonPressedOTPFaceId(final OTPHandlerType type) {
        totpWithFaceId(null, type == OTPHandlerType.Offline ? null : mSendAuthRequest);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onButtonPressedOTPTouchId(final OTPHandlerType type) {
        totpWithTouchId(null, type == OTPHandlerType.Offline ? null : mSendAuthRequest);
    }

    //endregion

}
