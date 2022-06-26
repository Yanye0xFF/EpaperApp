package com.demo.epaper.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.demo.epaper.R;
import com.demo.epaper.entity.City;
import com.demo.epaper.utils.SqliteHelper;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


import java.util.List;

public class CitySelectionAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<City> cityList;
    private List<City> displayList;
    private final int selectRes;
    private SqliteHelper sqlite;

    public CitySelectionAdapter(String filesDir, int selectRes, List<City> cityList, List<City> displayList) {
        sqlite = new SqliteHelper(filesDir);
        this.selectRes = selectRes;
        this.cityList = cityList;
        this.displayList = displayList;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_city, parent, false);
        return new CityViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        City item = displayList.get(position);
        CityViewHolder viewHolder = (CityViewHolder)holder;
        viewHolder.cityName.setText(item.getName());

        if(item.isSelect()) {
            viewHolder.cityLayout.setBackgroundResource(selectRes);
            viewHolder.cityName.setTextColor(Color.WHITE);
        }else {
            viewHolder.cityLayout.setBackgroundResource(R.drawable.round_rectangle);
            viewHolder.cityName.setTextColor(0xFF7F7F7F);
        }

        if(viewHolder.itemView.getTag() == null) {
            viewHolder.itemView.setTag(200);
            viewHolder.itemView.setOnClickListener((View view) -> updateDisplayList(holder.getAdapterPosition()));
        }
    }

    private void updateDisplayList(int position) {
        City item = displayList.get(position);
        if(item.getParentId() == null) {
            if(item.isSelect()) {
                item.setSelect(false);
                displayList.clear();
                displayList.addAll(cityList);
            }else {
                item.setSelect(true);
                displayList.clear();
                displayList.add(item);
                sqlite.listUrban(displayList, item.getChildId());
            }
        }else {
            City index = displayList.get(0);
            if(item.isSelect()) {
                item.setSelect(false);
                displayList.clear();
                displayList.add(index);
                sqlite.listUrban(displayList, index.getChildId());
            }else {
                item.setSelect(true);
                displayList.clear();
                displayList.add(index);
                displayList.add(item);
            }
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return displayList.size();
    }

    public City getItem(int position) {
        return displayList.get(position);
    }

    public void close() {
        sqlite.close();
        sqlite = null;
        cityList = null;
        displayList = null;
    }

    private static final class CityViewHolder extends RecyclerView.ViewHolder {
        public LinearLayout cityLayout;
        public TextView cityName;
        public CityViewHolder(@NonNull View itemView) {
            super(itemView);
            cityLayout = itemView.findViewById(R.id.city_layout);
            cityName = itemView.findViewById(R.id.tv_city_name);
        }
    }
}
