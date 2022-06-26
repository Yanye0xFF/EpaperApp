package com.demo.epaper.view;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.demo.epaper.utils.AppUtils;

import org.jetbrains.annotations.NotNull;

public class BrushView extends View {
    private Paint paint;
    private int radius;

    private boolean checked;
    private int dotColor;
    private int outColor;
    private int circleType;

    private Path bezierPath;
    private float density;
    private static final int CHECKED_OUTER_COLOR = 0xFF5DB7F6;
    private static final int CHECKED_INNER_COLOR = 0xFFFFFFFF;

    public static final int CIRCLE_TYPE_COLOR = 99;
    public static final int CIRCLE_TYPE_DRAW = 100;
    public static final int CIRCLE_TYPE_LINE = 101;
    public static final int CIRCLE_TYPE_RECTANGLE = 102;
    public static final int CIRCLE_TYPE_OVAL = 103;
    public static final int CIRCLE_TYPE_TEXT = 104;
    public static final int CIRCLE_TYPE_ERASE = 105;
    public static final int CIRCLE_TYPE_FILL = 106;
    public static final int CIRCLE_TYPE_DONE = 108;

    private int strokeWidth;
    private int screenWidth;

    public BrushView(@NonNull @NotNull Context context) {
        super(context);
        init(context);
    }

    public BrushView(@NonNull Context context, @Nullable  AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public BrushView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        density = context.getResources().getDisplayMetrics().density;

        Resources resources = context.getResources();
        DisplayMetrics dm = resources.getDisplayMetrics();
        screenWidth = dm.widthPixels;

        circleType = CIRCLE_TYPE_COLOR;
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStrokeCap(Paint.Cap.ROUND);
    }

    private float centerX, centerY, half;

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        int size = Math.min(w, h);
        radius = (size / 2);
        centerX = w / 2.0F;
        centerY = h / 2.0F;
        half = (radius / 3.0f);

        if(screenWidth >= 1440) {
            strokeWidth = 6;
        }else if(screenWidth >= 1080) {
            strokeWidth = 5;
        }else if(screenWidth >= 720) {
            strokeWidth = 4;
        }else {
            strokeWidth = 3;
        }

        if(bezierPath == null) {
            bezierPath = new Path();
            bezierPath.moveTo(centerX - 25, centerY + 10);
            bezierPath.quadTo(centerX - 10, centerY - 40 , centerX + 5, centerY + 10);
            bezierPath.moveTo(centerX + 5, centerY + 10);
            bezierPath.quadTo(centerX + 20, centerY - 40 , centerX + 25, centerY + 10);
        }
    }

    public void setColor(int outColor, int dotColor) {
        this.outColor = outColor;
        this.dotColor = dotColor;
    }

    public void setCircleType(int type) {
        this.circleType = type;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
        invalidate();
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        paint.setColor(checked ? CHECKED_OUTER_COLOR : outColor);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(centerX, centerY, radius, paint);
        paint.setColor(checked ? CHECKED_INNER_COLOR : dotColor);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(strokeWidth);

        switch (circleType) {
            case CIRCLE_TYPE_COLOR:
                paint.setStyle(Paint.Style.FILL);
                canvas.drawCircle(centerX, centerY, (radius / 3.0F), paint);
                break;
            case CIRCLE_TYPE_DRAW:
                canvas.rotate(65.0F, centerX, centerY);
                canvas.drawPath(bezierPath, paint);
                break;
            case CIRCLE_TYPE_LINE:
                canvas.drawLine((centerX - half), (centerY + half), (centerX + half), (centerY - half), paint);
                break;
            case CIRCLE_TYPE_RECTANGLE:
                canvas.drawRoundRect((centerX - half), (centerY - half), (centerX + half), (centerY + half), 2, 2, paint);
                break;
            case CIRCLE_TYPE_OVAL:
                canvas.drawCircle(centerX, centerY, half, paint);
                break;
            case CIRCLE_TYPE_TEXT:
                paint.setStyle(Paint.Style.FILL);
                paint.setTextSize(AppUtils.dp2Px(density, 20));
                canvas.drawText("A", (centerX - 18), (centerY + 15), paint);
                break;
            case CIRCLE_TYPE_ERASE:
                canvas.rotate(45, centerX, centerY);
                canvas.drawRoundRect((centerX - 14), (centerY - 23), (centerX + 14), (centerY + 23), 2, 2, paint);
                paint.setStyle(Paint.Style.FILL);
                canvas.drawRect((centerX - 14), (centerY + 10), (centerX + 14), (centerY + 23), paint);
                break;
            case CIRCLE_TYPE_FILL:
                canvas.drawLine((centerX - 20), centerY, (centerX + 20), centerY, paint);
                canvas.drawLine((centerX - 20), centerY, (centerX - 25), centerY + 20, paint);
                canvas.drawLine((centerX + 20), centerY, (centerX + 15), centerY + 20, paint);

                canvas.drawLine(centerX - 10, centerY, centerX - 15, centerY + 20, paint);
                canvas.drawLine(centerX + 10, centerY, centerX + 5, centerY + 20, paint);
                canvas.drawLine(centerX , centerY, centerX - 5, centerY + 20, paint);

                paint.setStyle(Paint.Style.FILL);
                canvas.drawRoundRect((centerX - 10), (centerY - 20), (centerX + 10), centerY, 2, 2, paint);
                break;
            case CIRCLE_TYPE_DONE:
                canvas.rotate(45, centerX, centerY);
                canvas.drawLine(centerX - 10, (centerY + 15), (centerX + 10), centerY + 15, paint);
                canvas.drawLine(centerX + 10, (centerY - 28), centerX + 10, centerY + 15, paint);
                break;
            default:
                break;
        }
    }
}
