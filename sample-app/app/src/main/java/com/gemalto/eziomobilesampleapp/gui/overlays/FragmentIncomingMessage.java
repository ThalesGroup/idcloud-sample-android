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

package com.gemalto.eziomobilesampleapp.gui.overlays;

import android.annotation.SuppressLint;
import android.os.Bundle;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.gemalto.eziomobilesampleapp.R;

public class FragmentIncomingMessage extends Fragment {

    //region Defines

    public static final String FRAGMENT_ARGUMENT_CAPTION = "ArgumentCaption";

    private View.OnClickListener mApproveClickListener = null;
    private View.OnClickListener mRejectClickListener = null;

    //endregion

    //region Life Cycle

    @Nullable
    @Override
    @SuppressLint("InflateParams")
    public View onCreateView(
            @NonNull final LayoutInflater inflater,
            @Nullable final ViewGroup container,
            @Nullable final Bundle savedInstanceState
    ) {
        final View retValue = inflater.inflate(R.layout.fragment_incoming_message, null);
        TextView mTextCaption = retValue.findViewById(R.id.text_caption);
        final Bundle agrs = this.getArguments();
        if (agrs != null) {
            mTextCaption.setText(agrs.getString(FRAGMENT_ARGUMENT_CAPTION));
        }

        Button mButtonApprove = retValue.findViewById(R.id.button_approve);
        mButtonApprove.setOnClickListener(this::onButtonPressedApprove);

        Button mButtonReject = retValue.findViewById(R.id.button_reject);
        mButtonReject.setOnClickListener(this::onButtonPressedReject);

        return retValue;
    }

    //endregion

    //region Private Helpers

    private void hideDialog() {
        if (getActivity() != null)
            getActivity().getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
                    .remove(this)
                    .commit();
    }

    //endregion

    //region User Interface

    private void onButtonPressedApprove(final View sender) {
        hideDialog();

        if (mApproveClickListener != null) {
            mApproveClickListener.onClick(FragmentIncomingMessage.this.getView());
        }
    }

    private void onButtonPressedReject(final View sender) {
        hideDialog();

        if (mRejectClickListener != null) {
            mRejectClickListener.onClick(FragmentIncomingMessage.this.getView());
        }
    }

    //endregion

    //region Public API

    public void setApproveClickListener(View.OnClickListener mApproveClickListener) {
        this.mApproveClickListener = mApproveClickListener;
    }

    public void setRejectClickListener(View.OnClickListener mRejectClickListener) {
        this.mRejectClickListener = mRejectClickListener;
    }

    //endregion

}
