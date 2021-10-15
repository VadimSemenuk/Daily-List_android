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

import com.dailylist.vadimsemenyk.R;
import com.dailylist.vadimsemenyk.natives.App;
import com.dailylist.vadimsemenyk.natives.Enums.NoteRepeatTypes;
import com.dailylist.vadimsemenyk.natives.Helpers.DateHelper;
import com.dailylist.vadimsemenyk.natives.Models.Note;
import com.dailylist.vadimsemenyk.natives.Repositories.NoteRepository;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;

public class Notifications {
    static String CHANNEL_ID = "com.dailylist.vadimsemenyk.notification";
    static String CHANNEL_NAME = "Daily List";

    static String ACTION_SHOW = "com.dailylist.vadimsemenyk.notification.show";
    static String ACTION_FINISH = "com.dailylist.vadimsemenyk.notification.finish";
    static String ACTION_OPEN_NOTE = "com.dailylist.vadimsemenyk.notification.open_note";

    static String EXTRA_ID = "id";
    static String EXTRA_TRIGGER_DATE = "trigger_date";

    static String SP_NOTIFICATION_OPTIONS = "com.dailylist.vadimsemenyk.notification.options";

    public Notifications() { }

    // schedule notification

    static public void schedule(int id) {
        schedule(id, false);
    }

    static public void schedule(int id, boolean isRepeatReschedule) {
        Long triggerDateTimeMS = getTriggerDateTimeMS(id, isRepeatReschedule);

        Intent intent = new Intent(App.getAppContext(), NotificationsReceiver.class);
        intent.setAction(getShowActionName(id));
        intent.putExtra(EXTRA_ID, id);
        intent.putExtra(EXTRA_TRIGGER_DATE, triggerDateTimeMS);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(App.getAppContext(), 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);

        AlarmManager alarmManager = (AlarmManager) App.getAppContext().getSystemService(Context.ALARM_SERVICE);
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerDateTimeMS, pendingIntent);
            } else {
                alarmManager.setExact(AlarmManager.RTC, triggerDateTimeMS, pendingIntent);
            }
        } catch (Exception ignore) {
            // Samsung devices have a known bug where a 500 alarms limit
            // can crash the app
        }
    }

    static private Long getTriggerDateTimeMS(int id, boolean isRepeatReschedule) {
        Note note = NoteRepository.getInstance().getNote(id);

        if (note.repeatType == NoteRepeatTypes.NO_REPEAT) {
            return DateHelper.getDateTime(note.date, note.startDateTime).getTimeInMillis();
        } else {
            boolean isCurrentTimeBeforeTriggerTime = DateHelper.getTime(Calendar.getInstance()).before(note.startDateTime);
            boolean isCurrentTimeEqualsTriggerTime = DateHelper.getTime(Calendar.getInstance()).equals(note.startDateTime);
            boolean includeCurrentDate = isCurrentTimeBeforeTriggerTime || (isCurrentTimeEqualsTriggerTime && !isRepeatReschedule);

            Calendar nextTriggerDate = getRepeatNextTriggerDate(note.repeatType, note.repeatValues, includeCurrentDate);
            Calendar nextTriggerTime = note.startDateTime;

            ArrayList<Note> closestForkedNotes = NoteRepository.getInstance().queryNotes(
                    "SELECT " + NoteRepository.noteSQLFields
                            + " FROM Notes n"
                            + " WHERE forkFrom = ? AND (date <= ? OR (date = ? AND startTime < ?))"
                            + " ORDER BY date DESC, startTime DESC"
                            + " LIMIT 1",
                    new String[] {
                            Integer.toString(id),
                            Long.toString(nextTriggerDate.getTimeInMillis()),
                            Long.toString(nextTriggerDate.getTimeInMillis()),
                            Long.toString(note.startDateTime.getTimeInMillis()),
                    }
            );

            if (!closestForkedNotes.isEmpty()) {
                Note closestForkedNote = closestForkedNotes.get(0);
                nextTriggerDate = closestForkedNote.date;
                nextTriggerTime = closestForkedNote.startDateTime;
            }

            if (nextTriggerDate == null) {
                return null;
            }

            return DateHelper.getDateTime(nextTriggerDate, nextTriggerTime).getTimeInMillis();

            // TODO: save note id's for show notification on next trigger
        }
    }

    static private Calendar getRepeatNextTriggerDate(NoteRepeatTypes repeatType, ArrayList<Long> repeatValues, boolean includeCurrentDate) {
        if (repeatType == NoteRepeatTypes.DAY) {
            Calendar resultDate = DateHelper.startOf(Calendar.getInstance(), "day");

            if (!includeCurrentDate) {
                resultDate.add(Calendar.DATE, 1);
            }

            return resultDate;
        } else if (repeatType == NoteRepeatTypes.WEEK) {
            int currentWeekDay = DateHelper.getDayOfWeekNumber(Calendar.getInstance());
            ArrayList<Long> _repeatValues = new ArrayList<>(repeatValues);

            Collections.sort(_repeatValues);

            Long result = _repeatValues.get(0);
            for (Long repeatValue : _repeatValues) {
                if (includeCurrentDate ? (repeatValue > currentWeekDay) : (repeatValue >= currentWeekDay)) {
                    result = repeatValue;
                    break;
                }
            }

            Calendar resultDate = DateHelper.startOf(Calendar.getInstance(), "day");
            resultDate.set(Calendar.DAY_OF_WEEK, DateHelper.getDayOfWeek(result.intValue()));

            return resultDate;
        } else if (repeatType == NoteRepeatTypes.ANY) {
            Long currentDateMS = DateHelper.startOf(Calendar.getInstance(), "day").getTimeInMillis();

            Long result = null;
            for (Long repeatValueUTC : repeatValues) {
                Long repeatValue = DateHelper.convertFromUTCToLocal(repeatValueUTC).getTimeInMillis();

                if (includeCurrentDate ? (repeatValue > currentDateMS) : (repeatValue >= currentDateMS)) {
                    result = repeatValue;
                    break;
                }
            }

            if (result == null) {
                return null;
            }

            return DateHelper.getCalendar(result);
        }

        return null;
    }

    static public void cancel(int id) {
        Intent intent = new Intent(App.getAppContext(), NotificationsReceiver.class);
        intent.setAction(getShowActionName(id));
        PendingIntent pendingIntent = PendingIntent.getBroadcast(App.getAppContext(), 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);

        AlarmManager alarmManager = (AlarmManager) App.getAppContext().getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);
    }

    static private String getShowActionName(int id) {
        return ACTION_SHOW + "_" + id;
    }

    // show notification

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
                .setWhen(options.triggerTime.getTimeInMillis());

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
}
