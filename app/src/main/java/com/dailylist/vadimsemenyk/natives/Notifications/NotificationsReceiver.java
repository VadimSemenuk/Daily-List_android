package com.dailylist.vadimsemenyk.natives.Notifications;

import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.dailylist.vadimsemenyk.natives.Models.Note;
import com.dailylist.vadimsemenyk.natives.Repositories.NoteRepository;

import java.util.ArrayList;

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

            String noteIDsJoined = extras.getString(Notifications.EXTRA_NOTES);

            if (!noteIDsJoined.isEmpty()) {
                ArrayList<Note> notes = NoteRepository.getInstance().queryNotes(
                        "SELECT " + NoteRepository.noteSQLFields
                                + " FROM Notes n"
                                + " WHERE id IN (?)",
                        new String[] { noteIDsJoined }
                );

                for (Note note : notes) {
                    NotificationOptions notificationOptions = new NotificationOptions();
                    notificationOptions.id = note.id;
                    notificationOptions.title = note.title;
                    notificationOptions.text = Notifications.getText(note.contentItems);
                    notificationOptions.triggerTime = note.startDateTime;

                    Notification notification = Notifications.build(notificationOptions);
                    Notifications.show(notificationOptions.id, notification);
                }
            }

            boolean isRepeat = extras.getBoolean(Notifications.EXTRA_IS_REPEAT);
            if (isRepeat) {
                Notifications.schedule(id, true);
            }
        }
    }
}