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
        boolean isCurrentTimeAfterTriggerTime = DateHelper.startOf(Calendar.getInstance(), "minute").after(DateHelper.startOf(DateHelper.getDateTime(Calendar.getInstance(), DateHelper.convertFromUTCToLocal(options.triggerTimeUTCMS)), "minute"));

        if (options.repeatType == NoteRepeatTypes.DAY) {
            Calendar resultDateTime = DateHelper.startOf(Calendar.getInstance(), "day");

            if (isCurrentTimeAfterTriggerTime) {
                resultDateTime.add(Calendar.DATE, 1);
            }

            return resultDateTime;
        } else if (options.repeatType == NoteRepeatTypes.WEEK) {
            int currentWeekDay = DateHelper.getDayOfWeekNumber(Calendar.getInstance());
            ArrayList<Long> repeatValues = new ArrayList<>(options.repeatValues);

            Collections.sort(repeatValues);

            Long result = repeatValues.get(0);
            for (Long repeatValue : repeatValues) {
                if (isCurrentTimeAfterTriggerTime ? (repeatValue > currentWeekDay) : (repeatValue >= currentWeekDay)) {
                    result = repeatValue;
                    break;
                }
            }

            Calendar resultDateTime = DateHelper.startOf(Calendar.getInstance(), "day");
            resultDateTime.set(Calendar.DAY_OF_WEEK, DateHelper.getDayOfWeek(result.intValue()));

            return resultDateTime;
        } else if (options.repeatType == NoteRepeatTypes.ANY) {
            Long currentDateTimeMS = DateHelper.startOf(Calendar.getInstance(), "day").getTimeInMillis();

            Long result = null;
            for (Long repeatValueUTC : options.repeatValues) {
                Long repeatValue = DateHelper.convertFromUTCToLocal(repeatValueUTC).getTimeInMillis();

                if (isCurrentTimeAfterTriggerTime ? (repeatValue > currentDateTimeMS) : (repeatValue >= currentDateTimeMS)) {
                    result = repeatValue;
                    break;
                }
            }

            if (result == null) {
                return null;
            }

            Calendar resultDateTime = Calendar.getInstance();
            resultDateTime.setTimeInMillis(result);

            return resultDateTime;
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

    static public void show(int id, Notification notification) {
        NotificationManager notificationManager = (NotificationManager) App.getAppContext().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(id, notification);
    }

    static public Notification build(NotificationOptions options) {
        createDefaultChannel();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(App.getAppContext(), CHANNEL_ID);

        builder
                .setDefaults(NotificationCompat.DEFAULT_VIBRATE | NotificationCompat.DEFAULT_SOUND | NotificationCompat.DEFAULT_LIGHTS)
                .setExtras(getExtras(options))
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
                .setWhen(Calendar.getInstance().getTimeInMillis());

        return builder.build();
    }

    static private PendingIntent getActionPendingIntent(String action, int id) {
        Intent intent = new Intent(App.getAppContext(), NotificationActionReceiver.class);
        intent.setAction(action);
        intent.putExtra(EXTRA_ID, id);
        return PendingIntent.getService(App.getAppContext(), 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
    }

    static private Bundle getExtras(NotificationOptions options) {
        Bundle extras = new Bundle();

        extras.putInt(EXTRA_ID, options.id);

        return extras;
    }

    static private void createDefaultChannel() {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O) {
            return;
        }

        NotificationManager notificationManager = (NotificationManager) App.getAppContext().getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
        notificationManager.createNotificationChannel(channel);
    }

    static private void saveOptions(NotificationOptions options) {
        Gson gson = new Gson();
        String optionsJSON = gson.toJson(options);

        SharedPreferences sp = App.getAppContext().getSharedPreferences(SP_NOTIFICATION_OPTIONS, Context.MODE_PRIVATE);
        sp.edit().putString(Integer.toString(options.id), optionsJSON).apply();
    }

    static public void clearOptions(int id) {
        SharedPreferences sp = App.getAppContext().getSharedPreferences(SP_NOTIFICATION_OPTIONS, Context.MODE_PRIVATE);
        sp.edit().remove(Integer.toString(id)).apply();
    }
}
