package com.dailylist.vadimsemenyk.natives.Notifications;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;

import com.dailylist.vadimsemenyk.R;
import com.dailylist.vadimsemenyk.natives.App;
import com.dailylist.vadimsemenyk.natives.Enums.NoteRepeatTypes;
import com.dailylist.vadimsemenyk.natives.Enums.NoteTypes;
import com.dailylist.vadimsemenyk.natives.Helpers.DateHelper;
import com.dailylist.vadimsemenyk.natives.Models.Note;
import com.dailylist.vadimsemenyk.natives.Repositories.NoteRepository;

import java.util.ArrayList;
import java.util.Calendar;

public class Notifications {
    static String CHANNEL_ID = "com.dailylist.vadimsemenyk.notification";
    static String CHANNEL_NAME = "Daily List";

    static String ACTION_SHOW = "com.dailylist.vadimsemenyk.notification.show";
    static String ACTION_FINISH = "com.dailylist.vadimsemenyk.notification.finish";
    static String ACTION_OPEN_NOTE = "com.dailylist.vadimsemenyk.notification.open_note";

    static String EXTRA_ID = "id";
    static String EXTRA_NOTES = "notes";
    static String EXTRA_SHOULD_RESCHEDULE = "should_reschedule";

    public Notifications() { }

    // region schedule notification
    static public void schedule(int id, boolean isRepeatReschedule) {
        TriggerOptions trigger = NoteRepository.getInstance().getNextNotificationTrigger(id, isRepeatReschedule);
        if (trigger == null) {
            return;
        }

        Intent intent = new Intent(App.getAppContext(), NotificationsReceiver.class);
        intent.setAction(getShowActionName(id));
        intent.putExtra(EXTRA_ID, trigger.id);
        intent.putExtra(EXTRA_NOTES, TextUtils.join(",", trigger.noteIDs));
        intent.putExtra(EXTRA_SHOULD_RESCHEDULE, trigger.shouldReschedule);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(App.getAppContext(), 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);

        AlarmManager alarmManager = (AlarmManager) App.getAppContext().getSystemService(Context.ALARM_SERVICE);
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger.dateTime.getTimeInMillis(), pendingIntent);
            } else {
                alarmManager.setExact(AlarmManager.RTC, trigger.dateTime.getTimeInMillis(), pendingIntent);
            }
        } catch (Exception ignore) {
            // Samsung devices have a known bug where a 500 alarms limit
            // can crash the app
        }
    }


    static public void scheduleAll() {
        String searchStartDateMsUTC = Long.toString(DateHelper.convertFromLocalToUTC(DateHelper.startOf(Calendar.getInstance(), "day")).getTimeInMillis());

        ArrayList<Note> notes = NoteRepository.getInstance().queryNotes(
                "SELECT id FROM Notes"
                        + " WHERE ((repeatType = ? AND isNotificationEnabled = ? AND date >= ?)"
                        + " OR (repeatType != ? AND (repeatEndDate IS NULL OR repeatEndDate >= ?)))"
                        + " AND mode = ?",
                new String[] {
                        NoteRepeatTypes.NO_REPEAT.getValue(), "1", searchStartDateMsUTC,
                        NoteRepeatTypes.NO_REPEAT.getValue(), searchStartDateMsUTC,
                        Integer.toString(NoteTypes.Diary.getValue())
                }
        );

        for (Note note : notes) {
            Notifications.schedule(note.id, false);
        }
    }

    static public void cancel(int id) {
        Intent intent = new Intent(App.getAppContext(), NotificationsReceiver.class);
        intent.setAction(getShowActionName(id));
        PendingIntent pendingIntent = PendingIntent.getBroadcast(App.getAppContext(), 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);

        AlarmManager alarmManager = (AlarmManager) App.getAppContext().getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);
    }

    static public void cancelAll() {
        String searchStartDateMsUTC = Long.toString(DateHelper.convertFromLocalToUTC(DateHelper.startOf(Calendar.getInstance(), "day")).getTimeInMillis());

        ArrayList<Note> notes = NoteRepository.getInstance().queryNotes(
                "SELECT id FROM Notes"
                        + " WHERE ((repeatType = ? AND isNotificationEnabled = ? AND date >= ?)"
                        + " OR (repeatType != ? AND (repeatEndDate IS NULL OR repeatEndDate >= ?)))"
                        + " AND mode = ?",
                new String[] {
                        NoteRepeatTypes.NO_REPEAT.getValue(), "1", searchStartDateMsUTC,
                        NoteRepeatTypes.NO_REPEAT.getValue(), searchStartDateMsUTC,
                        Integer.toString(NoteTypes.Diary.getValue())
                }
        );

        for (Note note : notes) {
            Notifications.cancel(note.id);
        }
    }

    static private String getShowActionName(int id) {
        return ACTION_SHOW + "_" + id;
    }
    // endregion

    // region show notification
    static public void show(int id, Notification notification) {
        NotificationManager notificationManager = (NotificationManager) App.getAppContext().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(id, notification);
    }

    static public Notification build(NotificationOptions options) {
        createDefaultChannel();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(App.getAppContext(), CHANNEL_ID);

        Bundle extras = new Bundle();
        extras.putInt(EXTRA_ID, options.id);

        builder
                .setDefaults(NotificationCompat.DEFAULT_VIBRATE | NotificationCompat.DEFAULT_SOUND | NotificationCompat.DEFAULT_LIGHTS)
                .setExtras(extras)
                .setOnlyAlertOnce(false)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setShowWhen(true)
                .setUsesChronometer(false)
                .setAutoCancel(false)
                .setOngoing(false)
                .setSmallIcon(R.mipmap.notification)
                .addAction(new NotificationCompat.Action.Builder(android.R.drawable.screen_background_dark, App.getAppContext().getString(R.string.notification_finish_action), getActionPendingIntent(ACTION_FINISH, options.id)).build())
                .setContentIntent(getActionPendingIntent(ACTION_OPEN_NOTE, options.id))
                .setContentTitle(options.title.isEmpty() ? null : options.title)
                .setContentText(options.text.isEmpty() ? null : options.text)
                .setTicker(options.text.isEmpty() ? null : options.text)
                .setWhen(DateHelper.getDateTime(DateHelper.startOf(Calendar.getInstance(), "day"), options.triggerTime).getTimeInMillis());

        return builder.build();
    }

    static private PendingIntent getActionPendingIntent(String action, int id) {
        Intent intent = new Intent(App.getAppContext(), NotificationActionReceiver.class);
        intent.setAction(action);
        intent.putExtra(EXTRA_ID, id);
        return PendingIntent.getService(App.getAppContext(), 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
    }

    static private void createDefaultChannel() {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O) {
            return;
        }

        NotificationManager notificationManager = (NotificationManager) App.getAppContext().getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
        notificationManager.createNotificationChannel(channel);
    }
    // endregion
}
