package com.dailylist.vadimsemenyk.natives.Notifications;

import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.google.gson.Gson;

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

            SharedPreferences sp = context.getSharedPreferences(Notifications.SP_NOTIFICATION_OPTIONS, Context.MODE_PRIVATE);
            String optionsJSON = sp.getString(Integer.toString(id), null);

            NotificationOptions options = null;
            Gson gson = new Gson();
            if (optionsJSON != null && optionsJSON.length() > 0) {
                options = gson.fromJson(optionsJSON, NotificationOptions.class);
            }

            if (options == null) {
                return;
            }

            Notification notification = Notifications.build(options);
            Notifications.show(options.id, notification);
            Notifications.clearOptions(options.id);
        }
    }
}