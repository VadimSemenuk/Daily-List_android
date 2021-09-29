package com.dailylist.vadimsemenyk.natives.Helpers;

import java.util.Calendar;
import java.util.TimeZone;

public class DateHelper {
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

    public static int getDayOfWeekNumber(Calendar date) {
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
