package com.demo.epaper.entity;

import androidx.annotation.NonNull;

public class City {

    private String parentId;
    private String childId;
    private String name;
    private String pinyin;
    private boolean select;
    private boolean mark;

    public City() {
    }

    public City(String childId, String name) {
        this.childId = childId;
        this.name = name;
    }

    public City(String parentId, String cityId, String name, String pinyin) {
        this.parentId = parentId;
        this.childId = cityId;
        this.name = name;
        this.pinyin = pinyin;
    }

    @NonNull
    @Override
    public City clone() {
        return new City(this.parentId, this.childId, this.name, this.pinyin);
    }

    public String getParentId() {
        return parentId;
    }

    public String getChildId() {
        return childId;
    }

    public String getName() {
        return name;
    }

    public String getPinyin() {
        return pinyin;
    }

    public boolean isSelect() {
        return select;
    }

    public boolean isMark() {
        return mark;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public void setChildId(String childId) {
        this.childId = childId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPinyin(String pinyin) {
        this.pinyin = pinyin;
    }

    public void setSelect(boolean select) {
        this.select = select;
    }

    public void setMark(boolean mark) {
        this.mark = mark;
    }
}
