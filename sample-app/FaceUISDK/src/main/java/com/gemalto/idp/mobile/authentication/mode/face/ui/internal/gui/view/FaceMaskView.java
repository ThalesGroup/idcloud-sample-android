/**
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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import java.util.Timer;
import java.util.TimerTask;

// IMPORTANT: This source code is intended to serve training information purposes only. Please make sure to review our IdCloud documentation, including security guidelines.

/**
 *  A view used for FullEnrollMode to show user head position with an overlay on top of the video stream
 */
public class FaceMaskView extends View {
	private final static float FACE_MARGIN_RATIO = 0.05f; // %
	private final static float FACE_HEIGHT_RATIO = 0.60f; // %
	private final static float FACE_WIDTH_RATIO = 0.8f;
	
	private final static int COLOR_MASK = 0x88555555;
	private Bitmap m_bitmapBuffer;
	private Canvas m_canvasBuffer;

	private Paint m_transparentPaint;
	private RectF m_oval;
	
	@SuppressLint("RtlHardcoded")
	public enum MaskMode {
	    LEFT, TOP,
		RIGHT, BOTTOM
	}
	
	private final static int BLINK_DELAY = 1000;
	private Timer m_blinkTimer;
	private Handler mHandler = new Handler(Looper.getMainLooper());
	
	private MaskMode m_maskMode;
	
	public FaceMaskView(Context context) {
	    super(context);
	    init(context);
	}

	public FaceMaskView(Context context, AttributeSet attrs) {
	    super(context, attrs);
	    init(context);
	}

	public FaceMaskView(Context context, AttributeSet attrs, int defStyle) {
	    super(context, attrs, defStyle);
	    init(context);
	}

	private void init(Context context) {
		m_maskMode = MaskMode.TOP;
		
		m_transparentPaint = new Paint();
		m_transparentPaint.setColor(getResources().getColor(android.R.color.transparent));
		m_transparentPaint.setXfermode(new PorterDuffXfermode(Mode.CLEAR));
		m_transparentPaint.setAntiAlias(true);
		
		m_oval = new RectF();
	}
	
	public void setMaskMode(MaskMode mode) {
		if(((m_maskMode == MaskMode.LEFT || m_maskMode == MaskMode.RIGHT) 
				&& (mode == MaskMode.TOP || mode == MaskMode.BOTTOM))
			|| ((m_maskMode == MaskMode.TOP || m_maskMode == MaskMode.BOTTOM) 
				&& (mode == MaskMode.LEFT || mode == MaskMode.RIGHT)) ){
			m_maskMode = mode;
			invalidate();
		}
		
	}
	
	public MaskMode getMaskMode() {
		return m_maskMode;
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
	    super.onSizeChanged(w, h, oldw, oldh);
	    
	    if(m_bitmapBuffer != null) {
	    	m_bitmapBuffer.recycle();
	    	m_bitmapBuffer = null;
	    }
	    m_bitmapBuffer = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
	    m_bitmapBuffer.eraseColor(Color.TRANSPARENT);
	    m_canvasBuffer = new android.graphics.Canvas(m_bitmapBuffer);
	}
	
	@Override
	public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
	    super.onMeasure(widthMeasureSpec, widthMeasureSpec);
	}
	
	@Override
	public void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		
		int w = getWidth();
		int h = getHeight();
		
		Resources r = getResources();
	    final float faceMaskHeight = FACE_HEIGHT_RATIO * w;
	    final float ovalH = faceMaskHeight;//TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, FACE_HEIGHT, r.getDisplayMetrics());
	    final float ovalW = ovalH*FACE_WIDTH_RATIO;
	    
	    final float faceMargin = FACE_MARGIN_RATIO * ovalH;
	    final float borderMargin = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, faceMargin, r.getDisplayMetrics());
	    
	    
		float cx, cy;
		cx = cy = 0;
		if(m_maskMode == MaskMode.TOP) {
			cx = w/2;
			cy = 0 + ovalH/2 + borderMargin;
		}
		else if(m_maskMode == MaskMode.LEFT) {
			cx = ovalW/2 + borderMargin;
			cy = h/2;
		}
		else if(m_maskMode == MaskMode.RIGHT) {
			cx = w - ovalW/2 - borderMargin;
			cy = h/2;
		}
		else if(m_maskMode == MaskMode.BOTTOM) {
			cx = w/2;
			cy = h - ovalH/2 - borderMargin;
		}
		m_oval.set(cx-ovalW/2, cy-ovalH/2, cx+ovalW/2, cy+ovalH/2);
				
		// Draw
		m_canvasBuffer.drawColor(Color.TRANSPARENT, Mode.CLEAR); // clear Bitmap
		
		m_canvasBuffer.drawColor(COLOR_MASK);
		
		m_canvasBuffer.drawOval(m_oval, m_transparentPaint);

		canvas.drawBitmap(m_bitmapBuffer, 0, 0, null);
		
	}
	
	@Override
	protected void onAttachedToWindow() {
		startBlinkTimer();
		super.onAttachedToWindow();
	}

	@Override
	protected void onDetachedFromWindow() {
		cancelBlinkTimer();
		super.onDetachedFromWindow();
	}
	
	protected void cancelBlinkTimer() {
		if(m_blinkTimer != null) {
			m_blinkTimer.cancel();
			m_blinkTimer = null;
		}
	}
	
	protected void startBlinkTimer() {
		cancelBlinkTimer();
		m_blinkTimer = new Timer();
		m_blinkTimer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				mHandler.post(new Runnable() {
					
					@Override
					public void run() {
						if(m_maskMode == MaskMode.TOP) {
							m_maskMode = MaskMode.BOTTOM;
						}
						else if(m_maskMode == MaskMode.LEFT) {
							m_maskMode = MaskMode.RIGHT;
						}
						else if(m_maskMode == MaskMode.RIGHT) {
							m_maskMode = MaskMode.LEFT;
						}
						else if(m_maskMode == MaskMode.BOTTOM) {
							m_maskMode = MaskMode.TOP;
						}
						invalidate();
					}
				});
			}
		}, BLINK_DELAY, BLINK_DELAY);
	}
}