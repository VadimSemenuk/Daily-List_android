package com.dailylist.vadimsemenyk.natives.Notifications;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;

import com.dailylist.vadimsemenyk.R;
import com.dailylist.vadimsemenyk.natives.App;
import com.dailylist.vadimsemenyk.natives.DBHelper;
import com.dailylist.vadimsemenyk.natives.Enums.NoteRepeatTypes;
import com.dailylist.vadimsemenyk.natives.Helpers.DateHelper;
import com.dailylist.vadimsemenyk.natives.Models.Note;
import com.dailylist.vadimsemenyk.natives.Models.NoteContentItem;
import com.dailylist.vadimsemenyk.natives.Models.NoteContentItemImage;
import com.dailylist.vadimsemenyk.natives.Models.NoteContentItemTextArea;
import com.dailylist.vadimsemenyk.natives.Repositories.NoteRepository;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class Notifications {
    static String CHANNEL_ID = "com.dailylist.vadimsemenyk.notification";
    static String CHANNEL_NAME = "Daily List";

    static String ACTION_SHOW = "com.dailylist.vadimsemenyk.notification.show";
    static String ACTION_FINISH = "com.dailylist.vadimsemenyk.notification.finish";
    static String ACTION_OPEN_NOTE = "com.dailylist.vadimsemenyk.notification.open_note";

    static String EXTRA_ID = "id";
    static String EXTRA_TRIGGER_DATE = "trigger_date";

    public Notifications() { }

    // schedule notification

    static public void schedule(int id) {
        schedule(id, false);
    }

    static public void schedule(int id, boolean isRepeatReschedule) {
        Map<String, Object> result = getTriggerDateTimeMS(id, isRepeatReschedule);
        if (result == null) {
            return;
        }
        Long triggerDateTimeMS = (Long) result.get("triggerDateTimeMS");
        String triggerNotesIDs = (String) result.get("ids");

        if (triggerDateTimeMS == null) {
            return;
        }

        Intent intent = new Intent(App.getAppContext(), NotificationsReceiver.class);
        intent.setAction(getShowActionName(id));
        intent.putExtra(EXTRA_ID, id);
        intent.putExtra(EXTRA_TRIGGER_DATE, triggerDateTimeMS);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(App.getAppContext(), 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);

        AlarmManager alarmManager = (AlarmManager) App.getAppContext().getSystemService(Context.ALARM_SERVICE);
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerDateTimeMS, pendingIntent);
            } else {
                alarmManager.setExact(AlarmManager.RTC, triggerDateTimeMS, pendingIntent);
            }
        } catch (Exception ignore) {
            // Samsung devices have a known bug where a 500 alarms limit
            // can crash the app
        }
    }

    static private Map<String, Object> getTriggerDateTimeMS(int id, boolean isRepeatReschedule) {
        Calendar nextTriggerDateTime = null;
        ArrayList<Integer> nextTriggerDateTimeNoteIDs = new ArrayList<>();

        Note note = NoteRepository.getInstance().getNote(id);

        if (note.repeatType == NoteRepeatTypes.NO_REPEAT) {
            nextTriggerDateTime = DateHelper.getDateTime(note.date, note.startDateTime);
            nextTriggerDateTimeNoteIDs.add(note.id);
        } else {
            Calendar searchFromDateTime = DateHelper.startOf(Calendar.getInstance(), "minute");
            if (isRepeatReschedule) {
                searchFromDateTime.add(Calendar.MINUTE, 1);
            }
            Calendar searchFromDate = DateHelper.startOf(searchFromDateTime, "day");

            ArrayList<Note> _forkedNotes = NoteRepository.getInstance().queryNotes(
                    "SELECT " + NoteRepository.noteSQLFields
                            + " FROM Notes n"
                            + " WHERE forkFrom = ? AND (date >= ? OR repeatItemDate >= ?)",
                    new String[] {
                            Integer.toString(id),
                            Long.toString(DateHelper.convertFromLocalToUTC(searchFromDate).getTimeInMillis()),
                            Long.toString(DateHelper.convertFromLocalToUTC(searchFromDate).getTimeInMillis()),
                    }
            );

            ArrayList<Note> forkedNotesByRepeatItemDate = new ArrayList<Note>();
            for (Note forkedNote : _forkedNotes) {
                if (!forkedNote.repeatItemDate.before(searchFromDate)) {
                    forkedNotesByRepeatItemDate.add(forkedNote);
                }
            }
            Collections.sort(forkedNotesByRepeatItemDate, new Comparator<Note>() {
                @Override
                public int compare(Note a, Note b) {
                    return (int) (a.repeatItemDate.getTimeInMillis() - b.repeatItemDate.getTimeInMillis());
                }
            });

            ArrayList<Note> forkedNotesByDateTime = new ArrayList<Note>();
            for (Note forkedNote : _forkedNotes) {
                if (!DateHelper.getDateTime(forkedNote.date, forkedNote.startDateTime).before(searchFromDateTime) && forkedNote.isNotificationEnabled) {
                    forkedNotesByDateTime.add(forkedNote);
                }
            }
            Collections.sort(forkedNotesByDateTime, new Comparator<Note>() {
                @Override
                public int compare(Note a, Note b) {
                    return (int) (DateHelper.getDateTime(a.date, a.startDateTime).getTimeInMillis() - DateHelper.getDateTime(b.date, b.startDateTime).getTimeInMillis());
                }
            });
            Calendar closestForkedNoteDateTime = null;
            ArrayList<Integer> closestForkedNoteDateTimeNoteIDs = new ArrayList<>();
            if (!forkedNotesByDateTime.isEmpty()) {
                closestForkedNoteDateTime = DateHelper.getDateTime(forkedNotesByDateTime.get(0).date, forkedNotesByDateTime.get(0).startDateTime);
                for (Note forkedNote : forkedNotesByDateTime) {
                    if (!DateHelper.getDateTime(forkedNote.date, forkedNote.startDateTime).equals(closestForkedNoteDateTime)) {
                        closestForkedNoteDateTimeNoteIDs.add(forkedNote.id);
                    }
                }
            }

            Calendar _nextTriggerDateTime = null;
            boolean isNextTriggerDateTimeFound = false;
            while(!isNextTriggerDateTimeFound) {
                Calendar _searchFromDateTime = null;
                if (_nextTriggerDateTime == null) {
                    _searchFromDateTime = (Calendar) searchFromDateTime.clone();
                } else {
                    _searchFromDateTime = (Calendar) _nextTriggerDateTime.clone();
                    _searchFromDateTime.add(Calendar.MINUTE, 1);
                }
                _nextTriggerDateTime = getRepeatNextTriggerDateTime(_searchFromDateTime, note.repeatType, note.repeatValues, note.startDateTime);

                if (!note.isNotificationEnabled || (note.repeatEndDate != null && note.repeatEndDate.after(_nextTriggerDateTime))) {
                    _nextTriggerDateTime = null;
                }

                if (_nextTriggerDateTime == null) {
                    if (closestForkedNoteDateTime != null) {
                        nextTriggerDateTime = (Calendar) closestForkedNoteDateTime.clone();
                        nextTriggerDateTimeNoteIDs.addAll(closestForkedNoteDateTimeNoteIDs);
                    }
                    isNextTriggerDateTimeFound = true;
                } else {
                    boolean isTriggerDateReplacedByForkedNote = false;
                    for (Note forkedNote : forkedNotesByRepeatItemDate) {
                        if (forkedNote.repeatItemDate.equals(DateHelper.startOf(_nextTriggerDateTime, "day"))) {
                            isTriggerDateReplacedByForkedNote = true;
                            break;
                        }
                    }

                    if (closestForkedNoteDateTime != null && !closestForkedNoteDateTime.after(_nextTriggerDateTime)) {
                        nextTriggerDateTime = (Calendar) closestForkedNoteDateTime.clone();
                        nextTriggerDateTimeNoteIDs.addAll(closestForkedNoteDateTimeNoteIDs);

                        if (closestForkedNoteDateTime.equals(_nextTriggerDateTime) && !isTriggerDateReplacedByForkedNote) {
                            nextTriggerDateTimeNoteIDs.add(note.id);
                        }

                        isNextTriggerDateTimeFound = true;
                    } else if (!isTriggerDateReplacedByForkedNote) {
                        nextTriggerDateTime = (Calendar) _nextTriggerDateTime.clone();
                        nextTriggerDateTimeNoteIDs.add(note.id);

                        isNextTriggerDateTimeFound = true;
                    } else {
                        if (
                                (closestForkedNoteDateTime == null || closestForkedNoteDateTime.before(_nextTriggerDateTime))
                                || (forkedNotesByRepeatItemDate.isEmpty() || forkedNotesByRepeatItemDate.get(forkedNotesByRepeatItemDate.size() - 1).repeatItemDate.before(DateHelper.startOf(_nextTriggerDateTime, "day")))
                        ) {
                            isNextTriggerDateTimeFound = true;
                        }
                    }
                }
            }
        }

        if (nextTriggerDateTime == null) {
            return null;
        } else {
            Map<String, Object> result = new HashMap<String, Object>();
            result.put("ids", TextUtils.join(", ", nextTriggerDateTimeNoteIDs));
            result.put("triggerDateTimeMS", nextTriggerDateTime.getTimeInMillis());
            return result;
        }
    }

    static private Calendar getRepeatNextTriggerDateTime(Calendar searchStartDateTime, NoteRepeatTypes repeatType, ArrayList<Long> repeatValues, Calendar time) {
        boolean includeCurrentDate = !DateHelper.getTime(searchStartDateTime).after(time);

        Calendar resultDate = null;

        if (repeatType == NoteRepeatTypes.DAY) {
            Calendar _resultDate = DateHelper.startOf(Calendar.getInstance(), "day");

            if (!includeCurrentDate) {
                resultDate.add(Calendar.DATE, 1);
            }

            resultDate = _resultDate;
        } else if (repeatType == NoteRepeatTypes.WEEK) {
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
        } else if (repeatType == NoteRepeatTypes.ANY) {
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

    static public void cancel(int id) {
        Intent intent = new Intent(App.getAppContext(), NotificationsReceiver.class);
        intent.setAction(getShowActionName(id));
        PendingIntent pendingIntent = PendingIntent.getBroadcast(App.getAppContext(), 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);

        AlarmManager alarmManager = (AlarmManager) App.getAppContext().getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);
    }

    static private String getShowActionName(int id) {
        return ACTION_SHOW + "_" + id;
    }

    // show notification

    static public void show(int id, Notification notification) {
        NotificationManager notificationManager = (NotificationManager) App.getAppContext().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(id, notification);
    }

    static public Notification build(NotificationOptions options) {
        createDefaultChannel();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(App.getAppContext(), CHANNEL_ID);

        Bundle extras = new Bundle();
        extras.putInt(EXTRA_ID, options.id);

        builder
                .setDefaults(NotificationCompat.DEFAULT_VIBRATE | NotificationCompat.DEFAULT_SOUND | NotificationCompat.DEFAULT_LIGHTS)
                .setExtras(extras)
                .setOnlyAlertOnce(false)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setShowWhen(true)
                .setUsesChronometer(false)
                .setAutoCancel(false)
                .setOngoing(false)
                .setSmallIcon(R.mipmap.notification)
                .addAction(new NotificationCompat.Action.Builder(android.R.drawable.screen_background_dark, App.getAppContext().getString(R.string.notification_finish_action), getActionPendingIntent(ACTION_FINISH, options.id)).build())
                .setContentIntent(getActionPendingIntent(ACTION_OPEN_NOTE, options.id))
                .setContentTitle(options.title.isEmpty() ? null : options.title)
                .setContentText(options.text.isEmpty() ? null : options.text)
                .setTicker(options.text.isEmpty() ? null : options.text)
                .setWhen(options.triggerTime.getTimeInMillis());

        return builder.build();
    }

    static private PendingIntent getActionPendingIntent(String action, int id) {
        Intent intent = new Intent(App.getAppContext(), NotificationActionReceiver.class);
        intent.setAction(action);
        intent.putExtra(EXTRA_ID, id);
        return PendingIntent.getService(App.getAppContext(), 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
    }

    static private void createDefaultChannel() {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O) {
            return;
        }

        NotificationManager notificationManager = (NotificationManager) App.getAppContext().getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
        notificationManager.createNotificationChannel(channel);
    }


    static public String getText(Note note) {
        String text = "";
        if (!note.contentItems.isEmpty()) {
            if (note.contentItems.get(0) instanceof NoteContentItemImage) {
                text = "picture";
            } else {
                NoteContentItem firstNotEmptyTextContentItem = null;

                for (NoteContentItem contentItem : note.contentItems) {
                    if (contentItem.value.isEmpty()) {
                        firstNotEmptyTextContentItem = contentItem;
                        break;
                    }
                }

                if (firstNotEmptyTextContentItem instanceof NoteContentItemTextArea) {
                    text = firstNotEmptyTextContentItem.value;
                } else {
                    ArrayList<String> listItems = new ArrayList<String>();
                    for (NoteContentItem contentItem : note.contentItems) {
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
}
