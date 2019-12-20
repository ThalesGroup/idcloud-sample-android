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

package com.gemalto.idp.mobile.authentication.mode.face.ui;

import com.gemalto.idp.mobile.authentication.AuthInput;
import com.gemalto.idp.mobile.authentication.mode.face.FaceAuthStatus;
import com.gemalto.idp.mobile.core.IdpException;

// IMPORTANT: This source code is intended to serve training information purposes only. Please make sure to review our IdCloud documentation, including security guidelines.

/**
 * Callback from the verification fragment
 */
public interface VerificationCallback {

    /**
     * On verification success. AuthInput object is returned.
     * @param authInput
     */
    void onVerificationSuccess(final AuthInput authInput);

    /**
     * On verification cancelled.
     */
    void onCancel();

    /**
     * On verification failed. Check status to see why.
     * @param status
     */
    void onVerificationFailed(FaceAuthStatus status);

    /**
     * On verification retry. (invoked on single failure)
     * @param status
     * @param remainingRetries
     */
    void onVerificationRetry(FaceAuthStatus status, int remainingRetries);

    /**
     * Error in internal SDK.
     * @param e
     */
    void onError(IdpException e);
}
