package com.dailylist.vadimsemenyk.natives.Widget;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService.RemoteViewsFactory;

import com.dailylist.vadimsemenyk.R;
import com.dailylist.vadimsemenyk.natives.Enums.NoteTypes;
import com.dailylist.vadimsemenyk.natives.Helpers.DateHelper;
import com.dailylist.vadimsemenyk.natives.Helpers.Helpers;
import com.dailylist.vadimsemenyk.natives.Models.Note;
import com.dailylist.vadimsemenyk.natives.Models.NoteContentItem;
import com.dailylist.vadimsemenyk.natives.Models.NoteContentItemListItem;
import com.dailylist.vadimsemenyk.natives.Models.NoteContentItemTextArea;
import com.dailylist.vadimsemenyk.natives.Repositories.NoteRepository;
import com.dailylist.vadimsemenyk.natives.Models.Settings;
import com.dailylist.vadimsemenyk.natives.Repositories.SettingsRepository;

public class WidgetListFactory implements RemoteViewsFactory {
    ArrayList<Note> data;
    Context context;
    int widgetID;
    Settings settings;

    WidgetListFactory(Context ctx, Intent intent) {
        context = ctx;
        widgetID = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
    }

    @Override
    public void onCreate() {
        data = new ArrayList<Note>();
    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public RemoteViews getViewAt(int position) {
        RemoteViews remoteView = new RemoteViews(context.getPackageName(), R.layout.note);

        Note note = data.get(position);

        if (!note.title.isEmpty()) {
            remoteView.setInt(R.id.title, "setVisibility", View.VISIBLE);
            remoteView.setTextViewText(R.id.title, note.title);
        } else {
            remoteView.setInt(R.id.title, "setVisibility", View.GONE);
        }

        remoteView.setInt(R.id.color_tag, "setBackgroundColor", Color.parseColor(note.colorTag.equals("transparent") ? "#00000000" : note.colorTag));

        if (note.startTime != null || note.endTime != null) {
            remoteView.setInt(R.id.meta, "setVisibility", View.VISIBLE);

            SimpleDateFormat timeFormatter = new SimpleDateFormat("HH:mm");

            remoteView.setTextViewText(R.id.start_time, note.startTime != null ? timeFormatter.format(note.startTime.getTime()) : "");
            remoteView.setTextViewText(R.id.end_time, note.endTime != null ? " - " + timeFormatter.format(note.endTime.getTime()) : "");
        } else {
            remoteView.setInt(R.id.meta, "setVisibility", View.GONE);
        }

        remoteView.removeAllViews(R.id.content_items);

        for (int a = 0; a < note.contentItems.size(); a++) {
            NoteContentItem _contentField = note.contentItems.get(a);

            if (_contentField instanceof NoteContentItemTextArea) {
                NoteContentItemTextArea contentField = (NoteContentItemTextArea) _contentField;

                RemoteViews textView = new RemoteViews(context.getPackageName(), R.layout.text_area_content_item);
                textView.setTextViewText(R.id.text_area_content_item_text, contentField.value);

                remoteView.addView(R.id.content_items, textView);

            }
            if (_contentField instanceof NoteContentItemListItem) {
                NoteContentItemListItem contentField = (NoteContentItemListItem) _contentField;

                RemoteViews textView = new RemoteViews(context.getPackageName(), R.layout.list_item_content_item);
                textView.setTextViewText(R.id.list_item_content_item_text, contentField.value);
                textView.setInt(R.id.list_item_content_item_checkbox, "setImageResource", contentField.checked ? R.drawable.checkbox_checked : R.drawable.checkbox);

                remoteView.addView(R.id.content_items, textView);
            }
        }

        if (
                settings.sortFinBehaviour == 1 &&
                (
                        ((position == 0) && data.get(position).isFinished)
                        || ((position != 0) && data.get(position).isFinished && !data.get(position - 1).isFinished)
                )
        ) {
            remoteView.setTextViewText(R.id.sublist_title, Helpers.getLocalizedResources(new Locale(settings.lang)).getString(R.string.widget_list_finished_section));
            remoteView.setInt(R.id.sublist_title, "setVisibility", View.VISIBLE);
        } else {
            remoteView.setInt(R.id.sublist_title, "setVisibility", View.GONE);
        }

        remoteView.setInt(R.id.finish_img, "setImageResource", note.isFinished ? R.drawable.checkbox_checked : R.drawable.checkbox);

        Intent itemClickIntent = new Intent();
        itemClickIntent.putExtra("item_id", note.id);
        itemClickIntent.putExtra("action_target", "item");
        remoteView.setOnClickFillInIntent(R.id.note, itemClickIntent);

        Intent finishClickIntent = new Intent();
        finishClickIntent.putExtra("item_id", note.id);
        finishClickIntent.putExtra("action_target", "finish");
        remoteView.setOnClickFillInIntent(R.id.finish, finishClickIntent);

        return remoteView;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public void onDataSetChanged() {
        SharedPreferences sp = context.getSharedPreferences(WidgetProvider.WIDGET_SP, Context.MODE_PRIVATE);
        int _type = sp.getInt(WidgetProvider.WIDGET_SP_LIST_TYPE + "_" + widgetID,  1);
        NoteTypes type = NoteTypes.getDefinition(_type);

        Calendar date = DateHelper.startOf(DateHelper.convertFromLocalToUTC(Calendar.getInstance()), "day");

        settings = SettingsRepository.getInstance().getSettings();

        if (settings.password != null) {
            data = new ArrayList<>();
            return;
        }

        data = NoteRepository.getInstance().getNotes(type, date, settings);
    }

    @Override
    public void onDestroy() {

    }
}