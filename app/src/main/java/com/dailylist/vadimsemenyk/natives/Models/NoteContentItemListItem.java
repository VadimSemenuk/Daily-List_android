package com.dailylist.vadimsemenyk.natives.Models;

import java.io.Serializable;

public class NoteContentItemListItem extends NoteContentItem implements Serializable {
    public String value;
    public Boolean checked;

    public NoteContentItemListItem(String text, Boolean isChecked) {
        this.value = text;
        this.checked = isChecked;
    }
}