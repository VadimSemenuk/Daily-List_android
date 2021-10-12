package com.dailylist.vadimsemenyk.natives.Notifications;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;

import com.dailylist.vadimsemenyk.natives.Helpers.Helpers;
import com.dailylist.vadimsemenyk.natives.Natives;
import com.dailylist.vadimsemenyk.natives.Repositories.NoteRepository;

import org.json.JSONException;
import org.json.JSONObject;

public class NotificationActionReceiver extends IntentService {
    public NotificationActionReceiver() {
        super("NotificationActionReceiver");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent == null) {
            return;
        }

        Bundle extras = intent.getExtras();
        if (extras == null) {
            return;
        }

        int id = extras.getInt(Notifications.EXTRA_ID, -1);
        if (id == -1) {
            return;
        }

        if (intent.getAction().equalsIgnoreCase(Notifications.ACTION_FINISH)) {
            NoteRepository.getInstance().triggerNoteFinishState(id);
            Natives.fireEvent("noteStateChange", false);
        } else if (intent.getAction().equalsIgnoreCase(Notifications.ACTION_OPEN_NOTE)) {
            Helpers.launchApp();

            JSONObject params = new JSONObject();
            try {
                params.put("id", id);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            Natives.fireEvent("showNote", params, true);
        }
    }
}
