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
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.gemalto.eziomobilesampleapp.MainActivity
import com.gemalto.eziomobilesampleapp.R
import com.gemalto.eziomobilesampleapp.helpers.Protocols
import com.gemalto.idp.mobile.authentication.mode.pin.PinAuthInput
import com.gemalto.idp.mobile.ui.UiModule
import com.gemalto.idp.mobile.ui.secureinput.SecureInputBuilder
import com.gemalto.idp.mobile.ui.secureinput.SecureInputService
import com.gemalto.idp.mobile.ui.secureinput.SecureInputUi
import com.gemalto.idp.mobile.ui.secureinput.SecureKeypadListener

class FragmentSecureKeypad : Fragment(), SecureKeypadListener {
    //region Defines
    private val mFirstPin: MutableList<FragmentSecureKeypadChar> =
        ArrayList<FragmentSecureKeypadChar>()
    private val mSecondPin: MutableList<FragmentSecureKeypadChar> =
        ArrayList<FragmentSecureKeypadChar>()

    private var mHandler: Protocols.SecureInputHandler? = null
    private var secureInput: SecureInputUi? = null
    private var builder: SecureInputBuilder? = null
    private var mChangePin = false

    @SuppressLint("InflateParams")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val retValue = inflater.inflate(R.layout.fragment_secure_keypad, null)

        val labelFirst = retValue.findViewById<TextView>(R.id.label_first)
        val labelSecond = retValue.findViewById<TextView>(R.id.label_second)

        // Get all character fragments.
        val firstPinView = retValue.findViewById<LinearLayout>(R.id.first_pin)
        val secondPinView = retValue.findViewById<LinearLayout>(R.id.second_pin)
        for (loopFragment in getChildFragmentManager().getFragments()) {
            if (loopFragment.getView() == null) continue

            if (loopFragment.getView()?.getParent() === firstPinView) {
                mFirstPin.add(loopFragment as FragmentSecureKeypadChar)
            } else if (loopFragment.getView()?.getParent() === secondPinView) {
                mSecondPin.add(loopFragment as FragmentSecureKeypadChar)
            }
        }

        // Update captions based on operation.
        retValue.findViewById<View?>(R.id.secure_keypad_second_view)
            .setVisibility(if (mChangePin) View.VISIBLE else View.GONE)
        if (mChangePin) {
            labelFirst.setText(R.string.STRING_PIN_CHANGE_LABEL_FIRST)
            labelSecond.setText(R.string.STRING_PIN_CHANGE_LABEL_SECOND)
        } else {
            labelFirst.setText(R.string.STRING_PIN_CHANGE_LABEL_ENTERY_PIN)
        }

        // Get secure keypad builder.
        builder = SecureInputService.create(UiModule.create()).getSecureInputBuilder()

        // Configure secure keypad behavior and visual.
        builder?.setScreenBackgroundColor(getResources().getColor(android.R.color.transparent))
        builder?.setMaximumAndMinimumInputLength(4, 4)
        builder?.setOkButtonBehavior(SecureInputBuilder.OkButtonBehavior.CUSTOM)
        builder?.setButtonPressVisibility(true)
        builder?.setKeypadFrameColor(getResources().getColor(android.R.color.transparent))
        builder?.setButtonPressVisibility(true)
        builder?.setButtonBackgroundColor(
            SecureInputBuilder.UiControlState.NORMAL,
            getResources().getColor(android.R.color.transparent)
        )
        builder?.setKeypadGridGradientColors(
            getResources().getColor(android.R.color.transparent),
            getResources().getColor(android.R.color.black)
        )
        builder?.swapOkAndDeleteButton()
        builder?.setDeleteButtonText("✕")
        builder?.setDeleteButtonGradientColor(
            SecureInputBuilder.UiControlState.NORMAL,
            getResources().getColor(android.R.color.transparent),
            getResources().getColor(android.R.color.transparent)
        )
        builder?.setDeleteButtonGradientColor(
            SecureInputBuilder.UiControlState.DISABLED,
            getResources().getColor(android.R.color.transparent),
            getResources().getColor(android.R.color.transparent)
        )
        builder?.setOkButtonText("✔")
        builder?.setOkButtonGradientColor(
            SecureInputBuilder.UiControlState.NORMAL,
            getResources().getColor(android.R.color.transparent),
            getResources().getColor(android.R.color.transparent)
        )
        builder?.setOkButtonGradientColor(
            SecureInputBuilder.UiControlState.DISABLED,
            getResources().getColor(android.R.color.transparent),
            getResources().getColor(android.R.color.transparent)
        )
        builder?.showTopScreen(false)

        secureInput = builder?.buildKeypad(
            false, mChangePin,
            false, this
        )

        // Add secure keypad into bottom part.
        secureInput?.let {
            getChildFragmentManager().beginTransaction()
                .replace(R.id.secure_keypad_bottom, it.getDialogFragment())
        }
            ?.commit()

        // Add touch listener, so we can switch between inputs
        firstPinView.setOnClickListener(View.OnClickListener { view: View? ->
            secureInput?.selectInputField(0)
            onInputFieldSelected(0)
        })

        secondPinView.setOnClickListener(View.OnClickListener { view: View? ->
            secureInput?.selectInputField(1)
            onInputFieldSelected(1)
        })

        return retValue
    }

    //endregion
    //region SecureKeypadListener
    override fun onKeyPressedCountChanged(newCount: Int, inputField: Int) {
        val list = if (inputField == 0) mFirstPin else mSecondPin

        check(newCount <= list.size) { "SecurePinPad Top View out of bounds" }

        for (loopIndex in list.indices) {
            list.get(loopIndex).isPresent = loopIndex < newCount
        }
    }

    override fun onInputFieldSelected(inputField: Int) {
        for (loopChar in mFirstPin) {
            loopChar.isHighlighted = inputField == 0
        }
        for (loopChar in mSecondPin) {
            loopChar.isHighlighted = inputField == 1
        }
    }

    override fun onOkButtonPressed() {
        // Unused
    }

    override fun onDeleteButtonPressed() {
        // Unused
    }

    override fun onFinish(pinAuthInput: PinAuthInput, pinAuthInput1: PinAuthInput?) {
        // Hide self.
        val mainActivity = getActivity() as MainActivity?
        if (mainActivity != null) {
            mainActivity.hideLastStackFragment()
        }

        // Notify handler
        mHandler?.onSecureInputFinished(pinAuthInput, pinAuthInput1)

        builder?.wipe()
    }

    override fun onError(error: String?) {
        val mainActivity = getActivity() as MainActivity?
        if (mainActivity != null) {
            mainActivity.hideLastStackFragment()
            mainActivity.showErrorIfExists(error)
        }
    } //endregion

    companion object {
        //endregion
        //region Life Cycle
        fun create(
            handler: Protocols.SecureInputHandler,
            changePin: Boolean
        ): FragmentSecureKeypad {
            val retValue = FragmentSecureKeypad()
            retValue.mHandler = handler
            retValue.mChangePin = changePin
            return retValue
        }
    }
}
