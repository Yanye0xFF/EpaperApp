package com.demo.epaper.view;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import com.demo.epaper.R;
import com.demo.epaper.utils.AppUtils;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

public class DottedProgressBar extends View {
    private boolean isActiveDrawable;
    private boolean isInactiveDrawable;
    private Drawable mActiveDot;
    private int mActiveDotColor;
    private int mActiveDotIndex;
    private final float mDotSize;
    private int mEmptyDotsColor;
    private Drawable mInactiveDot;
    private final int mJumpingSpeed;
    private int mNumberOfDots;
    private int mPaddingLeft;
    private final Paint mPaint;
    private final float mSpacing;

    private final Runnable mRunnable = () -> {
        if (DottedProgressBar.this.mNumberOfDots != 0) {
            DottedProgressBar dottedProgressBar = DottedProgressBar.this;
            dottedProgressBar.mActiveDotIndex = (dottedProgressBar.mActiveDotIndex + 1) % DottedProgressBar.this.mNumberOfDots;
        }
        DottedProgressBar.this.invalidate();
        DottedProgressBar.this.mHandler.postDelayed(DottedProgressBar.this.mRunnable, DottedProgressBar.this.mJumpingSpeed);
    };
    private boolean isInProgress = false;
    private final Handler mHandler = new Handler();

    public DottedProgressBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.isActiveDrawable = false;
        this.isInactiveDrawable = false;
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.DottedProgressBar, 0, 0);
        try {
            TypedValue value = new TypedValue();
            a.getValue(R.styleable.DottedProgressBar_activeDot, value);
            if (value.type >= 28 && value.type <= 31) {
                this.isActiveDrawable = false;
                this.mActiveDotColor = ContextCompat.getColor(getContext(), value.resourceId);
            } else if (value.type == 3) {
                this.isActiveDrawable = true;
                this.mActiveDot = ContextCompat.getDrawable(getContext(), value.resourceId);
                if(mActiveDot != null) {
                    DrawableCompat.setTintList(DrawableCompat.wrap(this.mActiveDot), ColorStateList.valueOf(AppUtils.getThemeAccentColor(context)));
                }
            }
            a.getValue(R.styleable.DottedProgressBar_inactiveDot, value);
            if (value.type >= 28 && value.type <= 31) {
                this.isInactiveDrawable = false;
                this.mEmptyDotsColor = ContextCompat.getColor(getContext(), value.resourceId);
            } else if (value.type == 3) {
                this.isInactiveDrawable = true;
                this.mInactiveDot = ContextCompat.getDrawable(getContext(), value.resourceId);
            }
            this.mDotSize = a.getDimensionPixelSize(R.styleable.DottedProgressBar_dotSize, 5);
            this.mSpacing = a.getDimensionPixelSize(R.styleable.DottedProgressBar_spacing, 10);
            this.mActiveDotIndex = a.getInteger(R.styleable.DottedProgressBar_activeDotIndex, 0);
            this.mJumpingSpeed = a.getInt(R.styleable.DottedProgressBar_jumpingSpeed, 500);
            this.mPaint = new Paint(1);
            this.mPaint.setStyle(Paint.Style.FILL);
        } finally {
            a.recycle();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (int i = 0; i < this.mNumberOfDots; i++) {
            float f = this.mSpacing;
            int x = (int) (getPaddingLeft() + this.mPaddingLeft + (f / 2.0f) + (i * (f + this.mDotSize)));
            if (this.isInactiveDrawable) {
                this.mInactiveDot.setBounds(x, getPaddingTop(), (int) (x + this.mDotSize), getPaddingTop() + ((int) this.mDotSize));
                this.mInactiveDot.draw(canvas);
            } else {
                this.mPaint.setColor(this.mEmptyDotsColor);
                float f2 = this.mDotSize;
                canvas.drawCircle(x + (this.mDotSize / 2.0f), getPaddingTop() + (f2 / 2.0f), f2 / 2.0f, this.mPaint);
            }
        }
        if (this.isInProgress) {
            float f3 = this.mSpacing;
            int x2 = (int) (getPaddingLeft() + this.mPaddingLeft + (f3 / 2.0f) + (this.mActiveDotIndex * (f3 + this.mDotSize)));
            if (this.isActiveDrawable) {
                this.mActiveDot.setBounds(x2, getPaddingTop(), (int) (x2 + this.mDotSize), getPaddingTop() + ((int) this.mDotSize));
                this.mActiveDot.draw(canvas);
                return;
            }
            this.mPaint.setColor(this.mActiveDotColor);
            float f4 = this.mDotSize;
            canvas.drawCircle(x2 + (this.mDotSize / 2.0f), getPaddingTop() + (f4 / 2.0f), f4 / 2.0f, this.mPaint);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int parentWidth = View.MeasureSpec.getSize(widthMeasureSpec);
        int widthWithoutPadding = (parentWidth - getPaddingLeft()) - getPaddingRight();
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(parentWidth, getPaddingTop() + getPaddingBottom() + ((int) this.mDotSize));
        this.mNumberOfDots = calculateDotsNumber(widthWithoutPadding);
    }

    private int calculateDotsNumber(int width) {
        float f = this.mDotSize;
        float f2 = this.mSpacing;
        int number = (int) (width / (f + f2));
        this.mPaddingLeft = (int) ((width % (f + f2)) / 2.0f);
        return number;
    }

    public void startProgress() {
        this.isInProgress = true;
        this.mActiveDotIndex = -1;
        this.mHandler.removeCallbacks(this.mRunnable);
        this.mHandler.post(this.mRunnable);
    }

    public void stopProgress() {
        this.isInProgress = false;
        this.mHandler.removeCallbacks(this.mRunnable);
        invalidate();
    }
}
