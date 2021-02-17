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

import android.os.Handler;
import android.os.Looper;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.TextView;


/**
 * Circle timer animation.
 */
public class CircleTimerAnimation extends Animation {

    //region Defines

    private final CircleTimer mCircle;
    private final TextView mLifespanText;

    private final float mMaxPercentage;
    private final int mLifespanStart;
    private int mLifespanCurrent = -1;

    //endregion

    //region Life Cycle

    /**
     * Creates a new {@code CircleTimerAnimation} instance.
     *
     * @param circle {@code CircleTimer} object.
     */
    public CircleTimerAnimation(
            CircleTimer circle,
            TextView lifespanText,
            int lifespanMax,
            int lifespanCurrent
    ) {
        super();

        // Duration is rest of the time.
        setDuration(lifespanCurrent * 1000);

        // Make time linear.
        setInterpolator(value -> value);

        mCircle = circle;
        mLifespanText = lifespanText;
        mMaxPercentage = 360.f * ((float) lifespanCurrent / (float) lifespanMax);
        mLifespanStart = lifespanCurrent;
    }

    //endregion

    //region Timer Tick

    @Override
    protected void applyTransformation(float interpolatedTime, Transformation transformation) {
        updateWithInterpolateTime(interpolatedTime);
    }

    private void updateWithInterpolateTime(float time) {
        final float revPercentage = (1.f - time);
        final int remainingTime = (int) (revPercentage * mLifespanStart);

        mCircle.setDegreesUpTillPreFill(mMaxPercentage * revPercentage);
        mCircle.requestLayout();

        if (remainingTime != mLifespanCurrent) {
            new Handler(Looper.getMainLooper()).post(() -> mLifespanText.setText((remainingTime + 1) + "s"));
            mLifespanCurrent = remainingTime;
        }
    }

    //endregion

}
