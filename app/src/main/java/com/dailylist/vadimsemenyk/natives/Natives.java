package com.dailylist.vadimsemenyk.natives;

import android.app.Activity;
import android.content.Intent;

import com.dailylist.vadimsemenyk.natives.Widget.WidgetProvider;

import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.HashMap;

public class Natives extends CordovaPlugin {
    private static WeakReference<CordovaWebView> webView = null;
    private static Boolean isWebAppListenEvents = false;
    private static HashMap<String, String> scheduledEvents = new HashMap();

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);

        Natives.webView = new WeakReference<CordovaWebView>(webView);
    }

    public void onDestroy() {
        scheduledEvents.clear();
        isWebAppListenEvents = false;
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("eventAdded")) {
            isWebAppListenEvents = true;
            fireScheduledEvent(args.getString(0));
            return true;
        } else if (action.equals("updateWidget")) {
            updateWidget();
            return true;
        } else if (action.equals("updateWidgetList")) {
            updateWidgetList();
            return true;
        } else if (action.equals("scheduleDayChangeNotification")) {
             DayChangeHandler.unScheduleDayChangeEvent(cordova.getContext());
             DayChangeHandler.scheduleDayChangeEvent(cordova.getContext());
             return true;
         }
        return false;
    }


    static boolean isAppRunning() {
        return webView != null;
    }

    private static synchronized void fireScheduledEvent(String event) {
        if (scheduledEvents.get(event) != null) {
            sendJavascript(scheduledEvents.get(event));
            scheduledEvents.remove(event);
        }
    }

    public static void fireEvent(String event, boolean scheduleEvent) {
        fireEvent(event, new JSONObject(), scheduleEvent);
    }

    public static void fireEvent(String event, JSONObject data, boolean scheduleEvent) {
        String js = "cordova.plugins.natives.fireEvent(" + "\"" + event + "\"," + data.toString() + ")";

        if (!isWebAppListenEvents || !isAppRunning()) {
            if (scheduleEvent) {
                scheduledEvents.put(event, js);
            }
            return;
        }

        sendJavascript(js);
    }

    private static synchronized void sendJavascript(final String js) {
        final CordovaWebView view = webView.get();

        ((Activity)(view.getContext())).runOnUiThread(new Runnable() {
            public void run() {
                view.loadUrl("javascript:" + js);
            }
        });
    }

    private void updateWidget() {
        Intent updateIntent = new Intent(cordova.getContext(), WidgetProvider.class);
        updateIntent.setAction(WidgetProvider.ACTION_UPDATE);
        cordova.getContext().sendBroadcast(updateIntent);
    }

    private void updateWidgetList() {
        Intent updateIntent = new Intent(cordova.getContext(), WidgetProvider.class);
        updateIntent.setAction(WidgetProvider.ACTION_UPDATE_LIST);
        cordova.getContext().sendBroadcast(updateIntent);
    }
}