package com.demo.epaper.entity;

import com.demo.epaper.handler.FontPreProcessCallback;

import java.io.RandomAccessFile;

public class FontInfo {
    public int startChar, endChar;
    public int width, height;
    public int lineBytes;
    public int fontBytes;
    public String fileName;
    public RandomAccessFile file;
    public FontPreProcessCallback preProcess;

    public FontInfo(String name, int a, int b, int c, int d, int e, int f) {
        this.fileName = name;
        this.startChar = a;
        this.endChar = b;
        this.width = c;
        this.height = d;
        this.lineBytes = e;
        this.fontBytes = f;
    }

    public void setFontPreProcessCallback(FontPreProcessCallback cbk) {
        this.preProcess = cbk;
    }
}
