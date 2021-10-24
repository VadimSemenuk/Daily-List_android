package com.dailylist.vadimsemenyk.natives.Models;

import com.dailylist.vadimsemenyk.natives.Enums.NoteRepeatTypes;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;

public class Note implements Serializable {
    public int id;

    public String title;
    public ArrayList<NoteContentItem> contentItems;
    public Boolean isFinished;
    public Calendar date;
    public Calendar startTime;
    public Calendar endTime;
    public boolean isNotificationEnabled;
    public String colorTag;
    public Integer manualOrderIndex;
    public Integer forkFrom;
    public boolean isShadow;
    public NoteRepeatTypes repeatType;
    public ArrayList<Long> repeatValues;
    public Calendar repeatItemDate;
    public Calendar repeatStartDate;
    public Calendar repeatEndDate;

    public Note() {}
}