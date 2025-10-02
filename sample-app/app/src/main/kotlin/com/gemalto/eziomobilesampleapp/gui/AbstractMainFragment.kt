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
package com.gemalto.eziomobilesampleapp.gui

import androidx.fragment.app.Fragment
import com.gemalto.eziomobilesampleapp.MainActivity


/**
 * Helper class gathering all common methods and elements of all fragments.
 */
abstract class AbstractMainFragment : Fragment() {
    //region Life Cycle
    override fun onResume() {
        super.onResume()

        this.mainActivity?.enableDrawer(false)

        reloadGUI()
    }

    //endregion
    //region MainFragment
    protected val mainActivity: MainActivity?
        /**
         * Retrieves the `MainActivity` instance.
         *
         * @return `MainActivity` instance.
         */
        get() = (getActivity() as MainActivity?)

    /**
     * Deletes token.
     */
    open fun deleteToken() { // NOPMD - standard behaviour for most of subclasses is to do nothing
        // Override
    }

    /**
     * Toggles the touch id.
     */
    open fun toggleTouchId() { // NOPMD - standard behaviour for most of subclasses is to do nothing
        // Override
    }

    /**
     * Changes pin.
     */
    open fun changePin() { // NOPMD - standard behaviour for most of subclasses is to do nothing
        // Override
    }

    //endregion
    //region Abstract Methods
    /**
     * Reloads the GUI.
     */
    abstract fun reloadGUI()

    /**
     * Disables the GUI.
     */
    abstract fun disableGUI() //endregion
}
