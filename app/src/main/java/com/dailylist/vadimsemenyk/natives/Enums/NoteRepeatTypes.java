package com.dailylist.vadimsemenyk.natives.Enums;

import java.util.HashMap;
import java.util.Map;

public enum NoteRepeatTypes {
    NO_REPEAT("no-repeat"),
    DAY("day"),
    WEEK("week"),
    ANY("any");

    private final String value;
    private static Map map = new HashMap<>();

    private NoteRepeatTypes(String value) {
        this.value = value;
    }

    static {
        for (NoteRepeatTypes noteRepeatType : NoteRepeatTypes.values()) {
            map.put(noteRepeatType.value, noteRepeatType);
        }
    }

    public static NoteRepeatTypes getDefinition(String noteTypes) {
        return (NoteRepeatTypes) map.get(noteTypes);
    }

    public String getValue() {
        return value;
    }
}
