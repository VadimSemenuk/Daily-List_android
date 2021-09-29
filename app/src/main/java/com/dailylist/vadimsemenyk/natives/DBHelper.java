package com.dailylist.vadimsemenyk.natives;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {
    private static DBHelper instance = null;

    private static String DBName = "com.mamindeveloper.dailylist.db";
    private static int DBVersion = 1;

    public DBHelper(Context context) {
        super(context, DBName, null, DBVersion);
    }

    public static synchronized DBHelper getInstance() {
        return getInstance(App.getAppContext());
    }

    public static synchronized DBHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DBHelper(context);
        }
        return instance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }
}