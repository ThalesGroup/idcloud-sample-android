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

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import com.gemalto.eziomobilesampleapp.R;


/**
 * Custom circle countdown.
 */
public class CircleTimer extends View {

    //region Defines

    private final Paint mPaint;
    private final RectF mRectF;

    // The point from where the color-fill animation will start.
    private static final int STARTING_POINT_IN_DEGREES = 270;
    // The point up-till which user wants the circle to be pre-filled.
    private float mDegreesUpTillPreFill = 0;

    //endregion

    //region Life Cycle

    /**
     * Creates a new {@code TimeCircle} instance.
     *
     * @param context Android context.
     * @param attrs   Attributes.
     */
    public CircleTimer(final Context context, final AttributeSet attrs) {
        super(context, attrs);

        final float strokeWidth = convertDpIntoPixel(5);

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(strokeWidth);

        // Define the size of the circle
        mRectF = new RectF(strokeWidth,
                strokeWidth,
                (int) convertDpIntoPixel(170) + strokeWidth,
                (int) convertDpIntoPixel(170) + strokeWidth);
    }

    //endregion

    //region Drawing Routine

    @Override
    protected void onDraw(final Canvas canvas) {
        super.onDraw(canvas);

        mPaint.setColor(getResources().getColor(android.R.color.transparent));
        canvas.drawCircle(mRectF.centerX(), mRectF.centerY(), (int) convertDpIntoPixel(85), mPaint);

        mPaint.setColor(getResources().getColor(R.color.colorTextPrimary));
        canvas.drawArc(mRectF, STARTING_POINT_IN_DEGREES, -mDegreesUpTillPreFill, false, mPaint);
    }

    //endregion

    //region Public API

    /**
     * Gets the degrees.
     *
     * @return Degrees.
     */
    public float getDegreesUpTillPreFill() {
        return mDegreesUpTillPreFill;
    }

    /**
     * Set degrees.
     *
     * @param degreesUpTillPreFill Degrees.
     */
    public void setDegreesUpTillPreFill(final float degreesUpTillPreFill) {
        mDegreesUpTillPreFill = degreesUpTillPreFill;
    }

    //endregion

    //region Private Helpers

    /**
     * Method to convert DPs into Pixels..
     *
     * @param densityIndependentPixel Dp to convert.
     * @return Converted pixels.
     */
    private float convertDpIntoPixel(final float densityIndependentPixel) {
        final float scale = getResources().getDisplayMetrics().density;
        return densityIndependentPixel * scale + 0.5f;
    }

    //endregion

}
