package com.dailylist.vadimsemenyk.natives;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.M;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.dailylist.vadimsemenyk.natives.Models.Settings;
import com.dailylist.vadimsemenyk.natives.Repositories.NoteRepository;
import com.dailylist.vadimsemenyk.natives.Repositories.SettingsRepository;

import java.util.Calendar;

public class DayChangeHandler {
    public final static String ACTION_DAY_CHANGED = "com.dailylist.vadimsemenyk.day_changed";

    public static void scheduleDayChangeEvent(Context context) {
        Calendar dateTime = Calendar.getInstance();
        dateTime.set(Calendar.HOUR_OF_DAY, 0);
        dateTime.set(Calendar.MINUTE, 0);
        dateTime.set(Calendar.SECOND, 0);
        dateTime.set(Calendar.MILLISECOND, 0);
        dateTime.add(Calendar.MILLISECOND, 86400000);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (SDK_INT >= M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, dateTime.getTimeInMillis(), getWidgetUpdateReschedulePIntent(context));
        } else {
            alarmManager.setExact(AlarmManager.RTC, dateTime.getTimeInMillis(), getWidgetUpdateReschedulePIntent(context));
        }
    }

    public static void unScheduleDayChangeEvent(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(getWidgetUpdateReschedulePIntent(context));
    }

    private static PendingIntent getWidgetUpdateReschedulePIntent(Context context) {
        Intent intent = new Intent(context, DayChangeReceiver.class);
        intent.setAction(ACTION_DAY_CHANGED);
        return PendingIntent.getBroadcast(context, 0, intent, 0);
    }

    public static void onDateChange(Context context) {
        scheduleDayChangeEvent(context);

        DBHelper.createInstance(context.getApplicationContext());

        Settings settings = SettingsRepository.getInstance().getSettings();
        if (settings.autoMoveNotFinishedNotes) {
            NoteRepository.getInstance().moveNotFinishedNotesForToday();

            Natives.fireEvent("noteStateChange", false);
        }
    }
}
