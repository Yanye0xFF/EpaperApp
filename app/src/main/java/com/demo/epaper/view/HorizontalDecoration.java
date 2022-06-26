package com.demo.epaper.view;

import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class HorizontalDecoration extends RecyclerView.ItemDecoration {

    private int space;

    public HorizontalDecoration(int space) {
        this.space = space;
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent,
                               @NonNull RecyclerView.State state) {
        if(parent.getChildAdapterPosition(view) == 0) {
            outRect.left = space;
        }else {
            outRect.left = space / 2;
        }
    }
}
