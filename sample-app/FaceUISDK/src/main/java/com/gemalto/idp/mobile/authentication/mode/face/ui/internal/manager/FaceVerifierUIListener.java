package com.gemalto.idp.mobile.authentication.mode.face.ui.internal.manager;

import com.gemalto.idp.mobile.authentication.mode.face.FaceAuthFrameEvent;
import com.gemalto.idp.mobile.authentication.mode.face.ui.internal.gui.ErrorMode;

/**
 * This wraps the onFrameReceived listener. After processing the frame, it will invoked 1 or more
 * of the following callbacks to update the UI
 */
public interface FaceVerifierUIListener {

    /**
     * when new frameEvent is available, use it to update the View for image captured by camera.
     * This is called on every frame event received
     * @param frameEvent
     */
    void onNewFrame(final FaceAuthFrameEvent frameEvent);

    /**
     * Updat UI on step changed, show the progress in UI
     * @param step
     * @param errorMode
     * @param surroudMode
     */
    void onStepChanged(int step, ErrorMode errorMode, ErrorMode surroudMode);

    /**
     * Update UI on Face Position changed
     * @param step
     * @param lastFrameFaceDetected
     */
    void onFacePositionChanged(int step, boolean lastFrameFaceDetected);
}
