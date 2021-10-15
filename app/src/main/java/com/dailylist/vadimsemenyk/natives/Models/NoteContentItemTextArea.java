package com.dailylist.vadimsemenyk.natives.Models;

import java.io.Serializable;

public class NoteContentItemTextArea extends NoteContentItem implements Serializable {
    public NoteContentItemTextArea(String text) {
        this.value = text;
    }
}