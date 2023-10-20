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
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.gemalto.eziomobilesampleapp.R;

/**
 * Overlay loading indicator covering UI during blocking asynchronous operations.
 */
public class FragmentLoadingIndicator extends Fragment {

    //region Defines

    private TextView mLabelCaption = null;
    private String mLastCaption = null;

    //endregion

    //region Life cycle

    @Nullable
    @Override
    @SuppressLint("InflateParams")
    public View onCreateView(
            @NonNull final LayoutInflater inflater,
            @Nullable final ViewGroup container,
            @Nullable final Bundle savedInstanceState
    ) {
        final View retValue = inflater.inflate(R.layout.fragment_loading_indicator, null);

        mLabelCaption = retValue.findViewById(R.id.label_caption);
        if (mLastCaption != null) {
            mLabelCaption.setText(mLastCaption);
            mLabelCaption = null;
        }

        return retValue;
    }

    //endregion

    //region Private Helpers

    public void setCaption(final String caption) {
        if (mLabelCaption != null) {
            mLabelCaption.setText(caption);
        } else {
            mLastCaption = caption;
        }
    }

    //endregion
}
