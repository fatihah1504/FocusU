package com.example.focusu;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class NotificationHelper {

    private static final String CHANNEL_ID = "FocusU_Channel";
    private static final String CHANNEL_NAME = "FocusU Reminders";
    private static final String CHANNEL_DESC = "Notifications for upcoming assignments and exams";


    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription(CHANNEL_DESC);

            // Register the channel with the system
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    /**
     * Builds and displays a notification.
     * @param context The context from which this is called.
     * @param title The title of the notification.
     * @param content The main text content of the notification.
     * @param notificationId A unique ID for this notification to prevent them from overwriting each other.
     */
    public static void showNotification(Context context, String title, String content, int notificationId) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notifications) // IMPORTANT: You need an icon named 'ic_notification' in your res/drawable folder
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true); // Automatically removes the notification when the user taps it

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            notificationManager.notify(notificationId, builder.build());
        }
    }
}
