package com.dailylist.vadimsemenyk.natives.Repositories;

import android.database.Cursor;

import com.dailylist.vadimsemenyk.natives.DBHelper;
import com.dailylist.vadimsemenyk.natives.Enums.NoteActions;
import com.dailylist.vadimsemenyk.natives.Enums.NoteRepeatTypes;
import com.dailylist.vadimsemenyk.natives.Enums.NoteTypes;
import com.dailylist.vadimsemenyk.natives.Enums.SortDirection;
import com.dailylist.vadimsemenyk.natives.Enums.SortType;
import com.dailylist.vadimsemenyk.natives.Helpers.DateHelper;
import com.dailylist.vadimsemenyk.natives.Models.Note;
import com.dailylist.vadimsemenyk.natives.Models.Settings;
import com.dailylist.vadimsemenyk.natives.Models.NoteContentItem;
import com.dailylist.vadimsemenyk.natives.Models.NoteContentItemListItem;
import com.dailylist.vadimsemenyk.natives.Models.NoteContentItemTextArea;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.TimeZone;

public class NoteRepository {
    private String noteSQLFields = "id, title, contentItems, startTime, endTime, isNotificationEnabled, tag, isFinished, tags, manualOrderIndex, date, mode, repeatType, forkFrom, repeatItemDate"
            + ", (select GROUP_CONCAT(nrv.value, ',') from NotesRepeatValues nrv where nrv.noteId = n.id OR nrv.noteId = n.forkFrom) as repeatValues";

    private static final NoteRepository ourInstance = new NoteRepository();

    public static NoteRepository getInstance() {
        return ourInstance;
    }

    private NoteRepository() { }

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
                Calendar msNoteAStartOfDay = DateHelper.startOf(DateHelper.getCalendar(noteA.startDateTime.getTimeInMillis()), "day");
                noteASortValue = (int) (noteA.startDateTime.getTimeInMillis() - msNoteAStartOfDay.getTimeInMillis());
            }

            if (noteB.startDateTime != null) {
                Calendar msNoteBStartOfDay = DateHelper.startOf(DateHelper.getCalendar(noteB.startDateTime.getTimeInMillis()), "day");
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
            String sql = "SELECT " + noteSQLFields
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

            cursor = DBHelper.getInstance().getReadableDatabase().rawQuery(
                    sql,
                    new String[] {
                            NoteActions.DELETE.name(),
                            String.valueOf(date.getTimeInMillis()),
                            String.valueOf(date.getTimeInMillis()),
                            NoteRepeatTypes.DAY.getValue(),
                            NoteRepeatTypes.WEEK.getValue(),
                            String.valueOf(DateHelper.getDayOfWeekNumber(date)),
                            NoteRepeatTypes.ANY.getValue(),
                            String.valueOf(date.getTimeInMillis()),
                            Integer.toString(NoteTypes.Diary.getValue())
                    }
            );
        } else {
            String sql = "SELECT id, tag, isFinished, title, contentItems, manualOrderIndex, forkFrom, date, mode"
                    + " FROM Notes"
                    + " WHERE"
                    + " lastAction != ?"
                    + " AND mode = ?;";

            cursor = DBHelper.getInstance().getReadableDatabase().rawQuery(
                    sql,
                    new String[] {NoteActions.DELETE.name(), Integer.toString(NoteTypes.Note.getValue())}
            );
        }

        if (cursor.moveToFirst()) {
            do {
                notes.add(getNoteFromCursor(cursor));
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

    private Note getNote(int id) {
        return getNotes("n.id = ?", new String[] {Integer.toString(id)}).get(0);
    }

    public ArrayList<Note> getNotes(String conditions, String[] conditionsValues) {
        String sql = "SELECT " + noteSQLFields
                + " FROM Notes n"
                + " WHERE " + conditions + ";";

        Cursor cursor = DBHelper.getInstance().getReadableDatabase().rawQuery(sql, conditionsValues);

        ArrayList<Note> notes = new ArrayList<>();

        if (cursor.moveToFirst()) {
            do {
                notes.add(getNoteFromCursor(cursor));
            }
            while (cursor.moveToNext());
        }
        cursor.close();

        return notes;
    }

    private Note getNoteFromCursor(Cursor cursor) {
        Note note = new Note();

        note.id = cursor.getInt(cursor.getColumnIndex("id"));

        note.colorTag = cursor.getString(cursor.getColumnIndex("tag"));

        if (!cursor.isNull(cursor.getColumnIndex("startTime"))) {
            long _startDateTime = cursor.getLong(cursor.getColumnIndex("startTime"));
            note.startDateTime = DateHelper.convertFromUTCToLocal(DateHelper.getCalendar(_startDateTime, TimeZone.getTimeZone("UTC")));
        }

        if (!cursor.isNull(cursor.getColumnIndex("endTime"))) {
            long _endDateTime = cursor.getLong(cursor.getColumnIndex("endTime"));
            note.endDateTime = DateHelper.convertFromUTCToLocal(DateHelper.getCalendar(_endDateTime, TimeZone.getTimeZone("UTC")));
        }

        if (!cursor.isNull(cursor.getColumnIndex("date"))) {
            long _date = cursor.getLong(cursor.getColumnIndex("date"));
            note.date = DateHelper.convertFromUTCToLocal(DateHelper.getCalendar(_date, TimeZone.getTimeZone("UTC")));
        }

        if (!cursor.isNull(cursor.getColumnIndex("repeatItemDate"))) {
            long _repeatItemDate = cursor.getLong(cursor.getColumnIndex("repeatItemDate"));
            note.repeatItemDate = DateHelper.convertFromUTCToLocal(DateHelper.getCalendar(_repeatItemDate, TimeZone.getTimeZone("UTC")));
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

        note.isShadow = cursor.isNull(cursor.getColumnIndex("date")) && (NoteTypes.valueOf(cursor.getInt(cursor.getColumnIndex("mode"))) == NoteTypes.Diary);

        note.repeatType = NoteRepeatTypes.valueOf(cursor.getString(cursor.getColumnIndex("repeatType")));

        ArrayList<String> repeatValues = cursor.isNull(cursor.getColumnIndex("repeatValues")) ? null : new ArrayList<>(Arrays.asList(cursor.getString(cursor.getColumnIndex("repeatValues")).split(",")));
        note.repeatValues = new ArrayList<Long>();
        for (String repeatValue : repeatValues) {
            note.repeatValues.add(Long.parseLong(repeatValue));
        }

        return note;
    }

    public void moveNotFinishedNotesForToday() {
        Calendar todayDateTime = DateHelper.convertFromLocalToUTC(DateHelper.startOf(Calendar.getInstance(), "day"));

        String sql = "UPDATE Notes"
                + " SET date = ?"
                + ", manualOrderIndex = null"
                + ", lastAction = ?, lastActionTime = ?"
                + " WHERE repeatType = ? AND isFinished = 0 AND date < ?;";

        DBHelper.getInstance().getWritableDatabase().execSQL(
                sql,
                new String[] {
                        Long.toString(todayDateTime.getTimeInMillis()),
                        NoteActions.UPDATE.name(),
                        Long.toString(Calendar.getInstance().getTimeInMillis()),
                        NoteRepeatTypes.NO_REPEAT.getValue(),
                        Long.toString(Calendar.getInstance().getTimeInMillis()),
                }
        );
    }

    public void triggerNoteFinishState(int noteId) {
        Note note = getNote(noteId);

        boolean nextState = !note.isFinished;

        if (note.isShadow) {
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

        updateNoteLastAction(noteId, NoteActions.UPDATE.name());
    }

    private Integer formShadowToReal(int id) {
        Integer nextNoteId = null;

        Calendar date = DateHelper.startOf(DateHelper.convertFromLocalToUTC(Calendar.getInstance()), "day");

        String insertSQL = "INSERT INTO Notes (title, startTime, endTime, isNotificationEnabled, tag, repeatType, contentItems, isFinished, date, forkFrom, mode, manualOrderIndex, tags)"
                + " SELECT title, startTime, endTime, isNotificationEnabled, tag, repeatType, contentItems, isFinished, ? AS date, ? AS forkFrom, mode, manualOrderIndex, tags"
                + " FROM Notes"
                + " WHERE id = ?;";

        DBHelper.getInstance().getWritableDatabase().execSQL(
                insertSQL,
                new String[] {Long.toString(date.getTimeInMillis()), Integer.toString(id), Integer.toString(id)}
        );

        String idSQL = "SELECT last_insert_rowid()";
        Cursor cursor = DBHelper.getInstance().getReadableDatabase().rawQuery(idSQL, null);

        if (cursor.moveToFirst()) {
            do {
                nextNoteId = cursor.getInt(0);
            }
            while (cursor.moveToNext());
        }
        cursor.close();

        updateNoteLastAction(nextNoteId, NoteActions.ADD.name());

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
}