package com.dailylist.vadimsemenyk.natives.Helpers;

import android.content.Intent;

import com.dailylist.vadimsemenyk.natives.App;

public class Helpers {
    public Helpers() { }

    static public void launchApp() {
        Intent launchIntent = App.getAppContext().getPackageManager().getLaunchIntentForPackage("com.dailylist.vadimsemenyk");
        if (launchIntent != null) {
            App.getAppContext().startActivity(launchIntent);
        }
    }
}
