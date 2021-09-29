package com.dailylist.vadimsemenyk.natives;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class DayChangeReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() == null) {
            return;
        }

        if (
            intent.getAction().equalsIgnoreCase(DayChangeHandler.ACTION_DAY_CHANGED)
            || intent.getAction().equalsIgnoreCase(Intent.ACTION_TIMEZONE_CHANGED)
            || intent.getAction().equalsIgnoreCase(Intent.ACTION_BOOT_COMPLETED)
        ) {
            DayChangeHandler.onDateChange(context);
        }
//            if (SDK_INT >= 24) {
//                UserManager um = (UserManager) context.getSystemService(UserManager.class);
//                if (um == null || !um.isUserUnlocked()) {
//                    return;
//                }
//            }

    }
}
