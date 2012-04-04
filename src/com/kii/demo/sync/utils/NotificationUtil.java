//
//
//  Copyright 2012 Kii Corporation
//  http://kii.com
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.
//  
//

package com.kii.demo.sync.utils;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.kii.demo.sync.R;

public class NotificationUtil {
    private static final String TAG = "NotificationUtil";
    public static final int SYNC_PROGRESS_NOTIFICATION = 111;
    public static final int DOWNLOAD_PROGRESS_NOTIFICATION = 112;

    public static void showSyncProgressNotification(Context context,
            Intent intent) {
        try {
            Log.d(TAG, "showSyncProgressNotification");
            Notification notification = new Notification(
                    R.drawable.status_bar_sync,
                    context.getString(R.string.sync_progress_notification),
                    System.currentTimeMillis());
            PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
                    intent, PendingIntent.FLAG_CANCEL_CURRENT);
            notification.setLatestEventInfo(context,
                    context.getString(R.string.app_name),
                    context.getString(R.string.sync_progress_notification),
                    contentIntent);
            notification.flags |= Notification.FLAG_ONGOING_EVENT;
            NotificationManager nm = (NotificationManager) context
                    .getSystemService(Context.NOTIFICATION_SERVICE);
            nm.notify(SYNC_PROGRESS_NOTIFICATION, notification);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void cancelSyncProgressNotification(Context context) {
        Log.d(TAG, "cancelSyncProgressNotification");
        NotificationManager nm = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(SYNC_PROGRESS_NOTIFICATION);
    }

    
    public static void showDownloadProgressNotification(Context context,
            Intent intent, String title) {
        try {
            Log.d(TAG, "showDownloadProgressNotification");
            Notification notification = new Notification(
                    R.drawable.status_bar_sync,
                    context.getString(R.string.download_progress_notification),
                    System.currentTimeMillis());
            PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
                    intent, PendingIntent.FLAG_CANCEL_CURRENT);
            notification.setLatestEventInfo(context,
                    context.getString(R.string.app_name),
                    title,
                    contentIntent);
            notification.flags |= Notification.FLAG_ONGOING_EVENT;
            NotificationManager nm = (NotificationManager) context
                    .getSystemService(Context.NOTIFICATION_SERVICE);
            nm.notify(DOWNLOAD_PROGRESS_NOTIFICATION, notification);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void cancelDownloadProgressNotification(Context context) {
        Log.d(TAG, "cancelDownloadProgressNotification");
        NotificationManager nm = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(DOWNLOAD_PROGRESS_NOTIFICATION);
    }

}
