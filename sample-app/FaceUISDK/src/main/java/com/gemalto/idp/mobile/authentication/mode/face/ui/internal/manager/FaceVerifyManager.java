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

package com.gemalto.idp.mobile.authentication.mode.face.ui.internal.manager;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.Log;

import com.gemalto.idp.mobile.authentication.Authenticatable;
import com.gemalto.idp.mobile.authentication.mode.face.FaceAuthFrameEvent;
import com.gemalto.idp.mobile.authentication.mode.face.FaceAuthLivenessAction;
import com.gemalto.idp.mobile.authentication.mode.face.FaceAuthService;
import com.gemalto.idp.mobile.authentication.mode.face.FaceAuthStatus;
import com.gemalto.idp.mobile.authentication.mode.face.FaceAuthVerifier;
import com.gemalto.idp.mobile.authentication.mode.face.FaceAuthVerifierCallback;
import com.gemalto.idp.mobile.authentication.mode.face.FaceAuthVerifierListener;
import com.gemalto.idp.mobile.authentication.mode.face.ui.FaceManager;
import com.gemalto.idp.mobile.authentication.mode.face.ui.internal.gui.ErrorMode;
import com.gemalto.idp.mobile.authentication.mode.face.ui.internal.utils.logs.MyLog;

import java.util.EnumSet;
import java.util.Timer;
import java.util.TimerTask;

// IMPORTANT: This source code is intended to serve training information purposes only. Please make sure to review our IdCloud documentation, including security guidelines.

/**
 * The internal manager that process steps of the verification procedure
 */
public class FaceVerifyManager {
	private static final String TAG = FaceVerifyManager.class.getSimpleName();

	protected static FaceVerifyManager s_instance;
	protected FaceAuthService m_FaceAuthService;

	protected int CAM_WIDTH = 480;
	protected int CAM_HEIGHT = 640;
	protected int FACE_WIDTH = (int)((float)CAM_WIDTH*0.30f);
	protected int FACE_HEIGHT = FACE_WIDTH;

	private FaceAuthVerifier mFaceController;

	private int m_currentEnrollStep;
	private FaceVerifierUIListener faceVerifierUIListener;
	private FaceAuthVerifierCallback m_verifierCallback;

	private Bitmap m_loggedUserBitmap;
	private Rect m_loggedUserFaceRect;
	private EnumSet<FaceAuthLivenessAction> m_lastLiveness;

	public final static int STEP_WAIT_FACE = 0;
	public final static int STEP_KEEP_STILL = 1;
	public final static int STEP_BLINK = 2;
	public final static int STEP_BLINK_STILL = 3;
	public final static int STEP_PROCESSING = 4;
	public final static int STEP_PROCESSED = 5;

	private Timer m_timerTimeout;
	private int m_timoutMs;
	private boolean m_bTimerTimeout;

	private final static int NB_STEPS = STEP_PROCESSED+1;

	private boolean m_bLastFrameFaceDetected = false;

	private FaceAuthVerifierListener m_frameListener;

	private FaceVerifyManager() {
		MyLog.i(TAG, "FaceAuthManager");
		m_FaceAuthService = FaceManager.getInstance().getFaceAuthService();
	}

	public static synchronized FaceVerifyManager getInstance() {
		if (s_instance == null) {
			s_instance = new FaceVerifyManager();
		}
		return s_instance;
	}

	public void startAuth(
			final Authenticatable authenticatable,
			final FaceVerifierUIListener listener,
			final FaceAuthVerifierCallback callback,
			final int timeout) {
		MyLog.i(TAG, "startAuth");
		if(mFaceController != null) {
			mFaceController.cancelVerifyProcess(); // cancel in there are any other operations in progress
			mFaceController = null;
		}

		m_timoutMs = timeout;
		m_bTimerTimeout = false;
		m_timerTimeout = null;

		faceVerifierUIListener = listener;
		m_verifierCallback = callback;
		m_currentEnrollStep = 0;
		m_loggedUserBitmap = null;
		m_lastLiveness = null;
		m_bLastFrameFaceDetected = false;

		mFaceController = FaceManager.getInstance().getFaceAuthVerifier();

		m_frameListener = new FaceAuthVerifierListener() {
			@Override
			public void onFrameReceived(final FaceAuthFrameEvent frameEvent) {
				processNewFrameEvent(frameEvent);
			}
		};

		// Add frame listener to handle frameEvents from the SDK
		mFaceController.addFaceAuthVerifierListener(m_frameListener);
		// Init the UI to start the enrollment
		faceVerifierUIListener.onStepChanged(
				m_currentEnrollStep,
				ErrorMode.NONE,
				ErrorMode.DISABLED);

		mFaceController.authenticateUser(timeout, authenticatable, m_verifierCallback);

	}

	public void cancel() {
		MyLog.w(TAG, "cancel");
		if(mFaceController != null) {
			mFaceController.cancelVerifyProcess();
			cleanAuth();
		}
	}

	public void cleanAuth() {
		MyLog.i(TAG, "cleanAuth");
		if(mFaceController != null) {
			mFaceController.removeFaceAuthVerifierListener(m_frameListener);
			mFaceController = null;
		}
		if(m_timerTimeout != null ) {
			m_timerTimeout.cancel();
			m_timerTimeout = null;
		}
	}

	/**
	 * The frameEvent is processed to decide what is needed from the end user.
	 * if face is not detected in the frame, nothing is done and wait for the next frameEvent.
	 * if a face is detected, retrieve the liveness action from the frameEvent, and display on the UI
	 * to instruct the user to follow (KEEP_STILL/BLINK) in order to complete the verification.
	 * @param frameEvent
	 */
	private void processNewFrameEvent(FaceAuthFrameEvent frameEvent) {
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

		MyLog.d(TAG, "Frame - status="+status+" rect="+rect+" pitch="+frameEvent.getPitchAngle()+" yaw="+frameEvent.getYawAngle());
		faceVerifierUIListener.onNewFrame(frameEvent);

		EnumSet<FaceAuthLivenessAction> liveness = frameEvent.getLivenessAction();
		EnumSet<FaceAuthLivenessAction> lastLiveness = m_lastLiveness;
		if(liveness != null) {
			m_lastLiveness = liveness;
		}

		MyLog.i(TAG, "lastLiveness="+lastLiveness+" liveness="+liveness);

		// (rect!=null)--> face detected
		if(m_bLastFrameFaceDetected != (rect != null)) { // FaceDetection change state
			m_bLastFrameFaceDetected = (rect != null);
			faceVerifierUIListener.onFacePositionChanged(m_currentEnrollStep, m_bLastFrameFaceDetected);
		}

		if (rect != null) { //  Face Detected
			if(lastLiveness !=null && lastLiveness.contains(FaceAuthLivenessAction.KEEP_STILL) && liveness == null) {
				// Still in KEEP STILL phase, ask user to keep still
				faceVerifierUIListener.onStepChanged(m_currentEnrollStep,
						ErrorMode.NONE, m_bLastFrameFaceDetected ? ErrorMode.NONE : ErrorMode.ERROR);
			}

			if(m_currentEnrollStep == STEP_WAIT_FACE
					&& lastLiveness != null && lastLiveness.contains(FaceAuthLivenessAction.KEEP_STILL)
					&& liveness!=null && liveness.contains(FaceAuthLivenessAction.KEEP_STILL)){
				onNextStep(frameEvent);
			}
			// no STILL phase, directly go to BLINK
			else if(m_currentEnrollStep == STEP_WAIT_FACE
					&& lastLiveness != null && lastLiveness.contains(FaceAuthLivenessAction.BLINK)
					&& liveness!=null && liveness.contains(FaceAuthLivenessAction.BLINK)){
				m_currentEnrollStep = STEP_BLINK;
				faceVerifierUIListener.onStepChanged(m_currentEnrollStep,
						ErrorMode.NONE, m_bLastFrameFaceDetected ? ErrorMode.NONE : ErrorMode.ERROR);
			}
			else if(m_currentEnrollStep == STEP_KEEP_STILL) { // Front face
				if(lastLiveness !=null && lastLiveness.contains(FaceAuthLivenessAction.KEEP_STILL)
						&& (liveness != null && !liveness.contains(FaceAuthLivenessAction.KEEP_STILL))) {
					// Last liveness action contains KEEP STILL but not the current ones it means
					// KEEP STILL phase is over, move to next
					m_lastLiveness = null;
					m_loggedUserBitmap = frameEvent.getImage().toBitmap();
					m_loggedUserFaceRect = rect;
					onNextStep(frameEvent);
				}
			}
			else if(m_currentEnrollStep == STEP_BLINK) { // Blink Detected and processed (cause face detection stop)
//				if(lastLiveness !=null && lastLiveness.contains(FaceAuthLivenessAction.BLINK) && liveness == null) {
//					onNextStep(frameEvent);
//				}
				if(lastLiveness !=null && lastLiveness.contains(FaceAuthLivenessAction.BLINK)
						&& (liveness != null && liveness.contains(FaceAuthLivenessAction.KEEP_STILL))) {
					m_currentEnrollStep = STEP_BLINK_STILL;
					onNextStep(frameEvent);
				}
				if(m_timerTimeout == null ) {
					MyLog.i(TAG, "startCancelTimeout");
					m_timerTimeout = new Timer();
					m_timerTimeout.schedule(new TimerTask() {
						@Override
						public void run() {
							MyLog.w(TAG,"SOFT TIMEOUT!!!");
							m_bTimerTimeout = true;
							FaceVerifyManager.this.cancel();
						}
					}, m_timoutMs);
				}
			}
			else if(m_currentEnrollStep == STEP_PROCESSING) { // Continue Blink Processing
				if(lastLiveness !=null && lastLiveness.contains(FaceAuthLivenessAction.KEEP_STILL)) {
					if(liveness == null && status == FaceAuthStatus.SUCCESS) { // Blink Well treated => Process to real verification
						m_lastLiveness = null;
						onNextStep(frameEvent);
					}
				}
			} else if(m_currentEnrollStep == STEP_BLINK_STILL) { // last still after blinking
				if(lastLiveness !=null && lastLiveness.contains(FaceAuthLivenessAction.KEEP_STILL) && liveness == null) {
					if(liveness == null && status == FaceAuthStatus.SUCCESS) { // Blink Well treated => Process to real verification
						m_lastLiveness = null;
						onNextStep(frameEvent);
					}
				}
			}
		}
	}

	private void onNextStep(FaceAuthFrameEvent frameEvent) {
		MyLog.i(TAG, "onNextStep");
		m_currentEnrollStep++;
		faceVerifierUIListener.onStepChanged(m_currentEnrollStep,
				ErrorMode.NONE, m_bLastFrameFaceDetected ? ErrorMode.NONE : ErrorMode.ERROR);
	}

	public Bitmap getLoggedUserImage() {
		return m_loggedUserBitmap;
	}
}
