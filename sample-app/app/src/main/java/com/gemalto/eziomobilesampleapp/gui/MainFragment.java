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

import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.gemalto.eziomobilesampleapp.MainActivity;
import com.gemalto.eziomobilesampleapp.R;
import com.gemalto.eziomobilesampleapp.helpers.CMain;
import com.gemalto.eziomobilesampleapp.helpers.Protocols;
import com.gemalto.eziomobilesampleapp.helpers.ezio.TokenDevice;
import com.gemalto.idp.mobile.authentication.AuthInput;
import com.gemalto.idp.mobile.core.util.SecureString;

import java.util.Timer;
import java.util.TimerTask;

// IMPORTANT: This source code is intended to serve training information purposes only. Please make sure to review our IdCloud documentation, including security guidelines.

/**
 * Helper class gathering all common methods and elements of all fragments.
 */
public class MainFragment extends Fragment implements Protocols.OTPDelegate {

    //region Defines

    protected enum OTPHandlerType {
        Offline(0),
        InBand(1);

        OTPHandlerType(final int value) {
            mKey = value;
        }

        int getKey() {
            return mKey;
        }

        final private int mKey;
    }

    protected View mView = null;

    protected Button mButtonOTPFaceIdOffline = null;
    protected Button mButtonOTPFaceIdInBand = null;
    protected Button mButtonOTPTouchIdOffline = null;
    protected Button mButtonOTPTouchIdInBand = null;
    protected Button mButtonOTPPinOffline = null;
    protected Button mButtonOTPPinInBand = null;

    protected TextView mLabelOOBStatus = null;
    protected ImageView mLabelOOBStatusValue = null;

    protected ProgressBar mActivityOOBStatus = null;

    private Timer mTimer = null;
    private AlertDialog mOtpDialog = null;
    private SecureString mLastOTP = null;


    //endregion

    //region Life Cycle

    @Override public void onPause() {
        super.onPause();

        if(mOtpDialog != null) {
            mOtpDialog.dismiss();
            mOtpDialog = null;
        }
        invalidateOTPTimer();
    }

    @Override
    public void onResume() {
        super.onResume();

        reloadGUI();
    }

    @Override
    public void onStop() {
        super.onStop();

        invalidateOTPTimer();
    }

    //endregion


    //region MainFragment

    /**
     * Initializes the GUI.
     * @param inflater
     * @param fragmentId
     * @return
     */
    protected View initGui(final LayoutInflater inflater, final int fragmentId) {

        mView = inflater.inflate(fragmentId, null);

        // Common for Authentication and Transaction confirmation.
        mButtonOTPFaceIdOffline = getButonWithListener(R.id.button_otp_face_id_offline, new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                onButtonPressedOTPFaceId(OTPHandlerType.Offline);
            }
        });

        mButtonOTPFaceIdInBand = getButonWithListener(R.id.button_otp_face_id_in_band, new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                onButtonPressedOTPFaceId(OTPHandlerType.InBand);
            }
        });

        mButtonOTPTouchIdOffline = getButonWithListener(R.id.button_otp_touch_id_offline, new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                onButtonPressedOTPTouchId(OTPHandlerType.Offline);
            }
        });

        mButtonOTPTouchIdInBand = getButonWithListener(R.id.button_otp_touch_id_in_band, new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                onButtonPressedOTPTouchId(OTPHandlerType.InBand);
            }
        });

        mButtonOTPPinOffline = getButonWithListener(R.id.button_otp_pin_offline, new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                onButtonPressedOTPPin(OTPHandlerType.Offline);
            }
        });

        mButtonOTPPinInBand = getButonWithListener(R.id.button_otp_pin_in_band, new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                onButtonPressedOTPPin(OTPHandlerType.InBand);
            }
        });

        mLabelOOBStatus = mView.findViewById(R.id.main_fragment_label_oob_status);
        mLabelOOBStatusValue = mView.findViewById(R.id.main_fragment_label_status_value);
        mActivityOOBStatus = mView.findViewById(R.id.main_fragment_activity_oob_status);

        showToolbar();

        return mView;
    }

    /**
     * Disables GUI.
     */
    protected void disableGUI() {
        setButtonOTPPinEnabled(false);
        setButtonOTPTouchIdEnabled(false);
        setButtonOTPFaceIdEnabled(false);

        // Disable all tab bar items. Disabling just user interaction does not change color and it's not working in transition.
        getMainActivity().tabBarDisable();
    }

    /**
     * Retrieves the {@code MainActivity}.
     * @return {@code MainActivity}.
     */
    protected MainActivity getMainActivity() {
        return ((MainActivity) getActivity());
    }

    /**
     * Retrieves a {@code Button} with {@code OnClickListener} assigned to it.
     * @param buttonId Button id.
     * @param listener Listener.
     * @return {@code Button} with associated listener.
     */
    protected Button getButonWithListener(final int buttonId, final View.OnClickListener listener) {
        final Button retValue = mView.findViewById(buttonId);
        if (retValue != null) {
            retValue.setOnClickListener(listener);
        }
        return retValue;
    }

    /**
     * Hides the toolbar.
     */
    protected void hideToolbar() {
        getMainActivity().hideToolbar();
    }

    /**
     * Shows the toolbar.
     */
    protected void showToolbar() {
        getMainActivity().showToolbar();
    }

    /**
     * Reloads the GUI.
     */
    public void reloadGUI() {
        // Display push token registration status.
        updatePushRegistrationStatus();

        // No matter what was set. If there is a loading indicator, we should disable everything.
        if (getMainActivity().loadingIndicatorIsPresent()) {
            disableGUI();
        } else {
            // Enable or disable tab options based on SDK state.
            getMainActivity().tabBarUpdate();

            final TokenDevice device = CMain.sharedInstance().getManagerToken().getTokenDevice();

            // Those values are only for token based views.
            if (device != null && (mButtonOTPPinOffline != null || mButtonOTPTouchIdOffline != null || mButtonOTPFaceIdOffline != null)) {
                final TokenDevice.TokenStatus status = device.getTokenStatus();

                setButtonOTPPinEnabled(true);
                setButtonOTPTouchIdEnabled(status.isTouchEnabled);
                setButtonOTPFaceIdEnabled(status.isFaceEnabled);
            }
        }
    }

    /**
     * Updates the push registration status.
     */
    public void updatePushRegistrationStatus() {
        // This view is common even for settings which does not have status information.
        if (mLabelOOBStatus == null) {
            return;
        }

        // Push token was already registered
        if (CMain.sharedInstance().getManagerPush().isPushTokenRegistered()) {
            mLabelOOBStatus.setText(R.string.PUSH_STATUS_REGISTERED);
            mLabelOOBStatusValue.setVisibility(View.VISIBLE);
            mActivityOOBStatus.setVisibility(View.INVISIBLE);
        } else {
            mLabelOOBStatus.setText(R.string.PUSH_STATUS_PENDING);
            mLabelOOBStatusValue.setVisibility(View.INVISIBLE);
            mActivityOOBStatus.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Approves the OTP.
     * @param message Message.
     * @param serverChallenge Server challenge.
     * @param handler Callback to return the OTP.
     */
    public void approveOTP(@NonNull final String message,
                           @Nullable final SecureString serverChallenge,
                           @NonNull final Protocols.OTPDelegate handler) {
        // Override. Implement only in fragments with auth support.
    }

    //endregion

    //region Protocols.OTPDelegate

    /**
     * {@inheritDoc}
     */
    @Override
    public void onOTPDelegateFinished(final SecureString otp,
                                      final String error,
                                      final AuthInput authInput,
                                      final SecureString serverChallenge) {
        if (otp != null && error == null) {
            setLastOTP(otp);

            // Display popup with OTP value.
            final Context ctx = getMainActivity();
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    mTimer = new Timer();
                    mTimer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            checkOTPLifespan(authInput, serverChallenge);
                        }
                    }, 0, 1000);


                    final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(ctx);
                    dialogBuilder.setTitle(getString(R.string.OTP_VALUE_CAPTION));
                    dialogBuilder.setMessage(getOTPDescription(authInput, serverChallenge));
                    dialogBuilder.setPositiveButton(CMain.getString(R.string.COMMON_MESSAGE_OK), null);
                    dialogBuilder.setCancelable(true);
                    mOtpDialog = dialogBuilder.create();

                    mOtpDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(final DialogInterface dialogInterface) {
                            invalidateOTPTimer();
                            authInput.wipe();
                        }
                    });

                    mOtpDialog.show();
                }
            });
        } else {
            getMainActivity().showErrorIfExists(error);
        }
    }

    //endregion

    //region User Interface

    /**
     * Generates OTP using PIN.
     * @param type Type.
     */
    protected void onButtonPressedOTPPin(final OTPHandlerType type) {
        // Override
    }

    /**
     * Generates OTP using FaceId.
     * @param type Type.
     */
    protected void onButtonPressedOTPFaceId(final OTPHandlerType type) {
        // Override
    }

    /**
     * Generates OTP using TouchId.
     * @param type Type.
     */
    protected void onButtonPressedOTPTouchId(final OTPHandlerType type) {
        // Override
    }

    //endregion

    //region Timer tick

    private void checkOTPLifespan(final AuthInput authInput,
                                  final SecureString serverChallenge) {

        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                final String caption = getOTPDescription(authInput, serverChallenge);

                if (mOtpDialog != null) {
                    mOtpDialog.setMessage(caption);
                }
            }
        });

    }

    //endregion

    //region Private Helpers

    /**
     * Enables the OTP generate with PIN buttons.
     * @param enabled {@code True} if enabled, else {@code false}.
     */
    private void setButtonOTPPinEnabled(final boolean enabled) {
        if (mButtonOTPPinOffline != null) {
            mButtonOTPPinOffline.setEnabled(enabled);
        }
        if (mButtonOTPPinInBand != null) {
            mButtonOTPPinInBand.setEnabled(enabled);
        }
    }

    /**
     * Enables the OTP generate with FaceId buttons.
     * @param enabled {@code True} if enabled, else {@code false}.
     */
    private void setButtonOTPFaceIdEnabled(final boolean enabled) {
        if (mButtonOTPFaceIdOffline != null) {
            mButtonOTPFaceIdOffline.setEnabled(enabled);
        }
        if (mButtonOTPFaceIdInBand != null) {
            mButtonOTPFaceIdInBand.setEnabled(enabled);
        }
    }

    /**
     * Enables the OTP generate with TouchId buttons.
     * @param enabled {@code True} if enabled, else {@code false}.
     */
    private void setButtonOTPTouchIdEnabled(final boolean enabled) {
        if (mButtonOTPTouchIdOffline != null) {
            mButtonOTPTouchIdOffline.setEnabled(enabled);
        }
        if (mButtonOTPTouchIdInBand != null) {
            mButtonOTPTouchIdInBand.setEnabled(enabled);
        }
    }

    /**
     * Retrieves the OTP description.
     * @param authInput Authentication.
     * @param serverChallenge Server challenge.
     * @return
     */
    String getOTPDescription(final AuthInput authInput, final SecureString serverChallenge) {

        // Read last otp lifespan from device.
        final int[] lifeSpan = {CMain.sharedInstance().getManagerToken().getTokenDevice().getDevice().getLastOtpLifespan()};

        // OTP is still valid.
        if (lifeSpan[0] <= 0) {
            CMain.sharedInstance().getManagerToken().getTokenDevice().totpWithAuthInput(authInput, serverChallenge, new Protocols.OTPDelegate() {
                @Override
                public void onOTPDelegateFinished(final SecureString otp,
                                                  final String error,
                                                  final AuthInput authInput,
                                                  final SecureString serverChallenge) {
                    setLastOTP(otp);
                    lifeSpan[0] = CMain.sharedInstance().getManagerToken().getTokenDevice().getDevice().getLastOtpLifespan();
                }
            });
        }

        return getString(R.string.OTP_VALUE_DESCRIPTION_VALID, mLastOTP.toString(), lifeSpan[0]);
    }

    /**
     * Invalidates the timer.
     */
    private void invalidateOTPTimer() {
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }

        setLastOTP(null);
    }

    /**
     * Sets the last OTP.
     * @param otp OTP.
     */
    private void setLastOTP(final SecureString otp) {
        if (mLastOTP != null) {
            mLastOTP.wipe();
        }

        mLastOTP = otp != null ? otp.clone() : null;
    }

    //endregion
}
