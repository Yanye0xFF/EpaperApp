package com.demo.epaper.entity;

public class RecentWeather {
    public String cityName;
    public String todayDesc;
    public String tomorrowDesc;
    public String afterTomorrowDesc;

    public RecentWeather(String a, String b, String c, String d) {
        this.cityName = a;
        this.todayDesc = b;
        this.tomorrowDesc = c;
        this.afterTomorrowDesc = d;
    }
}
