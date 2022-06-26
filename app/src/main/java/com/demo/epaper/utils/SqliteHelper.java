package com.demo.epaper.utils;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;


import com.demo.epaper.entity.City;

import java.util.List;

public class SqliteHelper {

    private SQLiteDatabase database;

    public SqliteHelper(String filesDir) {
        this.database = SQLiteDatabase.openOrCreateDatabase(filesDir + "/db/city.db", null);
    }

    public int listProvince(List<City> dataSet) {
        int count = 0;
        final String query = "SELECT province_id, name FROM province";
        Cursor cursor = database.rawQuery(query, null);
        while(cursor.moveToNext()) {
            dataSet.add(new City(cursor.getString(0), cursor.getString(1)));
            count++;
        }
        cursor.close();
        return count;
    }

    public City getProvinceByUrbanPinyin(String urbanPinyin) {
        City city = null;
        final String query = "SELECT parent_province, urban_id, name FROM urban WHERE pingyin = \'" + urbanPinyin + "\' LIMIT 1";
        Cursor cursor = database.rawQuery(query, null);
        if(cursor.moveToNext()) {
            city = new City();
            city.setParentId(cursor.getString(0));
            city.setChildId(cursor.getString(1));
            city.setPinyin(urbanPinyin);
            city.setName(cursor.getString(2));
        }
        cursor.close();
        return city;
    }

    public int listUrban(List<City> dataSet, String parent) {
        int count = 0;
        final String query = "SELECT urban_id, name, pingyin FROM urban WHERE parent_province = \'" + parent + "\'";
        Cursor cursor = database.rawQuery(query, null);
        while(cursor.moveToNext()) {
            dataSet.add(new City(parent, cursor.getString(0), cursor.getString(1), cursor.getString(2)));
            count++;
        }
        cursor.close();
        return count;
    }

    public void deleteTable(String tableName){
        database.execSQL("DELETE FROM " + tableName);
    }

    public void close(){
        database.close();
    }
}
