/*
 *
 * MIT License
 *
 * Copyright (c) 2019 Thales DIS
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
 */

package com.gemalto.idp.mobile.authentication.mode.face.ui.internal.gui.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import com.gemalto.idp.mobile.authentication.mode.face.ui.R;

// IMPORTANT: This source code is intended to serve training information purposes only. Please make sure to review our IdCloud documentation, including security guidelines.

/**
 * A view that crop an image circularly to give impression of a circular video display
 */
public class ClippingView extends View {
	//private final static float CLIP_RADIUS = 150; // dp
	private final static float WIDTH_RATIO = 0.7f;

	private Bitmap bitmapBuffer;
	private Canvas canvasBuffer;
	private Bitmap backgroundDrawable;
	private Paint transparentPaint;
	private float m_cx;
	private float m_cy;
	private float m_radius;

	private Rect m_rectDrawableSrc;

	private Rect m_rectDrawableDest;
	
	public ClippingView(Context context) {
	    super(context);
	    init(context);
	}

	public ClippingView(Context context, AttributeSet attrs) {
	    super(context, attrs);
	    init(context);
	}

	public ClippingView(Context context, AttributeSet attrs, int defStyle) {
	    super(context, attrs, defStyle);
	    init(context);
	}

	private void init(Context context) {
		backgroundDrawable = getBackgroundBitmap();
		
		transparentPaint = new Paint();
		transparentPaint.setColor(getResources().getColor(android.R.color.transparent));
		transparentPaint.setXfermode(new PorterDuffXfermode(Mode.CLEAR));
		transparentPaint.setAntiAlias(true);
		
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
	    super.onSizeChanged(w, h, oldw, oldh);
	    
	    if(bitmapBuffer != null) {
	    	bitmapBuffer.recycle();
	    	bitmapBuffer = null;
	    }
	    
	    bitmapBuffer = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
	    bitmapBuffer.eraseColor(Color.TRANSPARENT);
	    canvasBuffer = new android.graphics.Canvas(bitmapBuffer);
	    
	    int paddBottom = getPaddingBottom();
	    
	    m_cx = w/2;
	    m_cy = h/2-paddBottom/2;
	    
		m_radius = WIDTH_RATIO*w/2;

		m_rectDrawableSrc = new Rect(0, 0, backgroundDrawable.getWidth()-1, backgroundDrawable.getHeight()-1);
		m_rectDrawableDest = new Rect(0, 0, w-1, h-1);
	}
	
	@Override
	public void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		
		canvasBuffer.drawBitmap(backgroundDrawable, m_rectDrawableSrc, m_rectDrawableDest, null);
		canvasBuffer.drawCircle(m_cx, m_cy, m_radius, transparentPaint);
		canvas.drawBitmap(bitmapBuffer, 0, 0, null);
	}
	
	private Bitmap getBackgroundBitmap() {
		Bitmap bmp = BitmapFactory.decodeResource(getContext().getResources(),
	            R.drawable.background_process);
		return bmp;
	}
}