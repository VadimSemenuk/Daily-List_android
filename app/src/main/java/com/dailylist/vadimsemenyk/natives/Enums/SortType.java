package com.dailylist.vadimsemenyk.natives.Enums;

import java.util.HashMap;
import java.util.Map;

public enum SortType {
    NOTE_TIME(1),
    ADDED_TIME(2);

    private final int value;
    private static Map map = new HashMap<>();

    private SortType(int value) {
        this.value = value;
    }

    static {
        for (SortType noteAction : SortType.values()) {
            map.put(noteAction.value, noteAction);
        }
    }

    public static SortType valueOf(int noteAction) {
        return (SortType) map.get(noteAction);
    }

    public int getValue() {
        return value;
    }
}