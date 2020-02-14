package com.gemalto.idp.mobile.authentication.mode.face.ui.internal;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragmentStateless;

import com.gemalto.idp.mobile.authentication.mode.face.ui.R;

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
		return new AlertDialog.Builder(getActivity(), R.style.AppTheme_Dialog)
			.setMessage(message)
			.setTitle(title)
			.setPositiveButton(R.string.OK, (dialog, whichButton) -> {
				dialog.cancel();
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