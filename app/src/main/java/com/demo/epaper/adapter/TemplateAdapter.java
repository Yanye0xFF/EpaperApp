package com.demo.epaper.adapter;

import android.app.Activity;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.demo.epaper.R;
import com.demo.epaper.entity.WeatherTemplate;
import com.demo.epaper.handler.ItemClickListener;
import com.demo.epaper.utils.AppUtils;
import com.demo.epaper.view.HorizontalDecoration;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class TemplateAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<WeatherTemplate> dataSet;
    private ItemClickListener clickListener;
    private Activity activity;
    private final float hSplit;

    public TemplateAdapter(Activity activity, List<WeatherTemplate> dataSet) {
        this.dataSet = dataSet;
        this.activity = activity;
        float density = activity.getResources().getDisplayMetrics().density;
        hSplit = AppUtils.dp2Px(density,25);
    }

    @NonNull
    @NotNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
        View view;
        if(viewType == WeatherTemplate.TITLE_BAR) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_title_bar, parent, false);
            return new TitleBarViewHolder(view);

        }else if(viewType == WeatherTemplate.CITY_SELECTOR) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_city_selection, parent, false);
            return new CitySelectionViewHolder(view);

        }else if(viewType == WeatherTemplate.TEMPLATE) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_template, parent, false);
            return new WeatherTemplateViewHolder(view);

        }else {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_button, parent, false);
            return new UserButtonViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull @NotNull RecyclerView.ViewHolder holder, int position) {
        WeatherTemplate item = dataSet.get(position);
        int viewType = item.getViewType();
        if(viewType == WeatherTemplate.TITLE_BAR) {
            TitleBarViewHolder viewHolder = (TitleBarViewHolder)holder;
            viewHolder.viewIndicator.setBackgroundColor(item.getResId());
            viewHolder.tvTitle.setText(item.getTitle());

        }else if(viewType == WeatherTemplate.CITY_SELECTOR) {
            CitySelectionViewHolder viewHolder = (CitySelectionViewHolder)holder;
            if(item.cityAdapter == null) {
                item.cityAdapter = new CitySelectionAdapter(activity.getExternalFilesDir(null).getAbsolutePath(),
                        R.drawable.round_fill_purple, item.cityDataSet, item.displayCityDataSet);

                LinearLayoutManager manager = new LinearLayoutManager(activity.getApplicationContext());
                manager.setOrientation(LinearLayoutManager.HORIZONTAL);
                viewHolder.recyclerCity.setLayoutManager(manager);

                viewHolder.recyclerCity.addItemDecoration(new HorizontalDecoration((int)hSplit));
                viewHolder.recyclerCity.setAdapter(item.cityAdapter);
            }

        }else if(viewType == WeatherTemplate.TEMPLATE) {
            WeatherTemplateViewHolder viewHolder = (WeatherTemplateViewHolder)holder;

            viewHolder.tvTitle.setText(item.getTitle());
            viewHolder.tvSubTitle.setText(item.getSubTitle());
            viewHolder.ivPreview.setImageResource(item.getResId());
            if(item.isChecked()) {
                viewHolder.tvTitle.setTextColor(Color.WHITE);
                viewHolder.tvSubTitle.setTextColor(Color.WHITE);
                viewHolder.cardTemplate.setCardBackgroundColor(0xff7bbfea);
            }else {
                viewHolder.tvTitle.setTextColor(Color.BLACK);
                viewHolder.cardTemplate.setCardBackgroundColor(0xFFFAFAFA);
            }

        }else {
            UserButtonViewHolder viewHolder = (UserButtonViewHolder)holder;

            viewHolder.tvButtonTitle.setText(item.getTitle());
            viewHolder.tvButtonTitle.setTextColor(item.getResId());
        }

        if((viewType > 1) && (holder.itemView.getTag() == null) && (clickListener != null)) {
            holder.itemView.setTag(200);
            holder.itemView.setOnClickListener((View view) ->
                    clickListener.onItemClick(0, holder.getAdapterPosition()));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, @NonNull List<Object> payloads) {
        if(payloads.isEmpty()) {
            onBindViewHolder(holder, position);
            return;
        }
        WeatherTemplate item = dataSet.get(position);
        if(item.getViewType() == WeatherTemplate.TEMPLATE) {
            WeatherTemplateViewHolder viewHolder = (WeatherTemplateViewHolder)holder;
            if(item.isChecked()) {
                viewHolder.tvTitle.setTextColor(Color.WHITE);
                viewHolder.tvSubTitle.setTextColor(Color.WHITE);
                viewHolder.cardTemplate.setCardBackgroundColor(0xff7bbfea);
            }else {
                viewHolder.tvTitle.setTextColor(Color.BLACK);
                viewHolder.tvSubTitle.setTextColor(0xFF757575);
                viewHolder.cardTemplate.setCardBackgroundColor(0xFFFAFAFA);
            }
        }
    }

    @Override
    public int getItemCount() {
        return dataSet.size();
    }

    @Override
    public int getItemViewType(int position) {
        return dataSet.get(position).getViewType();
    }

    public void setItemClickListener(ItemClickListener listener) {
        this.clickListener = listener;
    }

    public void close() {
        for(WeatherTemplate template : dataSet) {
            if(template.getViewType() == WeatherTemplate.CITY_SELECTOR) {
                template.cityAdapter.close();
                template.cityAdapter = null;
                template.cityDataSet = null;
                template.displayCityDataSet = null;
                break;
            }
        }
        dataSet = null;
        clickListener = null;
        activity = null;
    }

    private static final class TitleBarViewHolder extends RecyclerView.ViewHolder {
        private final View viewIndicator;
        private final TextView tvTitle;
        public TitleBarViewHolder(@NonNull @NotNull View itemView) {
            super(itemView);
            viewIndicator = itemView.findViewById(R.id.view_indicator);
            tvTitle = itemView.findViewById(R.id.tv_bar_title);
        }
    }

    private static final class CitySelectionViewHolder extends RecyclerView.ViewHolder {
        private final RecyclerView recyclerCity;
        public CitySelectionViewHolder(@NonNull @NotNull View itemView) {
            super(itemView);
            recyclerCity = itemView.findViewById(R.id.recycler_city);
        }
    }

    private static final class WeatherTemplateViewHolder extends RecyclerView.ViewHolder {
        private final CardView cardTemplate;
        private final AppCompatImageView ivPreview;
        private final TextView tvTitle;
        private final TextView tvSubTitle;
        public WeatherTemplateViewHolder(@NonNull @NotNull View itemView) {
            super(itemView);
            cardTemplate = itemView.findViewById(R.id.card_template);
            ivPreview = itemView.findViewById(R.id.iv_preview);
            tvTitle = itemView.findViewById(R.id.tv_temp_title);
            tvSubTitle = itemView.findViewById(R.id.tv_temp_subtitle);
        }
    }

    private static final class UserButtonViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvButtonTitle;
        public UserButtonViewHolder(@NonNull @NotNull View itemView) {
            super(itemView);
            tvButtonTitle = itemView.findViewById(R.id.tv_button_title);
        }
    }
}
