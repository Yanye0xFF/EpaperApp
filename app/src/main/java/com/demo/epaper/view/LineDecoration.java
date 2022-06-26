package com.demo.epaper.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.demo.epaper.utils.AppUtils;

public class LineDecoration extends RecyclerView.ItemDecoration {

    public static final int HORIZONTAL = LinearLayout.HORIZONTAL;
    public static final int VERTICAL = LinearLayout.VERTICAL;

    private static final int[] ATTRS = new int[]{android.R.attr.listDivider};

    private Drawable mDivider;

    private final int formIndex;
    private final int endIndex;
    private final float density;
    private int mOrientation;

    private final Rect mBounds = new Rect();

    public LineDecoration(Context context, int orientation, int formIndex, int endIndex) {
        this.formIndex = formIndex;
        this.endIndex = endIndex;
        density = context.getResources().getDisplayMetrics().density;
        TypedArray a = context.obtainStyledAttributes(ATTRS);
        this.mDivider = a.getDrawable(0);
        a.recycle();

        this.setOrientation(orientation);
    }

    public void setOrientation(int orientation) {
        if (orientation != 0 && orientation != 1) {
            throw new IllegalArgumentException("Invalid orientation. It should be either HORIZONTAL or VERTICAL");
        } else {
            this.mOrientation = orientation;
        }
    }

    public void setDrawable(@NonNull Drawable drawable) {
        this.mDivider = drawable;
    }

    @Override
    public void onDraw(@NonNull Canvas c, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        if (parent.getLayoutManager() == null || mDivider == null) {
            return;
        }
        if (mOrientation == VERTICAL) {
            drawVertical(c, parent);
        } else {
            drawHorizontal(c, parent);
        }
    }

    private void drawVertical(Canvas canvas, RecyclerView parent) {
        canvas.save();
        int left, right;
        int top, bottom;
        if (parent.getClipToPadding()) {
            left = parent.getPaddingLeft();
            right = parent.getWidth() - parent.getPaddingRight();
            canvas.clipRect(left, parent.getPaddingTop(), right,
                    parent.getHeight() - parent.getPaddingBottom());
        } else {
            left = 0;
            right = parent.getWidth();
        }

        int childCount = parent.getChildCount();
        View child;
        for (int i = 0; i < childCount; i++) {
            if(i >= formIndex && i < endIndex) {
                child = parent.getChildAt(i);
                parent.getDecoratedBoundsWithMargins(child, mBounds);
                bottom = mBounds.bottom + Math.round(child.getTranslationY());
                top = bottom - mDivider.getIntrinsicHeight();
                mDivider.setBounds(left, top, right, bottom);
                mDivider.draw(canvas);
            }
        }
        canvas.restore();
    }

    private void drawHorizontal(Canvas canvas, RecyclerView parent) {
        canvas.save();
        int top, bottom;
        float len, center;
        if (parent.getClipToPadding()) {
            top = parent.getPaddingTop();
            bottom = parent.getHeight() - parent.getPaddingBottom();
            len = AppUtils.dp2Px(density, 15);
            center = (bottom - top + 1) / 2.0f;
            top = (int)(center - len);
            bottom = (int)(center + len);
            canvas.clipRect(parent.getPaddingLeft(), top,
                    parent.getWidth() - parent.getPaddingRight(), bottom);
        } else {
            top = 0;
            bottom = parent.getHeight();
        }

        final int childCount = parent.getChildCount();
        for (int i = 0; i < childCount; i++) {
            if(i >= formIndex && i < endIndex) {
                final View child = parent.getChildAt(i);
                parent.getLayoutManager().getDecoratedBoundsWithMargins(child, mBounds);
                final int right = mBounds.right + Math.round(child.getTranslationX());
                final int left = right - mDivider.getIntrinsicWidth();
                mDivider.setBounds(left, top, right, bottom);
                mDivider.draw(canvas);
            }
        }
        canvas.restore();
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        if(this.mDivider == null) {
            outRect.set(0, 0, 0, 0);
            return;
        }
        int position = parent.getChildAdapterPosition(view);
        if(position >= formIndex &&  position < endIndex) {
            if(this.mOrientation == LinearLayout.VERTICAL) {
                outRect.set(0, 0, 0, this.mDivider.getIntrinsicHeight());
            }else {
                outRect.set(0, 0, this.mDivider.getIntrinsicWidth(), 0);
            }
        }else {
            outRect.set(0, 0, 0, 0);
        }
    }
}
