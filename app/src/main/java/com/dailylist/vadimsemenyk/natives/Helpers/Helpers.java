package com.dailylist.vadimsemenyk.natives.Helpers;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;

import com.dailylist.vadimsemenyk.natives.App;

import java.util.Locale;

public class Helpers {
    public Helpers() { }

    static public void launchApp() {
        Intent launchIntent = App.getAppContext().getPackageManager().getLaunchIntentForPackage("com.dailylist.vadimsemenyk");
        if (launchIntent != null) {
            App.getAppContext().startActivity(launchIntent);
        }
    }

    // TODO: create class with cache for each localizations resource reference
    static public Resources getLocalizedResources(Locale desiredLocale) {
        Configuration conf = App.getAppContext().getResources().getConfiguration();
        conf = new Configuration(conf);
        conf.setLocale(desiredLocale);
        Context localizedContext = App.getAppContext().createConfigurationContext(conf);
        return localizedContext.getResources();
    }
}
