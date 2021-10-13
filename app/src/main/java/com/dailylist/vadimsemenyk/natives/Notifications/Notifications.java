package com.dailylist.vadimsemenyk.natives.Notifications;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;

import com.dailylist.vadimsemenyk.R;
import com.dailylist.vadimsemenyk.natives.App;
import com.dailylist.vadimsemenyk.natives.Enums.NoteRepeatTypes;
import com.dailylist.vadimsemenyk.natives.Helpers.DateHelper;
import com.google.gson.Gson;

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

    static String SP_NOTIFICATION_OPTIONS = "com.dailylist.vadimsemenyk.notification.options";

    public Notifications() { }

    // schedule notification

    static public void schedule(NotificationOptions options) {
        saveOptions(options);

        Long triggerDateTimeMS = getTriggerDateTime(options);

        Intent intent = new Intent(App.getAppContext(), NotificationsReceiver.class);
        intent.setAction(getShowActionName(options.id));
        intent.putExtra(EXTRA_ID, options.id);
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

    static private Long getTriggerDateTime(NotificationOptions options) {
        if (options.repeatType == NoteRepeatTypes.NO_REPEAT) {
            return DateHelper.getDateTime(DateHelper.convertFromUTCToLocal(options.triggerDateUTCMS), DateHelper.convertFromUTCToLocal(options.triggerTimeUTCMS)).getTimeInMillis();
        } else {
            Calendar getRepeatNextTriggerDate = getRepeatNextTriggerDate(options);
            if (getRepeatNextTriggerDate == null) {
                return null;
            }
            return DateHelper.getDateTime(getRepeatNextTriggerDate, DateHelper.convertFromUTCToLocal(options.triggerTimeUTCMS)).getTimeInMillis();
        }
    }

    static private Calendar getRepeatNextTriggerDate(NotificationOptions options) {
        boolean isCurrentTimeBeforeTriggerTime = DateHelper.getTime(Calendar.getInstance()).before(DateHelper.getCalendar(options.triggerTimeUTCMS));

        if (options.repeatType == NoteRepeatTypes.DAY) {
            Calendar resultDate = DateHelper.startOf(Calendar.getInstance(), "day");

            if (!isCurrentTimeBeforeTriggerTime) {
                resultDate.add(Calendar.DATE, 1);
            }

            return resultDate;
        } else if (options.repeatType == NoteRepeatTypes.WEEK) {
            int currentWeekDay = DateHelper.getDayOfWeekNumber(Calendar.getInstance());
            ArrayList<Long> repeatValues = new ArrayList<>(options.repeatValues);

            Collections.sort(repeatValues);

            Long result = repeatValues.get(0);
            for (Long repeatValue : repeatValues) {
                if (!isCurrentTimeBeforeTriggerTime ? (repeatValue > currentWeekDay) : (repeatValue >= currentWeekDay)) {
                    result = repeatValue;
                    break;
                }
            }

            Calendar resultDate = DateHelper.startOf(Calendar.getInstance(), "day");
            resultDate.set(Calendar.DAY_OF_WEEK, DateHelper.getDayOfWeek(result.intValue()));

            return resultDate;
        } else if (options.repeatType == NoteRepeatTypes.ANY) {
            Long currentDateMS = DateHelper.startOf(Calendar.getInstance(), "day").getTimeInMillis();

            Long result = null;
            for (Long repeatValueUTC : options.repeatValues) {
                Long repeatValue = DateHelper.convertFromUTCToLocal(repeatValueUTC).getTimeInMillis();

                if (!isCurrentTimeBeforeTriggerTime ? (repeatValue > currentDateMS) : (repeatValue >= currentDateMS)) {
                    result = repeatValue;
                    break;
                }
            }

            if (result == null) {
                return null;
            }

            return DateHelper.getCalendar(result);
        }

        // TODO: if current minute schedule for 59 seconds

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

    static public NotificationOptions getNotificationOptions(int id) {
        SharedPreferences sp = App.getAppContext().getSharedPreferences(Notifications.SP_NOTIFICATION_OPTIONS, Context.MODE_PRIVATE);
        String optionsJSON = sp.getString(Integer.toString(id), null);

        NotificationOptions options = null;
        Gson gson = new Gson();
        if (optionsJSON != null && optionsJSON.length() > 0) {
            options = gson.fromJson(optionsJSON, NotificationOptions.class);
        }

        prepareNotificationOptions(options);

        return options;
    }

    static public NotificationOptions prepareNotificationOptions(NotificationOptions options) {
        options.triggerTime = DateHelper.convertFromUTCToLocal(options.triggerTimeUTCMS);

        if (options.triggerDate != null) {
            options.triggerDate = DateHelper.convertFromUTCToLocal(options.triggerDateUTCMS);
        }

        return options;
    }

    static public void saveOptions(NotificationOptions options) {
        Gson gson = new Gson();
        String optionsJSON = gson.toJson(options);

        SharedPreferences sp = App.getAppContext().getSharedPreferences(SP_NOTIFICATION_OPTIONS, Context.MODE_PRIVATE);
        sp.edit().putString(Integer.toString(options.id), optionsJSON).apply();
    }

    static public void clearOptions(int id) {
        SharedPreferences sp = App.getAppContext().getSharedPreferences(SP_NOTIFICATION_OPTIONS, Context.MODE_PRIVATE);
        sp.edit().remove(Integer.toString(id)).apply();
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
                .setStyle(new NotificationCompat.BigTextStyle().bigText(options.text))
                .setContentTitle(options.title)
                .setContentText(options.text)
                .setTicker(options.text)
                .setWhen(DateHelper.convertFromUTCToLocal(options.triggerTimeUTCMS).getTimeInMillis());

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
