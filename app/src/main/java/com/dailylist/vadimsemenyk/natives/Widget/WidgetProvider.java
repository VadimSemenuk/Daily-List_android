package com.dailylist.vadimsemenyk.natives.Widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.widget.RemoteViews;

import com.dailylist.vadimsemenyk.R;
import com.dailylist.vadimsemenyk.natives.DBHelper;
import com.dailylist.vadimsemenyk.natives.DayChangeHandler;
import com.dailylist.vadimsemenyk.natives.Enums.NoteTypes;
import com.dailylist.vadimsemenyk.natives.Natives;
import com.dailylist.vadimsemenyk.natives.Repositories.NoteRepository;
import com.dailylist.vadimsemenyk.natives.Models.Settings;
import com.dailylist.vadimsemenyk.natives.Repositories.SettingsRepository;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class WidgetProvider extends AppWidgetProvider {
    public final static String ACTION_LIST_ITEM_LICK = "com.dailylist.vadimsemenyk.widget.list_item_click";
    public final static String ACTION_OPEN_ADD = "com.dailylist.vadimsemenyk.widget.open_add";
    public final static String ACTION_OPEN_APP = "com.dailylist.vadimsemenyk.widget.open_app";
    public final static String ACTION_LIST_WITH_TIME = "cosetNoteFinishStatem.dailylist.vadimsemenyk.widget.set_list_with_time";
    public final static String ACTION_LIST_WITHOUT_TIME = "com.dailylist.vadimsemenyk.widget.set_list_without_time";
    public final static String ACTION_UPDATE = "com.dailylist.vadimsemenyk.widget.update";
    public final static String ACTION_UPDATE_LIST = "com.dailylist.vadimsemenyk.widget.update_list";

    public final static String WIDGET_SP = "com.dailylist.vadimsemenyk.widget";
    public final static String WIDGET_SP_LIST_TYPE = "list_type";

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);

        for (int id : appWidgetIds) {
            updateWidget(context, appWidgetManager, id);
        }
    }

    private void updateWidget(Context context, AppWidgetManager appWidgetManager, int id) {
        DBHelper.createInstance(context.getApplicationContext());

        Settings settings = SettingsRepository.getInstance().getSettings();

        RemoteViews widgetView = new RemoteViews(context.getPackageName(), R.layout.widget);

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", new Locale(settings.lang));
        widgetView.setTextViewText(R.id.date, dateFormat.format(Calendar.getInstance().getTime()));

        widgetView.setTextViewText(R.id.empty, getLocalizedResources(context, new Locale(settings.lang)).getString(R.string.widget_list_empty));

        setWidgetEvents(widgetView, context, id);

        setList(widgetView, context, id);
        setListClick(widgetView, context, id);

        appWidgetManager.updateAppWidget(id, widgetView);
        appWidgetManager.notifyAppWidgetViewDataChanged(id, R.id.list);
    }

    private void setWidgetEvents(RemoteViews widgetView, Context context, int widgetId) {
        Intent openAddIntent = new Intent(context, WidgetProvider.class);
        openAddIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
        openAddIntent.setAction(ACTION_OPEN_ADD);
        PendingIntent pIntent = PendingIntent.getBroadcast(context, widgetId, openAddIntent, 0);
        widgetView.setOnClickPendingIntent(R.id.add, pIntent);

        Intent setListWithTimeIntent = new Intent(context, WidgetProvider.class);
        setListWithTimeIntent.setAction(ACTION_LIST_WITH_TIME);
        setListWithTimeIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
        PendingIntent setListWithTimePIntent = PendingIntent.getBroadcast(context, widgetId, setListWithTimeIntent, 0);
        widgetView.setOnClickPendingIntent(R.id.show_notes_with_time, setListWithTimePIntent);

        Intent setListWithoutTimeIntent = new Intent(context, WidgetProvider.class);
        setListWithoutTimeIntent.setAction(ACTION_LIST_WITHOUT_TIME);
        setListWithoutTimeIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
        PendingIntent setListWithoutTimePIntent = PendingIntent.getBroadcast(context, widgetId, setListWithoutTimeIntent, 0);
        widgetView.setOnClickPendingIntent(R.id.show_notes_without_time, setListWithoutTimePIntent);

        Intent openAppIntent = new Intent(context, WidgetProvider.class);
        openAppIntent.setAction(ACTION_OPEN_APP);
        openAppIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
        PendingIntent openAppPIntent = PendingIntent.getBroadcast(context, widgetId, openAppIntent, 0);
        widgetView.setOnClickPendingIntent(R.id.app_icon, openAppPIntent);
    }

    private void setListClick(RemoteViews rv, Context context, int appWidgetId) {
        Intent listClickIntent = new Intent(context, WidgetProvider.class);
        listClickIntent.setAction(ACTION_LIST_ITEM_LICK);
        listClickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        PendingIntent listClickPIntent = PendingIntent.getBroadcast(context, appWidgetId, listClickIntent, 0);
        rv.setPendingIntentTemplate(R.id.list, listClickPIntent);
    }

    private void setList(RemoteViews rv, Context context, int appWidgetId) {
        Intent adapter = new Intent(context, WidgetListService.class);
        adapter.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        Uri data = Uri.parse(adapter.toUri(Intent.URI_INTENT_SCHEME));
        adapter.setData(data);
        rv.setEmptyView(R.id.list, R.id.empty);
        rv.setRemoteAdapter(R.id.list, adapter);
    }

    private void updateAllWidgets(Context context) {
        int ids[] = getAllWidgetIDs(context);
        for (int id : ids) {
            updateWidget(context, AppWidgetManager.getInstance(context), id);
        }
    }

    private void updateWidgetList(Context context, int widgetId) {
        DBHelper.createInstance(context.getApplicationContext());
        AppWidgetManager.getInstance(context).notifyAppWidgetViewDataChanged(widgetId, R.id.list);
    }

    private void updateAllWidgetsList(Context context) {
        int ids[] = getAllWidgetIDs(context);
        for (int id : ids) {
            updateWidgetList(context, id);
        }
    }

    private int[] getAllWidgetIDs(Context context) {
        ComponentName thisAppWidget = new ComponentName(context.getPackageName(), getClass().getName());
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        return appWidgetManager.getAppWidgetIds(thisAppWidget);
    }

    private void launchApp(Context context) {
        Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage("com.dailylist.vadimsemenyk");
        if (launchIntent != null) {
            context.startActivity(launchIntent);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        Bundle extras = intent.getExtras();

        if (intent.getAction().equalsIgnoreCase(ACTION_UPDATE_LIST)) {
            updateAllWidgetsList(context);
        } else if (intent.getAction().equalsIgnoreCase(ACTION_UPDATE)) {
            updateAllWidgets(context);
        } else if (intent.getAction().equalsIgnoreCase(DayChangeHandler.ACTION_DAY_CHANGED)) {
            updateAllWidgets(context);
        } else if (intent.getAction().equalsIgnoreCase(ACTION_LIST_ITEM_LICK)) {
            int widgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
            if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
                return;
            }

            SharedPreferences sp = context.getSharedPreferences(WidgetProvider.WIDGET_SP, Context.MODE_PRIVATE);
            int type = sp.getInt(WidgetProvider.WIDGET_SP_LIST_TYPE + "_" + widgetId,  1);

            int itemId = intent.getIntExtra("item_id", -1);
            if (itemId != -1) {
                String actionTarget = intent.getStringExtra("action_target");

                if (actionTarget != null && actionTarget.equals("item")) {
                    launchApp(context);

                    JSONObject params = new JSONObject();
                    try {
                        params.put("id", itemId);
                        params.put("type", type);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    Natives.fireEvent("noteClick", params, true);
                } else if (actionTarget != null && actionTarget.equals("finish")) {
                    DBHelper.createInstance(context.getApplicationContext());
                    NoteRepository.getInstance().triggerNoteFinishState(itemId);
                    updateWidgetList(context, widgetId);

                    Natives.fireEvent("noteStateChange", false);
                }
            }
        } else if (intent.getAction().equalsIgnoreCase(ACTION_OPEN_ADD)) {
            int widgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
            if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
                return;
            }

            launchApp(context);

            SharedPreferences sp = context.getSharedPreferences(WidgetProvider.WIDGET_SP, Context.MODE_PRIVATE);
            int type = sp.getInt(WidgetProvider.WIDGET_SP_LIST_TYPE + "_" + widgetId,  1);

            JSONObject params = new JSONObject();
            try {
                params.put("type", type);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            Natives.fireEvent("addClick", params, true);
        } else if (intent.getAction().equalsIgnoreCase(ACTION_LIST_WITH_TIME) || intent.getAction().equalsIgnoreCase(ACTION_LIST_WITHOUT_TIME)) {
            int widgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
            if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
                return;
            }

            NoteTypes nextListType = intent.getAction().equalsIgnoreCase(ACTION_LIST_WITH_TIME) ? NoteTypes.Diary : NoteTypes.Note;

            SharedPreferences sp = context.getSharedPreferences(WIDGET_SP, Context.MODE_PRIVATE);
            sp.edit().putInt(WIDGET_SP_LIST_TYPE + "_" + widgetId, nextListType.getValue()).apply();

            updateWidgetList(context, widgetId);
        } else if (intent.getAction().equalsIgnoreCase(ACTION_OPEN_APP)) {
            launchApp(context);
        }
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);

        SharedPreferences.Editor editor = context.getSharedPreferences(WIDGET_SP, Context.MODE_PRIVATE).edit();
        for (int widgetID : appWidgetIds) {
            editor.remove(WIDGET_SP_LIST_TYPE + "_" + widgetID);
        }
        editor.apply();
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
    }

    static Resources getLocalizedResources(Context context, Locale desiredLocale) {
        Configuration conf = context.getResources().getConfiguration();
        conf = new Configuration(conf);
        conf.setLocale(desiredLocale);
        Context localizedContext = context.createConfigurationContext(conf);
        return localizedContext.getResources();
    }
}