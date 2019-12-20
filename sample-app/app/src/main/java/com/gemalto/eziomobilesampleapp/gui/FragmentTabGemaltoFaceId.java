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

package com.gemalto.eziomobilesampleapp.gui;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.gemalto.eziomobilesampleapp.R;
import com.gemalto.eziomobilesampleapp.helpers.CMain;
import com.gemalto.idp.mobile.authentication.mode.face.FaceAuthStatus;
import com.gemalto.idp.mobile.authentication.mode.face.FaceAuthUnenrollerCallback;
import com.gemalto.idp.mobile.authentication.mode.face.ui.EnrollFragment;
import com.gemalto.idp.mobile.authentication.mode.face.ui.EnrollmentCallback;
import com.gemalto.idp.mobile.authentication.mode.face.ui.FaceManager;
import com.gemalto.idp.mobile.core.IdpException;

// IMPORTANT: This source code is intended to serve training information purposes only. Please make sure to review our IdCloud documentation, including security guidelines.

/**
 * Allow user to enroll / unenroll face id using Gemalto module.
 */
public class FragmentTabGemaltoFaceId extends MainFragmentWithAuthSolver {

    //region Defines

    private TextView mLabelFaceIdStatusValue = null;
    private TextView mLabelEntrollerTitle = null;
    private Button mButtonEnrollNewFaceId = null;

    //endregion

    //region Override

    @Nullable
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater,
                             @Nullable final ViewGroup container,
                             @Nullable final Bundle savedInstanceState) {
        return initGui(inflater, R.layout.fragment_gemalto_face_id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected View initGui(final LayoutInflater inflater, final int fragmentId) {
        final View retValue = super.initGui(inflater, fragmentId);

        mLabelFaceIdStatusValue = retValue.findViewById(R.id.label_status_value);
        mLabelEntrollerTitle = retValue.findViewById(R.id.label_enroll);
        mButtonEnrollNewFaceId = retValue.findViewById(R.id.btton_enroll_face_id);
        mButtonEnrollNewFaceId.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                onButtonPressedEnrollNewFaceId();
            }
        });

        return retValue;
    }

    //endregion


    //region MainFragment

    /**
     * {@inheritDoc}
     */
    @Override
    public void reloadGUI() {
        super.reloadGUI();

        // Display current gemalto face id status.
        final boolean enabled = !getMainActivity().loadingIndicatorIsPresent();
        final CMain.GemaloFaceIdState state = CMain.sharedInstance().getFaceIdState();
        mLabelFaceIdStatusValue.setText(state.getValueString());
        mLabelFaceIdStatusValue.setTextColor(ContextCompat.getColor(getMainActivity(), state.getValueColor()));

        // Hide bottom section if it's not relevant.
        if (state != CMain.GemaloFaceIdState.GemaloFaceIdStateInited && state != CMain.GemaloFaceIdState.GemaloFaceIdStateReadyToUse) {
            mLabelEntrollerTitle.setVisibility(View.INVISIBLE);
            mButtonEnrollNewFaceId.setVisibility(View.INVISIBLE);
        } else {
            mLabelEntrollerTitle.setVisibility(View.VISIBLE);
            mButtonEnrollNewFaceId.setVisibility(View.VISIBLE);
        }

        // Button is only enabled if face id is inited or ready to use.
        mButtonEnrollNewFaceId.setEnabled(enabled && !getMainActivity().loadingIndicatorIsPresent());

        if (state == CMain.GemaloFaceIdState.GemaloFaceIdStateInited) {
            mButtonEnrollNewFaceId.setText(R.string.GEMALTO_FACE_ID_ENROLL);
        } else {
            mButtonEnrollNewFaceId.setText(R.string.GEMALTO_FACE_ID_UNENROLL);
        }
    }

    //endregion

    //region User Interface

    /**
     * On button pressed listener for new face enrollment.
     */
    private void onButtonPressedEnrollNewFaceId() {
        // Do proper action based on current state.
        if (CMain.sharedInstance().getFaceIdState() == CMain.GemaloFaceIdState.GemaloFaceIdStateInited) {
            enroll();
        } else {
            unenroll();
        }
    }

    //endregion

    //region Private Helpers

    /**
     * Enrolls new face.
     */
    private void enroll() {
        final EnrollFragment enrollmentFragment = FaceManager.getInstance().getEnrollmentFragment(10, 1);
        enrollmentFragment.setEnrollmentCallback(new EnrollmentCallback() {
            @Override
            public void onEnrollmentSuccess() {
                CMain.sharedInstance().updateGemaltoFaceIdStatus();
            }

            @Override
            public void onCancel() {
                getMainActivity().hideLastStackFragment();
            }

            @Override
            public void onEnrollmentFailed(final FaceAuthStatus status) {
                getMainActivity().showErrorIfExists(status.toString());

            }

            @Override
            public void onEnrollmentRetry(final FaceAuthStatus status, final int remainingRetries) {
                // Unused
            }

            @Override
            public void onError(final IdpException exception) {
                getMainActivity().showErrorIfExists(exception.getLocalizedMessage());
            }
        });

        getActivity().getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
                .replace(R.id.fragment_container, enrollmentFragment, null)
                .addToBackStack(null)
                .commit();
    }

    /**
     * Un enrolls face.
     */
    private void unenroll() {
        FaceManager.getInstance().getFaceAuthEnroller().unenroll(new FaceAuthUnenrollerCallback() {
            @Override
            public void onUnenrollFinish(final FaceAuthStatus faceAuthStatus) {
                CMain.sharedInstance().updateGemaltoFaceIdStatus();
            }

            @Override
            public void onUnenrollError(final IdpException exception) {
                getMainActivity().showErrorIfExists(exception.getLocalizedMessage());
            }
        });
    }

    //endregion

}
