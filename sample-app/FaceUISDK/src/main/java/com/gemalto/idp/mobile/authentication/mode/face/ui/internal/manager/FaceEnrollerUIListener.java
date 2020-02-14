package com.gemalto.idp.mobile.authentication.mode.face.ui.internal.manager;

import com.gemalto.idp.mobile.authentication.mode.face.FaceAuthFrameEvent;
import com.gemalto.idp.mobile.authentication.mode.face.ui.internal.gui.ErrorMode;
import com.gemalto.idp.mobile.authentication.mode.face.ui.internal.gui.view.FaceMaskView;

/**
 * This wraps the onFrameReceived listener. After processing the frame, it will invoked 1 or more
 * of the following callbacks to update the UI
 */
public interface FaceEnrollerUIListener {
    /**
     * when new frameEvent is available, use it to update the View for image captured by camera.
     * This is called on every frame event received
     * @param frameEvent
     */
    void onNewFrame(FaceAuthFrameEvent frameEvent);

    /**
     * Updat UI on step changed, show the progress in UI
     * @param step
     * @param errorMode
     * @param maskMode
     * @param bSurroundMode
     */
    void onStepChanged(int step, ErrorMode errorMode, FaceMaskView.MaskMode maskMode, boolean bSurroundMode);

    /**
     * Update UI on Face Position changed
     * @param step
     * @param lastFaceMode
     */
    void onFacePositionChanged(int step, ErrorMode lastFaceMode);
}
