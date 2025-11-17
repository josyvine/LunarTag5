package com.lunartag.app.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.lunartag.app.services.SendService;

/**
 * A utility class to handle scheduling photo sends using the AlarmManager.
 */
public class Scheduler {

    private static final String TAG = "Scheduler";

    /**
     * Schedules an exact alarm to trigger the SendService for a specific photo.
     * @param context The application context.
     * @param photoId A unique identifier for the photo (e.g., its local database ID).
     * @param filePath The absolute path to the photo file to be sent.
     * @param scheduledTimeMillis The exact time in milliseconds when the send should be triggered.
     */
    public static void schedulePhotoSend(Context context, long photoId, String filePath, long scheduledTimeMillis) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            Log.e(TAG, "AlarmManager is null. Cannot schedule send.");
            return;
        }

        Intent intent = new Intent(context, SendService.class);
        intent.putExtra(SendService.EXTRA_FILE_PATH, filePath);

        // We use the photoId as the request code for the PendingIntent. This ensures
        // that each photo has a unique alarm. Using FLAG_IMMUTABLE is required for newer Android versions.
        int requestCode = (int) photoId;
        PendingIntent pendingIntent = PendingIntent.getService(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Check if we have permission to schedule exact alarms.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.e(TAG, "Cannot schedule exact alarms. The app needs the SCHEDULE_EXACT_ALARM permission.");
                // In a real app, you would guide the user to grant this permission.
                // For now, we will attempt to set a less precise alarm as a fallback.
                alarmManager.set(AlarmManager.RTC_WAKEUP, scheduledTimeMillis, pendingIntent);
                return;
            }
        }

        // Schedule the exact alarm. This will wake the device up from doze mode.
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, scheduledTimeMillis, pendingIntent);
        Log.d(TAG, "Scheduled send for photo ID " + photoId + " at " + scheduledTimeMillis);
    }

    /**
     * Cancels a previously scheduled alarm for a photo.
     * @param context The application context.
     * @param photoId The unique ID of the photo whose alarm should be canceled.
     */
    public static void cancelPhotoSend(Context context, long photoId) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }

        Intent intent = new Intent(context, SendService.class);
        int requestCode = (int) photoId;
        PendingIntent pendingIntent = PendingIntent.getService(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
        );

        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
            Log.d(TAG, "Canceled scheduled send for photo ID " + photoId);
        }
    }
                }
