package com.sandhyyasofttech.attendsmart.Utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            // Reschedule alarms after device restart
            PrefManager pref = new PrefManager(context);

            boolean notificationsEnabled = pref.getNotificationsEnabled();
            String shiftStartTime = pref.getShiftStartTime();

            if (notificationsEnabled && shiftStartTime != null && !shiftStartTime.isEmpty()) {
                AttendanceReminderHelper.schedule(context, shiftStartTime);
            }
        }
    }
}