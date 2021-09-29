package com.dailylist.vadimsemenyk.natives;

import android.app.Application;
import android.content.Context;

public class App extends Application {
    private static Context context = null;

    public void onCreate() {
        super.onCreate();
        App.context = getApplicationContext();
    }

    public static Context getAppContext() {
        return App.context;
    }
}
