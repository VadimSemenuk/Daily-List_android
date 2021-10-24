package com.dailylist.vadimsemenyk.natives.Notifications;

import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import com.dailylist.vadimsemenyk.natives.Models.Note;
import com.dailylist.vadimsemenyk.natives.Repositories.NoteRepository;

import java.util.ArrayList;
import java.util.Arrays;

public class NotificationsReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() == null) {
            return;
        }

        if (intent.getAction().contains(Notifications.ACTION_SHOW)) {
            TriggerOptions triggerOptions = getOptions(intent.getExtras());

            if (!triggerOptions.noteIDs.isEmpty()) {
                ArrayList<Note> notes = NoteRepository.getInstance().queryNotes(
                        "SELECT " + NoteRepository.noteSQLFields
                                + " FROM Notes n"
                                + " WHERE id IN (?)",
                        new String[] { TextUtils.join(",", triggerOptions.noteIDs) }
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

            if (triggerOptions.shouldReschedule && triggerOptions.id != -1) {
                Notifications.schedule(triggerOptions.id, true);
            }
        }
    }

    private TriggerOptions getOptions(Bundle extras) {
        ArrayList<String> _noteIDs = new ArrayList<String>(Arrays.asList(extras.getString(Notifications.EXTRA_NOTES).split(",")));
        ArrayList<Integer> noteIDs = new ArrayList<Integer>();
        for (String _noteID : _noteIDs) {
            noteIDs.add(Integer.parseInt(_noteID));
        }

        TriggerOptions options = new TriggerOptions();
        options.id = extras.getInt(Notifications.EXTRA_ID, -1);
        options.noteIDs = noteIDs;
        options.shouldReschedule = extras.getBoolean(Notifications.EXTRA_ID);

        return options;
    }
}