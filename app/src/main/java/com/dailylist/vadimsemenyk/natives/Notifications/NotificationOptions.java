package com.dailylist.vadimsemenyk.natives.Notifications;

import com.dailylist.vadimsemenyk.natives.Enums.NoteRepeatTypes;

import java.util.ArrayList;

public class NotificationOptions {
    public NotificationOptions() {}

    public int id;
    public String title;
    public String text;
    public NoteRepeatTypes repeatType;
    public Long triggerTimeUTCMS;

    // no-repeat
    public Long triggerDateUTCMS;

    // repeat
    public ArrayList<Long> repeatValues;
}
