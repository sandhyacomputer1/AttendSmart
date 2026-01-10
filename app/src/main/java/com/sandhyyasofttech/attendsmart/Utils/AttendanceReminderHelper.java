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

    private static final int REQUEST_CODE_CHECKIN = 101;
    private static final int REQUEST_CODE_CHECKOUT = 102;

    public static void scheduleCheckinReminder(Context context, String startTime) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
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

            shift.set(Calendar.YEAR, now.get(Calendar.YEAR));
            shift.set(Calendar.MONTH, now.get(Calendar.MONTH));
            shift.set(Calendar.DAY_OF_MONTH, now.get(Calendar.DAY_OF_MONTH));

            // 5 minutes before shift start (CHECK-IN)
            shift.add(Calendar.MINUTE, -5);

            if (shift.before(now) || shift.equals(now)) {
                shift.add(Calendar.DAY_OF_MONTH, 1);
            }

            PrefManager pref = new PrefManager(context);
            pref.setShiftStartTime(startTime);

            Intent intent = new Intent(context, AttendanceReminderReceiver.class);
            intent.putExtra("REMINDER_TYPE", "CHECKIN");

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    REQUEST_CODE_CHECKIN,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null) {
                alarmManager.cancel(pendingIntent);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, shift.getTimeInMillis(), pendingIntent);
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, shift.getTimeInMillis(), pendingIntent);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "Failed to schedule check-in reminder", Toast.LENGTH_SHORT).show();
        }
    }

    public static void scheduleCheckoutReminder(Context context, String endTime) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                    Toast.makeText(context, "Please allow scheduling exact alarms for reminders", Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                    context.startActivity(intent);
                    return;
                }
            }

            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.ENGLISH);
            Calendar now = Calendar.getInstance();
            Calendar shift = Calendar.getInstance();
            shift.setTime(sdf.parse(endTime));

            shift.set(Calendar.YEAR, now.get(Calendar.YEAR));
            shift.set(Calendar.MONTH, now.get(Calendar.MONTH));
            shift.set(Calendar.DAY_OF_MONTH, now.get(Calendar.DAY_OF_MONTH));

            // 5 minutes before shift end (CHECK-OUT)
            shift.add(Calendar.MINUTE, -5);

            if (shift.before(now) || shift.equals(now)) {
                shift.add(Calendar.DAY_OF_MONTH, 1);
            }

            PrefManager pref = new PrefManager(context);
            pref.setShiftEndTime(endTime);

            Intent intent = new Intent(context, AttendanceReminderReceiver.class);
            intent.putExtra("REMINDER_TYPE", "CHECKOUT");

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    REQUEST_CODE_CHECKOUT,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null) {
                alarmManager.cancel(pendingIntent);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, shift.getTimeInMillis(), pendingIntent);
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, shift.getTimeInMillis(), pendingIntent);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "Failed to schedule check-out reminder", Toast.LENGTH_SHORT).show();
        }
    }

    public static void cancelAllReminders(Context context) {
        cancelCheckinReminder(context);
        cancelCheckoutReminder(context);

        PrefManager pref = new PrefManager(context);
        pref.setShiftStartTime(null);
        pref.setShiftEndTime(null);
    }

    private static void cancelCheckinReminder(Context context) {
        Intent intent = new Intent(context, AttendanceReminderReceiver.class);
        intent.putExtra("REMINDER_TYPE", "CHECKIN");
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE_CHECKIN,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }
        try {
            pendingIntent.cancel();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void cancelCheckoutReminder(Context context) {
        Intent intent = new Intent(context, AttendanceReminderReceiver.class);
        intent.putExtra("REMINDER_TYPE", "CHECKOUT");
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE_CHECKOUT,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }
        try {
            pendingIntent.cancel();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Keep old method for backward compatibility
    @Deprecated
    public static void schedule(Context context, String startTime) {
        scheduleCheckinReminder(context, startTime);
    }

    @Deprecated
    public static void cancel(Context context) {
        cancelAllReminders(context);
    }
}
