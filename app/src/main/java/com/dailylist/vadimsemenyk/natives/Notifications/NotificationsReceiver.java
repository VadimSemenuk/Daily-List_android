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

            NotificationOptions options = Notifications.getNotificationOptions(id);

            if (options == null) {
                return;
            }

            if (options.repeatType != NoteRepeatTypes.NO_REPEAT) {
                Note forkedNote = NoteRepository.getInstance().getNotes("forkFrom = ?", new String[] {Integer.toString(options.id)}).get(0);

                options.id = forkedNote.id;

                if (forkedNote != null) {
                    if (forkedNote.date.equals(forkedNote.repeatItemDate) && forkedNote.startDateTime.equals(DateHelper.convertFromUTCToLocal(options.triggerTimeUTCMS))) {
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