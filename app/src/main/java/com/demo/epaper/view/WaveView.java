package com.demo.epaper.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import androidx.core.content.ContextCompat;

import com.demo.epaper.R;

import java.util.ArrayList;
import java.util.List;

public class WaveView extends View {
    private boolean isClear;
    private boolean isFill;
    private final List<Integer> mAlphas;
    private int mColor;
    private int mImageRadius;
    private boolean mIsWave;
    private int mMaxRadius;
    private Paint mPaint;
    private final List<Integer> mRadius;
    private int mWidth;

    public WaveView(Context context) {
        this(context, null);
    }

    public WaveView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WaveView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mColor = ContextCompat.getColor(getContext(), R.color.colorPrimaryBlue);
        this.mImageRadius = 50;
        this.mWidth = 3;
        this.mMaxRadius = 300;
        this.mIsWave = false;
        this.mAlphas = new ArrayList<>(128);
        this.mRadius = new ArrayList<>(128);
        this.isFill = true;
        this.isClear = true;
        init();
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.WaveView, defStyleAttr, 0);
        this.mColor = a.getColor(R.styleable.WaveView_wave_color, this.mColor);
        this.mWidth = a.getInt(R.styleable.WaveView_wave_width, this.mWidth);
        this.mImageRadius = a.getInt(R.styleable.WaveView_wave_coreImageRadius, this.mImageRadius);
        a.recycle();
    }

    private void init() {
        this.mPaint = new Paint();
        this.mPaint.setAntiAlias(true);
        this.mAlphas.add(255);
        this.mRadius.add(0);
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        this.mMaxRadius = (getWidth() > getHeight() ? getHeight() : getWidth()) / 2;
        invalidate();
    }

    @Override
    public void invalidate() {
        if (hasWindowFocus()) {
            super.invalidate();
        }
    }

    @Override
    public void onDraw(Canvas canvas) {
        if (this.isClear) {
            this.mPaint.setColor(ContextCompat.getColor(getContext(), R.color.white));
        } else {
            this.mPaint.setColor(this.mColor);
        }
        for (int i = 0; i < this.mAlphas.size(); i++) {
            int alpha = this.mAlphas.get(i);
            this.mPaint.setAlpha(alpha);
            Integer radius = this.mRadius.get(i);
            if (this.isFill) {
                this.mPaint.setStyle(Paint.Style.FILL);
                canvas.drawCircle(getWidth() / 2.0F, getHeight() / 2.0F, this.mImageRadius + radius, this.mPaint);
                this.mPaint.setStyle(Paint.Style.STROKE);
                this.mPaint.setStrokeWidth(5.0f);
                canvas.drawCircle(getWidth() / 2.0F, getHeight() / 2.0F, this.mImageRadius + radius, this.mPaint);
            } else {
                this.mPaint.setStyle(Paint.Style.STROKE);
                this.mPaint.setStrokeWidth(5.0f);
                canvas.drawCircle(getWidth() / 2.0F, getHeight() / 2.0F, this.mImageRadius + radius, this.mPaint);
            }
            if (alpha > 0 && this.mImageRadius + radius < this.mMaxRadius) {
                this.mAlphas.set(i, (int) ((1.0f - (((mImageRadius + radius) * 1.0f) / this.mMaxRadius)) * 255.0f));
                this.mRadius.set(i, radius + 1);
            } else if (alpha < 0 && this.mImageRadius + radius > this.mMaxRadius) {
                this.mRadius.remove(i);
                this.mAlphas.remove(i);
            }
        }
        List<Integer> list = this.mRadius;
        if (list.get(list.size() - 1) == this.mWidth) {
            addWave();
        }
        if (this.mIsWave) {
            invalidate();
        }
    }

    public void start() {
        this.mIsWave = true;
        this.isClear = false;
        invalidate();
    }

    public void stop() {
        this.mIsWave = false;
    }

    public boolean isWave() {
        return this.mIsWave;
    }

    public void setColor(int colorId) {
        this.mColor = colorId;
    }

    public void setWidth(int width) {
        this.mWidth = width;
    }

    public void setMaxRadius(int maxRadius) {
        this.mMaxRadius = maxRadius;
    }

    public void setImageRadius(int imageRadius) {
        this.mImageRadius = imageRadius;
    }

    public boolean isFill() {
        return this.isFill;
    }

    public void setFill(boolean fill) {
        this.isFill = fill;
    }

    public void addWave() {
        this.mAlphas.add(255);
        this.mRadius.add(0);
    }

    public void clearWave() {
        this.isClear = true;
    }
}