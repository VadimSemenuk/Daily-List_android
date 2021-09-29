package com.dailylist.vadimsemenyk.natives;


import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;


public class DBHelper extends SQLiteOpenHelper {
    private static DBHelper instance = null;

    public DBHelper(Context context) {
        super(context, "com.mamindeveloper.dailylist.db", null, 1);
    }

    public static void createInstance(Context context) {
        if (instance == null) {
            instance = new DBHelper(context);
        }
    }

    public static DBHelper getInstance() {
        return instance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }
}