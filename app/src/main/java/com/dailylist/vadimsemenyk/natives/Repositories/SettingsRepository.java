package com.dailylist.vadimsemenyk.natives.Repositories;

import android.database.Cursor;

import com.dailylist.vadimsemenyk.natives.DBHelper;
import com.dailylist.vadimsemenyk.natives.Enums.SortDirection;
import com.dailylist.vadimsemenyk.natives.Enums.SortType;
import com.dailylist.vadimsemenyk.natives.Models.Settings;

public class SettingsRepository {
    private static final SettingsRepository ourInstance = new SettingsRepository();

    public static SettingsRepository getInstance() {
        return ourInstance;
    }

    private SettingsRepository() {}

    public Settings getSettings() {
        Settings settings = new Settings();

        String sql = "SELECT sortFinBehaviour, sortType, sortDirection, lang, autoMoveNotFinishedNotes FROM Settings;";
        Cursor cursor = DBHelper.getInstance().getWritableDatabase().rawQuery(sql, null);

        if (cursor.moveToFirst()) {
            do {

                settings.sortFinBehaviour = cursor.getInt(cursor.getColumnIndex("sortFinBehaviour"));
                settings.sortType = SortType.valueOf(cursor.getInt(cursor.getColumnIndex("sortType")));
                settings.sortDirection = SortDirection.valueOf(cursor.getInt(cursor.getColumnIndex("sortDirection")));
                settings.lang = cursor.getString(cursor.getColumnIndex("lang"));
                settings.autoMoveNotFinishedNotes = cursor.getInt(cursor.getColumnIndex("autoMoveNotFinishedNotes")) == 1;
            }
            while (cursor.moveToNext());
        }
        cursor.close();

        return settings;
    }
}
