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

package com.gemalto.idp.mobile.authentication.mode.face.ui.internal.gui.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;

import com.gemalto.idp.mobile.authentication.mode.face.ui.internal.gui.ErrorMode;

import java.util.Timer;
import java.util.TimerTask;

/**
 * A Circular progress view used for Enroll Simple mode and Verify
 */
public class CircularProgressView extends View {
	private final static float FACE_WIDTH_RATIO = 0.7f; //%
	private final static float STROKE_RATIO = 0.08f; //%
	private final static float MARGIN_RATIO = 0.05f; //%

	private final static int COLOR_BORDER = 0xffbebebf;
	private final static int COLOR_VALID_START = 0x0087c447;
	private final static int COLOR_VALID_MIDDLE = 0xa788c548;
	private final static int COLOR_VALID_END = 0xff88c548;


	private final static int COLOR_CURRENT_WARNING = 0xffdebd3a;
	private final static int COLOR_CURRENT_ERROR = 0xffea3634;

	//private final static int NB_STEPS = 4;
	private final static int BLINK_DELAY = 1000;

	private Bitmap m_bitmapBuffer;
	private Canvas m_canvasBuffer;

	private RectF m_rect;
	private Paint m_paintValid;
	private Paint m_paintError;

	private ErrorMode m_currentMode;
	private boolean m_isRunning = false;

	private boolean m_surroundCircle;
	private ErrorMode m_surroundMode = ErrorMode.NONE;

	private int m_NbSteps = 4;


	private float m_angleSteps;
	private RectF m_rectFrame;
	private float m_startAngle;
	private Paint m_paintTransparent;
	private Paint m_paintBorder;
	private Paint m_paintSurround;
	private RectF m_rectSurround;
	private Timer m_blinkTimer;
	private boolean m_blinking;

	private Handler mHandler = new Handler(Looper.getMainLooper());


	public CircularProgressView(Context context, int nbSteps) {
	    super(context);
	    init(context, nbSteps);
	}

	public CircularProgressView(Context context, AttributeSet attrs) {
	    super(context, attrs);
	    initAttr(context, attrs);
	}

	public CircularProgressView(Context context, AttributeSet attrs, int defStyle) {
	    super(context, attrs, defStyle);
	    initAttr(context, attrs);
	}

	private void initAttr(Context context, AttributeSet attrs) {
		int nbSteps = 1;
		init(context, nbSteps);
	}
	private void init(Context context, int nbSteps) {
		m_NbSteps = nbSteps;

		m_currentMode = ErrorMode.NONE;

		m_angleSteps = 360.0f/m_NbSteps;
		m_startAngle = -90.0f;

		m_paintValid = new Paint();

		m_paintValid.setAntiAlias(true);
		m_paintValid.setStyle(Paint.Style.STROKE);


		m_paintBorder = new Paint();
		m_paintBorder.setColor(COLOR_BORDER);
		m_paintBorder.setAntiAlias(true);
		m_paintBorder.setStyle(Paint.Style.STROKE);

		m_paintTransparent = new Paint();
		m_paintTransparent.setColor(getResources().getColor(android.R.color.transparent));
		m_paintTransparent.setXfermode(new PorterDuffXfermode(Mode.CLEAR));
		m_paintTransparent.setAntiAlias(true);
		m_paintTransparent.setStyle(Paint.Style.STROKE);

		m_paintError = new Paint();
		m_paintError.setColor(COLOR_CURRENT_WARNING);
		m_paintError.setAntiAlias(true);
		m_paintError.setStyle(Paint.Style.STROKE);

		m_paintSurround = new Paint();
		m_paintSurround.setColor(COLOR_VALID_START);
		m_paintSurround.setAntiAlias(true);
		m_paintSurround.setStyle(Paint.Style.STROKE);
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
	    super.onSizeChanged(w, h, oldw, oldh);

	    if(m_bitmapBuffer != null) {
	    	m_bitmapBuffer.recycle();
	    	m_bitmapBuffer = null;
	    }
	    m_bitmapBuffer = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
	    m_canvasBuffer = new Canvas(m_bitmapBuffer);

	    m_rectFrame = new RectF(0, 0, w, h);

	    PointF center = new PointF((float)w/2, (float)h/2);

	    float width = (float)w*FACE_WIDTH_RATIO;
	    float strokeBorder = 2;
	    float strokeWidth = width*STROKE_RATIO;
	    width += strokeWidth*2+strokeBorder*2 + MARGIN_RATIO*width;

	    m_rect = new RectF(center.x-width/2+strokeWidth/2, center.y-width/2+strokeWidth/2,
	    		center.x+width/2-strokeWidth/2, center.y+width/2-strokeWidth/2);

	    m_paintValid.setStrokeWidth(strokeWidth);
	    m_paintError.setStrokeWidth(strokeWidth);
	    m_paintTransparent.setStrokeWidth(strokeWidth);
	    m_paintBorder.setStrokeWidth(strokeWidth+strokeBorder*2);

	    final float to = m_angleSteps / 360.0f;
	    final float[] positions = {0, to/2, to, 1};

	    int start = COLOR_VALID_START;
	    int end = COLOR_VALID_END;
	    int[] colors = {start, COLOR_VALID_MIDDLE, end, start};
		//SweepGradient gradient = new SweepGradient (0, width/2, Color.RED, Color.BLUE);
		SweepGradient gradient = new SweepGradient(w/2, h/2, colors , positions);
		m_paintValid.setShader(gradient);


		float strokeSurround = strokeWidth/4;
		float widthSurround = (float)w*FACE_WIDTH_RATIO;
		m_paintSurround.setStrokeWidth(strokeSurround);

		m_rectSurround = new RectF(center.x-widthSurround/2, center.y-widthSurround/2,
		    		center.x+widthSurround/2, center.y+widthSurround/2);
	}

	@Override
	public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
	    super.onMeasure(widthMeasureSpec, widthMeasureSpec);
	}

	public void setCurrentMode(ErrorMode mode) {
		m_currentMode = mode;
			invalidate();
	}

	private void startAnnimate() {
		RotateAnimation anim = new RotateAnimation(0, 360, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
		anim.setInterpolator(new LinearInterpolator());
		anim.setDuration(4000);
		anim.setRepeatCount(Animation.INFINITE);
		anim.setFillEnabled(true);
		anim.setFillAfter(true);

		anim.setRepeatMode(Animation.RESTART);
		startAnimation(anim);
	}

	private void stopAnnimate() {
		clearAnimation();
	}


	public void setProgress(boolean isRunning, ErrorMode mode) {
		if(m_isRunning != isRunning || m_currentMode != mode) {
			m_currentMode = mode;
			invalidate();

			if(m_isRunning != isRunning) {
				if(isRunning) {
					startAnnimate();
				}
				else {
					stopAnnimate();
				}
			}
			m_isRunning = isRunning;

		}
	}

	public boolean getIsRunning() {
		return m_isRunning;
	}
	public ErrorMode getCurrentErrorMode() {
		return m_currentMode;
	}

	@Override
	public void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		float rotation = (m_angleSteps);
		m_canvasBuffer.drawColor(Color.TRANSPARENT, Mode.CLEAR); // clear Bitmap

		if(m_surroundCircle) {
			m_paintSurround.setColor(getSurroundColorFromMode());
			m_canvasBuffer.drawOval(m_rectSurround, m_paintSurround); // clear Bitmap
		}

		if(m_isRunning) {
			m_canvasBuffer.rotate(m_startAngle, m_rectFrame.width() / 2, m_rectFrame.height() / 2);

			for (int i = 0; i < m_NbSteps; i++) {
				if (i != 0) {
					m_canvasBuffer.rotate(rotation, m_rectFrame.width() / 2, m_rectFrame.height() / 2);
				}

				if (m_currentMode == ErrorMode.NONE) {
					m_canvasBuffer.drawArc(m_rect, 0, m_angleSteps, false, m_paintValid);
				} else {
					int color = (m_currentMode == ErrorMode.WARNING) ? COLOR_CURRENT_WARNING : COLOR_CURRENT_ERROR;
					m_paintError.setColor(color);
					m_canvasBuffer.drawArc(m_rect, 0, m_angleSteps, false, m_paintError);//m_paintError);
				}
			}

			m_canvasBuffer.rotate(-m_startAngle-rotation*(m_NbSteps-1), m_rectFrame.width()/2, m_rectFrame.height()/2);
		}

		canvas.drawBitmap(m_bitmapBuffer, 0, 0, null);

	}

	private int getSurroundColorFromMode() {
		if(m_surroundMode == ErrorMode.NONE) { // Valid
			return COLOR_VALID_END;
		}
		else if(m_surroundMode == ErrorMode.ERROR) { // ERROR
			return COLOR_CURRENT_ERROR;
		}
		else if(m_surroundMode == ErrorMode.DISABLED) { // DISABLED
			return COLOR_BORDER;
		}
		return COLOR_BORDER;
	}


	final float getAngle(float angle) {
		float ret = angle;
		if(ret<0.0f) {
			return ret+360;
		}
		else {
			return ret;
		}
	}

	public boolean getSurroundMode() {
		return m_surroundCircle;
	}

	public void setSurroundMode(boolean active, ErrorMode error) {
		boolean bChanges = this.m_surroundCircle != active || this.m_surroundMode != error;
		this.m_surroundCircle = active;
		this.m_surroundMode  = error;
		if(bChanges) {
			invalidate();
		}
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
						m_blinking = !m_blinking;
						invalidate();
					}
				});
			}
		}, BLINK_DELAY, BLINK_DELAY);
	}

}