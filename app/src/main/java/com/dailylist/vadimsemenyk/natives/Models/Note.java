package com.dailylist.vadimsemenyk.natives.Models;

import com.dailylist.vadimsemenyk.natives.Enums.NoteRepeatTypes;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;

public class Note implements Serializable {
    // TODO: rename startDateTime to startTime, endDateTime to endTime

    public int id;

    public String colorTag;
    public Calendar startDateTime;
    public Calendar endDateTime;
    public Boolean isFinished;
    public String title;
    public ArrayList<NoteContentItem> contentItems;
    public Integer manualOrderIndex;
    public Integer forkFrom;
    public boolean isShadow;
    public NoteRepeatTypes repeatType;
    public ArrayList<Long> repeatValues;
    public Calendar repeatItemDate;
    public Calendar date;
    public boolean isNotificationEnabled;

    public Note() {}
}