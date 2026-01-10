package com.sandhyyasofttech.attendsmart.Utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.sandhyyasofttech.attendsmart.Activities.EmployeeDashboardActivity;
import com.sandhyyasofttech.attendsmart.R;

public class AttendanceReminderReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "attendance_reminder_channel";
    private static final String CHANNEL_NAME = "Attendance Reminders";
    private static final int NOTIFICATION_ID_CHECKIN = 1001;
    private static final int NOTIFICATION_ID_CHECKOUT = 1002;

    @Override
    public void onReceive(Context context, Intent intent) {
        createNotificationChannel(context);

        String reminderType = intent.getStringExtra("REMINDER_TYPE");
        showNotification(context, reminderType);

        // Reschedule both reminders for next day
        rescheduleForNextDay(context);
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Reminders for attendance check-in and check-out");
            channel.enableVibration(true);
            channel.enableLights(true);

            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void showNotification(Context context, String reminderType) {
        String title, message;
        int notificationId;

        if ("CHECKOUT".equals(reminderType)) {
            title = "⏰ Time to Check-Out!";
            message = "Your shift is ending in 5 minutes! Don't forget to mark your attendance out.";
            notificationId = NOTIFICATION_ID_CHECKOUT;
        } else { // CHECKIN (default)
            title = "⏰ Time to Check-In!";
            message = "Your shift starts in 5 minutes! Don't forget to punch in.";
            notificationId = NOTIFICATION_ID_CHECKIN;
        }

        Intent intent = new Intent(context, EmployeeDashboardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.salarylogo)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setVibrate(new long[]{1000, 1000, 1000})
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message));

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationManager != null) {
            notificationManager.notify(notificationId, builder.build());
        }
    }

    private void rescheduleForNextDay(Context context) {
        PrefManager pref = new PrefManager(context);
        boolean notificationsEnabled = pref.getNotificationsEnabled();

        if (notificationsEnabled) {
            String shiftStartTime = pref.getShiftStartTime();
            String shiftEndTime = pref.getShiftEndTime();

            if (shiftStartTime != null && !shiftStartTime.isEmpty()) {
                AttendanceReminderHelper.scheduleCheckinReminder(context, shiftStartTime);
            }

            if (shiftEndTime != null && !shiftEndTime.isEmpty()) {
                AttendanceReminderHelper.scheduleCheckoutReminder(context, shiftEndTime);
            }
        }
    }
}
