package com.demo.epaper.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.SparseIntArray;
import android.view.MotionEvent;
import android.view.View;

import com.demo.epaper.utils.AppUtils;

import java.util.Arrays;
import java.util.Locale;

import androidx.annotation.Nullable;

public class PixelView extends View {
    private int offsetX = 0, offsetY = 0;
    private int limitX, limitY;

    private int textY;
    private static final  int TEXT_SIZE = 32;

    private int pixelScale;

    private Paint paint;
    private Rect border;

    private Bitmap bitmap;
    private Path bmpPath;
    private Paint bmpPaint;
    private Canvas bmpCanvas;
    private Matrix bmpMatrix;

    private float scaleX = 0, scaleY = 0;
    private float density;

    public static final int EPD_WIDTH = 250;
    public static final int EPD_HEIGHT = 122;

    private int brushType;
    public static final int BRUSH_NONE = 0;
    public static final int BRUSH_DRAW = 100;
    public static final int BRUSH_LINE = 101;
    public static final int BRUSH_RECTANGLE = 102;
    public static final int BRUSH_OVAL = 103;
    public static final int BRUSH_TEXT = 104;
    public static final int BRUSH_IMAGE = 107;
    public static final int BRUSH_DONE = 108;

    private static final int BORDER_COLOR = 0xff1d953f;

    public static final int EPD_RAM_SIZE = 8000;

    public PixelView(Context context) {
        super(context);
        init(context);
    }

    public PixelView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public PixelView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        brushType = BRUSH_DRAW;
        paint = new Paint();
        paint.setAntiAlias(false);

        bmpPath = new Path();

        bmpPaint = new Paint();
        bmpPaint.setStyle(Paint.Style.STROKE);
        bmpPaint.setStrokeCap(Paint.Cap.ROUND);
        bmpPaint.setAntiAlias(false);
        bmpPaint.setColor(0xFF000000);
        bmpPaint.setStrokeWidth(2);

        bitmap = Bitmap.createBitmap(EPD_WIDTH, EPD_HEIGHT, Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(Color.WHITE);

        density = context.getResources().getDisplayMetrics().density;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int specHeightMode = MeasureSpec.getMode(heightMeasureSpec);
        int specWidthSize = MeasureSpec.getSize(widthMeasureSpec);

        int scaleHeightSize;

        if(specWidthSize >= 1440) {
            scaleHeightSize = (int)AppUtils.dp2Px(density, 250);
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

        limitX = (offsetX + (EPD_WIDTH * pixelScale));
        limitY = (offsetY + (EPD_HEIGHT * pixelScale));
        textY = (offsetY + (EPD_HEIGHT * pixelScale) + TEXT_SIZE);

        border = new Rect(
                (offsetX - pixelScale),
                (offsetY - pixelScale),
                (offsetX + EPD_WIDTH * pixelScale + pixelScale),
                (offsetY + EPD_HEIGHT * pixelScale + pixelScale)
        );

        bmpCanvas = new Canvas(bitmap);

        bmpMatrix = new Matrix();
        bmpMatrix.postScale(pixelScale, pixelScale);
        bmpMatrix.postTranslate(offsetX, offsetY);
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    private float startX, startY;
    private Bitmap saveBitmap;
    private String text;
    private int[] imageArray;
    private int imageWidth, imageHeight;
    private float imageX, imageY;
    private boolean allowDraw = true;
    private float pointX, pointY;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        int action = event.getAction();

        if(brushType == BRUSH_NONE) {
            return true;
        }
        if((action == MotionEvent.ACTION_DOWN) && ((x < offsetX) || (x > limitX) || (y < offsetY) || (y > limitY))) {
            allowDraw = false;
            return true;
        }

        scaleX = ((x - offsetX) / pixelScale);
        scaleY = ((y - offsetY) / pixelScale);

        if(action == MotionEvent.ACTION_DOWN) {
            if(brushType == BRUSH_DRAW) {
                bmpPath.reset();
                bmpPath.moveTo(scaleX, scaleY);
                bmpPath.lineTo(scaleX, scaleY);
            }else if((brushType >= BRUSH_LINE) && (brushType <= BRUSH_OVAL)) {
                startX = scaleX; startY = scaleY;
                if(saveBitmap == null) {
                    saveBitmap = Bitmap.createBitmap(bitmap);
                }
            }else if(brushType == BRUSH_TEXT) {
                startX = scaleX; startY = scaleY;
            } else if(brushType == BRUSH_IMAGE) {
                startX = scaleX; startY = scaleY;
            }
        }else if(action == MotionEvent.ACTION_MOVE && allowDraw) {
            if(brushType == BRUSH_DRAW) {
                bmpPath.lineTo(scaleX, scaleY);
                bmpCanvas.drawPath(bmpPath, bmpPaint);
            }else {
                bmpCanvas.drawBitmap(saveBitmap, 0, 0, bmpPaint);
            }
            if(brushType == BRUSH_LINE) {
                bmpCanvas.drawLine(startX, startY, scaleX, scaleY, bmpPaint);
            }else if(brushType == BRUSH_RECTANGLE) {
                bmpCanvas.drawRect(startX, startY, scaleX, scaleY, bmpPaint);
            }else if(brushType == BRUSH_OVAL) {
                bmpCanvas.drawOval(startX, startY, scaleX, scaleY, bmpPaint);
            }else if(brushType == BRUSH_TEXT) {
                pointX = imageX + (scaleX - startX);
                pointY = imageY + (scaleY - startY);
                bmpCanvas.drawText(text, pointX, pointY, bmpPaint);
            }else if(brushType == BRUSH_IMAGE) {
                pointX = imageX + (scaleX - startX);
                pointY = imageY + (scaleY - startY);
                bmpCanvas.drawBitmap(imageArray, 0, imageWidth, pointX, pointY, imageWidth, imageHeight, false, bmpPaint);
            }
            invalidate();
        }else if(action == MotionEvent.ACTION_UP) {
            allowDraw = true;
            if(brushType == BRUSH_DRAW) {
                bmpPath.reset();
            }else if((brushType >= BRUSH_LINE) && (brushType <= BRUSH_OVAL)) {
                if(saveBitmap != null) {
                    saveBitmap.recycle();
                    saveBitmap = null;
                }
            }else if((brushType == BRUSH_TEXT) || (brushType == BRUSH_IMAGE)) {
                imageX = pointX;
                imageY = pointY;
            }
            scaleX = -1; scaleY = -1;
            invalidate();
            performClick();
        }
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(BORDER_COLOR);
        paint.setStrokeWidth(pixelScale);
        canvas.drawRect(border, paint);

        canvas.drawBitmap(bitmap, bmpMatrix, paint);

        if((scaleX > 0) && (scaleY > 0)) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.BLACK);
            paint.setTextSize(pixelScale * 8);
            canvas.drawText(String.format(Locale.CHINA, "%d, %dpx", (int)scaleX, (int)scaleY), offsetX, textY, paint);
        }
    }

    public void clear() {
        bitmap.eraseColor(bmpPaint.getColor());
        invalidate();
    }

    private int cursor = 0;
    private static final int[] COLOR_SET = new int[]{0xFF000000, 0xFFFFFFFF, 0xFFFF0000};

    public int nextPaintColor() {
        cursor = (cursor == 2) ? 0 : (cursor + 1);
        int color = COLOR_SET[cursor];
        this.bmpPaint.setColor(color);
        return color;
    }

    public void setPaintColor(int color) {
        this.bmpPaint.setColor(color);
    }

    public void setPaintWidth(int width) {
        this.bmpPaint.setStrokeWidth(width);
        if((brushType == BRUSH_TEXT) && (saveBitmap != null)) {
            float textSize = AppUtils.dp2Px(density, width);
            bmpPaint.setTextSize(textSize);
            bmpCanvas.drawBitmap(saveBitmap, 0, 0, bmpPaint);
            pointY = Math.max(pointY, textSize);
            pointX = Math.max(pointX, 0);
            imageX = pointX;
            imageY = pointY;
            bmpCanvas.drawText(text, pointX, pointY, bmpPaint);
            invalidate();
        }
    }

    public void setPaintType(int type) {
        if(saveBitmap != null) {
            saveBitmap.recycle();
            saveBitmap = null;
        }
        if(type == BrushView.CIRCLE_TYPE_ERASE) {
            brushType = BRUSH_DRAW;
            this.bmpPaint.setColor(0xFFFFFFFF);
        }else {
            brushType = type;
        }
    }

    public void setPaintText(String text) {
        this.text = text;
        if(saveBitmap == null) {
            float width = bmpPaint.getStrokeWidth();
            float textSize = AppUtils.dp2Px(density, width);
            saveBitmap = Bitmap.createBitmap(bitmap);
            bmpPaint.setStyle(Paint.Style.FILL);
            bmpPaint.setTextSize(textSize);
            pointX = 0;
            pointY = textSize;
            bmpCanvas.drawText(text, pointX, pointY, bmpPaint);
            invalidate();
        }
    }

    public void paintTextDone() {
        brushType = BRUSH_NONE;
        bmpPaint.setStyle(Paint.Style.STROKE);
        if(saveBitmap != null) {
            saveBitmap.recycle();
            saveBitmap = null;
        }
    }

    public void setPaintImage(int[] color, int width, int height) {
        this.imageArray = color;
        this.imageWidth = width;
        this.imageHeight = height;
        if(saveBitmap == null) {
            saveBitmap = Bitmap.createBitmap(bitmap);
            imageX = 0; imageY = 0;
            bitmap.setPixels(color, 0, width, 0, 0, width, height);
        }else {
            bmpCanvas.drawBitmap(imageArray, 0, imageWidth, imageX, imageY, imageWidth, imageHeight, false, bmpPaint);
        }
        invalidate();
    }

    public void paintImageDone() {
        saveBitmap.recycle();
        saveBitmap = null;
        imageArray = null;
    }

    public void setPixels(int[] data, int width, int height) {
        bitmap.setPixels(data, 0, width, 0, 0, width, height);
        invalidate();
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

    public Bitmap getBitmap() {
        return this.bitmap;
    }

    public void recycle() {
        if(saveBitmap != null) {
            saveBitmap.recycle();
            saveBitmap = null;
        }
        bitmap.recycle();
        bitmap = null;
        bmpPath.close();
        bmpPath = null;
        imageArray = null;
    }
}
