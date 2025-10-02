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
package com.gemalto.eziomobilesampleapp.helpers.app.storage

import android.content.Context
import android.content.SharedPreferences
import com.gemalto.eziomobilesampleapp.helpers.Protocols.StorageProtocol
import com.gemalto.idp.mobile.core.ApplicationContextHolder

/**
 * SharedPreferences wrapper to unify API with secure storage.
 */
class SharedPreferences : StorageProtocol {
    private val mManager: SharedPreferences

    //endregion
    //region Life Cycle
    /**
     * Creates a new `SharedPreferences` instance.
     */
    init {
        mManager = ApplicationContextHolder.getContext()
            .getSharedPreferences(SAMPLE_STORAGE, Context.MODE_PRIVATE)
    }

    //endregion
    //region StorageProtocol
    override fun writeString(value: String?, key: String?): Boolean {
        mManager.edit().putString(key, value).apply()
        return true
    }

    override fun writeInteger(value: Int, key: String?): Boolean {
        mManager.edit().putInt(key, value).apply()
        return true
    }

    override fun readString(key: String?): String? {
        return mManager.getString(key, null)
    }

    override fun readInteger(key: String?): Int {
        return mManager.getInt(key, 0)
    }

    override fun removeValue(key: String?): Boolean {
        mManager.edit().remove(key).apply()
        return true
    } //endregion

    companion object {
        //region Defines
        private const val SAMPLE_STORAGE = "SampleStorage"
    }
}
