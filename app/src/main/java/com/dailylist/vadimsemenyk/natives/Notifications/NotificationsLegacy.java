package com.dailylist.vadimsemenyk.natives.Notifications;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.O;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.dailylist.vadimsemenyk.natives.App;
import com.dailylist.vadimsemenyk.natives.Enums.NoteRepeatTypes;
import com.dailylist.vadimsemenyk.natives.Helpers.DateHelper;
import com.dailylist.vadimsemenyk.natives.Models.Note;
import com.dailylist.vadimsemenyk.natives.Repositories.NoteRepository;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Set;

public class NotificationsLegacy {
    static final String PREF_KEY_ID = "NOTIFICATION_ID";
    static final String PREF_KEY_PID = "NOTIFICATION_PID";
    static final String CHANNEL_ID = "default-channel-id";

    static public void clearAll() {
        Set<String> ids = App.getAppContext().getSharedPreferences(PREF_KEY_ID, Context.MODE_PRIVATE).getAll().keySet();

        for (String id : ids) {
            try {
                Set<String> actions = App.getAppContext().getSharedPreferences(PREF_KEY_PID, Context.MODE_PRIVATE).getStringSet(id, null);
                for (String action : actions) {
                    Intent intent = new Intent(action);
                    PendingIntent pi = PendingIntent.getBroadcast(App.getAppContext(), 0, intent, 0);
                    if (pi != null) {
                        ((AlarmManager) App.getAppContext().getSystemService(Context.ALARM_SERVICE)).cancel(pi);
                    }
                }
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }

        App.getAppContext().getSharedPreferences(PREF_KEY_ID, Context.MODE_PRIVATE).edit().clear().apply();

        if (SDK_INT >= O) {
            ((NotificationManager) App.getAppContext().getSystemService(Context.NOTIFICATION_SERVICE)).deleteNotificationChannel(CHANNEL_ID);
        }
    }

    static public void scheduleNew() {
        ArrayList<Note> notes = NoteRepository.getInstance().getNotes(
                "isNotificationEnabled = ? AND (date IS NULL OR (date >= ? AND startTime >= ?))",
                new String[] {
                        "1",
                        Long.toString(DateHelper.convertFromLocalToUTC(DateHelper.startOf(Calendar.getInstance(), "day")).getTimeInMillis()),
                        Long.toString(DateHelper.convertFromLocalToUTC(DateHelper.getTime(Calendar.getInstance())).getTimeInMillis())
                }
        );

        for (Note note : notes) {
            Notifications.schedule(note.id, false);
        }
    }

    static private String getText(Note note) {
        return "123";
    }
}
