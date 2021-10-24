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
import com.dailylist.vadimsemenyk.natives.Helpers.DateHelper;
import com.dailylist.vadimsemenyk.natives.Models.Note;
import com.dailylist.vadimsemenyk.natives.Models.NoteContentItem;
import com.dailylist.vadimsemenyk.natives.Models.NoteContentItemImage;
import com.dailylist.vadimsemenyk.natives.Models.NoteContentItemTextArea;
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
    static String EXTRA_NOTES = "notes";
    static String EXTRA_SHOULD_RESCHEDULE = "should_reschedule";

    public Notifications() { }

    // schedule notification

    static public void schedule(int id, boolean isRepeatReschedule) {
        TriggerOptions trigger = NoteRepository.getInstance().getNextNotificationTrigger(id, isRepeatReschedule);

        if (trigger == null || trigger.dateTime == null) {
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

    static public String getText(ArrayList<NoteContentItem> contentItems) {
        String text = "";
        if (!contentItems.isEmpty()) {
            if (contentItems.get(0) instanceof NoteContentItemImage) {
                text = "picture";
            } else {
                NoteContentItem firstNotEmptyTextContentItem = null;

                for (NoteContentItem contentItem : contentItems) {
                    if (!contentItem.value.isEmpty()) {
                        firstNotEmptyTextContentItem = contentItem;
                        break;
                    }
                }

                if (firstNotEmptyTextContentItem instanceof NoteContentItemTextArea) {
                    text = firstNotEmptyTextContentItem.value;
                } else {
                    ArrayList<String> listItems = new ArrayList<String>();
                    for (NoteContentItem contentItem : contentItems) {
                        if (contentItem instanceof NoteContentItemTextArea) {
                            listItems.add(contentItem.value);
                        } else {
                            break;
                        }
                    }

                    text = TextUtils.join("; -", listItems) + ";";
                }
            }
        }
        return text;
    }
}
