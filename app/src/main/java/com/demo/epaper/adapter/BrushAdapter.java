package com.demo.epaper.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.demo.epaper.R;
import com.demo.epaper.entity.BrushSelector;
import com.demo.epaper.handler.ItemClickListener;
import com.demo.epaper.view.BrushView;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class BrushAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<BrushSelector> dataSetRef;
    private ItemClickListener listener;

    public BrushAdapter(List<BrushSelector> dataSet) {
        this.dataSetRef = dataSet;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_brush, parent, false);
        return new BrushSelectorViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        BrushSelector selector = dataSetRef.get(position);
        BrushSelectorViewHolder viewHolder = (BrushSelectorViewHolder)holder;
        viewHolder.brush.setColor(selector.getOutColor(), selector.getDotColor());
        viewHolder.brush.setCircleType(selector.getType());
        viewHolder.brush.setChecked(selector.isCheck());

        if(listener != null && (holder.itemView.getTag() == null)) {
            holder.itemView.setTag("SET");
            holder.itemView.setOnClickListener((View view) -> listener.onItemClick(0, holder.getAdapterPosition()));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull @NotNull RecyclerView.ViewHolder holder, int position, @NonNull @NotNull List<Object> payloads) {
        if(payloads.isEmpty()) {
            onBindViewHolder(holder, position);
        }else {
            BrushSelector selector = dataSetRef.get(position);
            BrushSelectorViewHolder viewHolder = (BrushSelectorViewHolder)holder;
            viewHolder.brush.setColor(selector.getOutColor(), selector.getDotColor());
            viewHolder.brush.setCircleType(selector.getType());
            viewHolder.brush.setChecked(selector.isCheck());
        }
    }

    public void setItemClickListener(ItemClickListener listener) {
        this.listener = listener;
    }

    @Override
    public int getItemCount() {
        return dataSetRef.size();
    }

    @Override
    public int getItemViewType(int position) {
        return 0;
    }

    private static class BrushSelectorViewHolder extends RecyclerView.ViewHolder {
        private final BrushView brush;
        public BrushSelectorViewHolder(@NonNull View itemView) {
            super(itemView);
            brush = itemView.findViewById(R.id.color_view);
        }
    }

    public void close() {
        dataSetRef = null;
        listener = null;
    }
}
