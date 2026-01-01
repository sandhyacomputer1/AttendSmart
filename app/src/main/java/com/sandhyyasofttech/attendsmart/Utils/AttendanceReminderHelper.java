package com.sandhyyasofttech.attendsmart.Utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class AttendanceReminderHelper {

    private static final int REQUEST_CODE = 101;

    public static void schedule(Context context, String startTime) {
        try {
            // Check if we can schedule exact alarms
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                    // Request permission to schedule exact alarms
                    Toast.makeText(context, "Please allow scheduling exact alarms for reminders", Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                    context.startActivity(intent);
                    return;
                }
            }

            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.ENGLISH);

            Calendar now = Calendar.getInstance();
            Calendar shift = Calendar.getInstance();
            shift.setTime(sdf.parse(startTime));

            // Set to today's date
            shift.set(Calendar.YEAR, now.get(Calendar.YEAR));
            shift.set(Calendar.MONTH, now.get(Calendar.MONTH));
            shift.set(Calendar.DAY_OF_MONTH, now.get(Calendar.DAY_OF_MONTH));

            // 5 minutes before shift start
            shift.add(Calendar.MINUTE, -5);

            // If the time has already passed today, schedule for tomorrow
            if (shift.before(now) || shift.equals(now)) {
                shift.add(Calendar.DAY_OF_MONTH, 1);
            }

            // Save shift start time in preferences
            PrefManager pref = new PrefManager(context);
            pref.setShiftStartTime(startTime);

            Intent intent = new Intent(context, AttendanceReminderReceiver.class);

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    REQUEST_CODE,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

            if (alarmManager != null) {
                // Cancel any existing alarm first
                alarmManager.cancel(pendingIntent);

                // Schedule new alarm
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            shift.getTimeInMillis(),
                            pendingIntent
                    );
                } else {
                    alarmManager.setExact(
                            AlarmManager.RTC_WAKEUP,
                            shift.getTimeInMillis(),
                            pendingIntent
                    );
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "Failed to schedule reminder", Toast.LENGTH_SHORT).show();
        }
    }

    public static void scheduleNextDay(Context context, String startTime) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.ENGLISH);

            Calendar shift = Calendar.getInstance();
            shift.setTime(sdf.parse(startTime));

            Calendar now = Calendar.getInstance();
            shift.set(Calendar.YEAR, now.get(Calendar.YEAR));
            shift.set(Calendar.MONTH, now.get(Calendar.MONTH));
            shift.set(Calendar.DAY_OF_MONTH, now.get(Calendar.DAY_OF_MONTH));

            // Add one day
            shift.add(Calendar.DAY_OF_MONTH, 1);

            // 5 minutes before shift start
            shift.add(Calendar.MINUTE, -5);

            Intent intent = new Intent(context, AttendanceReminderReceiver.class);

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    REQUEST_CODE,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

            if (alarmManager != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            shift.getTimeInMillis(),
                            pendingIntent
                    );
                } else {
                    alarmManager.setExact(
                            AlarmManager.RTC_WAKEUP,
                            shift.getTimeInMillis(),
                            pendingIntent
                    );
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void cancel(Context context) {
        Intent intent = new Intent(context, AttendanceReminderReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
        }

        // Clear saved shift time
        PrefManager pref = new PrefManager(context);
        pref.setShiftStartTime(null);
    }
}