package com.dailylist.vadimsemenyk.natives.Notifications;

import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.dailylist.vadimsemenyk.natives.Enums.NoteRepeatTypes;
import com.dailylist.vadimsemenyk.natives.Helpers.DateHelper;
import com.dailylist.vadimsemenyk.natives.Models.Note;
import com.dailylist.vadimsemenyk.natives.Repositories.NoteRepository;

import java.util.ArrayList;
import java.util.Calendar;

public class NotificationsReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() == null) {
            return;
        }

        if (intent.getAction().contains(Notifications.ACTION_SHOW)) {
            Bundle extras = intent.getExtras();

            int id = extras.getInt(Notifications.EXTRA_ID, -1);

            if (id == -1) {
                return;
            }

            NotificationOptions options = Notifications.getOptions(id);

            if (options == null) {
                return;
            }

            if (options.repeatType != NoteRepeatTypes.NO_REPEAT) {
                ArrayList<Note> forkedNotes = NoteRepository.getInstance().getNotes(
                        "forkFrom = ? and repeatItemDate = ?",
                        new String[] {
                                Integer.toString(options.id),
                                Long.toString(DateHelper.convertFromLocalToUTC(DateHelper.startOf(Calendar.getInstance(), "day")).getTimeInMillis())
                        }
                );
                if (forkedNotes.size() != 0) {
                    Note forkedNote = forkedNotes.get(0);

                    options.id = forkedNote.id;

                    if (forkedNote.date.equals(forkedNote.repeatItemDate) && forkedNote.startDateTime.equals(options.triggerTime)) {
                        options.title = forkedNote.title;
                        options.text = getNotificationText(forkedNote);
                    } else {
                        return;
                    }
                }
            }

            Notification notification = Notifications.build(options);
            Notifications.show(options.id, notification);

            if (options.repeatType == NoteRepeatTypes.NO_REPEAT) {
                Notifications.clearOptions(options.id);
            } else {
                Notifications.schedule(options);
            }
        }
    }

    private String getNotificationText(Note note) {
        return "test123";
    }
}