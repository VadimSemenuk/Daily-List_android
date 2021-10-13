package com.dailylist.vadimsemenyk.natives.Enums;

import com.google.gson.annotations.SerializedName;

import java.util.HashMap;
import java.util.Map;

public enum NoteRepeatTypes {
    @SerializedName("no-repeat")
    NO_REPEAT("no-repeat"),
    @SerializedName("day")
    DAY("day"),
    @SerializedName("week")
    WEEK("week"),
    @SerializedName("any")
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

    public String getValue() {
        return value;
    }
}
