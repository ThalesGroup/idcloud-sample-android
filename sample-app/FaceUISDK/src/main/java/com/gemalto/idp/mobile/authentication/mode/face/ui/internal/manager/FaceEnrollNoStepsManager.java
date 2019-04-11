/**
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

package com.gemalto.idp.mobile.authentication.mode.face.ui.internal.manager;

import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;

import com.gemalto.idp.mobile.authentication.mode.face.FaceAuthEnroller;
import com.gemalto.idp.mobile.authentication.mode.face.FaceAuthEnrollerCallback;
import com.gemalto.idp.mobile.authentication.mode.face.FaceAuthEnrollerListener;
import com.gemalto.idp.mobile.authentication.mode.face.FaceAuthException;
import com.gemalto.idp.mobile.authentication.mode.face.FaceAuthFrameEvent;
import com.gemalto.idp.mobile.authentication.mode.face.FaceAuthStatus;
import com.gemalto.idp.mobile.authentication.mode.face.ui.FaceManager;
import com.gemalto.idp.mobile.authentication.mode.face.ui.internal.gui.ErrorMode;
import com.gemalto.idp.mobile.authentication.mode.face.ui.internal.gui.view.FaceMaskView.MaskMode;
import com.gemalto.idp.mobile.authentication.mode.face.ui.internal.utils.logs.MyLog;

import java.util.Timer;
import java.util.TimerTask;

// IMPORTANT: This source code is intended to serve training information purposes only. Please make sure to review our IdCloud documentation, including security guidelines.

/**
 * The internal manager that process steps of the Simple Mode Enrollment procedure
 */
public class FaceEnrollNoStepsManager {
    private static final String TAG = FaceEnrollNoStepsManager.class.getSimpleName();

    protected static FaceEnrollNoStepsManager s_instance;

    // Constant for camera captured view display
    protected int CAM_WIDTH = 480;
    protected int CAM_HEIGHT = 640;
    protected int FACE_WIDTH = (int)((float)CAM_WIDTH*0.30f);
    protected int FACE_HEIGHT = (int)((float)FACE_WIDTH*1.4f);
    protected float FACE_LOCATION_ERROR_TOLERANCE = 0.05f;
    protected float FACE_ORIENTATION_ERROR_TOLERANCE = 10.0f;
    protected float FACE_ORIENTATION_ANGLE = 15.0f;

    private FaceAuthEnroller mFaceController;

    // Status to identify the state of the enrollment
    private ErrorMode mLastFaceMode;
    private FaceAuthStatus mLastStatus;
    private int m_currentEnrollStep;
    private boolean m_bWaitForEnrollSuccess;


    // Listeners and callbacks
    // Wrapper of frameEvent listener, depend on the frameEvent, it displays
    // the progress of the enrollment
    private FaceEnrollerUIListener faceEnrollerUIListener;

    // Listener passed into the SDK, it listens to Frame Events
    // NOTE: it must be removed from the enroller after each enrolllment
    private FaceAuthEnrollerListener m_frameListener;

    // Callback to report the final result of the enrollment
    private FaceAuthEnrollerCallback m_enrollerCallback;

    // The image to be displayed after the enrollment
    private Bitmap m_loggedUserBitmap;
    private Rect m_loggedUserFaceRect;

    // The timer to fake a processing delay after the successful enrollment
	private Timer m_completeTimeout;
    // The amount of time to delay after the sucessful enrollment
    private int m_FakeProcessingTime;

    // different steps of the enrollment
    public final static int STEP_WAIT_GO = 0; // showing the captured image only, image not processed by SDK
    public final static int STEP_WAIT_FACE = 1; // when user clicks 'start'
    public final static int STEP_PROCESSING = 2; // after a frame is added for processing
    public final static int STEP_SUCCESS = 3; // after the frame is successfully processed saved as template

    final static int NB_STEPS = STEP_SUCCESS+1;

    /**
     * private constructor
     */
    private FaceEnrollNoStepsManager() {
        MyLog.i(TAG, "FaceEnrollManager");
    }

    /**
     * create singleton instance
     * @return
     */
    public static synchronized FaceEnrollNoStepsManager getInstance() {
        if (s_instance == null) {
            s_instance = new FaceEnrollNoStepsManager();
        }
        return s_instance;
    }

    /**
     * Start the enrollment
     * @param listener ui listener
     * @param callback callback of the final enrollment result
     * @param timeout timeout of the enrollment
     * @param fakeProcessingTime faked delay after successful enrollment
     */
    public void startEnrollment(final FaceEnrollerUIListener listener,
                                final FaceAuthEnrollerCallback callback,
                                final int timeout,
                                final int fakeProcessingTime) {
        MyLog.i(TAG, "startEnrollment");
        if(mFaceController != null) {
            // cancel in there are any other operations in progress
            mFaceController.cancelEnrollmentProcess();
            mFaceController = null;
        }

        // Initialize status and states
        m_FakeProcessingTime = fakeProcessingTime;
        faceEnrollerUIListener = listener;
        m_enrollerCallback = callback;
        m_currentEnrollStep = 0;
        mLastStatus = null;
        m_bWaitForEnrollSuccess = false;
        m_loggedUserBitmap = null;
        mLastFaceMode = null;

        // create the enroller
        mFaceController = FaceManager.getInstance().getFaceAuthEnroller();

        m_frameListener = new FaceAuthEnrollerListener() {
            @Override
            public void onFrameReceived(final FaceAuthFrameEvent frameEvent) {
                processNewFrameEvent(frameEvent);
            }
        };

        // Add frame listener to handle frameEvents from the SDK
        mFaceController.addFaceAuthEnrollerListener(m_frameListener);
        // Init the UI to start the enrollment
        faceEnrollerUIListener.onStepChanged(
                getDisplayStepFromFullStep(),
                ErrorMode.NONE,
                getDisplayMaskModeFromFullStep(),
                getSurroundMode());

        mFaceController.enroll(timeout, m_enrollerCallback);
    }

    /**
     * Process the new frameEvent return from the SDK
     * @param frameEvent
     */
    private void processNewFrameEvent(final FaceAuthFrameEvent frameEvent) {
        // Get the status of the frameEvent
        FaceAuthStatus status = frameEvent.getStatus();
        // Get the rect that contains the detected face of the user
        Rect rect = frameEvent.getFaceCoordinate();
        if(rect.left == 0 && rect.top == 0 && rect.right == 0 && rect.bottom == 0) {
            // Face not detected
            rect = null;
        } else {
            // face detected, create new Rect according to Camera size configurations
            rect = new Rect(CAM_WIDTH-rect.top, CAM_HEIGHT-rect.left, CAM_WIDTH-rect.bottom, CAM_HEIGHT-rect.right);
        }

        MyLog.d(TAG, "Frame - status="+status+" rect="+rect
                +" pitch="+frameEvent.getPitchAngle()+" yaw="+frameEvent.getYawAngle()
                +" lastStatus="+frameEvent.getLastFaceExtractionStatus()+
                " liveness="+frameEvent.getLivenessAction());

        // Always update the UI to display what the camera has captured
        faceEnrollerUIListener.onNewFrame(frameEvent);

        // Complete enrollment under No_Steps mode goes through the following STEPs
        // it is default to WAIT_GO and simply display the captured view
        // STEP_WAIT_GO = 0;
        // STEP_WAIT_FACE = 1;
        // STEP_PROCESSING = 2;
        // STEP_SUCCESS = 3;

        if(m_bWaitForEnrollSuccess) {
            // 1 valid frame has already been added
            // Check if it is successfully extracted by the SDK
            onNextEnrollStepValidate(frameEvent);
        } else if (rect != null) {
            // Enter this only if face is detected, if not, ignore this and wait for next frame
            // On first frameEvent received, it should be at STEP_WAIT_FACE step
            if(m_currentEnrollStep == STEP_WAIT_FACE) {
                // Check position of the face Rect with respect to the Camera and its size
                // Note that this check is not mandatory but it ensures better quality of the template
                Point faceCenter = new Point(CAM_WIDTH/2, CAM_HEIGHT/2);
                if(isFaceWellPositionned(faceCenter, rect)
                        // expected yaw & pitch are set to 0, it means front face is expected
                        && isFaceWellOriented(0, 0, frameEvent.getYawAngle(), frameEvent.getPitchAngle())) {
                    m_loggedUserBitmap = frameEvent.getImage().toBitmap();
                    m_loggedUserFaceRect = rect;
                    // add the frame to the template and stay at STEP_WAIT_FACE, then wait for next frameEvent
                    addFaceFrame(frameEvent);
                }
            } else if(m_currentEnrollStep == STEP_WAIT_GO) {
                // WAIT only for Go from user
                // Do nothing
            }

            if(frameEvent.getStatus() == FaceAuthStatus.SUCCESS && mLastStatus == null) {
                // if already success, completed enrollment
                mLastStatus = status;
                onEnrollEnd();
            }
        }

        // ErrorMode is extracted in order to shown corresponding UI to the user
        // ErrorMode is used by the CircularProgressView
        ErrorMode faceMode = (rect != null) ? ErrorMode.NONE : ErrorMode.WARNING;
        if(mLastFaceMode != faceMode) {
            // FaceDetection change state
            // Only update UI when there is a change
            mLastFaceMode = faceMode;
            faceEnrollerUIListener.onFacePositionChanged(m_currentEnrollStep, mLastFaceMode);
        }
    }

    // Add a valid frame to the SDK for feature extraction
    private void addFaceFrame(final FaceAuthFrameEvent frameEvent) {
        MyLog.i(TAG, "add a valid face frame");
        try {
            mFaceController.addFaceFrameForEnrollment(frameEvent);
            MyLog.i(TAG, "Step"+m_currentEnrollStep+"- addFaceToEnroll() success");
            m_bWaitForEnrollSuccess = true;
            MyLog.d(TAG, "addFaceToEnroll: Wait Extraction Done");
        } catch(FaceAuthException error) {
            MyLog.w(TAG, "addFaceToEnroll: Error Try new Frame");
            MyLog.w(TAG, "addFaceToEnroll: Error addFaceToEnroll");
            MyLog.w(TAG, error.getLocalizedMessage());
        }
    }

    private void onNextEnrollStepValidate(final FaceAuthFrameEvent frameEvent) {
        // Check if the previous frame has been successfully extracted
        FaceAuthStatus status = frameEvent.getLastFaceExtractionStatus();
        MyLog.i(TAG, "onNextEnrollStepValidate() lastExtraction = "+status);

        if(status == FaceAuthStatus.SUCCESS || status == FaceAuthStatus.NONE) {
            MyLog.i(TAG, "Extraction: SUCCEESS");
            m_bWaitForEnrollSuccess = false;

            if(m_currentEnrollStep == STEP_WAIT_FACE) {
                // Proceed to STEP_PROCESSING
                m_currentEnrollStep++;
                // Update the CircularProgressView
                faceEnrollerUIListener.onStepChanged(getDisplayStepFromFullStep(),
                        ErrorMode.NONE,
                        getDisplayMaskModeFromFullStep(),
                        getSurroundMode());

                // Do complete delayed to fake processing time (complete lock framesEvent, even in thread)
                m_completeTimeout = new Timer();
                m_completeTimeout.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        MyLog.w(TAG,"Complete Timeout");
                        mFaceController.completeEnrollment(frameEvent);
                        MyLog.w(TAG, "completeEnrollBiometric(): SUCCESS!!!");
                        // Proceed to STEP_SUCCESS
                        m_currentEnrollStep++;
                        // Update the CircularProgressView
                        faceEnrollerUIListener.onStepChanged(getDisplayStepFromFullStep(),
                                ErrorMode.NONE,
                                getDisplayMaskModeFromFullStep(),
                                getSurroundMode());
                    }
                }, m_FakeProcessingTime);

            } else {
                // if it is after STEP_WAIT_FACE, proceed to finish
                m_currentEnrollStep++;
                MyLog.i(TAG, "Going to Next Step: "+m_currentEnrollStep);
                // Update the CircularProgressView
                faceEnrollerUIListener.onStepChanged(getDisplayStepFromFullStep(),
                        ErrorMode.NONE,
                        getDisplayMaskModeFromFullStep(),
                        getSurroundMode());
            }

        } else if(status == FaceAuthStatus.ALREADY_EXTRACTING || status == FaceAuthStatus.NONE) {
            // If previous frame is being extracted, do nothing
            MyLog.w(TAG, "Extraction: Waiting to finish...");
        } else {
            m_bWaitForEnrollSuccess = false;
            MyLog.w(TAG, "Extraction: Error Retry!");
        }
    }

    // Check if face is well positioned, must to close to the centre
    private boolean isFaceWellPositionned(Point faceCenter, Rect rect) {
        float toloeranceWidth = FACE_WIDTH* FACE_LOCATION_ERROR_TOLERANCE;
        float toloeranceHeight = FACE_HEIGHT* FACE_LOCATION_ERROR_TOLERANCE;
        RectF rectLimit = new RectF(faceCenter.x-FACE_WIDTH/2-toloeranceWidth, faceCenter.y-FACE_HEIGHT/2-toloeranceHeight,
                faceCenter.x+FACE_WIDTH/2+toloeranceWidth, faceCenter.y+FACE_HEIGHT/2+toloeranceHeight);

        RectF rectDetection = new RectF(rect);

        return rectLimit.contains(rectDetection);
    }

    // Check if the face is well oriented
    private boolean isFaceWellOriented(float yawWaited, float pitchWaited, float yaw, float pitch) {
        if((yaw < yawWaited-FACE_ORIENTATION_ERROR_TOLERANCE)
                || (yaw > yawWaited+FACE_ORIENTATION_ERROR_TOLERANCE)) {
            return false;
        }
        if((pitch < pitchWaited-FACE_ORIENTATION_ERROR_TOLERANCE)
                || (pitch > pitchWaited+FACE_ORIENTATION_ERROR_TOLERANCE)) {
            return false;
        }
        return true;
    }

    // click to start the enrollment
    // Change from STEP_WAIT_GO to STEP_WAIT_FACE
    public void startEnrollValidated() {
        MyLog.i(TAG, "startEnrollValidated");
        m_currentEnrollStep++;
        faceEnrollerUIListener.onStepChanged(getDisplayStepFromFullStep(),
                ErrorMode.NONE,
                getDisplayMaskModeFromFullStep(),
                getSurroundMode());
    }

    private void onEnrollEnd() {
        MyLog.i(TAG, "onEnrollEnd: Success Enoll");
        cleanAuth();
    }

    // Cancel the enrollment
    // NOTE: after cancellation, listener must be removed
    public void cancel() {
        MyLog.w(TAG, "cancel");
        if(mFaceController != null) {
            mFaceController.cancelEnrollmentProcess();
            cleanAuth();
        }
    }

    public void cleanAuth() {
        MyLog.i(TAG, "cleanAuth");
        if(mFaceController != null) {
            mFaceController.removeFaceAuthEnrollerListener(m_frameListener);
            mFaceController = null;
        }

        if(m_completeTimeout != null) {
            m_completeTimeout.cancel();
            m_completeTimeout = null;
        }
    }

    private int getDisplayStepFromFullStep() {
        return m_currentEnrollStep;
    }

    private boolean getSurroundMode() {
        if(m_currentEnrollStep == STEP_WAIT_GO) { // Front
            return true;
        }
        return false;
    }

    // Mask Mode is not used for simple mode enrollment
    private MaskMode getDisplayMaskModeFromFullStep() {
        return null;
    }

    public Bitmap getLoggedUserImage() {
        return m_loggedUserBitmap;
    }
}	
