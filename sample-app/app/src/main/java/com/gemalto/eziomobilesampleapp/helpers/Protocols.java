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

package com.gemalto.eziomobilesampleapp.helpers;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.gemalto.idp.mobile.authentication.AuthInput;
import com.gemalto.idp.mobile.authentication.mode.pin.PinAuthInput;
import com.gemalto.idp.mobile.core.util.SecureString;

/**
 * All app protocols, anonymous functions etc.
 */
public class Protocols {

    /**
     * Protocol for unification of shared preferences and secure storage.
     */
    public interface StorageProtocol {

        /**
         * Write string to storage. It will override existing key.
         * @param value Value we want to store.
         * @param key Key of value to be stored.
         * @return true if storing was successful.
         */
        boolean writeString(final String value, final String key);

        /**
         * Write int to storage. It will override existing key.
         * @param value Value we want to store.
         * @param key Key of value to be stored.
         * @return true if storing was successful.
         */
        boolean writeInteger(final int value, final String key);

        /**
         * Get stored string value. Return null if given key does not exists.
         * @param key Key of stored value.
         * @return Stored value.
         */
        String readString(final String key);

        /**
         * Get stored int value. Return null if given key does not exists.
         * @param key Key of stored value.
         * @return Stored value.
         */
        int readInteger(final String key);

        /**
         * Remove existing value from storage.
         * @param key Key of stored value.
         * @return true if removing was successful.
         */
        boolean removeValue(final String key);
    }

    /**
     * OTP Calculation method. Used from displaying result to sending response on server.
     */
    public interface OTPDelegate {
        /**
         * Triggered when OTP calculation is finished.
         * @param otp Calculated OTP. Null in case of error.
         * @param error Description of possible error. Return null in case of success.
         * @param authInput Original auth input used for calculation.
         *                  This is handy to automatic re-calc of OTP once it's not valid anymore.
         * @param serverChallenge Original server challenge. Same as authInput, but used only in OCRA.
         */
        void onOTPDelegateFinished(@Nullable final SecureString otp,
                                   @Nullable final String error,
                                   @Nullable final AuthInput authInput,
                                   final SecureString serverChallenge);
    }

    /**
     * Used as any anonymous operation when we need just result and possible error description.
     */
    public interface GenericHandler {
        /**
         * Triggered when operation is finished.
         * @param success Whenever was operation successful.
         * @param error Description of possible error. Return null in case of success.
         */
        void onFinished(final boolean success, @Nullable final String error);

        class Sync implements GenericHandler {
            private final GenericHandler mCallback;

            public Sync(final GenericHandler callback) {
                mCallback = callback;
            }

            @Override
            public void onFinished(final boolean success, final String error) {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        mCallback.onFinished(success, error);
                    }
                });
            }
        }
    }

    /**
     * Enrolling QR Code parser response.
     */
    public interface QRCodeManagerHandler {
        /**
         * Triggered when parsing is done.
         * @param success Whenever was parsing operation successful.
         * @param userId User id for enrollment.
         * @param regCode Registration code for enrollment.
         * @param error Description of possible error. Return null in case of success.
         */
        void onParseFinished(final boolean success,
                             @Nullable final String userId,
                             @Nullable final SecureString regCode,
                             @Nullable final String error);
    }

    /**
     * Return method from secure keypad used for entering or changing pin.
     */
    public interface SecureInputHandler {
        /**
         * Triggered only when user press ok button with entered pin.
         * @param firstPin Entered pin to generate OTP, or old pin in case of change pin action.
         * @param secondPin New pin value. Used only on change pin. Otherwise null.
         */
        void onSecureInputFinished(@NonNull final PinAuthInput firstPin,
                                   @Nullable final PinAuthInput secondPin);
    }

    /**
     * Used by all supported auth input options. Pin, touch id, face id etc...
     */
    public interface AuthInputHandler {
        /**
         * Method triggered when auth action is finished. (Back button will not trigger it).
         * @param authInput Desired auth input in case of success. Otherwise it's null.
         * @param error Description of possible error. Return null in case of success.
         */
        void onFinished(@Nullable final AuthInput authInput,
                        @Nullable final String error);
    }

    public interface PostMessageInterface {
        void onPostFinished(final boolean success, final String message);
    }

}
