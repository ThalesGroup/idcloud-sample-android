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

import android.os.Handler
import android.os.Looper
import android.view.animation.Animation
import android.view.animation.Interpolator
import android.view.animation.Transformation
import android.widget.TextView

/**
 * Circle timer animation.
 */
class CircleTimerAnimation(
    circle: CircleTimer?,
    lifespanText: TextView?,
    lifespanMax: Int,
    lifespanCurrent: Int
) : Animation() {
    //region Defines
    private val mCircle: CircleTimer?
    private val mLifespanText: TextView?

    private val mMaxPercentage: Float
    private val mLifespanStart: Int
    private var mLifespanCurrent = -1

    //endregion
    //region Life Cycle
    /**
     * Creates a new `CircleTimerAnimation` instance.
     *
     * @param circle `CircleTimer` object.
     */
    init {
        // Duration is rest of the time.
        setDuration((lifespanCurrent * 1000).toLong())

        // Make time linear.
        setInterpolator(Interpolator { value: Float -> value })

        mCircle = circle
        mLifespanText = lifespanText
        mMaxPercentage = 360f * (lifespanCurrent.toFloat() / lifespanMax.toFloat())
        mLifespanStart = lifespanCurrent
    }

    //endregion
    //region Timer Tick
    override fun applyTransformation(interpolatedTime: Float, transformation: Transformation?) {
        updateWithInterpolateTime(interpolatedTime)
    }

    private fun updateWithInterpolateTime(time: Float) {
        val revPercentage = (1f - time)
        val remainingTime = (revPercentage * mLifespanStart).toInt()

        mCircle?.degreesUpTillPreFill = mMaxPercentage * revPercentage
        mCircle?.requestLayout()

        if (remainingTime != mLifespanCurrent) {
            Handler(Looper.getMainLooper()).post(Runnable { mLifespanText?.setText((remainingTime + 1).toString() + "s") })
            mLifespanCurrent = remainingTime
        }
    } //endregion
}
