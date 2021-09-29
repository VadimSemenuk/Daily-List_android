package de.appplant.cordova.plugin.localnotification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.UserManager;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.SimpleTimeZone;
import java.util.TimeZone;

import de.appplant.cordova.plugin.notification.Builder;
import de.appplant.cordova.plugin.notification.Manager;
import de.appplant.cordova.plugin.notification.Notification;
import de.appplant.cordova.plugin.notification.Options;
import de.appplant.cordova.plugin.notification.Request;

import static android.os.Build.VERSION.SDK_INT;

public class TimeChangeReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() != null && intent.getAction().equals(Intent.ACTION_TIMEZONE_CHANGED)) {
            if (SDK_INT >= 24) {
                UserManager um = (UserManager) context.getSystemService(UserManager.class);
                if (um == null || um.isUserUnlocked() == false) return;
            }

            Manager mgr = Manager.getInstance(context);
            List<JSONObject> toasts = mgr.getOptions();

            for (JSONObject data : toasts) {
                Options options = new Options(context, data);

                mgr.cancel(options.getId());

                Request request    = new Request(options);
                Builder builder    = new Builder(options);
                Notification toast = buildNotification(builder);

                Date date = request.getTriggerDate();
                boolean after = date != null && date.after(new Date());

                if (!after && toast.isHighPrio()) {
                    toast.show();
                } else {
                    toast.clear();
                }

                if (after || toast.isRepeating()) {
                    mgr.schedule(request, TriggerReceiver.class);
                }
            }
        }
    }

    public Notification buildNotification (Builder builder) {
        return builder
                .setClickActivity(ClickReceiver.class)
                .setClearReceiver(ClearReceiver.class)
                .build();
    }
}
