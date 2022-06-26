package com.demo.epaper.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import com.demo.epaper.utils.AppUtils;

import androidx.annotation.Nullable;

public class BatteryView extends View {

    private float strokeWidth;
    private int batteryLevel;
    private Paint paint;

    private static final int BATTERY_LOW_LEVEL = 20;

    public BatteryView(Context context) {
        super(context);
        init(context);
    }

    public BatteryView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public BatteryView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        float density = context.getResources().getDisplayMetrics().density;
        strokeWidth = AppUtils.dp2Px(density,1);
        paint = new Paint();
        batteryLevel = 50;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    private int width, height;
    private RectF topRect;
    private float topBottom;
    private RectF borderRect;
    private RectF centerRect;

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        this.width = w;
        this.height = h;

        int widthSix =(w / 4);
        topBottom = (h * 1.6F / 10.0F);

        topRect = new RectF(widthSix,0, (widthSix * 3), topBottom);

        borderRect = new RectF(strokeWidth, topBottom , (w - strokeWidth), (h - strokeWidth));

        float centerHeight = (((float)h - topBottom) * (100 - batteryLevel) / 100.0F);
        centerRect = new RectF(strokeWidth, (topBottom + centerHeight), (w - strokeWidth), (h - strokeWidth));
    }

    public void setBatteryLevel(int level) {
        this.batteryLevel = level;
        float centerHeight = (((float)height - topBottom) * (100 - batteryLevel) / 100.0F);
        centerRect = new RectF(strokeWidth, (topBottom + centerHeight), (width - strokeWidth), (height - strokeWidth));
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xffcdcdcd);
        canvas.drawRoundRect(topRect, 2, 2, paint);
        if(batteryLevel <= BATTERY_LOW_LEVEL) {
            paint.setColor(Color.RED);
        }else {
            paint.setColor(0xFF52BF25);
        }
        canvas.drawRoundRect(centerRect, 2, 2, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(strokeWidth);
        paint.setColor(0xffcdcdcd);
        canvas.drawRoundRect(borderRect, 2, 2, paint);
    }
}
