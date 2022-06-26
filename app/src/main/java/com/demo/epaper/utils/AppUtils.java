package com.demo.epaper.utils;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import com.demo.epaper.R;

import androidx.core.graphics.drawable.DrawableCompat;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class AppUtils {

    public static void setImageViewColor(ImageView view, Context context) {
        Drawable temp = DrawableCompat.wrap(view.getDrawable().mutate());
        DrawableCompat.setTintList(temp, ColorStateList.valueOf(getThemeAccentColor(context)));
        view.setImageDrawable(temp);
    }

    public static void setImageViewColor(ImageView view) {
        setImageViewColor(view, getThemeAccentColor(view.getContext()));
    }

    public static void setImageViewColor(ImageView view, int color) {
        Drawable temp = DrawableCompat.wrap(view.getDrawable().mutate());
        DrawableCompat.setTintList(temp, ColorStateList.valueOf(color));
        view.setImageDrawable(temp);
    }

    public static Drawable setDrawableColor(Drawable drawable, Context context) {
        Drawable temp = DrawableCompat.wrap(drawable);
        DrawableCompat.setTintList(temp, ColorStateList.valueOf(getThemeAccentColor(context)));
        return temp;
    }

    public static void setImageSource(ImageView view, int resId, Context context) {
        view.setImageResource(resId);
        setImageViewColor(view, context);
    }

    public static int getThemeAccentColor(Context context) {
        TypedArray typedArray = context.getResources().obtainTypedArray(R.array.colorAccentArray);
        int[] colors = new int[typedArray.length()];
        for (int i = 0; i < typedArray.length(); i++) {
            colors[i] = typedArray.getColor(i, 0);
        }
        typedArray.recycle();
        return colors[2];
    }

    public static final int INT_BYTES = 4;
    public static final int SHORT_BYTES = 2;

    public static float dp2Px(float density, float dp) {
        return (dp * density + 0.5f);
    }


    public static void int2Bytes(byte[] buffer, int offset, int value, int limit) {
        for(int i = 0; i < limit; i++) {
            buffer[offset + i] = (byte)((value >>> (i * 8)) & 0xFF);
        }
    }

    public static int bytes2Int(byte[] bytes, int offset, int size) {
        int value = 0;
        for(int i = 0; i < size; i++) {
            value |= ((bytes[offset + i] & 0xFF) << (i * 8));
        }
        return value;
    }

    public static int string2Bytes(String str, byte[] buffer, int max) {
        int length = str.length();
        if((length & 0x1) == 1) {
            return -1;
        }
        int num = Math.min((length >>> 1), max);
        int count = 0;
        for(int i = 0; i < num; i++) {
            try {
                buffer[i] = (byte)Integer.parseInt(str.substring((i << 1), (i << 1) + 2),16);
                count++;
            }catch (NumberFormatException e) {
                e.printStackTrace();
                break;
            }
        }
        return count;
    }

    public static String formatFileSize(int fileSize) {
        final String[] units = new String[]{"Bytes", "KB", "MB", "GB"};
        int index = 0, size = fileSize;
        while(size >= 1024) {
            size /= 1024;
            if(index < 3) {
                index++;
            }
        }
        if(fileSize < 1024) {
            return String.format(Locale.CHINA, "%d%s", size, units[index]);
        }
        double displaySize = (fileSize / Math.pow(1024.0D, index));
        size = (int)displaySize;
        if(displaySize > size) {
            return String.format(Locale.CHINA, "%.2f%s", displaySize, units[index]);
        }else {
            return String.format(Locale.CHINA, "%d%s", size, units[index]);
        }
    }

    public static String formatMacAddress(String plainText) {
        final int MAC_ADDRESS_LENGTH = 12;
        if(plainText.length() != MAC_ADDRESS_LENGTH) {
            return null;
        }
        char ch;
        StringBuilder builder = new StringBuilder(MAC_ADDRESS_LENGTH + 5);
        for(int i = 0; i < MAC_ADDRESS_LENGTH; i++) {
            if((i > 0) && (i & 0x1) == 0) {
                builder.append(':');
            }
            ch = plainText.charAt(i);
            builder.append(ch);
        }
        return builder.toString();
    }

    public static boolean isMacAddressValid(String address) {
        if(address.length() != 12) {
            return false;
        }
        byte[] bytes = address.toUpperCase().getBytes(StandardCharsets.UTF_8);
        for(byte b : bytes) {
            if((b < 0x30) || (b > 0x46) || ((b > 0x39) && (b < 0x41))) {
                return false;
            }
        }
        return true;
    }

    private static final int[] WIFI_LEVELS = new int[]{-60, -70, -80, -90};
    public static int calcRSSILevel(int rssi) {
        for(int i = 0; i < 4; i++) {
            if(rssi > WIFI_LEVELS[i]) {
                return (4 - i);
            }
        }
        return 1;
    }

}

