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

import android.R
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

/**
 * Custom circle countdown.
 */
class CircleTimer(context: Context?, attrs: AttributeSet?) : View(context, attrs) {
    //region Defines
    private val mPaint: Paint
    private val mRectF: RectF

    /**
     * Gets the degrees.
     *
     * @return Degrees.
     */
    /**
     * Set degrees.
     *
     * @param degreesUpTillPreFill Degrees.
     */
    // The point up-till which user wants the circle to be pre-filled.
    var degreesUpTillPreFill: Float = 0f

    //endregion
    //region Life Cycle
    /**
     * Creates a new `TimeCircle` instance.
     *
     * @param context Android context.
     * @param attrs   Attributes.
     */
    init {
        val strokeWidth = convertDpIntoPixel(5f)

        mPaint = Paint()
        mPaint.setAntiAlias(true)
        mPaint.setStyle(Paint.Style.STROKE)
        mPaint.setStrokeWidth(strokeWidth)

        // Define the size of the circle
        mRectF = RectF(
            strokeWidth,
            strokeWidth,
            convertDpIntoPixel(170f).toInt() + strokeWidth,
            convertDpIntoPixel(170f).toInt() + strokeWidth
        )
    }

    //endregion
    //region Drawing Routine
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        mPaint.setColor(getResources().getColor(R.color.transparent))
        canvas.drawCircle(
            mRectF.centerX(),
            mRectF.centerY(),
            convertDpIntoPixel(85f).toInt().toFloat(),
            mPaint
        )

        mPaint.setColor(getResources().getColor(com.gemalto.eziomobilesampleapp.R.color.colorTextPrimary))
        canvas.drawArc(
            mRectF,
            STARTING_POINT_IN_DEGREES.toFloat(),
            -this.degreesUpTillPreFill,
            false,
            mPaint
        )
    }

    //endregion
    //region Public API

    //endregion
    //region Private Helpers
    /**
     * Method to convert DPs into Pixels..
     *
     * @param densityIndependentPixel Dp to convert.
     * @return Converted pixels.
     */
    private fun convertDpIntoPixel(densityIndependentPixel: Float): Float {
        val scale = getResources().getDisplayMetrics().density
        return densityIndependentPixel * scale + 0.5f
    } //endregion

    companion object {
        // The point from where the color-fill animation will start.
        private const val STARTING_POINT_IN_DEGREES = 270
    }
}
