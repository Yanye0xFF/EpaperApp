package com.demo.epaper.imageio;

import android.graphics.Color;

import com.demo.epaper.entity.AirCondition;
import com.demo.epaper.entity.CalendarInfo;
import com.demo.epaper.entity.FontInfo;
import com.demo.epaper.entity.RecentWeather;
import com.demo.epaper.entity.TodayWeather;
import com.demo.epaper.entity.ForecastWeather;
import com.demo.epaper.handler.FontPreProcessCallback;
import com.demo.epaper.utils.AppUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class WeatherImageGenerator {
    private static final int EPD_WIDTH = 250;
    private static final int EPD_HEIGHT = 122;
    private static final int BITMAP_BUFFER_MAX = 32;

    public static final int BITMAP_DIB = 0x4D42;
    public static final byte BI_RGB = 0;
    public static final byte MONOCHROME = 0x1;

    public static final int BFTYPE = 0;
    public static final int BFTYPE_LENGTH = 2;

    public static final int BFOFFBITS = 10;
    public static final int BFOFFBITS_LENGTH = 4;

    public static final int BIBITCOUNT = 28;
    public static final int BIBITCOUNT_LENGTH = 2;

    public static final int BICOMPRESSION = 30;
    public static final int BICOMPRESSION_LENGTH = 4;

    public static final int BIWIDTH = 18;
    public static final int BIWIDTH_LENGTH = 4;

    public static final int BIHEIGHT = 22;
    public static final int BIHEIGHT_LENGTH = 4;

    private final String filesDir;

    private int[] innerBuffer;

    private FontInfo font12x12CN;
    private FontInfo font8x16EN;
    private FontInfo fontArial16EN;

    private byte[] bitmapBuffer;

    public WeatherImageGenerator(String filesDir) {
        this.filesDir = filesDir;

        innerBuffer = new int[250 * 122];
        bitmapBuffer = new byte[BITMAP_BUFFER_MAX];

        font12x12CN = new FontInfo("GB231212.bin",0xA1A1, 0xFEFE, 12, 12, 2, 24);
        font12x12CN.setFontPreProcessCallback(preProcessCN);

        font8x16EN = new FontInfo("AS08_16.bin",0x20, 0x7E, 8, 16, 1, 16);
        font8x16EN.setFontPreProcessCallback(preProcessEN);

        fontArial16EN = new FontInfo("Arial16.bin", 0x20, 0x7E, 0, 16, 2, 32);
        fontArial16EN.setFontPreProcessCallback(preProcessArial);

        try {
            font12x12CN.file = new RandomAccessFile(
                    filesDir + "/font/" + font12x12CN.fileName, "r");
            font8x16EN.file = new RandomAccessFile(
                    filesDir + "/font/" + font8x16EN.fileName, "r");
            fontArial16EN.file = new RandomAccessFile(
                    filesDir + "/font/" + fontArial16EN.fileName, "r");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void generateToday(TodayWeather today, AirCondition airCondition, RecentWeather recent, CalendarInfo calendar) {
        int xpos, index;
        byte[] byteArray;
        Arrays.fill(innerBuffer, Color.WHITE);
        String tempStr;
        if(recent.cityName.length() > 4) {
            tempStr = recent.cityName.substring(0, 4);
        }else {
            tempStr = recent.cityName;
        }
        try {
            byteArray = tempStr.getBytes("GB2312");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return;
        }
        xpos = drawString(byteArray, 0, 0, font12x12CN, font8x16EN);

        try {
            byteArray = calendar.getCalendar().getBytes("GB2312");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return;
        }
        xpos = drawString(byteArray, xpos + 6, 0, font12x12CN, font8x16EN);

        try {
            byteArray = calendar.getWeek().getBytes("GB2312");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return;
        }
        xpos = drawString(byteArray, xpos + 6, 0, font12x12CN, font8x16EN);

        tempStr = String.format(Locale.CHINA, "%d：", calendar.getHour());
        try {
            byteArray = tempStr.getBytes("GB2312");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return;
        }
        xpos = drawString(byteArray, xpos + 6, 0, font12x12CN, font8x16EN);
        tempStr = String.format(Locale.CHINA, "%02d", calendar.getMinutes());
        try {
            byteArray = tempStr.getBytes("GB2312");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return;
        }
        drawString(byteArray, xpos - 6, 0, font12x12CN, font8x16EN);

        final int[] DIGIT_XPOS = new int[]{96, 131, 180, 215};
        final String[] DIGIT_FILE = new String[]{"digit0.bmp", "digit1.bmp", "digit2.bmp","digit3.bmp",
                "digit4.bmp","digit5.bmp", "digit6.bmp", "digit7.bmp","digit8.bmp", "digit9.bmp"};
        int month = calendar.getMonth();
        int day = calendar.getDay();
        int[] digits = new int[4];
        digits[0] = (month / 10); digits[1] = (month % 10);
        digits[2] = (day / 10); digits[3] = (day % 10);
        for(int i = 0; i < 4; i++) {
            drawBitmap(DIGIT_XPOS[i], 18, "font", DIGIT_FILE[digits[i]]);
        }
        drawBitmap(166, 18, "icon", "split.bmp");

        final String[] DAY_TAG = new String[]{"今天：", "明天：", "后天："};
        final int[] DAY_YPOS = new int[]{84, 97, 110};
        String[] subString = new String[3];

        index = recent.todayDesc.indexOf('：') + 1;
        subString[0] = recent.todayDesc.substring(index).replace(" ", "");
        index = recent.tomorrowDesc.indexOf('：') + 1;
        subString[1] = recent.tomorrowDesc.substring(index).replace(" ", "");
        index = recent.afterTomorrowDesc.indexOf('：') + 1;
        subString[2] = recent.afterTomorrowDesc.substring(index).replace(" ", "");

        for(int i = 0; i < 3; i++) {
            try {
                byteArray = DAY_TAG[i].getBytes("GB2312");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                break;
            }
            xpos = drawString(byteArray, 96, DAY_YPOS[i], font12x12CN, font8x16EN);
            try {
                byteArray = subString[i].getBytes("GB2312");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                break;
            }
            drawString(byteArray, xpos - 6, DAY_YPOS[i], font12x12CN, font8x16EN);
        }

        index = getWeatherId(subString[0]);
        tempStr = getWeatherIcon(index);

        drawBitmap(24, 22, "icon", tempStr + ".bmp");

        tempStr = String.format(Locale.CHINA, "%d'C", today.temperature);
        byteArray = tempStr.getBytes(StandardCharsets.UTF_8);
        drawFloatString(byteArray, 2, 16);

        tempStr = "紫外线：";
        try {
            byteArray = tempStr.getBytes("GB2312");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return;
        }
        xpos = drawString(byteArray, 12, 71, font12x12CN, font8x16EN);
        try {
            byteArray = today.uvDesc.getBytes("GB2312");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return;
        }
        drawString(byteArray, xpos - 6, 71, font12x12CN, font8x16EN);

        tempStr = "PM2:5：";
        try {
            byteArray = tempStr.getBytes("GB2312");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return;
        }
        xpos = drawString(byteArray, 10, 84, font12x12CN, font8x16EN);
        tempStr = String.format(Locale.CHINA, "%d", airCondition.pm25);
        try {
            byteArray = tempStr.getBytes("GB2312");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return;
        }
        drawString(byteArray, xpos - 4, 84, font12x12CN, font8x16EN);

        tempStr = "相对湿度：";
        try {
            byteArray = tempStr.getBytes("GB2312");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return;
        }
        xpos = drawString(byteArray, 2, 97, font12x12CN, font8x16EN);
        try {
            byteArray = today.humidityDesc.getBytes("GB2312");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return;
        }
        drawString(byteArray, xpos - 6, 97, font12x12CN, font8x16EN);

        tempStr = "空气质量：";
        try {
            byteArray = tempStr.getBytes("GB2312");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return;
        }
        xpos = drawString(byteArray, 6, 110, font12x12CN, font8x16EN);
        try {
            byteArray = airCondition.condition.getBytes("GB2312");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return;
        }
        drawString(byteArray, xpos - 6, 110, font12x12CN, font8x16EN);
    }

    public void generateForecast(TodayWeather today, List<ForecastWeather> forecastList, CalendarInfo calendar) {
        int xpos;
        byte[] byteArray;

        Arrays.fill(innerBuffer, Color.WHITE);
        String tempStr;

        if(today.cityName.length() > 4) {
            tempStr = today.cityName.substring(0, 4);
        }else {
            tempStr = today.cityName;
        }
        try {
            byteArray = tempStr.getBytes("GB2312");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return;
        }
        xpos = drawString(byteArray, 0, 0, font12x12CN, font8x16EN);

        try {
            byteArray = calendar.getCalendar().getBytes("GB2312");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return;
        }
        xpos = drawString(byteArray, xpos + 6, 0, font12x12CN, font8x16EN);

        try {
            byteArray = calendar.getWeek().getBytes("GB2312");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return;
        }
        xpos = drawString(byteArray, xpos + 6, 0, font12x12CN, font8x16EN);

        tempStr = String.format(Locale.CHINA, "%d：", calendar.getHour());
        try {
            byteArray = tempStr.getBytes("GB2312");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return;
        }
        xpos = drawString(byteArray, xpos + 6, 0, font12x12CN, font8x16EN);
        tempStr = String.format(Locale.CHINA, "%02d", calendar.getMinutes());
        try {
            byteArray = tempStr.getBytes("GB2312");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return;
        }
        drawString(byteArray, xpos - 6, 0, font12x12CN, font8x16EN);

        final int[] drawIconX = new int[]{0, 125, 0, 125};
        final int[] drawIconY =  new int[]{13, 13, 62, 62};

        ForecastWeather forecast;
        int imageId;

        for(int n = 0; n < ForecastWeather.FORECAST_MAX; n++) {
            forecast = forecastList.get(n);

            imageId = getWeatherId(forecast.getWeatherDesc());
            tempStr = getWeatherIcon(imageId);
            drawBitmap(drawIconX[n], drawIconY[n], "icon", tempStr + ".bmp");

            try {
                byteArray = forecast.getDayDesc().getBytes("GB2312");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                break;
            }
            drawString(byteArray, (drawIconX[n] + 48), drawIconY[n], font12x12CN, font8x16EN);

            try {
                byteArray = forecast.getWeatherDesc().getBytes("GB2312");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                break;
            }
            drawString(byteArray, (drawIconX[n] + 48), (drawIconY[n] + 12), font12x12CN, font8x16EN);

            try {
                byteArray = forecast.getWindDesc().getBytes("GB2312");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                break;
            }
            drawString(byteArray, (drawIconX[n] + 48), (drawIconY[n] + 24), font12x12CN, font8x16EN);

            tempStr = String.format(Locale.CHINA, "%d～%d℃",
                    forecast.getLowestTemp(),
                    forecast.getHighestTemp());
            try {
                byteArray = tempStr.getBytes("GB2312");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                break;
            }
            drawString(byteArray, (drawIconX[n] + 48), (drawIconY[n] + 36), font12x12CN, font8x16EN);
        }

        try {
            byteArray = calendar.getLunarYear().getBytes("GB2312");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return;
        }
        xpos = drawString(byteArray, 0, 110, font12x12CN, font8x16EN);

        try {
            byteArray = calendar.getLunarMonth().getBytes("GB2312");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return;
        }
        xpos = drawString(byteArray, xpos + 6, 110, font12x12CN, font8x16EN);

        tempStr = "湿度：";
        try {
            byteArray = tempStr.getBytes("GB2312");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return;
        }
        xpos = drawString(byteArray, xpos + 6, 110, font12x12CN, font8x16EN);
        try {
            byteArray = today.humidityDesc.getBytes("GB2312");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return;
        }
        xpos = drawString(byteArray, xpos - 6, 110, font12x12CN, font8x16EN);

        tempStr = "阳光：";
        try {
            byteArray = tempStr.getBytes("GB2312");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return;
        }
        xpos = drawString(byteArray, xpos + 6, 110, font12x12CN, font8x16EN);
        try {
            byteArray = today.uvDesc.getBytes("GB2312");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return;
        }
        drawString(byteArray, xpos - 6, 110, font12x12CN, font8x16EN);
    }


    public int[] getInnerBuffer() {
        return innerBuffer;
    }

    private void drawPoint(int x, int y, int color) {
        innerBuffer[y * EPD_WIDTH + x] = color;
    }

    private void drawBitmap(int x, int y, String folder, String fileName) {
        int temp, flag;
        int i, j;
        int drawX, drawY = y;
        int width, height, byteIndex, dataOffset;
        final int[] COLOR_LUT = new int[]{0xFF000000, 0xFFFFFFFF};
        File file = new File(filesDir + "/" + folder + "/" + fileName);
        if(!file.exists()) {
            return;
        }
        byte[] bmpRaw = new byte[(int)file.length()];

        try {
            InputStream ips = new FileInputStream(file);
            temp = ips.read(bmpRaw);
            ips.close();
        } catch (IOException e) {
            e.printStackTrace();
            temp = -1;
        }
        if(temp == -1) {
            return;
        }

        temp = AppUtils.bytes2Int(bmpRaw, BFTYPE, BFTYPE_LENGTH);
        if(BITMAP_DIB != temp) {
            return;
        }
        temp = AppUtils.bytes2Int(bmpRaw, BIBITCOUNT, BIBITCOUNT_LENGTH);
        if(MONOCHROME != temp) {
            return;
        }
        temp = AppUtils.bytes2Int(bmpRaw, BICOMPRESSION, BICOMPRESSION_LENGTH);
        if(BI_RGB != temp) {
            return;
        }
        width = AppUtils.bytes2Int(bmpRaw, BIWIDTH, BIWIDTH_LENGTH);
        height = AppUtils.bytes2Int(bmpRaw, BIHEIGHT, BIHEIGHT_LENGTH);
        dataOffset = AppUtils.bytes2Int(bmpRaw, BFOFFBITS, BFOFFBITS_LENGTH);
        if(width > EPD_WIDTH || height > EPD_HEIGHT) {
            return;
        }
        i = 32;
        temp = (width / i);
        temp = ((temp * 32) < width) ? (temp + 1) : temp;
        temp <<= 2;
        for(i = (height - 1); i >= 0; i--) {
            drawX = x;
            System.arraycopy(bmpRaw, (dataOffset + (temp * i )), bitmapBuffer, 0, temp);
            for(j = 0; j < width; j++) {
                byteIndex = (j / 8);
                flag = ((bitmapBuffer[byteIndex] >> (7 - (j - byteIndex * 8))) & 0x1);
                drawPoint(drawX++, drawY, COLOR_LUT[flag]);
            }
            drawY++;
        }
    }

    private int drawString(byte[] str, int x, int y, FontInfo fontCN, FontInfo fontEN) {
        int ch, offset = 0;
        int xBackup = x;
        int increaseWidth;
        FontInfo fontRef;

        do {
            ch = (str[offset] & 0xFF);
            if(ch > 0x7F) {
                ch = (str[offset + 1] & 0xFF);
                ch <<= 8;
                ch |= (str[offset] & 0xFF);
                fontRef = fontCN;
                increaseWidth = fontCN.width;
                offset += 2;
            }else {
                fontRef = fontEN;
                increaseWidth = fontEN.width;
                offset ++;
            }
            if(x > (EPD_WIDTH - increaseWidth)) {
                if(xBackup > (EPD_WIDTH - increaseWidth)) {
                    break;
                }
                x = xBackup;
                y += fontCN.height;
            }
            if(y > (EPD_HEIGHT - fontCN.height)) {
                break;
            }
            drawChar(ch, x, y, fontCN.height, fontRef);
            x += increaseWidth;

        }while(offset < str.length);

        return x;
    }

    private void drawChar(int ch, int x, int y, int height, FontInfo font) {
        final int[] COLOR_LUT = new int[]{0xFFFFFFFF, 0xFF000000};
        if((ch < font.startChar) || (ch > font.endChar)) {
            return;
        }
        if((font.lineBytes > 4) || (font.fontBytes > BITMAP_BUFFER_MAX)) {
            return;
        }
        int index = font.preProcess.calcIndex(ch);
        try {
            font.file.seek(index);
            font.file.read(bitmapBuffer, 0, font.fontBytes);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        int bitmapLine, flag;
        int split = (font.lineBytes * 8);
        for(int j = 0; j < height; j++) {
            bitmapLine = font.preProcess.endianSwap(bitmapBuffer, j, font.lineBytes);
            for(int i = 0; i < font.width; i++) {
                flag = ((bitmapLine >>> (split - i - 1)) & 0x1);
                drawPoint(x + i, y + j, COLOR_LUT[flag]);
            }
        }
    }

    private void drawFloatString(byte[] str, int x, int y) {
        int ch, width, xStart = x;
        for(int i = 0; i < str.length; i++) {
            ch = (str[i] & 0xFF);
            width = drawFloatChar(ch, xStart, y, 16, fontArial16EN);
            if((ch >= 0x30) && (ch <= 0x39)) {
                xStart += (width + 1);
            }else {
                xStart += width;
            }
        }
    }

    private int drawFloatChar(int ch, int x, int y, int height, FontInfo font) {
        if((ch < font.startChar) || (ch > font.endChar)) {
            return 0;
        }
        if((font.lineBytes > 4) || (font.fontBytes > BITMAP_BUFFER_MAX) || (font.height != 16)) {
            return 0;
        }
        int index = font.preProcess.calcIndex(ch) + 2;
        try {
            font.file.seek(index);
            font.file.read(bitmapBuffer, 0, font.fontBytes);
        } catch (IOException e) {
            e.printStackTrace();
            return 0;
        }

        int mask = 0x0;
        int low, high, width = 0;
        int[] bitmapLine = new int[16];

        for(int i = 0, j = 0; i < 32; i += 2, j++) {
            high = (bitmapBuffer[i] & 0xFF);
            low = (bitmapBuffer[i + 1] & 0xFF);
            bitmapLine[j] = (high << 8) | low;
            mask |= bitmapLine[j];
        }
        for(int i = 0; i < 16; i++) {
            if(((mask >> i) & 0x1) == 1) {
                width = (16 - i);
                break;
            }
        }
        int split = (font.lineBytes * 8);
        for(int j = 0; j < height; j++) {
            for(int i = 0; i < width; i++) {
                if(((bitmapLine[j] >>> (split - i - 1)) & 0x1) == 0x1) {
                    drawPoint(x + i, y + j, Color.BLACK);
                }
            }
        }
        return width;
    }

    private final FontPreProcessCallback preProcessCN = new FontPreProcessCallback() {
        @Override
        public int calcIndex(int code) {
            int LSB = (code >> 8) & 0xFF;
            int MSB  = (code & 0xFF);
            if((MSB >= 0xA1) && (MSB <= 0xA9) && (LSB >= 0xA1)) {
                return ((MSB - 0xA1) * 94 + (LSB - 0xA1)) * 24;
            }else if((MSB >= 0xB0) && (MSB <= 0xF7) && (LSB >= 0xA1)) {
                return ((MSB - 0xB0) * 94 + (LSB - 0xA1) + 846) * 24;
            }else {
                return 0;
            }
        }

        @Override
        public int endianSwap(byte[] bitmap, int height, int lineBytes) {
            int highPart = (bitmap[height * lineBytes] & 0xFF);
            int lowPart = (bitmap[height * lineBytes + 1] & 0xFF);
            return (highPart << 8) | lowPart;
        }
    };

    private final FontPreProcessCallback preProcessEN = new FontPreProcessCallback() {
        @Override
        public int calcIndex(int code) {
            return (code - 0x20) * 16;
        }

        @Override
        public int endianSwap(byte[] bitmap, int height, int lineBytes) {
            return (int)(bitmap[height * lineBytes] & 0xFF);
        }
    };

    private final FontPreProcessCallback preProcessArial = new FontPreProcessCallback() {
        @Override
        public int calcIndex(int code) {
            return (code - 0x20) * 34;
        }

        @Override
        public int endianSwap(byte[] bitmap, int height, int lineBytes) {
            int highPart = (bitmap[height * lineBytes] & 0xFF);
            int lowPart = (bitmap[height * lineBytes + 1] & 0xFF);
            return (highPart << 8) | lowPart;
        }
    };

    private static final String[] WEATHER_ICONS = new String[]{"sunny", "cloud", "yin", "rain", "rain2sun", "thunder",
            "snow", "fog", "wind", "hail","unknown"};

    private static final int[][] PAIRS = new int[][] {
            {0, 0}, {1, 1}, {2, 2}, {3, 4}, {4, 5},
            {6, 3}, {7, 3}, {8, 3}, {9, 3}, {10, 3},
            {14, 6}, {15, 6}, {99, 6}, {19, 9}, {32, 9},
            {20, 7}, {30, 8}
    };

    private String getWeatherIcon(int weatherId) {
        int i = 0;
        final int size = 17;
        for(; i < size; i++) {
            if(PAIRS[i][0] == weatherId) {
                break;
            }
        }
        return ((i < size) ? WEATHER_ICONS[PAIRS[i][1]] : WEATHER_ICONS[10]);
    }

    private static final String PATTERN = "晴雷雨云雪阴雾风";
    private static final int[] MATCHES = new int[]{0, 4, 6, 1, 14, 2, 20, 30};

    private int getWeatherId(String weatherDesc) {
        String keyWord;
        for(int i = 0; i < MATCHES.length; i++) {
            keyWord = PATTERN.substring(i, i + 1);
            if(weatherDesc.contains(keyWord)) {
                return MATCHES[i];
            }
        }
        return 127;
    }

    public void close() {
        innerBuffer = null;
        bitmapBuffer = null;
        try {
            font12x12CN.file.close();
            font12x12CN.file = null;
            font8x16EN.file.close();
            font8x16EN.file = null;
            fontArial16EN.file.close();
            fontArial16EN.file = null;
        } catch (IOException e) {
            e.printStackTrace();
        }
        font12x12CN = null;
        font8x16EN = null;
        fontArial16EN = null;
    }

}
