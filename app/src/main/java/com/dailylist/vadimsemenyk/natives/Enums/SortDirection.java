package com.dailylist.vadimsemenyk.natives.Enums;

import java.util.HashMap;
import java.util.Map;

public enum SortDirection {
    DESC(1),
    ASC(2);

    private final int value;
    private static Map map = new HashMap<>();

    private SortDirection(int value) {
        this.value = value;
    }

    static {
        for (SortDirection noteAction : SortDirection.values()) {
            map.put(noteAction.value, noteAction);
        }
    }

    public static SortDirection valueOf(int noteAction) {
        return (SortDirection) map.get(noteAction);
    }

    public int getValue() {
        return value;
    }
}
