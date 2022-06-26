package com.demo.epaper.entity;

public class ForecastWeather {

    public static int FORECAST_MAX = 4;

    private final String dayDesc;
    private final String weatherDesc;
    private final String windDesc;

    private final int lowestTemp;
    private final int highestTemp;

    public ForecastWeather(String day, String weather, String wind, int low, int high) {
        this.dayDesc = day;
        this.weatherDesc = weather;
        this.windDesc = wind;

        this.lowestTemp = low;
        this.highestTemp = high;
    }

    public String getDayDesc() {
        return dayDesc;
    }

    public String getWeatherDesc() {
        return weatherDesc;
    }

    public String getWindDesc() {
        return windDesc;
    }

    public int getLowestTemp() {
        return lowestTemp;
    }

    public int getHighestTemp() {
        return highestTemp;
    }
}
