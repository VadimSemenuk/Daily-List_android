package com.dailylist.vadimsemenyk.natives.Repositories;

import android.database.Cursor;

import com.dailylist.vadimsemenyk.natives.DBHelper;
import com.dailylist.vadimsemenyk.natives.Enums.NoteTypes;
import com.dailylist.vadimsemenyk.natives.Enums.SortDirection;
import com.dailylist.vadimsemenyk.natives.Enums.SortType;
import com.dailylist.vadimsemenyk.natives.Models.Note;
import com.dailylist.vadimsemenyk.natives.Models.Settings;
import com.dailylist.vadimsemenyk.natives.Widget.NoteContentItem;
import com.dailylist.vadimsemenyk.natives.Widget.NoteContentItemListItem;
import com.dailylist.vadimsemenyk.natives.Widget.NoteContentItemTextArea;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.TimeZone;

public class NoteRepository {
    private static final NoteRepository ourInstance = new NoteRepository();

    public static NoteRepository getInstance() {
        return ourInstance;
    }

    private NoteRepository() {
    }

    static class SortByAddedTime implements Comparator<Object> {
        SortDirection direction;

        private SortByAddedTime(SortDirection direction) {
            this.direction = direction;
        }

        public int compare(Object a, Object b) {
            Note noteA = (Note) a;
            Note noteB = (Note) b;

            if (noteA.manualOrderIndex == null && noteB.manualOrderIndex == null) {
                int aVal = noteA.forkFrom != null ? noteA.forkFrom : noteA.id;
                int bVal = noteB.forkFrom != null ? noteB.forkFrom : noteB.id;

                return direction == SortDirection.ASC ? aVal - bVal : bVal - aVal;
            } else if (noteA.manualOrderIndex == null) {
                return 1;
            } else if (noteB.manualOrderIndex == null) {
                return -1;
            } else {
                return noteA.manualOrderIndex - noteB.manualOrderIndex;
            }
        }
    }

    static class SortByNoteTime implements Comparator<Object> {
        SortDirection direction;

        private SortByNoteTime(SortDirection direction) {
            this.direction = direction;
        }

        public int compare(Object a, Object b) {
            Note noteA = (Note) a;
            Note noteB = (Note) b;
            int noteASortValue = 0;
            int noteBSortValue = 0;

            if (noteA.startDateTime != null) {
                Calendar msNoteAStartOfDay = Calendar.getInstance();
                msNoteAStartOfDay.setTimeInMillis(noteA.startDateTime.getTimeInMillis());
                startOfDay(msNoteAStartOfDay);
                noteASortValue = (int) (noteA.startDateTime.getTimeInMillis() - msNoteAStartOfDay.getTimeInMillis());
            }

            if (noteB.startDateTime != null) {
                Calendar msNoteBStartOfDay = Calendar.getInstance();
                msNoteBStartOfDay.setTimeInMillis(noteB.startDateTime.getTimeInMillis());
                startOfDay(msNoteBStartOfDay);
                noteBSortValue = (int) (noteB.startDateTime.getTimeInMillis() - msNoteBStartOfDay.getTimeInMillis());
            }

            if (direction == SortDirection.ASC) {
                return noteASortValue - noteBSortValue;
            } else {
                return noteBSortValue - noteASortValue;
            }
        }
    }

    static class SortByFinished implements Comparator<Object> {

        private SortByFinished() {
        }

        public int compare(Object a, Object b) {
            return (((Note) a).isFinished ? 1 : 0) - (((Note) b).isFinished ? 1 : 0);
        }
    }

    public ArrayList<Note> getNotes(NoteTypes type, Calendar date, Settings settings) {
        ArrayList<Note> notes = new ArrayList<>();

        Cursor cursor = null;

        if (type == NoteTypes.Diary) {
            String sql = "SELECT id, tag, startTime, endTime, isFinished, title, contentItems, manualOrderIndex, forkFrom, date, mode"
                    + " FROM Notes n"
                    + " LEFT JOIN NotesRepeatValues rep ON n.id = rep.noteId"
                    + " WHERE"
                    + " n.lastAction != ?"
                    + " AND ("
                    + "     n.date = ?"
                    + "     OR ("
                    + "         n.date IS NULL AND NOT EXISTS (SELECT forkFrom FROM Notes WHERE forkFrom = n.id AND date = ?)"
                    + "         AND ("
                    + "             n.repeatType = ?"
                    + "             OR (n.repeatType = ? AND rep.value = ?)"
                    + "             OR (n.repeatType = ? AND rep.value = ?)"
                    + "         )"
                    + "     )"
                    + " )"
                    + " AND n.mode = ?;";

            cursor = DBHelper.getInstance().getWritableDatabase().rawQuery(
                    sql,
                    new String[] {"DELETE", String.valueOf(date.getTimeInMillis()), String.valueOf(date.getTimeInMillis()), "day", "week", String.valueOf(getDayOfWeekNumber(date)), "any", String.valueOf(date.getTimeInMillis()), Integer.toString(NoteTypes.Diary.getValue())}
            );
        } else {
            String sql = "SELECT id, tag, isFinished, title, contentItems, manualOrderIndex, forkFrom, date, mode"
                    + " FROM Notes"
                    + " WHERE"
                    + " lastAction != ?"
                    + " AND mode = ?;";

            cursor = DBHelper.getInstance().getWritableDatabase().rawQuery(
                    sql,
                    new String[] {"DELETE", Integer.toString(NoteTypes.Note.getValue())}
            );
        }

        if (cursor.moveToFirst()) {
            do {
                Note note = new Note();

                note.id = cursor.getInt(cursor.getColumnIndex("id"));

                note.colorTag = cursor.getString(cursor.getColumnIndex("tag"));

                if (!cursor.isNull(cursor.getColumnIndex("startTime"))) {
                    long _startDateTime = cursor.getLong(cursor.getColumnIndex("startTime"));
                    Calendar startDateTime = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                    startDateTime.setTimeInMillis(_startDateTime);
                    note.startDateTime = convertFromUTCToLocal(startDateTime);
                }

                if (!cursor.isNull(cursor.getColumnIndex("endTime"))) {
                    long _endDateTime = cursor.getLong(cursor.getColumnIndex("endTime"));
                    Calendar endDateTime = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                    endDateTime.setTimeInMillis(_endDateTime);
                    note.endDateTime = convertFromUTCToLocal(endDateTime);
                }

                note.isFinished = cursor.getInt(cursor.getColumnIndex("isFinished")) == 1;

                note.title = cursor.getString(cursor.getColumnIndex("title"));

                String contentItemsJson = cursor.getString(cursor.getColumnIndex("contentItems"));
                ArrayList<NoteContentItem> contentItems = new ArrayList<NoteContentItem>();
                Gson gson = new Gson();
                JsonParser parser = new JsonParser();
                if (contentItemsJson.length() > 0) {
                    JsonArray array = parser.parse(contentItemsJson).getAsJsonArray();
                    for (int i = 0; i < array.size(); i++) {
                        NoteContentItem contentField = null;

                        if (array.get(i) instanceof JsonObject) {
                            JsonObject obj = array.get(i).getAsJsonObject();
                            String contentItemType = obj.get("type").getAsString();
                            if (contentItemType.equals("listItem")) {
                                contentField = gson.fromJson(array.get(i), NoteContentItemListItem.class);
                            } else if (contentItemType.equals("text")) {
                                contentField = gson.fromJson(array.get(i), NoteContentItemTextArea.class);
                            }
                            contentItems.add(contentField);
                        }
                    }
                }
                note.contentItems = contentItems;

                note.manualOrderIndex = cursor.isNull(cursor.getColumnIndex("manualOrderIndex")) ? null : cursor.getInt(cursor.getColumnIndex("manualOrderIndex"));

                note.forkFrom = cursor.isNull(cursor.getColumnIndex("forkFrom")) ? null : cursor.getInt(cursor.getColumnIndex("forkFrom"));

                notes.add(note);
            }
            while (cursor.moveToNext());
        }
        cursor.close();

        if (settings.sortType == SortType.NOTE_TIME) {
            Collections.sort(notes, new SortByNoteTime(settings.sortDirection));
        } else {
            Collections.sort(notes, new SortByAddedTime(settings.sortDirection));
        }

        if (settings.sortFinBehaviour == 1) {
            Collections.sort(notes, new SortByFinished());
        }

        return notes;
    }

    public void moveNotFinishedNotesForToday() {
        Calendar todayDateTime = convertFromLocalToUTC(startOfDay(Calendar.getInstance()));

        String sql = "UPDATE Notes"
                + " SET date = ?"
                + ", manualOrderIndex = null"
                + ", lastAction = ?, lastActionTime = ?"
                + " WHERE repeatType = ? AND isFinished = 0 AND date < ?;";

        DBHelper.getInstance().getWritableDatabase().execSQL(
                sql,
                new String[] {
                        Long.toString(todayDateTime.getTimeInMillis()),
                        "UPDATE",
                        Long.toString(Calendar.getInstance().getTimeInMillis()),
                        "no-repeat",
                        Long.toString(Calendar.getInstance().getTimeInMillis()),
                }
        );
    }

    public void triggerNoteFinishState(int noteId) {
        Cursor noteCursor = getRawNote(noteId);

        boolean nextState = !(noteCursor.getInt(noteCursor.getColumnIndex("isFinished")) == 1);
        boolean isShadow = noteCursor.isNull(noteCursor.getColumnIndex("date")) && (NoteTypes.valueOf(noteCursor.getInt(noteCursor.getColumnIndex("mode"))) == NoteTypes.Diary);

        noteCursor.close();

        if (isShadow) {
            noteId = formShadowToReal(noteId);
        }

        Settings settings = SettingsRepository.getInstance().getSettings();

        boolean resetManualOrderIndex = false;
        if (settings.sortFinBehaviour == 1 && nextState) {
            resetManualOrderIndex = true;
        }

        String sql = "UPDATE Notes"
                + " SET isFinished = ?"
                + (resetManualOrderIndex ? ", manualOrderIndex = null" : "")
                + " WHERE id = ?;";

        DBHelper.getInstance().getWritableDatabase().execSQL(
                sql,
                new String[] {Integer.toString(nextState ? 1 : 0), Integer.toString(noteId)}
        );

        updateNoteLastAction(noteId, "UPDATE");
    }

    private Cursor getRawNote(int id) {
        String sql = "SELECT id, title, startTime, endTime, isNotificationEnabled, tag, repeatType, contentItems, isFinished, date, forkFrom, mode, manualOrderIndex, tags, lastAction, lastActionTime"
                + " FROM Notes"
                + " WHERE id = ?;";

        Cursor cursor = DBHelper.getInstance().getWritableDatabase().rawQuery(
                sql,
                new String[] {Integer.toString(id)}
        );

        cursor.moveToNext();

        return cursor;
    }

    private Integer formShadowToReal(int id) {
        Integer nextNoteId = null;

        Calendar date = NoteRepository.convertFromLocalToUTC(Calendar.getInstance());
        NoteRepository.startOfDay(date);

        String insertSQL = "INSERT INTO Notes (title, startTime, endTime, isNotificationEnabled, tag, repeatType, contentItems, isFinished, date, forkFrom, mode, manualOrderIndex, tags)"
                + " SELECT title, startTime, endTime, isNotificationEnabled, tag, repeatType, contentItems, isFinished, ? AS date, ? AS forkFrom, mode, manualOrderIndex, tags"
                + " FROM Notes"
                + " WHERE id = ?;";

        DBHelper.getInstance().getWritableDatabase().execSQL(
                insertSQL,
                new String[] {Long.toString(date.getTimeInMillis()), Integer.toString(id), Integer.toString(id)}
        );

        String idSQL = "SELECT last_insert_rowid()";
        Cursor cursor = DBHelper.getInstance().getWritableDatabase().rawQuery(idSQL, null);

        if (cursor.moveToFirst()) {
            do {
                nextNoteId = cursor.getInt(0);
            }
            while (cursor.moveToNext());
        }
        cursor.close();

        updateNoteLastAction(nextNoteId, "ADD");

        return nextNoteId;
    }

    private void updateNoteLastAction(int id, String action) {
        String insertSQL = "UPDATE Notes"
                + " SET lastAction = ?, lastActionTime = ?"
                + " WHERE id = ?;";

        DBHelper.getInstance().getWritableDatabase().execSQL(
                insertSQL,
                new String[]{
                        action,
                        Long.toString(Calendar.getInstance().getTimeInMillis()),
                        Integer.toString(id)
                }
        );
    }

    public static Calendar convertFromUTCToLocal(Calendar utcDateTime) {
        int utcYear = utcDateTime.get(Calendar.YEAR);
        int utcMonth = utcDateTime.get(Calendar.MONTH);
        int utcDate = utcDateTime.get(Calendar.DATE);
        int utcHour = utcDateTime.get(Calendar.HOUR_OF_DAY);
        int utcMinute = utcDateTime.get(Calendar.MINUTE);

        Calendar dateTimeLocal = Calendar.getInstance();
        dateTimeLocal.set(utcYear, utcMonth, utcDate, utcHour, utcMinute, 0);

        return dateTimeLocal;
    }

    public static Calendar convertFromLocalToUTC(Calendar localDateTime) {
        int year = localDateTime.get(Calendar.YEAR);
        int month = localDateTime.get(Calendar.MONTH);
        int date = localDateTime.get(Calendar.DATE);
        int hour = localDateTime.get(Calendar.HOUR_OF_DAY);
        int minute = localDateTime.get(Calendar.MINUTE);

        Calendar dateUTC = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        dateUTC.set(year, month, date, hour, minute, 0);
        dateUTC.set(Calendar.MILLISECOND, 0);

        return dateUTC;
    }

    public static Calendar startOfDay(Calendar dateTime) {
        dateTime.set(Calendar.HOUR_OF_DAY, 0);
        dateTime.set(Calendar.MINUTE, 0);
        dateTime.set(Calendar.SECOND, 0);
        dateTime.set(Calendar.MILLISECOND, 0);

        return dateTime;
    }

    private int getDayOfWeekNumber(Calendar date) {
        int dayOfWeek = date.get(Calendar.DAY_OF_WEEK);
        int dayOfWeekNumber = 0;

        switch(dayOfWeek) {
            case Calendar.MONDAY:
                dayOfWeekNumber = 1;
                break;
            case Calendar.TUESDAY:
                dayOfWeekNumber = 2;
                break;
            case Calendar.WEDNESDAY:
                dayOfWeekNumber = 3;
                break;
            case Calendar.THURSDAY:
                dayOfWeekNumber = 4;
                break;
            case Calendar.FRIDAY:
                dayOfWeekNumber = 5;
                break;
            case Calendar.SATURDAY:
                dayOfWeekNumber = 6;
                break;
            case Calendar.SUNDAY:
                dayOfWeekNumber = 7;
                break;
            default:
                dayOfWeekNumber = 1;
        }

        return dayOfWeekNumber;
    }
}