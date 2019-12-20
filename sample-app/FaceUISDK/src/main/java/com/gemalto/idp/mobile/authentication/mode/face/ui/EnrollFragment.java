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

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.gemalto.idp.mobile.authentication.mode.face.FaceAuthEnrollerCallback;
import com.gemalto.idp.mobile.authentication.mode.face.FaceAuthFrameEvent;
import com.gemalto.idp.mobile.authentication.mode.face.FaceAuthStatus;
import com.gemalto.idp.mobile.authentication.mode.face.ui.internal.ErrorDialogFragment;
import com.gemalto.idp.mobile.authentication.mode.face.ui.internal.gui.ErrorMode;
import com.gemalto.idp.mobile.authentication.mode.face.ui.internal.gui.view.CircularProgressView;
import com.gemalto.idp.mobile.authentication.mode.face.ui.internal.gui.view.FaceMaskView.MaskMode;
import com.gemalto.idp.mobile.authentication.mode.face.ui.internal.manager.FaceEnrollNoStepsManager;
import com.gemalto.idp.mobile.authentication.mode.face.ui.internal.manager.FaceEnrollerUIListener;
import com.gemalto.idp.mobile.authentication.mode.face.ui.internal.utils.logs.MyLog;
import com.gemalto.idp.mobile.authentication.mode.face.view.FaceView;
import com.gemalto.idp.mobile.core.IdpException;

// IMPORTANT: This source code is intended to serve training information purposes only. Please make sure to review our IdCloud documentation, including security guidelines.

/**
 * The new Simple mode Enroll Activity
 */
public class EnrollFragment extends Fragment implements ErrorDialogFragment.ErrorDialogFragmentListener {
    private static final String TAG = EnrollFragment.class.getSimpleName();

    public static final String EXTRA_TIMEOUT = "EXTRA_TIMEOUT";
    public static final String RETRIES = "RETRIES";

    private static final int TIMEOUT = 60000;
    private static boolean displayErrorDialog = false;

    private static final int TIME_MIN_PROCESSING = 2000;
    private int m_resultModeTempo;

    private EnrollmentCallback m_callback;

    private boolean m_bCanceled = false;

    private FaceView mFaceView;
    private CircularProgressView mProgressStepView;
    private ImageView mIvRegisteredUser;
    private RelativeLayout mLayoutRegisteredUser;
    private TextView mTvInstructions;

    private Button mBtnCancel, mBtnStart, mBtnSuccess;

    private static int m_nbRetries = 0;
    private static int MAX_RETRY = 5;
    private boolean shouldStop = false;

    protected static EnrollFragment newInstance(int timeout, int retries){
        EnrollFragment fragment = new EnrollFragment();
        Bundle args = new Bundle();
        args.putInt(EXTRA_TIMEOUT, timeout);
        args.putInt(RETRIES, retries);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        m_resultModeTempo = 2000;
        m_resultModeTempo = getArguments().getInt(EXTRA_TIMEOUT, 2000);
        MAX_RETRY = getArguments().getInt(RETRIES, 5);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle saveInstanceState){
        View rootView = inflater.inflate(R.layout.activity_enroll, container, false);

        // FaceView is provided by the Ezio SDK
        mFaceView = (FaceView) rootView.findViewById(R.id.nFaceView);

        // Animated circular progress view to show the progress, provided by UI SDK
        mProgressStepView = (CircularProgressView) rootView.findViewById(R.id.stepProgressView);

        mIvRegisteredUser = (ImageView) rootView.findViewById(R.id.imageViewRegisteredUser);
        mLayoutRegisteredUser = (RelativeLayout) rootView.findViewById(R.id.layoutRegisteredUser);
        mTvInstructions = (TextView) rootView.findViewById(R.id.textViewInstruction);

        /**
         * Setup buttons
         */
        mBtnCancel = (Button) rootView.findViewById(R.id.buttonCancel);
        mBtnCancel.setEnabled(false);
        mBtnCancel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                m_bCanceled = true;
                shouldStop = true;
                if(m_callback!=null) {
                    m_callback.onCancel();
                }
            }
        });
        
        mBtnStart = (Button) rootView.findViewById(R.id.buttonStart);
        mBtnStart.setEnabled(false);
        mBtnStart.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mBtnStart.setVisibility(View.GONE);
                FaceEnrollNoStepsManager.getInstance().startEnrollValidated();
            }
        });

        mBtnSuccess = (Button) rootView.findViewById(R.id.buttonSuccess);
        mBtnSuccess.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                shouldStop = true;
                if(m_callback!=null) {
                    m_callback.onEnrollmentSuccess();
                }
            }
        });

        return rootView;
    }

    @Override
    public void onPause() {
        MyLog.i(TAG, "onPause");
        FaceEnrollNoStepsManager.getInstance().cancel();
        m_callback = null;
        super.onPause();
    }

    @Override
    public void onResume() {
        MyLog.i(TAG, "onResume");
        m_nbRetries = 0;
        // Start the enrollment when the fragment is attached to a frameLayout
        // and becomes visible
        runEnroll();
        super.onResume();
    }

    // ===========================================================
    //  Set Callback to the App that uses the UI SDK
    // ===========================================================
    public void setEnrollmentCallback(EnrollmentCallback callback){
        this.m_callback = callback;
    }

    // Start the enrolment UI and camera
	private void runEnroll() {
        m_bCanceled = false;
        shouldStop = false;
		MyLog.i(TAG, "runEnroll");
		mProgressStepView.setProgress(false, ErrorMode.NONE);
        mProgressStepView.setSurroundMode(true, ErrorMode.DISABLED);
        mTvInstructions.setVisibility(View.INVISIBLE);

        mFaceView.setVisibility(View.VISIBLE);
        mLayoutRegisteredUser.setVisibility(View.GONE);

        mBtnCancel.setVisibility(View.VISIBLE);
        mBtnStart.setVisibility(View.VISIBLE);
        mBtnSuccess.setVisibility(View.GONE);

        if(m_nbRetries >= MAX_RETRY) {
            return;
        }

        FaceManager.getInstance().load(getActivity());

        final FaceEnrollerUIListener listener = setUpFaceEnrollerUIListener();
        final FaceAuthEnrollerCallback callback = setUpAuthEnrollerCallback(listener);

        FaceEnrollNoStepsManager.getInstance().startEnrollment(listener, callback, TIMEOUT, TIME_MIN_PROCESSING);
	}

    // Set up the listener for UI updates
    private FaceEnrollerUIListener setUpFaceEnrollerUIListener(){
        FaceEnrollerUIListener listener = new FaceEnrollerUIListener() {
            // Private Frame Event listener called from the Enroll Manager
            @Override
            public void onNewFrame(FaceAuthFrameEvent frameEvent) {
                MyLog.v(TAG, "onNewFrame");
                mBtnCancel.setEnabled(true);
                mFaceView.setFaceFrameEvent(frameEvent);
            }

            // Private step Changed Event listener called from the Enroll Manager
            @Override
            public void onStepChanged(int step, ErrorMode errorMode, MaskMode maskMode, boolean bSurroundMode) {
                final int stepF = step;
                final MaskMode maskModeF = maskMode;

                MyLog.d(TAG, "onStepChanged: step="+stepF+" maskMode="+maskModeF);
                if(stepF == FaceEnrollNoStepsManager.STEP_WAIT_FACE) { // Wait Face (Start Pressed)
                    mProgressStepView.setProgress(true, ErrorMode.NONE);
                }
                else if(stepF == FaceEnrollNoStepsManager.STEP_PROCESSING) { // Processing
                    mProgressStepView.setSurroundMode(true, ErrorMode.DISABLED);
                }
            }

            // Private face detection status called from the Enroll Manager
            @Override
            public void onFacePositionChanged(int step, ErrorMode faceMode) {
                final int stepF = step;
                final ErrorMode faceModeF = faceMode;

                MyLog.d(TAG, "onFacePositionChanged: step="+stepF+" lastFaceMode="+faceModeF);
                if(stepF == FaceEnrollNoStepsManager.STEP_WAIT_GO || stepF == FaceEnrollNoStepsManager.STEP_WAIT_FACE) {
                    // Wait TO GO
                    mBtnStart.setEnabled(faceModeF == ErrorMode.NONE);
                    mProgressStepView.setSurroundMode(mProgressStepView.getSurroundMode(), faceModeF);
                }
            }
        };

        return listener;
    }

    // setup the enrollment callbacks
    private FaceAuthEnrollerCallback setUpAuthEnrollerCallback(final FaceEnrollerUIListener faceEnrollerUIListener){
        FaceAuthEnrollerCallback callback = new FaceAuthEnrollerCallback() {
            /**
             * Invoked when enrollment is finished.
             * Check the status to see if enrollment is successful.
             * @param faceAuthStatus
             */
            @Override
            public void onEnrollFinish(FaceAuthStatus faceAuthStatus) {
                MyLog.w(TAG, "enrollBiometric: ENDED!!! status ="+faceAuthStatus);
                if(!m_bCanceled && faceAuthStatus==FaceAuthStatus.CANCELED) {
                    //TODO: Fix Neuro SDK
                    MyLog.e(TAG, "HACKY Fix of Neuro >> FORCE STATUS CANCEL TO BAD_QUALITY");
                    faceAuthStatus = FaceAuthStatus.BAD_QUALITY;
                }

                if(faceAuthStatus == FaceAuthStatus.SUCCESS) {
                    FaceEnrollNoStepsManager.getInstance().cleanAuth();
                    mProgressStepView.setProgress(true, ErrorMode.NONE);
                    onEnrollSuccess(FaceEnrollNoStepsManager.getInstance().getLoggedUserImage());
                }  else if(faceAuthStatus !=  FaceAuthStatus.CANCELED) {
                    MyLog.i(TAG, "result= "+faceAuthStatus+" retries="+m_nbRetries);
                    m_nbRetries++;

                    FaceEnrollNoStepsManager.getInstance().cleanAuth();
                    mProgressStepView.setProgress(true, ErrorMode.ERROR);

                    if(m_callback!=null) {
                        ErrorDialogFragment.newInstance(EnrollFragment.this,
                                getActivity().getString(FaceMessagesHelper.getErrorMessageForErrorCode(faceAuthStatus)))
                                .showAllowingStateLoss(getActivity().getSupportFragmentManager(), "error");
                    }

                    if(m_nbRetries >= MAX_RETRY){
                        FaceEnrollNoStepsManager.getInstance().cleanAuth();
                        if(m_callback!=null) {
                            m_callback.onEnrollmentFailed(faceAuthStatus);
                        }
                    } else if(shouldStop){
                        FaceEnrollNoStepsManager.getInstance().cleanAuth();
                    }else{
                        MyLog.d(TAG, "Face verification failed : Remaining retries: " + (MAX_RETRY-m_nbRetries));
                        FaceEnrollNoStepsManager.getInstance().cleanAuth();
                        if(m_callback!=null) {
                            m_callback.onEnrollmentRetry(faceAuthStatus, MAX_RETRY - m_nbRetries);
                        }
                        // Uncomment this if you wish to disable to error dialog and auto-retry
                        //FaceEnrollNoStepsManager.getInstance().startEnrollment(faceEnrollerUIListener, this, TIMEOUT, TIME_MIN_PROCESSING);
                    }
                }
            }

            /**
             * Invoked when error occurs before Face Enrolment can be started.
             * eg. Device camera support, required permission not granted etc.
             * @param e
             */
            @Override
            public void onEnrollError(IdpException e) {
                MyLog.w(TAG, "enrollBiometric: ENDED!!! "+e.getMessage());
                FaceEnrollNoStepsManager.getInstance().cancel();
                if(m_callback!=null) {
                    m_callback.onError(e);
                }
            }
        };

        return callback;
    }

    // ===========================================================
    //  end enrollment
    // ===========================================================
    private void onEnrollSuccess(Bitmap image) {
        MyLog.i(TAG, "onEnrollSuccess");
        mLayoutRegisteredUser.setVisibility(View.VISIBLE);
        mFaceView.setVisibility(View.GONE);

        // Uncomment to display a round face image instead of placeholder
        //if(image != null) {
        //    Bitmap cover = ImageShapeTool.getRoundedCroppedBitmap(image, image.getWidth());
        //    mIvRegisteredUser.setImageBitmap(cover);
        //} else {
            // Always use the avatar for now as requested
            mIvRegisteredUser.setImageResource(R.drawable.face_demo);
        //}

        mBtnStart.setVisibility(View.GONE);
        mBtnCancel.setVisibility(View.GONE);

        if(m_resultModeTempo >= 0) {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if(m_callback!=null) {
                        m_callback.onEnrollmentSuccess();
                    }
                }
            }, m_resultModeTempo);
        }
        else {
            mBtnSuccess.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Error Dialog Ok Listener
     */
    @Override
    public void onDialogOk() {
        MyLog.d(TAG, "onDialogOk");
        if(m_callback!=null && !shouldStop) {
            runEnroll();
        }
    }
}
