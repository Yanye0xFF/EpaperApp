package com.demo.epaper.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.demo.epaper.R;
import com.demo.epaper.utils.AppUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ColorView extends View {

    public static final int COLOR_BLACK_WHITE = 0;
    public static final int COLOR_RED_WHITE = 1;
    public static final int COLOR_BLACK_WHITE_RED = 2;

    private int type;
    private boolean checked;
    private OnClickListener listener;


    public ColorView(Context context) {
        this(context, null);
    }

    public ColorView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ColorView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.ColorView, defStyleAttr, 0);
        this.type = a.getInt(R.styleable.ColorView_color_type, COLOR_BLACK_WHITE);
        a.recycle();

        init(context);
    }

    private Paint paint;

    private void init(Context context) {
        paint = new Paint();
        float density = context.getResources().getDisplayMetrics().density;
        paint.setStrokeWidth(AppUtils.dp2Px(density,3));
    }

    public void setType(int type) {
        this.type = type;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    private int width, height;
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        this.width = w;
        this.height = h;
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(event.getAction() == MotionEvent.ACTION_DOWN) {
            if(listener != null) {
                listener.onClick(this);
            }
            performClick();
        }
        return true;
    }

    @Override
    public void setOnClickListener(@NonNull OnClickListener listener) {
        this.listener = listener;
    }

    private static final int[] COLOR_LUT2 = new int[]{0xff293047, 0xfff15b6c};
    private static final int CHECKED_COLOR = 0xff45b97c;
    private static final int NORMAL_COLOR = 0xffafb4db;
    @Override
    protected void onDraw(Canvas canvas) {
        int middle;
        paint.setStyle(Paint.Style.FILL);
        if(type == COLOR_BLACK_WHITE_RED) {
            middle = (width / 3);
            paint.setColor(COLOR_LUT2[0]);
            canvas.drawRect(0, 0, middle, height, paint);
            paint.setColor(Color.WHITE);
            canvas.drawRect(middle, 0, (middle * 2), height, paint);
            paint.setColor(COLOR_LUT2[1]);
            canvas.drawRect((middle * 2), 0, width, height, paint);
        }else {
            middle = (width / 2);
            paint.setColor(COLOR_LUT2[type]);
            canvas.drawRect(0, 0, middle, height, paint);
            paint.setColor(Color.WHITE);
            canvas.drawRect(middle, 0, middle, height, paint);
        }
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(checked ? CHECKED_COLOR : NORMAL_COLOR);
        canvas.drawRoundRect(0, 0, width, height, 12, 12, paint);
    }
}
