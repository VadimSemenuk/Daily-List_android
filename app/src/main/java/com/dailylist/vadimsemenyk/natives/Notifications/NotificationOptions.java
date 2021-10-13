package com.dailylist.vadimsemenyk.natives.Notifications;

import com.dailylist.vadimsemenyk.natives.Enums.NoteRepeatTypes;

import java.util.ArrayList;
import java.util.Calendar;

public class NotificationOptions {
    public NotificationOptions() {}

    public int id;
    public String title;
    public String text;
    public NoteRepeatTypes repeatType;
    public Long triggerTimeUTCMS;
    public Calendar triggerTime;

    // no-repeat
    public Long triggerDateUTCMS;
    public Calendar triggerDate;

    // repeat
    public ArrayList<Long> repeatValues;
}