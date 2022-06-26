package com.demo.epaper.adapter;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.recyclerview.widget.RecyclerView;

import com.demo.epaper.R;
import com.demo.epaper.entity.FileItem;
import com.demo.epaper.handler.ItemClickListener;
import com.demo.epaper.utils.AppUtils;

import org.jetbrains.annotations.NotNull;

public class FilePickerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<FileItem> dataSet;
    private ItemClickListener listener;

    public FilePickerAdapter(List<FileItem> dataSet) {
        this.dataSet = dataSet;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if(viewType == FileItem.TYPE_EMPTY) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_folder_empty, parent, false);
            return new PlaceHolderViewHolder(view);
        }else if(viewType == FileItem.TYPE_OPERATOR) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_file_operator, parent, false);
            return new OperatorViewHolder(view);
        }else {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_file, parent, false);
            return new FileViewHolder(view);
        }
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        FileItem fileItem = dataSet.get(position);
        int viewType = fileItem.getViewType();
        if(viewType == FileItem.TYPE_OPERATOR) {
            OperatorViewHolder viewHolder = (OperatorViewHolder)holder;
            viewHolder.image.setImageResource(fileItem.getSize());
            viewHolder.title.setText(fileItem.getTitle());

        }else if(viewType == FileItem.TYPE_FILE) {
            FileViewHolder viewHolder = (FileViewHolder)holder;
            if(fileItem.getPath().toLowerCase().endsWith(".bin")) {
                viewHolder.ivIcon.setImageResource(R.mipmap.ic_file_app);
            }else {
                viewHolder.ivIcon.setImageResource(R.mipmap.ic_file_common);
            }
            viewHolder.tvTitle.setText(fileItem.getTitle());
            viewHolder.tvSubTitle.setText("文件大小: " + AppUtils.formatFileSize(fileItem.getSize()));

        }else if(viewType == FileItem.TYPE_FOLDER) {
            FileViewHolder viewHolder = (FileViewHolder)holder;
            viewHolder.ivIcon.setImageResource(R.mipmap.ic_file_folder);
            viewHolder.tvTitle.setText(fileItem.getTitle());
            if(fileItem.getSize() == 0) {
                viewHolder.tvSubTitle.setText("空的文件夹");
            }else {
                viewHolder.tvSubTitle.setText(String.format(Locale.CHINA, "总计%d个文件及文件夹", fileItem.getSize()));
            }
        }
        if((listener != null) && (viewType != FileItem.TYPE_EMPTY) && (holder.itemView.getTag()== null)) {
            holder.itemView.setTag("SET");
            holder.itemView.setOnClickListener((View view) ->
                    listener.onItemClick(0, holder.getAdapterPosition()));
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
        this.listener = listener;
    }

    private static class PlaceHolderViewHolder extends RecyclerView.ViewHolder {
        private PlaceHolderViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    private static class FileViewHolder extends RecyclerView.ViewHolder {
        private final AppCompatImageView ivIcon;
        private final TextView tvTitle;
        private final TextView tvSubTitle;
        public FileViewHolder(@NonNull @NotNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.ic_file_icon);
            tvTitle = itemView.findViewById(R.id.tv_file_name);
            tvSubTitle = itemView.findViewById(R.id.tv_file_desc);
        }
    }

    private static class OperatorViewHolder extends RecyclerView.ViewHolder {
        private final AppCompatImageView image;
        private final TextView title;
        private OperatorViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.ic_operator_icon);
            title = itemView.findViewById(R.id.tv_operator_title);
        }
    }
}
