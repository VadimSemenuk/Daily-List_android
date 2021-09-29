package com.dailylist.vadimsemenyk.natives.Models;

import com.dailylist.vadimsemenyk.natives.Widget.NoteContentItem;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;

public class Note implements Serializable {
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

    public Note() {}
}