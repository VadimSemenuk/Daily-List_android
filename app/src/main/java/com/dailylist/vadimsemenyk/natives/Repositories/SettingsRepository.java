package com.dailylist.vadimsemenyk.natives.Repositories;

import android.database.Cursor;

import com.dailylist.vadimsemenyk.natives.DBHelper;
import com.dailylist.vadimsemenyk.natives.Enums.SortDirection;
import com.dailylist.vadimsemenyk.natives.Enums.SortType;
import com.dailylist.vadimsemenyk.natives.Models.Settings;

public class SettingsRepository {
    static private SettingsRepository instance = null;

    private SettingsRepository() {}

    static public SettingsRepository getInstance() {
        if (instance == null) {
            instance = new SettingsRepository();
        }
        return instance;
    }

    public Settings getSettings() {
        Settings settings = new Settings();

        String sql = "SELECT sortFinBehaviour, sortType, sortDirection, lang, autoMoveNotFinishedNotes, password, showNotificationForFinishedNotes FROM Settings;";
        Cursor cursor = DBHelper.getInstance().getReadableDatabase().rawQuery(sql, null);

        if (cursor.moveToFirst()) {
            do {
                settings.sortFinBehaviour = cursor.getInt(cursor.getColumnIndex("sortFinBehaviour"));
                settings.sortType = SortType.getDefinition(cursor.getInt(cursor.getColumnIndex("sortType")));
                settings.sortDirection = SortDirection.getDefinition(cursor.getInt(cursor.getColumnIndex("sortDirection")));
                settings.lang = cursor.getString(cursor.getColumnIndex("lang"));
                settings.autoMoveNotFinishedNotes = cursor.getInt(cursor.getColumnIndex("autoMoveNotFinishedNotes")) == 1;
                settings.password = cursor.getString(cursor.getColumnIndex("password"));
                settings.showNotificationForFinishedNotes = cursor.getInt(cursor.getColumnIndex("showNotificationForFinishedNotes")) == 1;
            }
            while (cursor.moveToNext());
        }
        cursor.close();

        return settings;
    }
}
