package com.dailylist.vadimsemenyk.natives.Notifications;

import java.util.ArrayList;
import java.util.Calendar;

public class TriggerOptions {
    public int id;
    public Calendar dateTime;
    public ArrayList<Integer> noteIDs;
    public boolean shouldReschedule;
}
