package com.dailylist.vadimsemenyk.natives.Repositories;

import android.database.Cursor;
import android.text.TextUtils;

import com.dailylist.vadimsemenyk.natives.DBHelper;
import com.dailylist.vadimsemenyk.natives.Enums.NoteActions;
import com.dailylist.vadimsemenyk.natives.Enums.NoteRepeatTypes;
import com.dailylist.vadimsemenyk.natives.Enums.NoteTypes;
import com.dailylist.vadimsemenyk.natives.Enums.SortDirection;
import com.dailylist.vadimsemenyk.natives.Enums.SortType;
import com.dailylist.vadimsemenyk.natives.Helpers.DateHelper;
import com.dailylist.vadimsemenyk.natives.Models.Note;
import com.dailylist.vadimsemenyk.natives.Models.NoteContentItemImage;
import com.dailylist.vadimsemenyk.natives.Models.Settings;
import com.dailylist.vadimsemenyk.natives.Models.NoteContentItem;
import com.dailylist.vadimsemenyk.natives.Models.NoteContentItemListItem;
import com.dailylist.vadimsemenyk.natives.Models.NoteContentItemTextArea;
import com.dailylist.vadimsemenyk.natives.Notifications.TriggerOptions;
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
    static public String REPEAT_VALUES_FIELD = "(select GROUP_CONCAT(value, ',') from NotesRepeatValues where noteId = id OR noteId = forkFrom) as repeatValues";
    static private String NOTE_FIELDS = "id, title, contentItems, startTime, endTime, isNotificationEnabled, tag, isFinished, isNotificationEnabled, tags, manualOrderIndex, date, mode, repeatType, forkFrom, repeatItemDate, " + REPEAT_VALUES_FIELD;

    static private NoteRepository instance = null;

    private NoteRepository() {}

    static public NoteRepository getInstance() {
        if (instance == null) {
            instance = new NoteRepository();
        }
        return instance;
    }

    // region sorting
    static class SortByAddedTime implements Comparator<Note> {
        SortDirection direction;

        private SortByAddedTime(SortDirection direction) {
            this.direction = direction;
        }

        public int compare(Note a, Note b) {
            if (a.manualOrderIndex == null && b.manualOrderIndex == null) {
                int aVal = a.forkFrom != null ? a.forkFrom : a.id;
                int bVal = b.forkFrom != null ? b.forkFrom : b.id;

                return direction == SortDirection.ASC ? aVal - bVal : bVal - aVal;
            } else if (a.manualOrderIndex == null) {
                return 1;
            } else if (b.manualOrderIndex == null) {
                return -1;
            } else {
                return a.manualOrderIndex - b.manualOrderIndex;
            }
        }
    }

    static class SortByNoteTime implements Comparator<Note> {
        SortDirection direction;

        private SortByNoteTime(SortDirection direction) {
            this.direction = direction;
        }

        public int compare(Note a, Note b) {
            if (direction == SortDirection.ASC) {
                return (int) (a.startTime.getTimeInMillis() - b.startTime.getTimeInMillis());
            } else {
                return (int) (b.startTime.getTimeInMillis() - a.startTime.getTimeInMillis());
            }
        }
    }

    static class SortByFinished implements Comparator<Note> {
        public int compare(Note a, Note b) {
            return (a.isFinished ? 1 : 0) - (b.isFinished ? 1 : 0);
        }
    }
    // endregion

    // region shared query and format
    public ArrayList<Note> queryNotes(String sql, String[] params) {
        Cursor cursor = DBHelper.getInstance().getReadableDatabase().rawQuery(sql, params);

        ArrayList<Note> notes = new ArrayList<Note>();

        if (cursor.moveToFirst()) {
            do {
                notes.add(getNoteFromCursor(cursor));
            }
            while (cursor.moveToNext());
        }
        cursor.close();

        return notes;
    }

    public Note queryNote(String sql, String[] params) {
        ArrayList<Note> notes = queryNotes(sql, params);
        if (notes.isEmpty()) {
            return null;
        } else {
            return notes.get(0);
        }
    }

    private Note getNoteFromCursor(Cursor cursor) {
        Note note = new Note();

        if (cursor.getColumnIndex("id") != -1) {
            note.id = cursor.getInt(cursor.getColumnIndex("id"));
        }

        if (!cursor.isNull(cursor.getColumnIndex("repeatItemDate"))) {
            long _repeatItemDate = cursor.getLong(cursor.getColumnIndex("repeatItemDate"));
            note.repeatItemDate = DateHelper.convertFromUTCToLocal(DateHelper.getCalendar(_repeatItemDate, TimeZone.getTimeZone("UTC")));
        }

        if (!cursor.isNull(cursor.getColumnIndex("repeatStartDate"))) {
            long _repeatStartDate = cursor.getLong(cursor.getColumnIndex("repeatStartDate"));
            note.repeatStartDate = DateHelper.convertFromUTCToLocal(DateHelper.getCalendar(_repeatStartDate, TimeZone.getTimeZone("UTC")));
        }

        if (!cursor.isNull(cursor.getColumnIndex("repeatEndDate"))) {
            long _repeatEndDate = cursor.getLong(cursor.getColumnIndex("repeatEndDate"));
            note.repeatEndDate = DateHelper.convertFromUTCToLocal(DateHelper.getCalendar(_repeatEndDate, TimeZone.getTimeZone("UTC")));
        }

        if (cursor.getColumnIndex("title") != -1) {
            note.title = cursor.getString(cursor.getColumnIndex("title"));
        }

        // TODO: check for null pointer exception
        if (cursor.getColumnIndex("contentItems") != -1) {
            String contentItemsJson = cursor.getString(cursor.getColumnIndex("contentItems"));
            ArrayList<NoteContentItem> contentItems = new ArrayList<NoteContentItem>();
            if (!contentItemsJson.isEmpty()) {
                Gson gson = new Gson();
                JsonArray array = JsonParser.parseString(contentItemsJson).getAsJsonArray();
                for (int i = 0; i < array.size(); i++) {
                    NoteContentItem contentField = null;

                    if (array.get(i) instanceof JsonObject) {
                        JsonObject obj = array.get(i).getAsJsonObject();
                        String contentItemType = obj.get("type").getAsString();
                        if (contentItemType.equals("listItem")) {
                            contentField = gson.fromJson(array.get(i), NoteContentItemListItem.class);
                        } else if (contentItemType.equals("text")) {
                            contentField = gson.fromJson(array.get(i), NoteContentItemTextArea.class);
                        } else if (contentItemType.equals("snapshot")) {
                            contentField = gson.fromJson(array.get(i), NoteContentItemImage.class);
                        }
                        contentItems.add(contentField);
                    }
                }
            }
            note.contentItems = contentItems;
        }

        if (cursor.getColumnIndex("isFinished") != -1) {
            note.isFinished = cursor.getInt(cursor.getColumnIndex("isFinished")) == 1;
        }

        if (cursor.getColumnIndex("date") != -1 && !cursor.isNull(cursor.getColumnIndex("date"))) {
            long _date = cursor.getLong(cursor.getColumnIndex("date"));
            note.date = DateHelper.convertFromUTCToLocal(DateHelper.getCalendar(_date, TimeZone.getTimeZone("UTC")));
        }

        if (cursor.getColumnIndex("startTime") != -1 && !cursor.isNull(cursor.getColumnIndex("startTime"))) {
            long _startDateTime = cursor.getLong(cursor.getColumnIndex("startTime"));
            note.startTime = DateHelper.convertFromUTCToLocal(DateHelper.getCalendar(_startDateTime, TimeZone.getTimeZone("UTC")));
        }

        if (cursor.getColumnIndex("endTime") != -1 && !cursor.isNull(cursor.getColumnIndex("endTime"))) {
            long _endDateTime = cursor.getLong(cursor.getColumnIndex("endTime"));
            note.endTime = DateHelper.convertFromUTCToLocal(DateHelper.getCalendar(_endDateTime, TimeZone.getTimeZone("UTC")));
        }

        if (cursor.getColumnIndex("isNotificationEnabled") != -1) {
            note.isNotificationEnabled = cursor.getInt(cursor.getColumnIndex("isNotificationEnabled")) == 1;
        }

        if (cursor.getColumnIndex("colorTag") != -1) {
            note.colorTag = cursor.getString(cursor.getColumnIndex("tag"));
        }

        if (cursor.getColumnIndex("manualOrderIndex") != -1 && !cursor.isNull(cursor.getColumnIndex("manualOrderIndex"))) {
            note.manualOrderIndex = cursor.getInt(cursor.getColumnIndex("manualOrderIndex"));
        }

        if (cursor.getColumnIndex("forkFrom") != -1 && !cursor.isNull(cursor.getColumnIndex("forkFrom"))) {
            note.forkFrom = cursor.getInt(cursor.getColumnIndex("forkFrom"));
        }

        if (cursor.getColumnIndex("date") != -1 && cursor.getColumnIndex("mode") != -1) {
            note.isShadow = cursor.isNull(cursor.getColumnIndex("date")) && (NoteTypes.getDefinition(cursor.getInt(cursor.getColumnIndex("mode"))) == NoteTypes.Diary);
        }

        if (cursor.getColumnIndex("repeatType") != -1) {
            note.repeatType = NoteRepeatTypes.getDefinition(cursor.getString(cursor.getColumnIndex("repeatType")));
        }

        if (cursor.getColumnIndex("repeatValues") != -1) {
            ArrayList<String> repeatValues = cursor.isNull(cursor.getColumnIndex("repeatValues")) ? new ArrayList<>() : new ArrayList<>(Arrays.asList(cursor.getString(cursor.getColumnIndex("repeatValues")).split(",")));
            note.repeatValues = new ArrayList<Long>();
            for (String repeatValue : repeatValues) {
                note.repeatValues.add(Long.parseLong(repeatValue));
            }

            if (note.repeatType == NoteRepeatTypes.ANY) {
                for (int i = 0; i < note.repeatValues.size(); i++) {
                    note.repeatValues.set(i, DateHelper.convertFromUTCToLocal(note.repeatValues.get(i)).getTimeInMillis());
                }
            }
        }

        return note;
    }
    // endregion

    // region query
    public ArrayList<Note> getNotes(NoteTypes type, Calendar date, Settings settings) {
        String sql;
        String[] params;

        if (type == NoteTypes.Diary) {
            sql = "SELECT " + NOTE_FIELDS
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

            params = new String[] {
                    NoteActions.DELETE.name(),
                    String.valueOf(date.getTimeInMillis()),
                    String.valueOf(date.getTimeInMillis()),
                    NoteRepeatTypes.DAY.getValue(),
                    NoteRepeatTypes.WEEK.getValue(),
                    String.valueOf(DateHelper.getDayOfWeekNumber(date)),
                    NoteRepeatTypes.ANY.getValue(),
                    String.valueOf(date.getTimeInMillis()),
                    Integer.toString(NoteTypes.Diary.getValue())
            };
        } else {
            sql = "SELECT id, tag, isFinished, title, contentItems, manualOrderIndex, forkFrom, date, mode"
                    + " FROM Notes"
                    + " WHERE"
                    + " lastAction != ?"
                    + " AND mode = ?;";

            params = new String[] {NoteActions.DELETE.name(), Integer.toString(NoteTypes.Note.getValue())};
        }

        ArrayList<Note> notes = queryNotes(sql, params);

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

    public ArrayList<Note> getNotes(String where, String[] params) {
        String sql = "SELECT " + NOTE_FIELDS
                + " FROM Notes n"
                + " WHERE " + where + ";";

        return queryNotes(sql, params);
    }

    public Note getNote(int id) {
        return getNotes("id = ?", new String[] {Integer.toString(id)}).get(0);
    }
    // endregion

    // region updates
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
    // endregion

    // region notifications
    public TriggerOptions getNextNotificationTrigger(int id, boolean isRepeatReschedule) {
        Calendar nextTriggerDateTime = null;
        ArrayList<Integer> nextTriggerNotesIDs = new ArrayList<>();

        Note note = queryNote(
                "SELECT id, date, startTime, isNotificationEnabled, repeatType, repeatEndDate, " + REPEAT_VALUES_FIELD + " FROM Notes WHERE id = ?",
                new String[] {Integer.toString(id)}
        );
        if (note == null) {
            return null;
        }

        if (note.repeatType == NoteRepeatTypes.NO_REPEAT) {
            if (!note.isNotificationEnabled) {
                return null;
            }

            nextTriggerDateTime = DateHelper.getDateTime(note.date, note.startTime);
            nextTriggerNotesIDs.add((Integer) note.id);
        } else {
            Calendar searchFromDateTime = DateHelper.startOf(Calendar.getInstance(), "minute");
            if (isRepeatReschedule) {
                searchFromDateTime.add(Calendar.MINUTE, 1);
            }

            nextTriggerDateTime = getRepeatNextTriggerDateTime(searchFromDateTime, note.repeatType, note.repeatValues, note.startTime);
            if (!note.isNotificationEnabled || (note.repeatEndDate != null && note.repeatEndDate.before(DateHelper.startOf(nextTriggerDateTime, "day")))) {
                nextTriggerDateTime = null;
            }
            Calendar nextTriggerDateTimeInit = nextTriggerDateTime == null ? null : (Calendar) nextTriggerDateTime.clone();

            String searchFromDateUTCMS = Long.toString(DateHelper.convertFromLocalToUTC(DateHelper.startOf(searchFromDateTime, "day")).getTimeInMillis());
            String searchFromTimeUTCMS = Long.toString(DateHelper.convertFromLocalToUTC(DateHelper.getTime(searchFromDateTime)).getTimeInMillis());
            ArrayList<Note> closestDateTimeForkedNotes = queryNotes(
                    "SELECT id, date, startTime"
                    + " FROM Notes n"
                    + " INNER JOIN ("
                        + " SELECT forkFrom, date, startTime"
                        + " FROM Notes"
                        + " WHERE forkFrom = ? AND isNotificationEnabled = ? AND (date > ? OR (date = ? AND startTime >= ?))"
                        + " ORDER BY date ASC, startTime ASC"
                        + " LIMIT 1"
                    + " ) n1 USING (forkFrom, date, startTime)"
                    + " WHERE n.forkFrom = ?",
                    new String[] {Integer.toString(id), "1", searchFromDateUTCMS, searchFromDateUTCMS, searchFromTimeUTCMS, Integer.toString(id)}
            );
            Calendar closestForkedNoteDateTime = null;
            ArrayList<Integer> closestForkedNotesIDs = new ArrayList<Integer>();
            if (!closestDateTimeForkedNotes.isEmpty()) {
                closestForkedNoteDateTime = DateHelper.getDateTime(closestDateTimeForkedNotes.get(0).date, closestDateTimeForkedNotes.get(0).startTime);
                for (Note _note : closestDateTimeForkedNotes) {
                    closestForkedNotesIDs.add(_note.id);
                }
            }

            if (closestForkedNoteDateTime != null && (nextTriggerDateTime == null || !closestForkedNoteDateTime.after(nextTriggerDateTime))) {
                nextTriggerDateTime = closestForkedNoteDateTime;
                nextTriggerNotesIDs.addAll(closestForkedNotesIDs);
            }

            if (nextTriggerDateTimeInit != null && nextTriggerDateTimeInit.equals(nextTriggerDateTime)) {
                Note nextTriggerDateForkedNote = NoteRepository.getInstance().queryNote(
                        "SELECT id FROM Notes WHERE forkFrom = ? AND repeatItemDate = ?",
                        new String[] {
                                Integer.toString(id),
                                Long.toString(DateHelper.convertFromLocalToUTC(DateHelper.startOf(nextTriggerDateTime, "day")).getTimeInMillis()),
                        }
                );
                if (nextTriggerDateForkedNote == null) {
                    nextTriggerNotesIDs.add((Integer) note.id);
                }
            }

            if (nextTriggerDateTime == null) {
                return null;
            }
        }

        TriggerOptions options = new TriggerOptions();
        options.id = id;
        options.dateTime = nextTriggerDateTime;
        options.noteIDs = nextTriggerNotesIDs;
        options.shouldReschedule = note.repeatType != NoteRepeatTypes.NO_REPEAT;

        return options;
    }

    private Calendar getRepeatNextTriggerDateTime(Calendar searchStartDateTime, NoteRepeatTypes repeatType, ArrayList<Long> repeatValues, Calendar time) {
        boolean includeCurrentDate = !DateHelper.getTime(searchStartDateTime).after(time);

        Calendar resultDate = null;

        if (repeatType == NoteRepeatTypes.DAY) {
            Calendar _resultDate = DateHelper.startOf(Calendar.getInstance(), "day");

            if (!includeCurrentDate) {
                _resultDate.add(Calendar.DATE, 1);
            }

            resultDate = _resultDate;
        } else if (repeatType == NoteRepeatTypes.WEEK && !repeatValues.isEmpty()) {
            int currentWeekDay = DateHelper.getDayOfWeekNumber(Calendar.getInstance());
            ArrayList<Long> _repeatValues = new ArrayList<>(repeatValues);

            Collections.sort(_repeatValues);

            Long result = _repeatValues.get(0);
            for (Long repeatValue : _repeatValues) {
                if (includeCurrentDate ? (repeatValue > currentWeekDay) : (repeatValue >= currentWeekDay)) {
                    result = repeatValue;
                    break;
                }
            }

            Calendar _resultDate = DateHelper.startOf(Calendar.getInstance(), "day");
            _resultDate.set(Calendar.DAY_OF_WEEK, DateHelper.getDayOfWeek(result.intValue()));

            resultDate = _resultDate;
        } else if (repeatType == NoteRepeatTypes.ANY && !repeatValues.isEmpty()) {
            Long currentDateMS = DateHelper.startOf(Calendar.getInstance(), "day").getTimeInMillis();

            Long result = null;
            for (Long repeatValueUTC : repeatValues) {
                Long repeatValue = DateHelper.convertFromUTCToLocal(repeatValueUTC).getTimeInMillis();

                if (includeCurrentDate ? (repeatValue > currentDateMS) : (repeatValue >= currentDateMS)) {
                    result = repeatValue;
                    break;
                }
            }

            if (result != null) {
                resultDate = DateHelper.getCalendar(result);
            }

        }

        if (resultDate != null) {
            return DateHelper.getDateTime(resultDate, time);
        } else {
            return null;
        }
    }

    public String getNotificationText(ArrayList<NoteContentItem> contentItems) {
        String text = "";
        if (!contentItems.isEmpty()) {
            if (contentItems.get(0) instanceof NoteContentItemImage) {
                text = "picture";
            } else {
                NoteContentItem firstNotEmptyTextContentItem = null;

                for (NoteContentItem contentItem : contentItems) {
                    if (!contentItem.value.isEmpty()) {
                        firstNotEmptyTextContentItem = contentItem;
                        break;
                    }
                }

                if (firstNotEmptyTextContentItem instanceof NoteContentItemTextArea) {
                    text = firstNotEmptyTextContentItem.value;
                } else {
                    ArrayList<String> listItems = new ArrayList<String>();
                    for (NoteContentItem contentItem : contentItems) {
                        if (contentItem instanceof NoteContentItemTextArea) {
                            listItems.add(contentItem.value);
                        } else {
                            break;
                        }
                    }

                    text = TextUtils.join("; -", listItems) + ";";
                }
            }
        }
        return text;
    }
    // endregion
}