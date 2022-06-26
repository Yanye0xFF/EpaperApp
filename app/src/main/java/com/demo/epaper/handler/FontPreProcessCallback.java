package com.demo.epaper.handler;

public interface FontPreProcessCallback {
    int calcIndex(int code);
    int endianSwap(byte[] bitmap, int height, int lineBytes);
}
