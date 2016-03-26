package io.ipoli.android.app.utils;

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class Time {
    private final int minutes;

    private Time(int hours, int minutes) {
        this.minutes = hours * 60 + minutes;
    }

    public static Time fromMinutesAfterMidnight(int minutes) {
        int h = (int) TimeUnit.MINUTES.toHours(minutes);
        int m = minutes - h * 60;
        return Time.at(h, m);
    }

    public static Time at(String timeString) {
        return new Time(parseHours(timeString), parseMinutes(timeString));
    }

    public static Time at(int hours, int minutes) {
        return new Time(hours, minutes);
    }

    public static Time atHours(int hours) {
        return at(hours, 0);
    }

    public static Time after(int hours, int minutes) {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.HOUR_OF_DAY, hours);
        c.add(Calendar.MINUTE, minutes);
        return new Time(c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE));
    }

    public static Time ago(int hours, int minutes) {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.HOUR_OF_DAY, - hours);
        c.add(Calendar.MINUTE, - minutes);
        return new Time(c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE));
    }

    public static Time afterHours(int hours) {
        return after(hours, 0);
    }

    public static Time afterMinutes(int minutes) {
        return after(0, minutes);
    }

    public static Time hoursAgo(int hours) {
        return ago(hours, 0);
    }

    public static Time minutesAgo(int minutes) {
        return ago(0, minutes);
    }

    private static int parseHours(String time) {
        String[] pieces = time.split(":");
        return Integer.parseInt(pieces[0]);
    }

    private static int parseMinutes(String time) {
        String[] pieces = time.split(":");
        return Integer.parseInt(pieces[1]);
    }

    public Date toDate() {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(0);
        c.set(Calendar.HOUR_OF_DAY, getHours());
        c.set(Calendar.MINUTE, getMinutes());
        return c.getTime();
    }

    public int toMinutesAfterMidnight() {
        return minutes;
    }

    public int getHours() {
        return (int) TimeUnit.MINUTES.toHours(minutes);
    }

    public int getMinutes() {
        return minutes - getHours() * 60;
    }

    public static Time now() {
        Calendar c = Calendar.getInstance();
        return new Time(c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE));
    }

    public static Time of(Date date) {
        if (date == null) {
            return null;
        }
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        return new Time(c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE));
    }
}