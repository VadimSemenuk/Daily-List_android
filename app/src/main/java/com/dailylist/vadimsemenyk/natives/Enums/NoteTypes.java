package com.dailylist.vadimsemenyk.natives.Enums;

import java.util.HashMap;
import java.util.Map;

public enum NoteTypes {
    Diary(1),
    Note(2);

    private final int value;
    private static Map map = new HashMap<>();

    private NoteTypes(int value) {
        this.value = value;
    }

    static {
        for (NoteTypes noteType : NoteTypes.values()) {
            map.put(noteType.value, noteType);
        }
    }

    public static NoteTypes valueOf(int noteTypes) {
        return (NoteTypes) map.get(noteTypes);
    }

    public int getValue() {
        return value;
    }
}