package com.demo.epaper.adapter;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.demo.epaper.R;
import com.demo.epaper.entity.BleDevice;

import java.util.List;

public class DeviceListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<BleDevice> dataSet;
    private AdapterView.OnItemClickListener listener;

    public DeviceListAdapter(List<BleDevice> dataSet) {
        this.dataSet = dataSet;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ble_device, parent, false);
        return new DeviceViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        DeviceViewHolder viewHolder = (DeviceViewHolder)holder;
        BleDevice device = dataSet.get(position);

        viewHolder.tvName.setText(device.getName());
        viewHolder.tvAddress.setText(device.getMac());
        viewHolder.tvRssi.setText(device.getRssi() + "dBm");

        if((listener != null) && (holder.itemView.getTag() == null)) {
            viewHolder.btnConnect.setOnClickListener((View view) ->
                listener.onItemClick(null, view, holder.getAdapterPosition(), 0));
        }
    }

    @Override
    public int getItemCount() {
        return dataSet.size();
    }

    public void setItemClickListener(AdapterView.OnItemClickListener listener) {
        this.listener = listener;
    }

    private static class DeviceViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvName;
        private final TextView tvAddress;
        private final TextView tvRssi;
        private final Button btnConnect;

        public DeviceViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.text_name);
            tvAddress = itemView.findViewById(R.id.text_address);
            tvRssi = itemView.findViewById(R.id.text_rssi);
            btnConnect = itemView.findViewById(R.id.btn_connect);
        }
    }
}
