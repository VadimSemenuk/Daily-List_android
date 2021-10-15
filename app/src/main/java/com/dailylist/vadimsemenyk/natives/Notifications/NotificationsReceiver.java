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

            Note note = NoteRepository.getInstance().getNote(id);

            ArrayList<NotificationOptions> optionsList = new ArrayList<NotificationOptions>();

            if (note.repeatType == NoteRepeatTypes.NO_REPEAT) {
                 optionsList.add(getNotificationOptions(note));
            } else {
                ArrayList<Note> repeatItemForkedNotes = NoteRepository.getInstance().getNotes(
                        "forkFrom = ? AND repeatItemDate = ? AND startTime = ?",
                        new String[] {
                                Integer.toString(note.id),
                                Long.toString(note.date.getTimeInMillis()),
                                Long.toString(note.startDateTime.getTimeInMillis()),
                        }
                );

                ArrayList<Note> forkedNotes = NoteRepository.getInstance().getNotes(
                        "forkFrom = ? AND date = ? AND startTime = ?",
                        new String[] {
                                Integer.toString(note.id),
                                Long.toString(note.date.getTimeInMillis()),
                                Long.toString(note.startDateTime.getTimeInMillis()),
                        }
                );

                if (!forkedNotes.isEmpty()) {
                    for (Note forkedNote : forkedNotes) {
                        optionsList.add(getNotificationOptions(forkedNote));
                    }
                }
            }

            for (NotificationOptions options : optionsList) {
                Notification notification = Notifications.build(options);
                Notifications.show(options.id, notification);
            }

            if (note.repeatType != NoteRepeatTypes.NO_REPEAT) {
                Notifications.schedule(note.id, true);
            }
        }
    }

    private NotificationOptions getNotificationOptions(Note note) {
        NotificationOptions options = new NotificationOptions();
        options.id = note.id;
        options.title = note.title;
        options.text = getNotificationText(note);
        options.triggerTime = note.startDateTime;

        return options;
    }

    private String getNotificationText(Note note) {
        return "test123";
    }
}