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

package com.gemalto.eziomobilesampleapp.gui;

import android.annotation.SuppressLint;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.gemalto.eziomobilesampleapp.R;

/**
 * Fragment to request mandatory runtime permissions.
 */
public class FragmentMissingPermissions extends AbstractMainFragment {

    //region Defines

    private Button mButtonPermissions;

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
        final View retValue = inflater.inflate(R.layout.fragment_missing_permissions, null);

        mButtonPermissions = retValue.findViewById(R.id.permissions_button);
        mButtonPermissions.setOnClickListener(this::onButtonPressedPermissions);

        return retValue;
    }

    //endregion

    //region MainFragment

    /**
     * {@inheritDoc}
     */
    @Override
    public void reloadGUI() {
        // No matter what was set. If there is a loading indicator, we should disable everything.
        if (getMainActivity().isOverlayViewVisible()) {
            disableGUI();
        } else {
            mButtonPermissions.setEnabled(true);
        }

        getMainActivity().enableDrawer(false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void disableGUI() {
        mButtonPermissions.setEnabled(false);
    }

    //endregion

    //region User Interface

    /**
     * Requests the mandatory runtime permissions.
     */
    private void onButtonPressedPermissions(final View sender) {
        getMainActivity().checkMandatoryPermissions(true);
    }

    //endregion

}
