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
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.TextView;

import com.gemalto.eziomobilesampleapp.Configuration;
import com.gemalto.eziomobilesampleapp.R;
import com.gemalto.eziomobilesampleapp.gui.overlays.CircleTimer;
import com.gemalto.eziomobilesampleapp.gui.overlays.CircleTimerAnimation;
import com.gemalto.eziomobilesampleapp.helpers.Main;
import com.gemalto.eziomobilesampleapp.helpers.Protocols;
import com.gemalto.idp.mobile.authentication.AuthInput;
import com.gemalto.idp.mobile.core.util.SecureString;

import java.util.Timer;
import java.util.TimerTask;

/**
 * OTP generation Fragment.
 */
public class FragmentOtp extends AbstractMainFragmentWithAuthSolver implements Protocols.OTPDelegate {

    //region Defines

    private SecureString mLastOTP;
    private Timer mTimer;
    private TextView mOtp;
    private TextView mLifespan;
    private CircleTimer mCircleTimer;
    private CircleTimerAnimation mCircleTimerAnimation;

    private String mAmount = null;
    private String mBeneficiary = null;

    private final AlphaAnimation mTextChangeAnim = new AlphaAnimation(1.f, .0f);

    //endregion

    //region Life Cycle

    public static FragmentOtp authentication(final AuthInput authInput) {
        final FragmentOtp retValue = new FragmentOtp();

        // Calculate OTP.
        Main.sharedInstance().getManagerToken().getTokenDevice()
                .totpWithAuthInput(authInput, null, retValue);

        return retValue;
    }

    public static FragmentOtp transactionSign(
            final AuthInput authInput,
            final SecureString serverChallenge,
            final String amount,
            final String beneficiary
    ) {
        final FragmentOtp retValue = new FragmentOtp();
        retValue.mAmount = amount;
        retValue.mBeneficiary = beneficiary;

        // Calculate OTP.
        Main.sharedInstance().getManagerToken().getTokenDevice()
                .totpWithAuthInput(authInput, serverChallenge, retValue);

        return retValue;
    }

    @Nullable
    @Override
    @SuppressLint("InflateParams")
    public View onCreateView(
            @NonNull final LayoutInflater inflater,
            @Nullable final ViewGroup container,
            @Nullable final Bundle savedInstanceState
    ) {
        final View retValue = inflater.inflate(R.layout.fragment_otp, null);

        final TextView caption = retValue.findViewById(R.id.tv_fragment_caption);
        caption.setText(mAmount != null && mBeneficiary != null ?
                R.string.ui_button_otp_sign : R.string.ui_button_otp_auth);

        final TextView domainTextView = retValue.findViewById(R.id.tv_fragment_description);
        if (domainTextView != null) {
            domainTextView.setText(String.format("Domain: %s", Configuration.CFG_OOB_DOMAIN));
        }

        mOtp = retValue.findViewById(R.id.text_view_otp);
        mLifespan = retValue.findViewById(R.id.text_view_countdown);
        mCircleTimer = retValue.findViewById(R.id.circleTimer);

        // Animation for OTP text change
        mTextChangeAnim.setDuration(200);
        mTextChangeAnim.setRepeatCount(1);
        mTextChangeAnim.setRepeatMode(Animation.REVERSE);
        mTextChangeAnim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(final Animation animation) {
                // nothing to do
            }

            @Override
            public void onAnimationEnd(final Animation animation) {
                // nothing to do
            }

            @Override
            public void onAnimationRepeat(final Animation animation) {
                // Format longer OTP's
                final StringBuilder otpValue = new StringBuilder(mLastOTP.toString());
                if (otpValue.length() > 4) {
                    otpValue.insert(otpValue.length() / 2, " ");
                }
                mOtp.setText(otpValue.toString());
            }
        });

        return retValue;
    }

    @Override
    public void onStop() {
        super.onStop();

        if (mTimer != null) {
            mTimer.cancel();
        }
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
        }

        getMainActivity().enableDrawer(false);
        getMainActivity().reloadGui();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void disableGUI() {
    }

    //endregion

    //region OTPDelegate

    public void onOTPDelegateFinished(
            final SecureString otp,
            final String error,
            final AuthInput authInput,
            final SecureString serverChallenge
    ) {
        if (otp != null && error == null) {
            setLastOTP(otp);
            final int lifespan = Main.sharedInstance().getManagerToken().getTokenDevice().getDevice()
                    .getLastOtpLifespan();
            if (lifespan > 0) {
                scheduleAnimation(lifespan);
            }

            new Handler(Looper.getMainLooper()).post(() -> {
                // Animate OTP text change
                mOtp.startAnimation(mTextChangeAnim);

                // Schedule timer to check lifetime and recalculate OTP.
                if (mTimer == null) {
                    mTimer = new Timer();
                    mTimer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            checkOTPLifespan(authInput, serverChallenge);
                        }
                    }, 0, 500);
                }
            });
        } else {
            getMainActivity().showErrorIfExists(error);
        }
    }

    //endregion

    //region Private Helpers

    protected void checkOTPLifespan(
            final AuthInput authInput,
            final SecureString serverChallenge
    ) {
        // Read last otp lifespan from device.
        final int lifeSpan = Main.sharedInstance().getManagerToken().getTokenDevice().getDevice().getLastOtpLifespan();

        // OTP is still valid.
        if (lifeSpan <= 0) {
            Main.sharedInstance().getManagerToken().getTokenDevice()
                    .totpWithAuthInput(authInput, serverChallenge, this);

        }
    }

    private void scheduleAnimation(final int lifeSpan) {
        new Handler(Looper.getMainLooper()).post(() -> {
            if (mCircleTimerAnimation != null) {
                mCircleTimerAnimation.cancel();
            }

            mCircleTimerAnimation = new CircleTimerAnimation(mCircleTimer, mLifespan, Configuration.CFG_OTP_LIFESPAN, lifeSpan);
            mCircleTimer.startAnimation(mCircleTimerAnimation);
        });
    }

    protected void setLastOTP(final SecureString otp) {
        if (mLastOTP != null) {
            mLastOTP.wipe();
        }

        mLastOTP = otp != null ? otp.clone() : null;
    }

    //endregion

}
