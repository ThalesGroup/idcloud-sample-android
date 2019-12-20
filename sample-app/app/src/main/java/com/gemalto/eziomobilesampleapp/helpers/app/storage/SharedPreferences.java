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

package com.gemalto.eziomobilesampleapp.helpers.app.storage;

import android.content.Context;

import com.gemalto.eziomobilesampleapp.helpers.Protocols;
import com.gemalto.idp.mobile.core.ApplicationContextHolder;

// IMPORTANT: This source code is intended to serve training information purposes only. Please make sure to review our IdCloud documentation, including security guidelines.

/**
 * SharedPreferences wrapper to unify API with secure storage.
 */
public class SharedPreferences implements Protocols.StorageProtocol {

    //region Defines

    private static final String C_SAMPLE_STORAGE = "SampleStorage";

    private final android.content.SharedPreferences mManager;

    //endregion

    //region Life Cycle

    public SharedPreferences() {
        mManager = ApplicationContextHolder.getContext().getSharedPreferences(C_SAMPLE_STORAGE, Context.MODE_PRIVATE);
    }

    //endregion

    //region StorageProtocol

    @Override
    public boolean writeString(final String value, final String key) {
        mManager.edit().putString(key, value).apply();
        return true;
    }

    @Override
    public boolean writeInteger(final int value, final String key) {
        mManager.edit().putInt(key, value).apply();
        return true;
    }

    @Override
    public String readString(final String key) {
        return mManager.getString(key, null);
    }

    @Override
    public int readInteger(final String key) {
        return mManager.getInt(key, 0);
    }

    @Override
    public boolean removeValue(final String key) {
        mManager.edit().remove(key).apply();
        return true;
    }

    //endregion
}
