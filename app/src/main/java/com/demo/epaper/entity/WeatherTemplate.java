package com.demo.epaper.entity;

import com.demo.epaper.adapter.CitySelectionAdapter;

import java.util.List;

public class WeatherTemplate {

    public static final int TITLE_BAR = 0;
    public static final int CITY_SELECTOR = 1;
    public static final int TEMPLATE = 2;
    public static final int USER_BUTTON = 3;

    private int viewType;

    private String title;
    private String subTitle;

    private int resId;
    private boolean checked;

    public CitySelectionAdapter cityAdapter;
    public List<City> cityDataSet;
    public List<City> displayCityDataSet;

    public WeatherTemplate(int type) {
        this.viewType = type;
    }

    public WeatherTemplate(String title, int resId, int type) {
        this.viewType = type;
        this.title = title;
        this.resId = resId;
    }

    public void setCitySelection(List<City> cityDataSet, List<City> displayCityDataSet) {
        this.cityDataSet = cityDataSet;
        this.displayCityDataSet = displayCityDataSet;
    }

    public void setViewType(int viewType) {
        this.viewType = viewType;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setSubTitle(String subTitle) {
        this.subTitle = subTitle;
    }

    public void setResId(int resId) {
        this.resId = resId;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }

    public int getViewType() {
        return viewType;
    }

    public String getTitle() {
        return title;
    }

    public String getSubTitle() {
        return subTitle;
    }

    public int getResId() {
        return resId;
    }

    public boolean isChecked() {
        return checked;
    }
}
