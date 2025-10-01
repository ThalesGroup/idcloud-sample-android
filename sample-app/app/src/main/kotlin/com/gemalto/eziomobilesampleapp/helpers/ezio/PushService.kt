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
package com.gemalto.eziomobilesampleapp.helpers.ezio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import com.gemalto.eziomobilesampleapp.EzioSampleApp
import com.gemalto.eziomobilesampleapp.MainActivity
import com.gemalto.eziomobilesampleapp.R
import com.gemalto.eziomobilesampleapp.helpers.Main
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Handle of incoming push notifications and token changes.
 */
class PushService : FirebaseMessagingService() {
    //region FirebaseMessagingService
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // Ignore non data notifications.
        if (remoteMessage.getData() == null) {
            return
        }

        val currentActivity = (getApplicationContext() as EzioSampleApp).currentActivity
        if (currentActivity != null) {
            // App is in foreground. Simple process message.
            Main.sharedInstance()?.managerPush?.processIncomingPush(remoteMessage.getData())
        } else {
            sendNotification(remoteMessage.getData())
        }
    }

    override fun onNewToken(token: String) {
        Main.sharedInstance()?.managerPush?.registerToken(token)
    }

    //endregion
    //region Private Helpers
    private fun sendNotification(data: MutableMap<String?, String?>) {
        // First try to get notification manager.

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager?
        if (notificationManager == null) {
            return
        }

        // Intent to run app and pass notification data.
        val notificationIntent = Intent(this, MainActivity::class.java)
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        for (key in data.keys) {
            notificationIntent.putExtra(key, data.get(key))
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = getString(R.string.default_notification_channel_id)
        val message =
            if (data.containsKey(DATA_KEY_MESSAGE)) data.get(DATA_KEY_MESSAGE) else getString(
                R.string.PUSH_APPROVE_QUESTION
            )

        // New android does require channel.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                channelId,
                getString(R.string.PUSH_FCM_CHANNEL_NAME),
                NotificationManager.IMPORTANCE_HIGH
            )

            // Configure the notification channel."MSP Frame Channel"
            notificationChannel.setDescription(getString(R.string.PUSH_FCM_CHANNEL_DESCRIPTION))
            notificationChannel.enableLights(true)
            notificationChannel.setLightColor(Color.RED)
            notificationChannel.setVibrationPattern(longArrayOf(0, 1000, 500, 1000))
            notificationChannel.enableVibration(true)
            notificationManager.createNotificationChannel(notificationChannel)
        }

        // Build final notification.
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
        notificationBuilder.setAutoCancel(true)
            .setDefaults(Notification.DEFAULT_ALL)
            .setWhen(System.currentTimeMillis())
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(message)
            .setContentIntent(pendingIntent)

        notificationManager.notify( /*notification id*/1, notificationBuilder.build())
    } //endregion

    companion object {
        private const val DATA_KEY_MESSAGE = "message"
    }
}