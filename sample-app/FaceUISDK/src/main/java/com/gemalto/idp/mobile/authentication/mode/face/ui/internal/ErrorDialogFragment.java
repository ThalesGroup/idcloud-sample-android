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

package com.gemalto.idp.mobile.authentication.mode.face.ui.internal;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragmentStateless;

import com.gemalto.idp.mobile.authentication.mode.face.ui.R;

// IMPORTANT: This source code is intended to serve training information purposes only. Please make sure to review our IdCloud documentation, including security guidelines.

/**
 * Error Dialog used to display errors with a dialog Fragment in state loss mode
 */
public class ErrorDialogFragment extends DialogFragmentStateless {

	// ===========================================================
	// Private static fields
	// ===========================================================

	private static final String EXTRA_MESSAGE = "message";
	private static final String EXTRA_TITLE = "title";

	/**
	 * An ErrorDialog interface to implement in your activity in order to have a listener on Ok / Cancel buttons
	 */
	public interface ErrorDialogFragmentListener {
		/**
		 * Method called when user click the Ok button
		 */
		void onDialogOk();
	}

	private ErrorDialogFragmentListener mListener;
	
	// ===========================================================
	// Public static methods
	// ===========================================================

	/**
	 * Create a new ErrorDialogFragment instance without title
	 * @param message The message to be displayed
	 */
	public static ErrorDialogFragment newInstance(ErrorDialogFragmentListener listener,
												  String message) {
		return newInstance(listener, null, message);
	}

	/**+
	 * Create a new ErrorDialogFragment instance without title
	 * @param title The title of the Error dialog
	 * @param message The message to be displayed
	 */
	public static ErrorDialogFragment newInstance(ErrorDialogFragmentListener listener,
												  String title,
												  String message) {
		ErrorDialogFragment frag = new ErrorDialogFragment();
		frag.setErrorDialogFragmentListener(listener);
		Bundle args = new Bundle();
		args.putString(EXTRA_MESSAGE, message);
		args.putString(EXTRA_TITLE, title);
		frag.setArguments(args);
		return frag;
	}

	// ===========================================================
	// Private constructor
	// ===========================================================

	public ErrorDialogFragment() {
	}

	public void setErrorDialogFragmentListener(ErrorDialogFragmentListener listener){
		this.mListener = listener;
	}

	// ===========================================================
	// Public methods
	// ===========================================================

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		String message = getArguments().getString(EXTRA_MESSAGE);
		String title = getArguments().getString(EXTRA_TITLE);
		return new AlertDialog.Builder(getActivity())
			.setMessage(message)
			.setTitle(title)
			.setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
                    dialog.cancel();
				}
		}).create();
	}
	
	@Override
	public void onAttach(Activity activity) {
	    super.onAttach(activity);
	}
	
	@Override
	public void onDetach () {
        if(mListener != null) {
            mListener.onDialogOk();
            this.mListener = null;
        }
		super.onDetach();
	}

}