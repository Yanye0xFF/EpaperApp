package com.demo.epaper.entity;

public class CalendarInfo {

    private final String calendar;
    private final String week;
    private final String lunarYear;
    private final String lunarMonth;

    private int month;
    private int day;
    private int hour;
    private int minutes;

    public CalendarInfo(String calendar, String week, String lunarYear, String lunarMonth) {
        this.calendar = calendar;
        this.week = week;
        this.lunarYear = lunarYear;
        this.lunarMonth = lunarMonth;
    }

    public void setTime(int month, int day, int hour, int minutes) {
        this.month = month;
        this.day = day;
        this.hour = hour;
        this.minutes = minutes;
    }

    public int getHour() {
        return  this.hour;
    }

    public int getMinutes() {
        return this.minutes;
    }

    public int getMonth() {
        return month;
    }

    public int getDay() {
        return day;
    }

    public String getCalendar() {
        return calendar;
    }

    public String getWeek() {
        return week;
    }

    public String getLunarYear() {
        return lunarYear;
    }

    public String getLunarMonth() {
        return lunarMonth;
    }
}
