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

package com.gemalto.idp.mobile.authentication.mode.face.ui.internal.utils.logs;


import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

// IMPORTANT: This source code is intended to serve training information purposes only. Please make sure to review our IdCloud documentation, including security guidelines.

/**
 *
 * To summarize the preceding code, the MyLog class is simply wrapping the calls to the android
 * Log class. The rest of your application can simply reference MyLog.d("MyApp", "Sample debug message");
 * and if the device/emulator running the app has the log level set to debug the message will
 * appear. The benefit here is you won't have to worry about removing print lines
 * or maintaining variables for levels that you might forget to change before building
 * your release apk.
 * Changing the log level on device is actually very simple.
 * Simply run : adb shell setprop log.tag.<YOUR_LOG_TAG> <LEVEL>
 *
 * MyLog.init();
 MyLog.setLogLevel(MyLog.ALL);
 MyLog.i(TAG, "onCreate");
 try {
 String filename = Environment.getExternalStorageDirectory() + "/eon_weez_logs.log";
 MyLog.printLogToFile2(filename);
 } catch (IOException e) {
 e.printStackTrace();
 }
 */
public class MyLog {

	// Field descriptor #8 I
	public static final int ALL = 0;
	// Field descriptor #8 I
	public static final int NOTHING = 255;
	// Field descriptor #8 I
	public static final int VERBOSE = 2;
	// Field descriptor #8 I
	public static final int DEBUG = 3;
	// Field descriptor #8 I
	public static final int INFO = 4;
	// Field descriptor #8 I
	public static final int WARN = 5;
	// Field descriptor #8 I
	public static final int ERROR = 6;


	private static int m_logLevel = MyLog.ALL;

	// use the classname for the logger, this way you can refactor
	private final static Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);


	public static void init() {
		LOGGER.setLevel(Level.OFF);
	}

	public static void init(int level) {
		LOGGER.setLevel(Level.OFF);
		setLogLevel(level);
	}

	/**
	 * Set level to MyLog.NOTHING to disable logs
	 */
	public static void setLogLevel(int level)
	{
		m_logLevel = level;
	}

	/**
	 * Basic Log.isLoggable is not secure because you can enable it
	 *
	 * Checks to see whether or not a log for the specified tag is loggable at
	 * the specified level. The default level of any tag is set to INFO.
	 * This means that any level above and including INFO will be logged.
	 * Before you make any calls to a logging method you should check to see
	 * if your tag should be logged. You can change the default level by setting
	 * a system property: 'setprop log.tag.<YOUR_LOG_TAG> <LEVEL>'
	 * Where level is either VERBOSE, DEBUG, INFO, WARN, ERROR, ASSERT, or SUPPRESS.
	 * SUPPRESS will turn off all logging for your tag.
	 * You can also create a local.prop file that with the following in it:
	 * 'log.tag.<YOUR_LOG_TAG>=<LEVEL>' and place that in /data/local.prop.
	 */
	public static boolean isLoggable(String tag, int level)
	{
		return level >= m_logLevel;
		// return Log.isLoggable(tag, level);
	}

	public static boolean isDebug()
	{
		return m_logLevel <= DEBUG;
	}

	public static void v(String tag, String msg) {
		if (MyLog.isLoggable(tag, Log.VERBOSE)) {
			Log.v(tag, msg);
			LOGGER.finest(formatLogger(tag, msg));
		}
	}

	private static String formatLogger(String tag, String msg) {
		int p_Id = android.os.Process.myPid();
		StringBuffer buff = new StringBuffer();
		buff.append(tag);
		buff.append("( ");
		buff.append(p_Id);
		buff.append("): ");
		buff.append(msg);

		//return tag+"( 0): "+msg;
		return buff.toString();
	}


	public static void d(String tag, String msg) {
		if (MyLog.isLoggable(tag, Log.DEBUG)) {
			Log.d(tag, msg);
			LOGGER.fine(formatLogger(tag, msg));
		}
	}

	public static void i(String tag, String msg) {
		if (MyLog.isLoggable(tag, Log.INFO)) {
			Log.i(tag, msg);
			LOGGER.info(formatLogger(tag, msg));
		}
	}

	public static void w(String tag, String msg) {
		if (MyLog.isLoggable(tag, Log.WARN)) {
			Log.w(tag, msg);
			LOGGER.warning(formatLogger(tag, msg));
		}
	}

	public static void e(String tag, String msg) {
		if (MyLog.isLoggable(tag, Log.ERROR)) {
			Log.e(tag, msg);
			LOGGER.severe(formatLogger(tag, msg));
		}
	}

	public static void e(String tag, String msg, Throwable tr) {
		if (MyLog.isLoggable(tag, Log.ERROR)) {
			Log.e(tag, msg, tr);
			LOGGER.severe(formatLogger(tag, msg + " "+tr.toString()));
		}
	}

	public static void printLogToFileRedirect(Context context, String filename){
		//String filename = context.getExternalFilesDir(null).getPath() + File.separator + "my_app.log";
		String command = "logcat -f "+ filename + " -v time -d *:V";

		Log.d("MyLog", "command: " + command);

		try{
			Runtime.getRuntime().exec(command);
		}
		catch(IOException e){
			e.printStackTrace();
		}
	}

	/**
	 *  MyLog.d(TAG, MyLog.getExtraBundleDescription(intent.getExtras()));
	 * @param bundle
	 * @return
	 */
	public static String getExtraBundleDescription(Bundle bundle){
		String log = "";
		for (String key : bundle.keySet()) {
			Object value = bundle.get(key);
			log += String.format("%s=%s (%s)", key,
					value.toString(), value.getClass().getName()) + "\n";
		}
		return log;
	}
}