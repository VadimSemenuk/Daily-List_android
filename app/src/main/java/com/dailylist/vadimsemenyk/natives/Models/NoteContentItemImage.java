package com.dailylist.vadimsemenyk.natives.Models;

import java.io.Serializable;

public class NoteContentItemImage extends NoteContentItem implements Serializable {
    public NoteContentItemImage(String text) {
        this.value = text;
    }
}