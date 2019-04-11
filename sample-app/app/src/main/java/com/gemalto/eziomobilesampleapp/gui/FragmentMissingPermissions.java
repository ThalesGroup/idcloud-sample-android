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

package com.gemalto.eziomobilesampleapp.gui;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.gemalto.eziomobilesampleapp.R;

// IMPORTANT: This source code is intended to serve training information purposes only. Please make sure to review our IdCloud documentation, including security guidelines.

public class FragmentMissingPermissions extends MainFragment {

    //region Defines

    public static final int FRAGMENT_CUSTOM_ID = -1;

    //endregion

    //region Override

    @Nullable
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater,
                             @Nullable final ViewGroup container,
                             @Nullable final Bundle savedInstanceState) {
        return initGui(inflater, R.layout.fragment_missing_permissions);
    }

    //endregion

    //region MainFragment

    /**
     * {@inheritDoc}
     */
    @Override
    protected View initGui(final LayoutInflater inflater, final int fragmentId) {
        final View retValue = super.initGui(inflater, fragmentId);

        final Button buttonPermissions = retValue.findViewById(R.id.permissions_button);
        buttonPermissions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                onButtonPressedPermissions();
            }
        });

        return retValue;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reloadGUI() {
        // Do not call super.

        // Disable bottom bar and wait for permissions.
        getMainActivity().tabBarDisable();
    }

    //endregion

    //region User Interface

    /**
     * On button pressed listener to request runtime permissions.
     */
    private void onButtonPressedPermissions() {
        getMainActivity().checkMandatoryPermissions(true);
    }

    //endregion
}
