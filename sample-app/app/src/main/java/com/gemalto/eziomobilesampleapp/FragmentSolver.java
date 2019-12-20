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

package com.gemalto.eziomobilesampleapp;

import com.gemalto.eziomobilesampleapp.gui.FragmentMissingPermissions;
import com.gemalto.eziomobilesampleapp.gui.FragmentTabAuthentication;
import com.gemalto.eziomobilesampleapp.gui.FragmentTabGemaltoFaceId;
import com.gemalto.eziomobilesampleapp.gui.FragmentTabProvision;
import com.gemalto.eziomobilesampleapp.gui.FragmentTabSettings;
import com.gemalto.eziomobilesampleapp.gui.FragmentTabTransaction;
import com.gemalto.eziomobilesampleapp.gui.MainFragment;

// IMPORTANT: This source code is intended to serve training information purposes only. Please make sure to review our IdCloud documentation, including security guidelines.

/**
 * Class to handle different fragments per configuration.
 */
public class FragmentSolver {

    /**
     * Returns the fragment id based on a given fragment.
     * @param fragment Fragment we want to get id from.
     * @return Fragment id or 0 if it's not defined.
     */
    static int getFragmentId(final MainFragment fragment) {
        if (fragment instanceof FragmentTabProvision) {
            return R.id.navigation_enroll;
        } else if (fragment instanceof FragmentTabAuthentication) {
            return R.id.navigation_authentication;
        } else if (fragment instanceof  FragmentTabTransaction) {
          return R.id.navigation_transaction;
        } else if (fragment instanceof FragmentTabSettings) {
            return R.id.navigation_settings;
        } else if (fragment instanceof FragmentTabGemaltoFaceId) {
            return R.id.navigation_gemalto_face_id;
        } else if  (fragment instanceof FragmentMissingPermissions){
            return FragmentMissingPermissions.FRAGMENT_CUSTOM_ID;
        } else {
            return 0;
        }
    }

    /**
     * Returns the fragment for a given id.
     * @param itemValue Id of fragment we want to get.
     * @return Desired fragment or null if it's not defined.
     */
    static MainFragment getFragmentById(final int itemValue) {
        switch (itemValue) {
            case R.id.navigation_enroll:
                return new FragmentTabProvision();
            case R.id.navigation_authentication:
                return new FragmentTabAuthentication();
            case R.id.navigation_transaction:
                return new FragmentTabTransaction();
            case R.id.navigation_settings:
                return new FragmentTabSettings();
            case R.id.navigation_gemalto_face_id:
                return new FragmentTabGemaltoFaceId();
            case FragmentMissingPermissions.FRAGMENT_CUSTOM_ID:
                return new FragmentMissingPermissions();
            default:
                return null;
        }
    }
}
