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

package com.gemalto.eziomobilesampleapp.helpers.ezio;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.support.v4.app.NotificationCompat;

import com.gemalto.eziomobilesampleapp.EzioSampleApp;
import com.gemalto.eziomobilesampleapp.MainActivity;
import com.gemalto.eziomobilesampleapp.R;
import com.gemalto.eziomobilesampleapp.helpers.Main;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;


/**
 * Handle of incoming push notifications and token changes.
 */
public class PushService extends FirebaseMessagingService {

    private static final String DATA_KEY_MESSAGE = "message";

    //region FirebaseMessagingService

    @Override
    public void onMessageReceived(final RemoteMessage remoteMessage) {
        // Ignore non data notifications.
        if (remoteMessage.getData() == null) {
            return;
        }

        final Activity currentActivity = ((EzioSampleApp) getApplicationContext()).getCurrentActivity();
        if (currentActivity != null) {
            // App is in foreground. Simple process message.
            Main.sharedInstance().getManagerPush().processIncomingPush(remoteMessage.getData());
        } else {
            sendNotification(remoteMessage.getData());
        }
    }

    @Override
    public void onNewToken(final String token) {
        Main.sharedInstance().getManagerPush().registerToken(token);
    }

    //endregion

    //region Private Helpers

    private void sendNotification(final Map<String, String> data) {

        // First try to get notification manager.
        final NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) {
            return;
        }

        // Intent to run app and pass notification data.
        final Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        for (final String key : data.keySet()) {
            notificationIntent.putExtra(key, data.get(key));
        }
        final PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        final String channelId = getString(R.string.default_notification_channel_id);
        final String message = data.containsKey(DATA_KEY_MESSAGE) ? data.get(DATA_KEY_MESSAGE) : getString(R.string.PUSH_APPROVE_QUESTION);

        // New android does require channel.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            final NotificationChannel notificationChannel = new NotificationChannel(channelId, getString(R.string.PUSH_FCM_CHANNEL_NAME), NotificationManager.IMPORTANCE_HIGH);

            // Configure the notification channel."MSP Frame Channel"
            notificationChannel.setDescription(getString(R.string.PUSH_FCM_CHANNEL_DESCRIPTION));
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.RED);
            notificationChannel.setVibrationPattern(new long[]{0, 1000, 500, 1000});
            notificationChannel.enableVibration(true);
            notificationManager.createNotificationChannel(notificationChannel);
        }

        // Build final notification.
        final NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, channelId);
        notificationBuilder.setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_ALL)
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(message)
                .setContentIntent(pendingIntent);

        notificationManager.notify(/*notification id*/1, notificationBuilder.build());
    }

    //endregion
}