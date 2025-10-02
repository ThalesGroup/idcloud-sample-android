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
package com.gemalto.eziomobilesampleapp.helpers

import android.os.Handler
import android.os.Looper
import com.gemalto.idp.mobile.authentication.AuthInput
import com.gemalto.idp.mobile.authentication.mode.pin.PinAuthInput
import com.gemalto.idp.mobile.core.util.SecureString

/**
 * All app protocols, anonymous functions etc.
 */
class Protocols {
    /**
     * Protocol for unification of shared preferences and secure storage.
     */
    interface StorageProtocol {
        /**
         * Write string to storage. It will override existing key.
         *
         * @param value Value we want to store.
         * @param key   Key of value to be stored.
         * @return true if storing was successful.
         */
        fun writeString(value: String?, key: String?): Boolean

        /**
         * Write int to storage. It will override existing key.
         *
         * @param value Value we want to store.
         * @param key   Key of value to be stored.
         * @return true if storing was successful.
         */
        fun writeInteger(value: Int, key: String?): Boolean

        /**
         * Get stored string value. Return null if given key does not exists.
         *
         * @param key Key of stored value.
         * @return Stored value.
         */
        fun readString(key: String?): String?

        /**
         * Get stored int value. Return null if given key does not exists.
         *
         * @param key Key of stored value.
         * @return Stored value.
         */
        fun readInteger(key: String?): Int

        /**
         * Remove existing value from storage.
         *
         * @param key Key of stored value.
         * @return true if removing was successful.
         */
        fun removeValue(key: String?): Boolean
    }

    /**
     * OTP Calculation method. Used from displaying result to sending response on server.
     */
    interface OTPDelegate {
        /**
         * Triggered when OTP calculation is finished.
         *
         * @param otp             Calculated OTP. Null in case of error.
         * @param error           Description of possible error. Return null in case of success.
         * @param authInput       Original auth input used for calculation.
         * This is handy to automatic re-calc of OTP once it's not valid anymore.
         * @param serverChallenge Original server challenge. Same as authInput, but used only in OCRA.
         */
        fun onOTPDelegateFinished(
            otp: SecureString?,
            error: String?,
            authInput: AuthInput?,
            serverChallenge: SecureString?
        )
    }

    /**
     * Used as any anonymous operation when we need just result and possible error description.
     */
    interface GenericHandler {
        /**
         * Triggered when operation is finished.
         *
         * @param success Whenever was operation successful.
         * @param error   Description of possible error. Return null in case of success.
         */
        fun onFinished(success: Boolean, error: String?)

        class Sync(private val mCallback: GenericHandler) : GenericHandler {
            override fun onFinished(success: Boolean, error: String?) {
                Handler(Looper.getMainLooper()).post(Runnable {
                    mCallback.onFinished(
                        success,
                        error
                    )
                })
            }
        }
    }

    /**
     * Enrolling QR Code parser response.
     */
    interface QRCodeManagerHandler {
        /**
         * Triggered when parsing is done.
         *
         * @param success Whenever was parsing operation successful.
         * @param userId  User id for enrollment.
         * @param regCode Registration code for enrollment.
         * @param error   Description of possible error. Return null in case of success.
         */
        fun onParseFinished(
            success: Boolean,
            userId: String?,
            regCode: SecureString?,
            error: String?
        )
    }

    /**
     * Return method from secure keypad used for entering or changing pin.
     */
    interface SecureInputHandler {
        /**
         * Triggered only when user press ok button with entered pin.
         *
         * @param firstPin  Entered pin to generate OTP, or old pin in case of change pin action.
         * @param secondPin New pin value. Used only on change pin. Otherwise null.
         */
        fun onSecureInputFinished(
            firstPin: PinAuthInput,
            secondPin: PinAuthInput?
        )
    }

    /**
     * Used by all supported auth input options. Pin, touch id, face id etc...
     */
    interface AuthInputHandler {
        /**
         * Method triggered when auth action is finished. (Back button will not trigger it).
         *
         * @param authInput Desired auth input in case of success. Otherwise it's null.
         * @param error     Description of possible error. Return null in case of success.
         */
        fun onFinished(
            authInput: AuthInput?,
            error: String?
        )
    }

    interface PostMessageInterface {
        fun onPostFinished(success: Boolean, message: String?)
    }
}
