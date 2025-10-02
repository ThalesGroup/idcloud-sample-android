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
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.gemalto.eziomobilesampleapp.R

class FragmentIncomingMessage : Fragment() {
    private var mApproveClickListener: View.OnClickListener? = null
    private var mRejectClickListener: View.OnClickListener? = null

    //endregion
    //region Life Cycle
    @SuppressLint("InflateParams")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val retValue = inflater.inflate(R.layout.fragment_incoming_message, null)
        val mTextCaption = retValue.findViewById<TextView>(R.id.text_caption)
        val agrs = this.getArguments()
        if (agrs != null) {
            mTextCaption.setText(agrs.getString(FRAGMENT_ARGUMENT_CAPTION))
        }

        val mButtonApprove = retValue.findViewById<Button>(R.id.button_approve)
        mButtonApprove.setOnClickListener(View.OnClickListener { sender: View? ->
            this.onButtonPressedApprove(
                sender
            )
        })

        val mButtonReject = retValue.findViewById<Button>(R.id.button_reject)
        mButtonReject.setOnClickListener(View.OnClickListener { sender: View? ->
            this.onButtonPressedReject(
                sender
            )
        })

        return retValue
    }

    //endregion
    //region Private Helpers
    private fun hideDialog() {
        if (getActivity() != null) requireActivity().getSupportFragmentManager().beginTransaction()
            .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
            .remove(this)
            .commit()
    }

    //endregion
    //region User Interface
    private fun onButtonPressedApprove(sender: View?) {
        hideDialog()

        if (mApproveClickListener != null) {
            mApproveClickListener?.onClick(this@FragmentIncomingMessage.getView())
        }
    }

    private fun onButtonPressedReject(sender: View?) {
        hideDialog()

        if (mRejectClickListener != null) {
            mRejectClickListener?.onClick(this@FragmentIncomingMessage.getView())
        }
    }

    //endregion
    //region Public API
    fun setApproveClickListener(mApproveClickListener: View.OnClickListener?) {
        this.mApproveClickListener = mApproveClickListener
    }

    fun setRejectClickListener(mRejectClickListener: View.OnClickListener?) {
        this.mRejectClickListener = mRejectClickListener
    } //endregion

    companion object {
        //region Defines
        const val FRAGMENT_ARGUMENT_CAPTION: String = "ArgumentCaption"
    }
}
