package com.demo.epaper.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.SparseIntArray;
import android.view.View;

import com.demo.epaper.utils.AppUtils;

import java.util.Arrays;

import androidx.annotation.Nullable;

public class EpaperView extends View {

    private int offsetX = 0, offsetY = 0;

    private int pixelScale;

    private Paint paint;
    private Rect border;

    private Bitmap bitmap;
    private Matrix bmpMatrix;

    private float density;

    public static final int EPD_WIDTH = 250;
    public static final int EPD_HEIGHT = 122;

    private static final int BORDER_COLOR = 0xff1d953f;

    public static final int EPD_RAM_SIZE = 8000;

    public EpaperView(Context context) {
        this(context, null, 0);
    }

    public EpaperView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public EpaperView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        paint = new Paint();
        paint.setAntiAlias(false);

        bitmap = Bitmap.createBitmap(EPD_WIDTH, EPD_HEIGHT, Bitmap.Config.ARGB_8888);

        density = context.getResources().getDisplayMetrics().density;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int specHeightMode = MeasureSpec.getMode(heightMeasureSpec);
        int specWidthSize = MeasureSpec.getSize(widthMeasureSpec);

        int scaleHeightSize;

        if(specWidthSize >= 1440) {
            scaleHeightSize = (int) AppUtils.dp2Px(density, 250);
        }else if(specWidthSize >= 1080) {
            scaleHeightSize = (int)AppUtils.dp2Px(density, 230);
        }else if(specWidthSize >= 720) {
            scaleHeightSize = (int)AppUtils.dp2Px(density, 180);
        }else {
            scaleHeightSize = (int)AppUtils.dp2Px(density, 140);
        }

        if(specHeightMode == View.MeasureSpec.EXACTLY) {
            scaleHeightSize = MeasureSpec.getSize(heightMeasureSpec);
        }else if(specHeightMode == View.MeasureSpec.AT_MOST) {
            int recommendHeight = MeasureSpec.getSize(heightMeasureSpec);
            scaleHeightSize = Math.min(recommendHeight, scaleHeightSize);
        }
        setMeasuredDimension(specWidthSize, scaleHeightSize);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        if(w >= 1440) {
            pixelScale = 5;
        }else if(w >= 1080) {
            pixelScale = 4;
        }else if(w >= 720) {
            pixelScale = 2;
        }else {
            pixelScale = 1;
        }

        if(w > (EPD_WIDTH * pixelScale)) {
            offsetX = (w - EPD_WIDTH * pixelScale) / 2;
        }
        if(h > (EPD_HEIGHT * pixelScale)) {
            offsetY = (h - EPD_HEIGHT * pixelScale) / 2;
        }

        border = new Rect(
                (offsetX - pixelScale),
                (offsetY - pixelScale),
                (offsetX + EPD_WIDTH * pixelScale + pixelScale),
                (offsetY + EPD_HEIGHT * pixelScale + pixelScale)
        );

        bmpMatrix = new Matrix();
        bmpMatrix.postScale(pixelScale, pixelScale);
        bmpMatrix.postTranslate(offsetX, offsetY);
    }

    public void setPixels(int[] data, int width, int height) {
        bitmap.setPixels(data, 0, width, 0, 0, width, height);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(BORDER_COLOR);
        paint.setStrokeWidth(pixelScale);
        canvas.drawRect(border, paint);

        canvas.drawBitmap(bitmap, bmpMatrix, paint);
    }

    public void getTransformedData(byte[] displayBuffer) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int index, shift, y2, color;
        byte bits;

        SparseIntArray ColorLUT = new SparseIntArray(3);
        ColorLUT.append(0xFF000000, 0);
        ColorLUT.append(0xFFFFFFFF, 1);
        ColorLUT.append(0xFFFF0000, 3);

        Arrays.fill(displayBuffer, (byte)0x0);
        int[] pixels = new int[EPD_WIDTH * EPD_HEIGHT];
        bitmap.getPixels(pixels, 0, EPD_WIDTH, 0, 0, EPD_WIDTH, EPD_HEIGHT);

        for(int y = 0; y < height; y++) {
            for(int x = 0; x < width; x++) {
                color = pixels[(y * EPD_WIDTH) + x];
                bits = (byte)(ColorLUT.get(color) & 0x3);
                y2 = ((EPD_WIDTH - 1) - x);
                index = (y2 * 32) + (y / 4);
                shift = ((y & 3) << 1);
                displayBuffer[index] |= (bits << shift);
            }
        }
    }

    public void recycle() {
        bitmap.recycle();
        bitmap = null;
    }

}
