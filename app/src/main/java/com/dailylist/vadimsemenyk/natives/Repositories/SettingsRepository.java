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
        // TODO: check for null pointer exception

        Settings settings = new Settings();

        String sql = "SELECT sortFinBehaviour, sortType, sortDirection, lang, autoMoveNotFinishedNotes, password FROM Settings;";
        Cursor cursor = DBHelper.getInstance().getReadableDatabase().rawQuery(sql, null);

        if (cursor.moveToFirst()) {
            do {

                settings.sortFinBehaviour = cursor.getInt(cursor.getColumnIndex("sortFinBehaviour"));
                settings.sortType = SortType.getDefinition(cursor.getInt(cursor.getColumnIndex("sortType")));
                settings.sortDirection = SortDirection.getDefinition(cursor.getInt(cursor.getColumnIndex("sortDirection")));
                settings.lang = cursor.getString(cursor.getColumnIndex("lang"));
                settings.autoMoveNotFinishedNotes = cursor.getInt(cursor.getColumnIndex("autoMoveNotFinishedNotes")) == 1;
                settings.password = cursor.getString(cursor.getColumnIndex("password"));
            }
            while (cursor.moveToNext());
        }
        cursor.close();

        return settings;
    }
}
