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
package com.gemalto.eziomobilesampleapp.gui.overlays

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.gemalto.eziomobilesampleapp.R

class FragmentSecureKeypadChar : Fragment() {
    //region Defines
    private var mCharDot: TextView? = null
    private var mCharLine: View? = null

    private var mHighlighted = false
    private var mPresent = false

    //endregion
    //region Life Cycle
    @SuppressLint("InflateParams")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val retValue = inflater.inflate(R.layout.fragment_secure_keypad_char, null)

        mCharDot = retValue.findViewById<TextView>(R.id.char_dot)
        mCharLine = retValue.findViewById<View>(R.id.char_line)

        this.isHighlighted = false
        this.isPresent = false

        return retValue
    }

    var isHighlighted: Boolean
        //endregion
        get() = mHighlighted
        set(highlighted) {
            val color =
                if (highlighted) R.color.colorPrimary else android.R.color.darker_gray
            mCharLine?.setBackgroundColor(getResources().getColor(color))
            mHighlighted = highlighted
        }

    var isPresent: Boolean
        get() = mPresent
        set(present) {
            mCharDot?.setVisibility(if (present) View.VISIBLE else View.INVISIBLE)
            mPresent = present
        } //endregion
}
