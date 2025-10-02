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
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.gemalto.eziomobilesampleapp.MainActivity;
import com.gemalto.eziomobilesampleapp.R;
import com.gemalto.eziomobilesampleapp.helpers.Protocols;
import com.gemalto.idp.mobile.authentication.mode.pin.PinAuthInput;
import com.gemalto.idp.mobile.ui.UiModule;
import com.gemalto.idp.mobile.ui.secureinput.SecureInputBuilder;
import com.gemalto.idp.mobile.ui.secureinput.SecureInputService;
import com.gemalto.idp.mobile.ui.secureinput.SecureInputUi;
import com.gemalto.idp.mobile.ui.secureinput.SecureKeypadListener;

import java.util.ArrayList;
import java.util.List;

public class FragmentSecureKeypad extends Fragment implements SecureKeypadListener {

    //region Defines

    private final List<FragmentSecureKeypadChar> mFirstPin = new ArrayList<>();
    private final List<FragmentSecureKeypadChar> mSecondPin = new ArrayList<>();

    private Protocols.SecureInputHandler mHandler;
    private SecureInputUi secureInput;
    private SecureInputBuilder builder;
    private boolean mChangePin;

    //endregion

    //region Life Cycle
    public static FragmentSecureKeypad create(
            @NonNull final Protocols.SecureInputHandler handler,
            final boolean changePin
    ) {
        final FragmentSecureKeypad retValue = new FragmentSecureKeypad();
        retValue.mHandler = handler;
        retValue.mChangePin = changePin;
        return retValue;
    }

    @Nullable
    @Override
    @SuppressLint("InflateParams")
    public View onCreateView(
            @NonNull final LayoutInflater inflater,
            @Nullable final ViewGroup container,
            @Nullable final Bundle savedInstanceState
    ) {
        final View retValue = inflater.inflate(R.layout.fragment_secure_keypad, null);

        final TextView labelFirst = retValue.findViewById(R.id.label_first);
        final TextView labelSecond = retValue.findViewById(R.id.label_second);

        // Get all character fragments.
        final LinearLayout firstPinView = retValue.findViewById(R.id.first_pin);
        final LinearLayout secondPinView = retValue.findViewById(R.id.second_pin);
        for (final Fragment loopFragment : getChildFragmentManager().getFragments()) {
            if (loopFragment.getView() == null) continue;

            if (loopFragment.getView().getParent() == firstPinView) {
                mFirstPin.add((FragmentSecureKeypadChar) loopFragment);
            } else if (loopFragment.getView().getParent() == secondPinView) {
                mSecondPin.add((FragmentSecureKeypadChar) loopFragment);
            }
        }

        // Update captions based on operation.
        retValue.findViewById(R.id.secure_keypad_second_view).setVisibility(mChangePin ? View.VISIBLE : View.GONE);
        if (mChangePin) {
            labelFirst.setText(R.string.STRING_PIN_CHANGE_LABEL_FIRST);
            labelSecond.setText(R.string.STRING_PIN_CHANGE_LABEL_SECOND);
        } else {
            labelFirst.setText(R.string.STRING_PIN_CHANGE_LABEL_ENTERY_PIN);
        }

        // Get secure keypad builder.
        builder = SecureInputService.create(UiModule.create()).getSecureInputBuilder();

        // Configure secure keypad behavior and visual.
        builder.setScreenBackgroundColor(getResources().getColor(android.R.color.transparent));
        builder.setMaximumAndMinimumInputLength(4, 4);
        builder.setOkButtonBehavior(SecureInputBuilder.OkButtonBehavior.CUSTOM);
        builder.setButtonPressVisibility(true);
        builder.setKeypadFrameColor(getResources().getColor(android.R.color.transparent));
        builder.setButtonPressVisibility(true);
        builder.setButtonBackgroundColor(SecureInputBuilder.UiControlState.NORMAL,
                getResources().getColor(android.R.color.transparent));
        builder.setKeypadGridGradientColors(getResources().getColor(android.R.color.transparent),
                getResources().getColor(android.R.color.black));
        builder.swapOkAndDeleteButton();
        builder.setDeleteButtonText("✕");
        builder.setDeleteButtonGradientColor(SecureInputBuilder.UiControlState.NORMAL,
                getResources().getColor(android.R.color.transparent),
                getResources().getColor(android.R.color.transparent));
        builder.setDeleteButtonGradientColor(SecureInputBuilder.UiControlState.DISABLED,
                getResources().getColor(android.R.color.transparent),
                getResources().getColor(android.R.color.transparent));
        builder.setOkButtonText("✔");
        builder.setOkButtonGradientColor(SecureInputBuilder.UiControlState.NORMAL,
                getResources().getColor(android.R.color.transparent),
                getResources().getColor(android.R.color.transparent));
        builder.setOkButtonGradientColor(SecureInputBuilder.UiControlState.DISABLED,
                getResources().getColor(android.R.color.transparent),
                getResources().getColor(android.R.color.transparent));
        builder.showTopScreen(false);

        secureInput = builder.buildKeypad(false, mChangePin,
                false, this);

        // Add secure keypad into bottom part.
        getChildFragmentManager().beginTransaction()
                .replace(R.id.secure_keypad_bottom, secureInput.getDialogFragment())
                .commit();

        // Add touch listener, so we can switch between inputs
        firstPinView.setOnClickListener(view -> {
            secureInput.selectInputField(0);
            onInputFieldSelected(0);
        });

        secondPinView.setOnClickListener(view -> {
            secureInput.selectInputField(1);
            onInputFieldSelected(1);
        });

        return retValue;
    }

    //endregion

    //region SecureKeypadListener

    @Override
    public void onKeyPressedCountChanged(final int newCount, final int inputField) {
        final List<FragmentSecureKeypadChar> list = inputField == 0 ? mFirstPin : mSecondPin;

        if (newCount > list.size()) {
            throw new IllegalStateException("SecurePinPad Top View out of bounds");
        }

        for (int loopIndex = 0; loopIndex < list.size(); loopIndex++) {
            list.get(loopIndex).setPresent(loopIndex < newCount);
        }
    }

    @Override
    public void onInputFieldSelected(final int inputField) {
        for (final FragmentSecureKeypadChar loopChar : mFirstPin) {
            loopChar.setHighlighted(inputField == 0);
        }
        for (final FragmentSecureKeypadChar loopChar : mSecondPin) {
            loopChar.setHighlighted(inputField == 1);
        }
    }

    @Override
    public void onOkButtonPressed() {
        // Unused
    }

    @Override
    public void onDeleteButtonPressed() {
        // Unused
    }

    @Override
    public void onFinish(final PinAuthInput pinAuthInput, final PinAuthInput pinAuthInput1) {
        // Hide self.
        final MainActivity mainActivity = (MainActivity) getActivity();
        if (mainActivity != null) {
            mainActivity.hideLastStackFragment();
        }

        // Notify handler
        mHandler.onSecureInputFinished(pinAuthInput, pinAuthInput1);

        builder.wipe();
    }

    @Override
    public void onError(final String error) {
        final MainActivity mainActivity = (MainActivity) getActivity();
        if (mainActivity != null) {
            mainActivity.hideLastStackFragment();
            mainActivity.showErrorIfExists(error);
        }
    }

    //endregion

}
