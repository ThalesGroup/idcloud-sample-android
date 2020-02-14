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

package com.gemalto.idp.mobile.authentication.mode.face.ui;

import com.gemalto.idp.mobile.authentication.mode.face.FaceAuthStatus;

/**
 * This class help to display generic Facial UI messages
 */
public class FaceMessagesHelper {
	/**
	 * This method convert a FaceStatus error to a string ressource Id. Use this method if youd don't wan't to implement your own messages.
	 * @param error The FaceStatus error returned by the activity
	 * @return Message resource Id
	 */
	public static int getErrorMessageForErrorCode(FaceAuthStatus error) {
		if(error != null) {
			if (error == FaceAuthStatus.ERROR || error == FaceAuthStatus.ALREADY_EXTRACTING) {
				return R.string.error_UNKNOWN;
			} else if (error == FaceAuthStatus.BAD_QUALITY) {
				return R.string.error_BAD_QUALITY;
			} else if (error == FaceAuthStatus.CAMERA_NOT_FOUND) {
				return R.string.error_CAMERA_NOT_FOUND;
			} else if (error == FaceAuthStatus.MATCH_NOT_FOUND) {
				return R.string.error_MATCH_NOT_FOUND;
			} else if (error == FaceAuthStatus.USER_NOT_FOUND) {
				return R.string.error_USER_NOT_FOUND;
			} else if (error == FaceAuthStatus.USER_REENROLL_NEEDED) {
				return R.string.error_USER_REENROLL_NEEDED;
			} else if (error == FaceAuthStatus.USER_REENROLL_NEEDED) {
				return R.string.error_USER_REENROLL_NEEDED;
			} else if (error == FaceAuthStatus.LIVENESS_CHECK_FAILED) {
				return R.string.error_LIVENESS_CHECK_FAILED;
			} else if (error == FaceAuthStatus.TIMEOUT) {
				return R.string.error_TIMEOUT;
			}
		}
		return R.string.error_UNKNOWN;
	}
}
