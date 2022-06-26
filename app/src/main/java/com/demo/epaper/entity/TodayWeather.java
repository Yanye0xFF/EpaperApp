package com.demo.epaper.entity;

public class TodayWeather {
    public String cityName;
    public int temperature;
    public String dateDesc;
    public String weekDesc;
    public String humidityDesc;
    public String uvDesc;

    public TodayWeather() {
    }

    public void setCityName(String cityName) {
        this.cityName = cityName;
    }

    public void setTemperature(int temperature) {
        this.temperature = temperature;
    }

    public void setDateDesc(String dateDesc) {
        this.dateDesc = dateDesc;
    }

    public void setWeekDesc(String weekDesc) {
        this.weekDesc = weekDesc;
    }

    public void setHumidityDesc(String humidityDesc) {
        this.humidityDesc = humidityDesc;
    }

    public void setUvDesc(String uvDesc) {
        this.uvDesc = uvDesc;
    }
}
