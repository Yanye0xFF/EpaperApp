package com.demo.epaper.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class SignalView extends View {

    private Paint paint;
    private int signalLevel;

    public SignalView(Context context) {
        super(context);
        init();
    }

    public SignalView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SignalView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeCap(Paint.Cap.ROUND);
        this.signalLevel = 2;
    }

    public void setSignalLevel(int level) {
        this.signalLevel = level;
        this.invalidate();
    }

    private RectF[] rectLevel;

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        int splitWidth = (w / 8);
        int splitHeight = (h / 4);
        if(rectLevel == null) {
            rectLevel = new RectF[4];
        }

        for(int i = 0; i < 4; i++) {
            rectLevel[i] = new RectF((i * 2 * splitWidth), (splitHeight * (3 - i)), ((i * 2 + 1) * splitWidth), h);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int i;
        paint.setColor(0xFF17BAA5);
        for(i = 0; i < signalLevel; i++) {
            canvas.drawRoundRect(rectLevel[i], 2, 2, paint);
        }
        paint.setColor(0xffcdcdcd);
        for(i = signalLevel; i < 4; i++) {
            canvas.drawRoundRect(rectLevel[i], 2, 2, paint);
        }
    }
}
